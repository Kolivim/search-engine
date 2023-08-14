package searchengine.services;

import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.Page;
import searchengine.model.Site;
import searchengine.model.StatusType;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;

import javax.persistence.LockModeType;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.RecursiveAction;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Slf4j
@Service
@NoArgsConstructor
@EnableTransactionManagement
public class PageWriter extends RecursiveAction {
    @Autowired
    private PageRepository pageRepository;
    @Autowired
    private SiteRepository siteRepository;
    private IndexingService indexingService;
    private LemmatizationService lemmatizationService;
    private Page page;
    private Site site;
    private String linkAbs = "";
    private volatile boolean isIndexingSiteStarted;
    private ReadWriteLock lock = new ReentrantReadWriteLock();
    private boolean isForcedPageIndexing = false;

    public static final String USER_AGENT1 = "Mozilla/5.0 (compatible; MJ12bot/v1.4.5; " +
            "http://www.majestic12.co.uk/bot.php?+)";
    public static final String USER_AGENT2 = "Microsoft Edge (Win 10 x64): " +
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) " +
            "Chrome/46.0.2486.0 Safari/537.36 Edge/13.10586";
    public static final String USER_AGENT3 = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
            "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/73.0.3683.86 Safari/537.36";
    public static final String USER_AGENT4 = "Mozilla/5.0 (Windows NT 6.1; Win64; x64; rv:25.0) " +
            "Gecko/20100101 Firefox/25.0";
    public static final String USER_AGENT5 = "Mozilla/5.0 (Macintosh; Intel Mac OS X 13.4; rv:109.0) " +
            "Gecko/20100101 Firefox/114.0";
    public String userAgent = USER_AGENT5;

    public void setSite(Site site) {
        this.site = site;
    }

    public PageWriter(Site site) throws InterruptedException {
        this.site = site;
        pageRepository = (PageRepository) SpringUtils.ctx.getBean(PageRepository.class);
        siteRepository = (SiteRepository) SpringUtils.ctx.getBean(SiteRepository.class);
        indexingService = (IndexingService) SpringUtils.ctx.getBean(IndexingServiceImpl.class);
        lemmatizationService = (LemmatizationService) SpringUtils.ctx.getBean(LemmatizationService.class);

        Page pageValues = new Page();
        pageValues.setPath("/");
        pageValues.setSite(site);
        pageValues.setSiteId(site.getId());

        try {
            setCodeAndContentToPage(site.getUrl(), pageValues);
        } catch (HttpStatusException e) {
            log.info("В PageWriter() - Для переданного сайта: {} - HttpStatusException парсинга " +
                    "главной страницы сайта = {}", site.getUrl(), e.getMessage());
            if (e.getStatusCode() == 503) {
                Thread.sleep(getRandomTimeout());
                userAgent = getRandomUserAgent();
                new PageWriter(site);
                log.info("В PageWriter() - Для переданного сайта: {} - HttpStatusException парсинга -" +
                        "перезапуск парсинга главной страницы, UA ={}", site.getUrl(), userAgent);
            } else {
                setSiteError("Ошибка индексации: главная страница сайта не доступна");
            }

        } catch (IOException e) {
            log.info("В PageWriter() - Для переданного сайта: {} - IOException парсинга " +
                    "главной страницы сайта = {}", site.getUrl(), e.getMessage());
            setSiteError("Ошибка индексации: главная страница сайта не доступна");
        }

        pageRepository.save(pageValues);
        this.page = pageValues;
        this.linkAbs = site.getUrl();

        lemmatizationService.indexNewPage(page);
    }

    public int getRandomTimeout() {
        int randomTimeout = (int) ((Math.random() * ((5000 - 1000) + 1000)));
        return randomTimeout;
    }

    public void setCodeAndContentToPage(String path, Page pageValues) throws IOException {
        Connection.Response jsoupResponsePage = Jsoup.connect(path)
                .userAgent(userAgent)
                .referrer("http://www.google.com")
                .execute()
                .bufferUp();
        pageValues.setContent(jsoupResponsePage.parse().html());
        pageValues.setCode(jsoupResponsePage.statusCode());
    }

    public PageWriter(Page pageValues, String linkAU) {
        this.page = pageValues;
        linkAbs = linkAU;
        this.site = page.getSite();
        pageRepository = (PageRepository) SpringUtils.ctx.getBean(PageRepository.class);
        siteRepository = (SiteRepository) SpringUtils.ctx.getBean(SiteRepository.class);
        indexingService = (IndexingService) SpringUtils.ctx.getBean(IndexingServiceImpl.class);
        lemmatizationService = (LemmatizationService) SpringUtils.ctx.getBean(LemmatizationService.class);
    }

    public Page removedOrAddPage(String pagePath, String linkAU, Site site) {
        pageRepository = (PageRepository) SpringUtils.ctx.getBean(PageRepository.class);
        siteRepository = (SiteRepository) SpringUtils.ctx.getBean(SiteRepository.class);
        indexingService = (IndexingService) SpringUtils.ctx.getBean(IndexingServiceImpl.class);
        lemmatizationService = (LemmatizationService) SpringUtils.ctx.getBean(LemmatizationService.class);
        this.site = site;
        this.isForcedPageIndexing = true;

        if (pageRepository.existsByPathAndSite(pagePath, site)) {
            Page deletePage = pageRepository.findByPathAndSite(pagePath, site);
            pageRepository.delete(deletePage);
        }

        Page removedPage = addPage(pagePath, linkAU);
        log.info("В методе removedOrAddPage() - Для переданного пути: {} - завершен корректно, pageId = {}",
                linkAU, removedPage.getId());
        isForcedPageIndexing = false;
        return removedPage;
    }

    public boolean isLink(String valueUrl) {
        boolean isLink = true;
        if (valueUrl.contains(".pdf") || valueUrl.contains(".PNG") || valueUrl.contains("#")) {
            isLink = false;
        }
        return isLink;
    }

    public Page getIncorrectPage() {
        Page result = new Page();
        result.setPath("Ошибка - Не добавлять страницу");
        result.setSiteId(-1);
        result.setContent("Ошибка - Не добавлять страницу");
        result.setCode(-1);

        return result;
    }

    public Page getCorrectPage(String link, String linkAU) {
        Page pageValues = new Page();
        pageValues.setPath(link);
        pageValues.setSite(site);
        pageValues.setSiteId(site.getId());

        try {
            setCodeAndContentToPage(linkAU, pageValues);
        } catch (HttpStatusException e) {
            if (e.getStatusCode() != 404 && e.getStatusCode() != 403) {
                try {
                    Thread.sleep(getRandomTimeout());
                } catch (InterruptedException ex) {
                    log.error("Ex Int getCorrectPage()");
                }
                userAgent = getRandomUserAgent();
                addPage(link, linkAU);
                log.info("В getCorrectPage() - Для переданного пути: {} - HttpStatusException парсинга - " +
                        "перезапуск парсинга указанной страницы, UA ={}", linkAU, userAgent);
            } else {
                log.info("В getCorrectPage() - Для переданного пути: {} - HttpStatusException парсинга - " +
                        "страница не доступна, e = {}", linkAU, e.getMessage());
            }

        } catch (IOException e) {
            log.info("В getCorrectPage() - Для переданного пути: {} - IOException парсинга " +
                    "перезапуск парсинга указанной страницы", linkAU);
            addPage(link, linkAU);
        }

        return pageValues;
    }

    @Lock(value = LockModeType.OPTIMISTIC_FORCE_INCREMENT)
    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.SERIALIZABLE)
    public Page addPage(String link, String linkAU) {
        Page result = getIncorrectPage();
        Page pageValues = getCorrectPage(link, linkAU);

        isIndexingSiteStarted = indexingService.getIndexingStarted();
        if (isForcedPageIndexing) {
            isIndexingSiteStarted = isForcedPageIndexing;
        }

        if (!pageRepository.existsByPathAndSite(link, site) /*& isIndexingSiteStarted*/) {
            if (Thread.currentThread().isInterrupted() || !isIndexingSiteStarted) {
                setSiteError("Индексация остановлена пользователем");
            } else {
                pageRepository.save(pageValues);
                result = pageValues;
                lemmatizationService.indexNewPage(result);
                log.info("В методе addPage() - добавлена страница: {}", linkAbs);
            }
        }

        return result;
    }

    public void setSiteError(String error) {
        site.setStatus(StatusType.FAILED);
        site.setLastError(error);
        siteRepository.save(site);
        log.info("В методе setSiteError() - для переданного пути: {} - выполнено изменение " +
                        "статуса сайта {}, на {}, значение переменных isIndexingSiteStarted = {}, " +
                        "Thread.currentThread().isInterrupted() = {}", linkAbs, site.getUrl(), site.getStatus(),
                isIndexingSiteStarted, Thread.currentThread().isInterrupted());
    }

    public Document getDocumentPage(String linkAbsPage) throws InterruptedException, IOException {
        Thread.sleep(1500);

        Document pageLinkDocument = Jsoup.connect(linkAbsPage)
                .userAgent(userAgent)
                .referrer("http://www.google.com")
                .timeout(10 * 1000)
                .get();

        log.info("В методе Compute()->getDocumentPage() - получен Document для переданного пути: {}" +
                " , сайта [ {} ]", linkAbsPage, site.getUrl());

        return pageLinkDocument;
    }

    public String getLink(String link) {
        String linkSite = site.getUrl();
        String linkSite2 = site.getUrl().replaceFirst("www.", "");
        if (link.contains(linkSite)) {
            link = link.replaceFirst(linkSite, "");
        }
        if (link.contains(linkSite2)) {
            link = link.replaceFirst(linkSite2, "");

        }
        return link;
    }

    public boolean isPageToAdd(String linkAU, String link) {
        boolean isAddPage = false;

        String requestedPage = linkAbs;
        boolean isChildren = isChildren(linkAU, requestedPage);
        boolean isNotFindPage = !pageRepository.existsByPathAndSite(link, site);

        if (isIndexingSiteStarted & isNotFindPage & isLink(linkAU) & isChildren
                & !Thread.currentThread().isInterrupted()) {
            isAddPage = true;
        }

        log.info("В методе Compute()->isAddPage() - получен boolean (isAddPage) = {}, для переданного пути: {}",
                isAddPage, linkAU);

        return isAddPage;
    }

    public void parseLink(Element valueLink, List<PageWriter> pageWriterList) {

        String linkAU = valueLink.absUrl("href");
        String link = valueLink.attr("href");

        link = getLink(link);

        lock.readLock().lock();

        if (isPageToAdd(linkAU, link)) {
            Page pageValues = addPage(link, linkAU);
            if (pageValues.getId() != -1) {
                site.setStatusTime(new Date());
                siteRepository.save(site);
                PageWriter pageWriter = new PageWriter(pageValues, linkAU);
                pageWriter.fork();
                pageWriterList.add(pageWriter);
            }
        } else {
            log.info("\nВ методе compute()->parseLink() - получена ошибка при добавлении страницы: {}", linkAU);
        }
        lock.readLock().unlock();
    }

    public void parseAllLinks(Document pageLink, List<PageWriter> pageWriterList) throws IOException {
        Elements fullLinks = pageLink.select("a[href]");
        log.info("\nВ методе compute()->parseAllLinks - для Document: {} - получены адреса страниц(формат Element)",
                pageLink.title());
        for (Element valueLink : fullLinks) {
            parseLink(valueLink, pageWriterList);
        }
    }

    @Override
    protected void compute() {

        isIndexingSiteStarted = indexingService.getIndexingStarted();

        if (isIndexingSiteStarted & !Thread.currentThread().isInterrupted()) {

            List<PageWriter> pageWriterList = new ArrayList<>();
            try {
                Document pageLink = null;
                try {
                    pageLink = getDocumentPage(linkAbs);
                    if (pageLink == null) {
                        throw new IOException();
                    }
                } catch (IOException e) {
                    userAgent = getRandomUserAgent();
                    getDocumentPage(linkAbs);
                    log.error("В методе compute() при вызове getDocumentPage() - сработал IOException: {}, path: {}, " +
                            "перезапуск getDocumentPage()", e, linkAbs);
                }

                parseAllLinks(pageLink, pageWriterList);

                if (Thread.currentThread().isInterrupted()) {
                    pageWriterList.clear();
                }

                for (PageWriter pageWriter : pageWriterList) {
                    pageWriter.join();
                }

            } catch (InterruptedException e) {
                log.error("В методе compute() - сработал InterruptedException: {}, path: {}", e, linkAbs);
                setSiteError("Индексация остановлена пользователем");
                Thread.currentThread().interrupt();
            } catch (IllegalArgumentException e) {
                log.error("В методе compute() - сработал IllegalArgumentException: {}, path: {}", e, linkAbs);
            } catch (Exception e) {
                log.error("В методе compute() - сработал Exception: {}, path: {}", e, linkAbs);
            }
        } else {
            setSiteError("Индексация остановлена пользователем");
            Thread.currentThread().interrupt();
        }
    }

    public boolean isChildren(String valueUrl, String parentPage) {
        boolean isChildren = false;

        if (valueUrl.contains(parentPage) && !valueUrl.equals(parentPage)) {
            isChildren = true;
        }
        if (parentPage.contains("www.")) {
            parentPage = parentPage.replaceFirst("www.", "");
        }
        if (valueUrl.contains(parentPage) && !valueUrl.equals(parentPage)) {
            isChildren = true;
        }
        if (valueUrl.contains("www.")) {
            valueUrl = valueUrl.replaceFirst("www.", "");
        }

        if (valueUrl.contains(parentPage) && !valueUrl.equals(parentPage)) {
            isChildren = true;
        }
        if (valueUrl.contains(parentPage)) {
            valueUrl = valueUrl.replaceFirst(parentPage, "");
        }
        if (valueUrl.equals("")) {
            isChildren = false;
        }
        if (valueUrl.startsWith("http://")) {
            valueUrl = valueUrl.replaceFirst("http://", "");
        }
        if (valueUrl.startsWith("https://")) {
            valueUrl = valueUrl.replaceFirst("https://", "");
        }
        if (parentPage.startsWith("http://")) {
            parentPage = parentPage.replaceFirst("http://", "");
        }
        if (parentPage.startsWith("https://")) {
            parentPage = parentPage.replaceFirst("https://", "");
        }
        if (valueUrl.contains(parentPage) && !valueUrl.equals(parentPage)) {
            isChildren = true;
        }
        if (valueUrl.contains("/") && valueUrl.length() == 1) {
            isChildren = false;
        }
        return isChildren;
    }

    public String getRandomUserAgent() {
        ArrayList<String> userAgentList = new ArrayList<>();
        userAgentList.add(USER_AGENT1);
        userAgentList.add(USER_AGENT2);
        userAgentList.add(USER_AGENT3);
        userAgentList.add(USER_AGENT4);
        userAgentList.add(USER_AGENT5);
        String userAgent = "";
        int index = (int) ((Math.random() * ((5 - 1) + 1)) - 1);
        userAgent = userAgentList.get(index);
        return userAgent;
    }

    @Override
    public String toString() {
        return "PageWriter{" +
                "page=" + page +
                ", site=" + site +
                '}';
    }
}
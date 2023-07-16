package searchengine.services;

import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
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
    IndexingService indexingService;
    private LemmatizationService lemmatizationService;
    private Page page;
    private Site site;
    String linkAbs = "";
    private volatile boolean indexingStarted;
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
    public static final String USER_AGENT5 = "Microsoft Edge (Win 10 x64): Mozilla/5.0 " +
            "(Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) " +
            "Chrome/46.0.2486.0 Safari/537.36 Edge/13.10586";
    public static final String USER_AGENT = USER_AGENT5;

    public void setSite(Site site) {
        this.site = site;
    }

    public PageWriter(Site site) throws IOException {
        this.site = site;
        this.indexingStarted = true;
        pageRepository = (PageRepository) SpringUtils.ctx.getBean(PageRepository.class);
        siteRepository = (SiteRepository) SpringUtils.ctx.getBean(SiteRepository.class);
        indexingService = (IndexingService) SpringUtils.ctx.getBean(IndexingServiceImpl.class);
        lemmatizationService = (LemmatizationService) SpringUtils.ctx.getBean(LemmatizationService.class);

        Page pageValues = new Page();
        pageValues.setPath("/");
        pageValues.setSite(site);
        pageValues.setSiteId(site.getId());
        Connection.Response jsoupResponsePage = Jsoup.connect(site.getUrl())
                .userAgent(USER_AGENT)
                .referrer("http://www.google.com")
                .execute();
        pageValues.setCode(jsoupResponsePage.statusCode());
        pageValues.setContent(jsoupResponsePage.parse().html());
        pageRepository.save(pageValues);
        this.page = pageValues;
        this.linkAbs = site.getUrl();

        lemmatizationService.indexNewPage(page);
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

        try {
            Page removedPage = addPage(pagePath, linkAU);
            log.info("В методе removedOrAddPage() - Для переданного пути: {} - завершен корректно, pageId = {}",
                    linkAU, removedPage.getId());
            isForcedPageIndexing = false;
            return removedPage;
        } catch (IOException e) {
            log.error("В методе removedOrAddPage() - сработал IOException(e): {}, {}, на переданной странице: {}, " +
                    "для сайта: {}", e.getMessage(), e.getStackTrace(), linkAU, site.getUrl());
            return new Page();
        }
    }

    public boolean isLink(String valueUrl) {
        boolean isLink = true;
        if (valueUrl.contains(".pdf") || valueUrl.contains(".PNG") || valueUrl.contains("#")) {
            isLink = false;
        }
        return isLink;
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

    public Page getIncorrectPage() {
        Page result = new Page();
        result.setPath("Ошибка - Не добавлять страницу");
        result.setSiteId(-1);
        result.setContent("Ошибка - Не добавлять страницу");
        result.setCode(-1);

        return result;
    }

    public Page getCorrectPage(String link, String linkAU) throws IOException {
        Page pageValues = new Page();
        pageValues.setPath(link);
        pageValues.setSite(site);
        pageValues.setSiteId(site.getId());
        Connection.Response jsoupResponsePage = Jsoup.connect(linkAU).execute();
        pageValues.setCode(jsoupResponsePage.statusCode());
        pageValues.setContent(jsoupResponsePage.parse().html());

        return pageValues;
    }

    @Lock(value = LockModeType.OPTIMISTIC_FORCE_INCREMENT)
    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.SERIALIZABLE)
    public Page addPage(String link, String linkAU) throws IOException {
        Page result = getIncorrectPage();
        Page pageValues = getCorrectPage(link, linkAU);

        isIndexingSiteStarted = indexingService.getIndexingStarted();
        if (isForcedPageIndexing) {
            isIndexingSiteStarted = isForcedPageIndexing;
        }

        if (!pageRepository.existsByPathAndSite(link, site) & isIndexingSiteStarted) {
            if (Thread.currentThread().isInterrupted() || !isIndexingSiteStarted) {
                try {
                    throw new InterruptedException();
                } catch (InterruptedException e) {
                    site.setStatus(StatusType.FAILED);
                    site.setLastError("Индексация остановлена пользователем");
                    siteRepository.save(site);
                    log.error("В методе addPage() - для переданного пути: {} - остановлена индексация", page.getPath());
                }
            } else {
                pageRepository.save(pageValues);
                result = pageValues;
                lemmatizationService.indexNewPage(result);
                log.info("В методе addPage() - добавлена страница: {}", page.getPath());
            }
        }

        log.info("В методе Compute()->addPage() - получена к возврату страница с некорректными данными, " +
                "для остановки индексации страницы с путем = {}", linkAU);

        return result;
    }

    public boolean isNotFindPageRead(String linkR, Site siteR) {
        return !pageRepository.existsByPathAndSite(linkR, siteR);
    }

    public void setSiteError(String error) {
        site.setStatus(StatusType.FAILED);
        site.setLastError(error);
        siteRepository.save(site);
        log.info("В методе setSiteError()/Compute() - для переданного пути: {} - выполнено изменение " +
                        "статуса сайта {}, на {}, значение переменных isIndexingSiteStarted = {}, " +
                        "Thread.currentThread().isInterrupted() = {}", page.getPath(), site.getUrl(), site.getStatus(),
                isIndexingSiteStarted, Thread.currentThread().isInterrupted());
    }

    public Document getDocumentPage(String linkAbsPage) throws InterruptedException {

        Thread.sleep(1500);

        try {
            Document pageLinkDocument = Jsoup.connect(linkAbsPage)
                    .userAgent(USER_AGENT)
                    .referrer("http://www.google.com")
                    .get();

            log.info("В методе Compute()->getDocumentPage() - получен Document для переданного пути: {}" +
                    " , сайта [ {} ]", linkAbsPage, site.getUrl());

            return pageLinkDocument;
        } catch (IOException e) {
            log.error("В методе getDocumentPage() - сработал IOException(e): {}, {}, " +
                    "на переданной странице: {}", e.getMessage(), e.getStackTrace(), linkAbsPage);
            return null;
        }
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

    public boolean isAddPage(String linkAU, String link) {
        boolean isAddPage = false;

        String requestedPage = linkAbs;
        boolean isChildren = isChildren(linkAU, requestedPage);
        boolean isNotFindPage = !pageRepository.existsByPathAndSite(link, site);
        indexingStarted = !pageRepository.existsByPathAndSite(link, site);

        if (isIndexingSiteStarted & indexingStarted & isNotFindPageRead(link, site)
                & isNotFindPage & isLink(linkAU) & isChildren
                & !Thread.currentThread().isInterrupted()) {
            isAddPage = true;
        }

        log.info("В методе Compute()->isAddPage() - получен boolean (isAddPage) = {}, для переданного пути: {} ( {} )",
                isAddPage, linkAU, link);

        return isAddPage;
    }

    public void parseLink(Element valueLink, List<PageWriter> pageWriterList) throws IOException {

        String linkAU = valueLink.absUrl("href");
        String link = valueLink.attr("href");

        link = getLink(link);

        lock.readLock().lock();

        if (isAddPage(linkAU, link)) {
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
        log.info("\nВ методе compute()->parseAllLinks - для Document: {}\nПолучены адреса страниц(формат Element): {}",
                pageLink, fullLinks);
        for (Element valueLink : fullLinks) {
            parseLink(valueLink, pageWriterList);
        }
    }

    @Override
    protected void compute() {
        if (Thread.currentThread().isInterrupted()) {
            try {
                throw new InterruptedException();
            } catch (InterruptedException e) {
                setSiteError("Индексация остановлена пользователем");
            }
        }

        isIndexingSiteStarted = indexingService.getIndexingStarted();

        if (isIndexingSiteStarted & !Thread.currentThread().isInterrupted()) {

            List<PageWriter> pageWriterList = new ArrayList<>();
            try {
                Document pageLink = getDocumentPage(linkAbs);
                if (pageLink == null) {
                    throw new IOException();
                }

                parseAllLinks(pageLink, pageWriterList);

                if (Thread.currentThread().isInterrupted()) {
                    pageWriterList.clear();
                }

                for (PageWriter pageWriter : pageWriterList) {
                    pageWriter.join();
                }

            } catch (InterruptedException e) {
                log.error("В методе compute() - сработал InterruptedException: {}, path: {}", e, page.getPath());
                setSiteError("Индексация остановлена пользователем");
                Thread.currentThread().interrupt();
            } catch (IOException e) {
                log.error("В методе compute() - сработал IOException: {}, path: {}", e, page.getPath());
            } catch (IllegalArgumentException e) {
                log.error("В методе compute() - сработал IllegalArgumentException: {}, path: {}", e, page.getPath());
            } catch (Exception e) {
                log.error("В методе compute() - сработал Exception: {}, path: {}", e, page.getPath());
            }
        } else {
            setSiteError("Индексация остановлена пользователем");
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public String toString() {
        return "PageWriter{" +
                "page=" + page +
                ", site=" + site +
                '}';
    }
}
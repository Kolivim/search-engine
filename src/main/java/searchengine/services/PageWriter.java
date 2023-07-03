package searchengine.services;

import com.fasterxml.jackson.databind.cfg.PackageVersion;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.hibernate.SessionFactory;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.w3c.dom.Node;
import searchengine.model.Page;
import searchengine.model.Site;
import searchengine.model.StatusType;

import javax.persistence.LockModeType;
import javax.print.DocFlavor;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.IOException;
import java.io.StringWriter;
import java.security.Timestamp;
import java.text.Format;
import java.util.*;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;
import java.util.concurrent.RecursiveTask;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Service
@NoArgsConstructor
@EnableTransactionManagement
//@Component
//public class PageWriter extends RecursiveTask<Set<Page>>
//public class PageWriter extends RecursiveTask<Boolean>{
public class PageWriter extends RecursiveAction {

    @Autowired
    private PageRepository pageRepository;
    @Autowired
    private SiteRepository siteRepository;
    IndexingService indexingService;
    private LemmatizationService lemmatizationService;  // L-I

    private Page page;

    private Site site;
    String linkAbs = "";
    private volatile boolean indexingStarted;
    private volatile boolean isIndexingSiteStarted;
    private ReadWriteLock lock = new ReentrantReadWriteLock();  // public ???
    private volatile Page pageFind;
    ReentrantLock isLock = new ReentrantLock();
    private boolean isForcedPageIndexing = false;
    // TODO: Убрать в _.yaml
    public static final String USER_AGENT1  = "Mozilla/5.0 (compatible; MJ12bot/v1.4.5; http://www.majestic12.co.uk/bot.php?+)";
    public static final String USER_AGENT2 = "Microsoft Edge (Win 10 x64): Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/46.0.2486.0 Safari/537.36 Edge/13.10586";
    public static final String USER_AGENT3 = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/73.0.3683.86 Safari/537.36";
    public static final String USER_AGENT4 = "Mozilla/5.0 (Windows NT 6.1; Win64; x64; rv:25.0) Gecko/20100101 Firefox/25.0";
    public static final String USER_AGENT5 = "Microsoft Edge (Win 10 x64): Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/46.0.2486.0 Safari/537.36 Edge/13.10586";
    //
    public static final String USER_AGENT = USER_AGENT3;
    public void setSite(Site site) {this.site = site;}
    public PageWriter(Site site) throws IOException
    {
        this.site = site;
        this.indexingStarted = true;
        pageRepository = (PageRepository) SpringUtils.ctx.getBean(PageRepository.class);
        siteRepository = (SiteRepository) SpringUtils.ctx.getBean(SiteRepository.class);
        indexingService = (IndexingService) SpringUtils.ctx.getBean(IndexingServiceImpl.class);

        lemmatizationService = (LemmatizationService) SpringUtils.ctx.getBean(LemmatizationService.class);  // L-I

        Page pageValues = new Page();
        pageValues.setPath("/");
        pageValues.setSite(site);
        pageValues.setSiteId(site.getId()); // 16 may
        Connection.Response jsoupResponsePage = Jsoup.connect(site.getUrl())
                .userAgent(USER_AGENT)
                .referrer("http://www.google.com")
                .execute();
        pageValues.setCode(jsoupResponsePage.statusCode());
        pageValues.setContent(jsoupResponsePage.parse().html());
        pageRepository.save(pageValues);
        this.page = pageValues;
        this.linkAbs = site.getUrl();

        lemmatizationService.indexNewPage(page);  // L-I
    }

    public PageWriter(Page pageValues, String linkAU)
    {
        this.page = pageValues;
        linkAbs = linkAU;
        this.site = page.getSite(); // Проверить работу в Debug !!!
        pageRepository = (PageRepository) SpringUtils.ctx.getBean(PageRepository.class);
        siteRepository = (SiteRepository) SpringUtils.ctx.getBean(SiteRepository.class);
        indexingService = (IndexingService) SpringUtils.ctx.getBean(IndexingServiceImpl.class);

        lemmatizationService = (LemmatizationService) SpringUtils.ctx.getBean(LemmatizationService.class);  // L-I
    }

    // Заменил на void removeOrAddPage
    public Integer removePage(String pagePath, String linkAU, Site site)   // TODO: Нужна ли проверка на остановку индексации ???
    {
        pageRepository = (PageRepository) SpringUtils.ctx.getBean(PageRepository.class);
        siteRepository = (SiteRepository) SpringUtils.ctx.getBean(SiteRepository.class);
        indexingService = (IndexingService) SpringUtils.ctx.getBean(IndexingServiceImpl.class);

        pageRepository.deleteByPathAndSite(pagePath, site);
        try {
            Page removedPage = addPage(pagePath, linkAU);
            return page.getId();
            } catch (IOException e)
                {
                    System.err.println("В классе PageWriter в методе removePage сработал IOException / RuntimeException(e) ///1 " + e.getMessage() +
                            " ///2 " + e.getStackTrace() + " ///3 " + e.getSuppressed() + " ///4 " + e.getCause() +
                            " ///5 " + e.getLocalizedMessage() + " ///6 " + e.getClass() + " ///7 на переданном адресе:  " + pagePath +
                            " ///8 сайта:  " + site.getUrl());
                }
        return -1; // Передача в случае ошибки в try/catch
    }
    //

    public Page removedOrAddPage(String pagePath, String linkAU, Site site)   // TODO: Нужна ли проверка на остановку индексации ???
    {
        pageRepository = (PageRepository) SpringUtils.ctx.getBean(PageRepository.class);
        siteRepository = (SiteRepository) SpringUtils.ctx.getBean(SiteRepository.class);
        indexingService = (IndexingService) SpringUtils.ctx.getBean(IndexingServiceImpl.class);
        this.site = site;
        this.isForcedPageIndexing = true;

        lemmatizationService = (LemmatizationService) SpringUtils.ctx.getBean(LemmatizationService.class);  // L-I

        if(pageRepository.existsByPathAndSite(pagePath, site))
        {
            Page deletePage = pageRepository.findByPathAndSite(pagePath, site);
            pageRepository.delete(deletePage);
//            pageRepository.deleteByPathAndSite(pagePath, site);
//            pageRepository.deleteByPathAndSiteId(pagePath, site.getId());
        }

        try {
                Page removedPage = addPage(pagePath, linkAU);
                System.out.println("Для переданного пути: " + linkAU + " - пройден верно метод removeOrAddPage, pageId = " + removedPage.getId()); //*
                isForcedPageIndexing = false;
                return removedPage;

            } catch (IOException e)
                {
                    System.err.println("В классе PageWriter в методе removeOrAddPage сработал IOException / RuntimeException(e) ///1 " + e.getMessage() +
                            " ///2 " + e.getStackTrace() + " ///3 " + e.getSuppressed() + " ///4 " + e.getCause() +
                            " ///5 " + e.getLocalizedMessage() + " ///6 " + e.getClass() + " ///7 на переданном адресе:  " + pagePath +
                            " ///8 сайта:  " + site.getUrl());
                    System.out.println("Для переданного пути: " + linkAU + " - пройден неправильно метод removeOrAddPage, pageId = ???"); //*
                    return new Page(); // Передача в случае ошибки в try/catch
                }
//        System.out.println("Для переданного пути: " + linkAU + " - пройден неправильно метод removeOrAddPage, pageId = ???"); //*
//        return new Page(); // Передача в случае ошибки в try/catch
    }

    public int removeOrAddPage(String pagePath, String linkAU, Site site)   // TODO: Нужна ли проверка на остановку индексации ???
    {
        pageRepository = (PageRepository) SpringUtils.ctx.getBean(PageRepository.class);
        siteRepository = (SiteRepository) SpringUtils.ctx.getBean(SiteRepository.class);
        indexingService = (IndexingService) SpringUtils.ctx.getBean(IndexingServiceImpl.class);
        this.site = site;
        this.isForcedPageIndexing = true;

        if(pageRepository.existsByPathAndSite(pagePath, site))
        {
            Page deletePage = pageRepository.findByPathAndSite(pagePath, site);
            pageRepository.delete(deletePage);
//            pageRepository.deleteByPathAndSite(pagePath, site);
//            pageRepository.deleteByPathAndSiteId(pagePath, site.getId());
        }

        try {
            Page removedPage = addPage(pagePath, linkAU);
            System.out.println("Для переданного пути: " + linkAU + " - пройден верно метод removeOrAddPage, pageId = " + removedPage.getId()); //*
            isForcedPageIndexing = false;
            return removedPage.getId();

        } catch (IOException e)
        {
            System.err.println("В классе PageWriter в методе removePage сработал IOException / RuntimeException(e) ///1 " + e.getMessage() +
                    " ///2 " + e.getStackTrace() + " ///3 " + e.getSuppressed() + " ///4 " + e.getCause() +
                    " ///5 " + e.getLocalizedMessage() + " ///6 " + e.getClass() + " ///7 на переданном адресе:  " + pagePath +
                    " ///8 сайта:  " + site.getUrl());
        }
        System.out.println("Для переданного пути: " + linkAU + " - пройден неправильно метод removeOrAddPage, pageId = ???"); //*
        return -1; // Передача в случае ошибки в try/catch
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
        } //

        if (valueUrl.contains(parentPage) && !valueUrl.equals(parentPage))
        {
            isChildren = true;
        }

        if (valueUrl.contains(parentPage))
        {
            valueUrl = valueUrl.replaceFirst(parentPage, "");
        }     // крайнее было


        if (valueUrl.equals("")) {
            isChildren = false;
        }

        //
        if (valueUrl.startsWith("http://"))
        {
            valueUrl = valueUrl.replaceFirst("http://", "");
        }
        if (valueUrl.startsWith("https://"))
        {
            valueUrl = valueUrl.replaceFirst("https://", "");
        }

        if (parentPage.startsWith("http://"))
        {
            parentPage = parentPage.replaceFirst("http://", "");
        }
        if (parentPage.startsWith("https://"))
        {
            parentPage = parentPage.replaceFirst("https://", "");
        }

        if (valueUrl.contains(parentPage) && !valueUrl.equals(parentPage))
        {
            isChildren = true;
        }
        //

        if (valueUrl.contains("/") && valueUrl.length() == 1)
        {
            isChildren = false;
        }

        return isChildren;
    }

    //    @EnableTransactionManagement
    @Lock(value = LockModeType.OPTIMISTIC_FORCE_INCREMENT)
    @Transactional (/*transactionManager = "entityManagerFactoryT",*/
            propagation = Propagation.REQUIRED,
            isolation = Isolation.SERIALIZABLE)
    public Page addPage(String link, String linkAU) throws IOException
//    public synchronized Page addPage(String link, String linkAU) throws IOException
    {
        Page result = new Page();   //  Page result = null;
        result.setPath("Не добавляем страницу");
        result.setSiteId(-1);
        result.setContent("Не добавляем страницу");
        result.setCode(-1);

        Page pageValues = new Page();
        pageValues.setPath(link);
        pageValues.setSite(site);
        pageValues.setSiteId(site.getId()); // 16 may

        Connection.Response jsoupResponsePage = Jsoup.connect(linkAU).execute(); // Connection.Response jsoupResponsePage = Jsoup.connect(site.getUrl()).execute();
        pageValues.setCode(jsoupResponsePage.statusCode());         // pageValues.setContent(documentToString(Jsoup.connect(page.getSite().getUrl()+path).get()));
        pageValues.setContent(jsoupResponsePage.parse().html());

        boolean tx = TransactionSynchronizationManager.isActualTransactionActive(); // TODO: Добавить запись в мое "логирование"
        isIndexingSiteStarted = indexingService.getIndexingStarted();

        // Добавленный блок для принудительной индексации страницы:
        if (isForcedPageIndexing)
            {isIndexingSiteStarted = isForcedPageIndexing;}
        //

        if (!pageRepository.existsByPathAndSite(link, site) & isIndexingSiteStarted )
        {
            //
            if(Thread.currentThread().isInterrupted() || !isIndexingSiteStarted)
            {
                try {
                    throw new InterruptedException();
                } catch (InterruptedException e) {
                    System.err.println("PW catch in if addPage: " + page.getPath());
                    site.setStatus(StatusType.FAILED);
                    siteRepository.save(site);
                    System.out.println("PW catch in if addPage: " + page.getPath() + " - Выполнено изменение статуса сайта: " + site.getUrl() + " , на: " + site.getStatus());
                }
            } else
                {
                    pageRepository.save(pageValues);
                    result = pageValues;

                    lemmatizationService.indexNewPage(result);  // L-I

                    System.out.println("Добавлена страница: " + pageValues.getPath() + " (" + linkAU + ")" + " , isIndexedSiteStarted = " + isIndexingSiteStarted);
                } // if else catch
        }
        return result;
    }

    public boolean isNotFindPageRead(String linkR, Site siteR)
    {
        return !pageRepository.existsByPathAndSite(linkR, siteR);
    }

    @Override
    protected void compute()
    {
        //
        if(Thread.currentThread().isInterrupted())
            {
                try {
                    throw new InterruptedException();
                } catch (InterruptedException e)
                {
                    site.setStatus(StatusType.FAILED);
                    siteRepository.save(site);
                    System.err.println("PW catch in if Compute: " + page.getPath() + " - Выполнено изменение статуса сайта: " + site.getUrl() + " , на: " + site.getStatus());
                }
            }
        //

        isIndexingSiteStarted = indexingService.getIndexingStarted();
//        isIndexingSiteStarted = site.getStatus().equals(StatusType.INDEXING);
        if(isIndexingSiteStarted & !Thread.currentThread().isInterrupted())
        {
            System.out.println("\nЗначение indexingStarted: " + isIndexingSiteStarted +  " ,на странице [" + page.getPath() + "] " + " [сайт: " + site.getUrl()+ " ]"); // *

            List<PageWriter> pageWriterList = new ArrayList<>();
            try {
                Thread.sleep(1500);

                // Удалить - неиспользуемое ???
                String path = page.getPath();
                if (path == null || path == "/")
                    {
                        path = "";
                    }
//                String requestedPage = page.getSite().getUrl() + path;
                //

                String requestedPage = linkAbs;

//                Document pageLink = Jsoup.connect(requestedPage)
                Document pageLink = Jsoup.connect(linkAbs)
                        .userAgent(USER_AGENT)
                        .referrer("http://www.google.com")  //  .ignoreHttpErrors(true)
                        .get(); // Рабочий !!! Вариант еще: .execute().parse();



                Elements fullLinks = pageLink.select("a[href]");
                for (Element valueLink : fullLinks)
                    {
                        String linkAU = valueLink.absUrl("href");
                        String link = valueLink.attr("href");

                        // TODO: Облагородить проверку и изменение link:
                        // 09.06
                        String linkSite = site.getUrl();
                        String linkSite2 = site.getUrl().replaceFirst("www.", "");
                        if (link.contains(linkSite))
                            {
                                link = link.replaceFirst(linkSite, ""); // Исправить на "Начинается с _" - public boolean startsWith(String prefix)
        //                    System.out.println("Сработал метод замены path для страницы: " + linkAU + " , итоговый link: " + link);   // *
                            }
                        if (link.contains(linkSite2))
                            {
                                link = link.replaceFirst(linkSite2, ""); // Исправить на "Начинается с _" - public boolean startsWith(String prefix)
        //                        System.out.println("Сработал метод замены path для страницы: " + linkAU + " , итоговый link: " + link);   // *
                            }
                        //

                        boolean isChildren = isChildren(linkAU, requestedPage);
    //                    boolean isNotFindPage2 = !pageRepository.existsByPath(link);    // Убрать !!!
                        boolean isNotFindPage3 = !pageRepository.existsByPathAndSite(link, site);

    //                    isLock.lock();
    //                    try {

                        indexingStarted = !pageRepository.existsByPathAndSite(link, site);

                        lock.readLock().lock();
    //                    synchronized (link)
    //                    {
                        if (isIndexingSiteStarted&indexingStarted & isNotFindPageRead(link, site) & isNotFindPage3 /*& isNotFindPage2 & isNotFindPage*/
                                & isLink(linkAU) & isChildren
                                & !Thread.currentThread().isInterrupted())
                            {
                                Page pageValues = addPage(link, linkAU); // ???
        //                        if (pageValues != null) {
                                if (pageValues.getId() != -1)
                                    {
            //                          site.addPage(pageValues); // Проверить в debug - из-за этого дубли в page появляются
                                        site.setStatusTime(new Date());
                                        siteRepository.save(site);
                                        PageWriter pageWriter = new PageWriter(pageValues, linkAU);
                                        pageWriter.fork();
                                        pageWriterList.add(pageWriter);
                                    }

                            } else{}
                        lock.readLock().unlock();
                    }

                if(Thread.currentThread().isInterrupted()) // ???
                    {    // ???
                        pageWriterList.clear();// ???
                    }    // ???

                for (PageWriter pageWriter : pageWriterList)
                    {
                        pageWriter.join();
                    }

            } catch (InterruptedException e) {
                System.err.println("В классе PageWriter в методе compute сработал InterruptedException / RuntimeException(e) ///1 " + e.getMessage() + " ///2 " + e.getStackTrace() + " ///3 " + e.getSuppressed() + " ///4 " + e.getCause() + " ///5 " + e.getLocalizedMessage() + " ///6 " + e.getClass() + " ///7 на странице:  " + page.getPath() + " ///8 сайта:  " + site.getUrl());
//                throw new RuntimeException(e);
                site.setStatus(StatusType.FAILED);
                siteRepository.save(site);
                System.out.println("\nВ классе PageWriter в методе compute сработал InterruptedException / RuntimeException(e), состояние isIndexingSiteStarted: " +
                        isIndexingSiteStarted + " , на странице: "+ page.getPath() + " ,на остановку потока: " + Thread.currentThread().isInterrupted() +
                        " - Выполнено изменение статуса сайта: " + site.getUrl() + " , на: " + site.getStatus());
                Thread.currentThread().interrupt(); // ?

            } catch (IOException e) {
                System.err.println("В классе PageWriter методе compute сработал IOException / RuntimeException(e) ///1 " + e.getMessage() +
                        " ///2 " + e.getStackTrace() + " ///3 " + e.getSuppressed() + " ///4 " + e.getCause() +
                        " ///5 " + e.getLocalizedMessage() + " ///6 " + e.getClass() + " ///7 на странице:  " + page.getPath() +
                        " ///8 сайта:  " + site.getUrl());
//                throw new RuntimeException(e);
            }

            // New, 26 may
            catch (IllegalArgumentException e) {
                System.err.println("В классе PageWriter методе compute сработал IllegalArgumentException / RuntimeException(e) ///1 " + e.getMessage() + " ///2 " + e.getStackTrace() + " ///3 " + e.getSuppressed() + " ///4 " + e.getCause() + " ///5 " + e.getLocalizedMessage() + " ///6 " + e.getClass() + " ///7 на странице:  " + page.getPath() + " ///8 сайта:  " + site.getUrl());
//                throw new RuntimeException(e);
            } catch (Exception e) {
                System.err.println("В классе PageWriter методе compute сработал Exception / RuntimeException(e) ///1 " + e.getMessage() + " ///2 " + e.getStackTrace() + " ///3 " + e.getSuppressed() + " ///4 " + e.getCause() + " ///5 " + e.getLocalizedMessage() + " ///6 " + e.getClass() + " ///7 на странице:  " + page.getPath() + " ///8 сайта:  " + site.getUrl());
//                throw new RuntimeException(e);
            }
        } // Закр if(indexing){}
        else
            {
                site.setStatus(StatusType.FAILED);
                siteRepository.save(site);
                Thread.currentThread().interrupt();
                System.out.println("\nPageWriter in else d if(indexing){} в Compute: Пользователь остановил индексацию, значение isIndexingSiteStarted: " +
                        isIndexingSiteStarted + " , на странице: "+ page.getPath() + " ,на остановку потока: " + Thread.currentThread().isInterrupted() +
                         " - Выполнено изменение статуса сайта: " + site.getUrl() + " , на: " + site.getStatus());
            }
    }

    @Override
    public String toString()
    {
        return "PageWriter{" +
                "page=" + page +
                ", site=" + site +
                '}';
    }
}

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /*
    private SiteRepository siteRepository;
    private PageRepository pageRepository;
    @Autowired
    public PageWriter(@Autowired SiteRepository siteRepository, @Autowired PageRepository pageRepository) {this.siteRepository = siteRepository;this.pageRepository = pageRepository;}
   */

   /*
    @Autowired
    public PageWriter(PageRepository pageRepository) {this.pageRepository = pageRepository;}
    */

//    @Autowired
//    private SessionFactory sessionFactory;

    /*
    public PageWriter(Page page
            , PageRepository pageRepository, SiteRepository siteRepository
//            , boolean indexingStarted
    )
    {
        this.page = page;
        this.site = page.getSite(); // Убрать по идее !!!
        this.pageRepository = pageRepository;
        this.siteRepository = siteRepository;
    }
    */

    /*
    public PageWriter(Page page
            , PageRepository pageRepository, SiteRepository siteRepository
            , String linkAbs
    )
    {
        this.page = page;
        this.site = page.getSite(); // Убрать по идее !!!
        this.pageRepository = pageRepository;
        this.siteRepository = siteRepository;
        this.linkAbs = linkAbs;
    }
    */

    /*
    public PageWriter(Site site
            , PageRepository pageRepository, SiteRepository siteRepository
    ) throws IOException
        {
            this.site = site;
    //        page = new Page();
    //        page.setSite(site);

//            this.pageRepository = pageRepository;
//            this.siteRepository = siteRepository;

            this.indexingStarted = true;


            Page pageSearch = new Page();
            pageSearch.setSite(site);
            pageSearch.setSiteId(site.getId());
            pageSearch.setPath(site.getUrl()); // 16 мая 23
            pageSearch.setContent(new Date().toString() + " - " + String.valueOf(TransactionSynchronizationManager.isActualTransactionActive()));
            this.page = pageSearch;
            linkAbs  = page.getPath();
            addPage(page.getPath(), page.getPath());
        }
        */

//    public PageWriter(PageRepository pageRepository, SiteRepository siteRepository) {
//        this.pageRepository = pageRepository;
//        this.siteRepository = siteRepository;
//    }



//    public static final String USER_AGENT ="Microsoft Edge (Win 10 x64): Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/46.0.2486.0 Safari/537.36 Edge/13.10586";
//    public static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/73.0.3683.86 Safari/537.36";
//    public static final String USER_AGENT = "Mozilla/5.0 (Windows NT 6.1; Win64; x64; rv:25.0) Gecko/20100101 Firefox/25.0";
//    public static final String USER_AGENT ="Microsoft Edge (Win 10 x64): Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/46.0.2486.0 Safari/537.36 Edge/13.10586";


    /*
    jsoup-setting:
    jsoup:
    userAgent: Mozilla/5.0 (Windows NT 6.1; Win64; x64; rv:25.0) Gecko/20100101 Firefox/25.0
    referrer: http://www:google.com
    timeout: 10000
    ignoreHttpErrors: true
    followRedirects: false
    */

    /*
    jsoup-setting:
    jsoup:
    userAgent: Mozilla/5.0 (Windows NT 6.1; Win64; x64; rv:25.0) Gecko/20100101 Firefox/25.0
    referrer: http://www:google.com
    timeout: 10000
    ignoreHttpErrors: true
    followRedirects: false
    */


    /*
    public PageWriter(Page page
            , PageRepository pageRepository, SiteRepository siteRepository
//            , boolean indexingStarted
    )
    {
        this.page = page;
        this.site = page.getSite(); // Убрать по идее !!!
        this.pageRepository = pageRepository;
        this.siteRepository = siteRepository;
    }
    */

    /*
    public PageWriter(Page page
            , PageRepository pageRepository, SiteRepository siteRepository
            , String linkAbs
    )
    {
        this.page = page;
        this.site = page.getSite(); // Убрать по идее !!!
        this.pageRepository = pageRepository;
        this.siteRepository = siteRepository;
        this.linkAbs = linkAbs;
    }
    */

    /*
    public PageWriter(Site site
            , PageRepository pageRepository, SiteRepository siteRepository
    ) throws IOException
        {
            this.site = site;
    //        page = new Page();
    //        page.setSite(site);

//            this.pageRepository = pageRepository;
//            this.siteRepository = siteRepository;

            this.indexingStarted = true;


            Page pageSearch = new Page();
            pageSearch.setSite(site);
            pageSearch.setSiteId(site.getId());
            pageSearch.setPath(site.getUrl()); // 16 мая 23
            pageSearch.setContent(new Date().toString() + " - " + String.valueOf(TransactionSynchronizationManager.isActualTransactionActive()));
            this.page = pageSearch;
            linkAbs  = page.getPath();
            addPage(page.getPath(), page.getPath());
        }
        */

//    public PageWriter(PageRepository pageRepository, SiteRepository siteRepository) {
//        this.pageRepository = pageRepository;
//        this.siteRepository = siteRepository;
//    }



//    public boolean isLinkA(String valueUrl) {
////        boolean isLink = true;
////        if (valueUrl.contains(".pdf") || valueUrl.contains(".PNG") || valueUrl.contains("#") ||
////                !valueUrl.contains("https://")) {
////            isLink = false;
////        }
////        return isLink;
////    }

        /*
    public void startPageSearch(Site siteSearch) {
//        Page page = new ForkJoinPool().invoke(new PageWriter(siteSearch, pageRepository, siteRepository));

        Page pageSearch = new Page();
        pageSearch.setSite(siteSearch);
        pageSearch.setPath(siteSearch.getUrl()); // 16 мая 23
//        Page page = new ForkJoinPool().invoke(new PageWriter(pageSearch, pageRepository, siteRepository));

        new ForkJoinPool().invoke(new PageWriter(pageSearch, pageRepository, siteRepository, siteSearch.getUrl()));
//        new ForkJoinPool().invoke(new PageWriter(pageSearch, pageRepository, siteRepository));
    }
        */



//  Page pageValues = new TestValue().addPageTV(link, linkAbs, site, pageRepository, siteRepository);
//                            Page pageValues = new Page();


//                            site.setStatusTime(new Date());
//                            site.addPage(pageValues);
//                            siteRepository.save(site);

//                            PageWriter pageWriter = new PageWriter(pageValues, pageRepository, siteRepository, linkAU);
//                            new TestValue().addPageTVS(link, linkAbs, pageWriter);
//                            pageWriter.fork();
//                            pageWriterList.add(pageWriter);



//                            pageRepository.isBlock(link);
//
//                        //                            Page pageValues = new Page();
//                        //                            pageValues.setPath(link);
//                        //                            pageValues.setSite(site);
//                        //                            // pageValues.setContent(documentToString(Jsoup.connect(page.getSite().getUrl()+path).get()));
//                        //                            pageValues.setContent("Тут д.б. <HTML> page!!! Дочерний к странице = ");
//                        //                            pageValues.setCode(Jsoup.connect(linkAU).execute().statusCode());
//
//
//                        //                            pageRepository.findByPathAndSite(pageValues.getPath(), site).ifPresentOrElse(page -> System.out.println("page имеется в БД = " + page),
//                        //                                                            () -> pageRepository.save(pageValues));
//
//                        //                            Page existPage = pageRepository
//                        //                                    .findByPathAndSite(pageValues.getPath(), pageValues.getSite())
//                        //                                    .orElseGet(() -> pageRepository.save(pageValues));
//
//
//                        //                            pageFind = pageValues;
//                        //                            Page existPage1 = pageRepository
//                        //                                    .findByPathAndSite(pageFind.getPath(), pageFind.getSite())
//                        //                                    .orElseGet(() -> pageRepository.save(pageFind));
//
//                        //                            pageRepository
//                        //                                    .findByPathForUpdate(pageFind.getPath())
//                        //                                    .orElseGet(() -> pageRepository.save(pageFind));
//
//                        //                            pageRepository.save(pageValues);
//
//                        //                            Page pageValues = addPage(linkAbs,linkAbs);
//                        //                            Page pageValues = addPage(link,linkAbs);



    /*
    public boolean isFindPage(String path) {
        boolean isFind = false;

//        Page page = pageRepository.findByPath(path);
//        if(pageRepository.findByPath(path) != null)


        Optional<Page> optionalPage = pageRepository.findByPath(path);
        if (optionalPage.isPresent()) {
            System.out.println("\nЗапись с путем: " + path + " уже имеется");
            isFind = true;
        }

        return isFind;
    }
    */
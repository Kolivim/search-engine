package searchengine.services;

import com.fasterxml.jackson.databind.cfg.PackageVersion;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.hibernate.SessionFactory;
import org.hibernate.exception.GenericJDBCException;
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

public class PageWriter extends RecursiveAction
{
    @Autowired
    private PageRepository pageRepository;
    @Autowired
    private SiteRepository siteRepository;
    IndexingService indexingService;    //TODO: Поставить везде на переменных private!
    private Page page;
    private Site site;
    private volatile boolean indexingStarted;
    private volatile boolean isIndexingSiteStarted;
    ReadWriteLock lock = new ReentrantReadWriteLock();
//    private volatile Page pageFind; // Убрать!!!
    ReentrantLock isLock = new ReentrantLock();
    String linkAbs = "";
    public static final String USER_AGENT = "Mozilla/5.0 (compatible; MJ12bot/v1.4.5; http://www.majestic12.co.uk/bot.php?+)";

    public PageWriter(Site site) throws IOException
    {
        this.site = site;
        this.indexingStarted = true;
        pageRepository = (PageRepository) SpringUtils.ctx.getBean(PageRepository.class);
        siteRepository = (SiteRepository) SpringUtils.ctx.getBean(SiteRepository.class);

        indexingService = (IndexingService) SpringUtils.ctx.getBean(IndexingServiceImpl.class);

        Page pageValues = new Page();
        pageValues.setPath("/");
        pageValues.setSite(site);
        pageValues.setSiteId(site.getId()); // 16 may

        /*    //
        pageValues.setContent(new Date().toString() + " - " + String.valueOf(TransactionSynchronizationManager.isActualTransactionActive()));   // Дописать !!!
        pageValues.setCode(Jsoup.connect(site.getUrl()).execute().statusCode());   //  int code = Jsoup.connect(linkAU).execute().statusCode();
        */    //

        //
        Connection.Response jsoupResponsePage = Jsoup.connect(site.getUrl()).execute();
        pageValues.setCode(jsoupResponsePage.statusCode());
        pageValues.setContent(jsoupResponsePage.parse().html());
        //

        pageRepository.save(pageValues);
        this.page = pageValues;
        this.linkAbs = site.getUrl();
    }

    public PageWriter(Page pageValues, String linkAU)
    {
        this.page = pageValues;
        linkAbs = linkAU;
        this.site = page.getSite(); // Проверить работу в Debug !!!
        pageRepository = (PageRepository) SpringUtils.ctx.getBean(PageRepository.class);
        siteRepository = (SiteRepository) SpringUtils.ctx.getBean(SiteRepository.class);
        indexingService = (IndexingService) SpringUtils.ctx.getBean(IndexingServiceImpl.class);
    }

    public boolean isLink(String valueUrl)
    {
        boolean isLink = true;
        if (valueUrl.contains(".pdf") || valueUrl.contains(".PNG") || valueUrl.contains("#")){isLink = false;}
        return isLink;
    }

    public boolean isChildren(String valueUrl, String parentPage)  //TODO: Оптимизировать проверку!
    {
        boolean isChildren = false;
        if (valueUrl.contains(parentPage) && !valueUrl.equals(parentPage))
        {
            isChildren = true;
        }
        if (parentPage.contains("www."))
        {
            parentPage = parentPage.replaceFirst("www.", "");
        }
        if (valueUrl.contains(parentPage) && !valueUrl.equals(parentPage))
        {
            isChildren = true;
        }
        if (valueUrl.contains("www."))
        {
            valueUrl = valueUrl.replaceFirst("www.", "");
        }
        if (valueUrl.contains(parentPage) && !valueUrl.equals(parentPage))
        {
            isChildren = true;
        }
        if (valueUrl.contains(parentPage))
        {
            valueUrl = valueUrl.replaceFirst(parentPage, "");
        }
        if (valueUrl.contains("/") && valueUrl.length() == 1)
        {
            isChildren = false;
        }
        if (valueUrl.equals(""))
        {
            isChildren = false;
        }
        return isChildren;
    }

    /*
    public boolean isFindPage(String path) {
        boolean isFind = false;

       // Page page = pageRepository.findByPath(path);
       //if(pageRepository.findByPath(path) != null)


        Optional<Page> optionalPage = pageRepository.findByPath(path);
        if (optionalPage.isPresent())
        {
            System.out.println("\nЗапись с путем: " + path + " уже имеется");
            isFind = true;
        }

        return isFind;
    }
    */

//    @EnableTransactionManagement
    @Lock(value = LockModeType.OPTIMISTIC_FORCE_INCREMENT)
    @Transactional (
//                    transactionManager = "entityManagerFactoryT",
                    propagation = Propagation.REQUIRED,
                    isolation = Isolation.SERIALIZABLE
                    )
    public Page addPage(String link, String linkAU) throws IOException
//    public synchronized Page addPage(String link, String linkAU) throws IOException
    {
        Page result = new Page();
        result.setPath("Не добавляем страницу");
        result.setSiteId(-1);
        result.setContent("Не добавляем страницу");
        result.setCode(-1);
//        Page result = null;

        Page pageValues = new Page();
        pageValues.setPath(link);
        pageValues.setSite(site);
        pageValues.setSiteId(site.getId()); // 16 may
        pageValues.setContent(new Date().toString() + " - " + String.valueOf(TransactionSynchronizationManager.isActualTransactionActive()));   // Дописать !!!
//        pageValues.setCode(Jsoup.connect(linkAU).execute().statusCode());   //  int code = Jsoup.connect(linkAU).execute().statusCode();

//        pageValues.setContent(Jsoup.connect(linkAU).userAgent(USER_AGENT).referrer("http://www.google.com").get().html());
//        try
//        {
        Connection.Response jsoupResponsePage = Jsoup.connect(linkAU).execute();
        pageValues.setCode(jsoupResponsePage.statusCode());
        pageValues.setContent(jsoupResponsePage.parse().html());
//        }
//            catch (IOException e)
//            {
//                System.err.println("В классе PageWriter методе addPage сработал IOException / RuntimeException(e) ///1 " + e.getMessage() + " ///2 " + e.getStackTrace() + " ///3 " + e.getSuppressed() + " ///4 " + e.getCause() + " ///5 " + e.getLocalizedMessage() + " ///6 " + e.getClass() + " ///7 на странице:  " + pageValues.getPath());
//            }

        boolean tx = TransactionSynchronizationManager.isActualTransactionActive();

        if (!pageRepository.existsByPathAndSite(link, site) & isIndexingSiteStarted )
            {
                //
                if(Thread.currentThread().isInterrupted())
                {
                    try
                    {
                        throw new InterruptedException();
                    } catch (InterruptedException e)
                        {
                            System.err.println("PW catch in if addPage: " + page.getPath());
                            site.setStatus(StatusType.FAILED);
                            siteRepository.save(site);
                            System.out.println("PW catch in if addPage: " + page.getPath() + " - Выполнено изменение статуса сайта: " + site.getUrl() + " , на: " + site.getStatus());
                        }
                } else
                    {
                        pageRepository.save(pageValues);
                        result = pageValues;
                        System.out.println("Добавлена страница: " + pageValues.getPath() + " (" + linkAU + ")" + " , link = " + link);
                    } // if else catch
            }
        return result;
    }


    @Override
    protected void compute()
    {
        //
        if(Thread.currentThread().isInterrupted())
        {
            try
            {
                throw new InterruptedException();
            } catch (InterruptedException e)
                {
                    System.err.println("PW catch in if Compute: " + page.getPath());
                    site.setStatus(StatusType.FAILED);
                    site.setLastError("Индексация остановлена пользователем");
                    siteRepository.save(site);
                    System.out.println("PW catch in if Compute: " + page.getPath() + " - Выполнено изменение статуса сайта: " + site.getUrl() + " , на: " + site.getStatus());
                }
        }
        //

        isIndexingSiteStarted = indexingService.getIndexingStarted();
        if(isIndexingSiteStarted & !Thread.currentThread().isInterrupted())
        {
            System.out.println("\nPW/Compute: indexing started на странице " + page.getPath() + " : " + isIndexingSiteStarted); // *

            List<PageWriter> pageWriterList = new ArrayList<>();
            try
            {
                Thread.sleep(1500);
                String path = page.getPath();
                if (path == null || path == "/") {path = "";} // Лиюо contains либо убрать !!!

                String requestedPage = linkAbs;
                Document pageLink = Jsoup.connect(linkAbs)
                        .userAgent(USER_AGENT)
                        .referrer("http://www.google.com")
//                        .ignoreHttpErrors(true)
                        .get();

                Elements fullLinks = pageLink.select("a[href]");

                /* //
                try
                    {
                        String contentHtml = pageLink.html();
                        System.out.println("\nТекст страницы " + page.getPath() + ":\n" + contentHtml.length() + "\n");   //*
                        page.setContent(contentHtml);
                        pageRepository.save(page);
                    } catch(GenericJDBCException e)
                        {
                            String contentHtml = pageLink.html();
                            System.err.println("\nTry/Catch Текст страницы " + page.getPath() + ":\n" + contentHtml.length() + "\n");   //*
                            page.setContent(contentHtml);
                            pageRepository.save(page);
                        }
                */ //

//                System.out.println("\nТекст страницы " + page.getPath() + ":\n" + pageLink.html().length() + "\n");   //*
//                System.out.println("\nТекст страницы " + page.getPath() + ":\n" + pageLink.html() + "\n");   //*

                for (Element valueLink : fullLinks)
                {
                    String linkAU = valueLink.absUrl("href");
                    String link = valueLink.attr("href");

                    // TODO: Облагородить проверку и изменение link:
                    // 09.06
                    String linkSite = site.getUrl();
                    String linkSite2 = site.getUrl().replaceFirst("www.", "");
//                    boolean isFullLink = link.contains(linkSite) || link.contains(linkSite2);
                    if (link.contains(linkSite))
                    {
                        link = link.replaceFirst(linkSite, ""); // Исправить на "Начинается с _" - public boolean startsWith(String prefix)
                    System.out.println("Сработал метод замены path для страницы: " + linkAU + " , итоговый link: " + link);   // *
                    }
                    if (link.contains(linkSite2))
                    {
                        link = link.replaceFirst(linkSite2, ""); // Исправить на "Начинается с _" - public boolean startsWith(String prefix)
                        System.out.println("Сработал метод замены path для страницы: " + linkAU + " , итоговый link: " + link);   // *
                    }
                    //

                    boolean isChildren = isChildren(linkAU, requestedPage);
                    boolean isNotFindPage3 = !pageRepository.existsByPathAndSite(link, site);
                    indexingStarted = !pageRepository.existsByPathAndSite(link, site);
                    lock.readLock().lock();

//                    boolean isNotFindPage2 = !pageRepository.existsByPath(link);    // Убрать !!!
//                    isLock.lock();
//                    try {
//                    synchronized (link)
//                    {

                    if (isIndexingSiteStarted & indexingStarted & isNotFindPage3
//                            & isNotFindPage2 & isNotFindPage & isNotFindPageRead(link, site)
                            & isLink(linkAU) & isChildren
                            & !Thread.currentThread().isInterrupted())
                    {
                        Page pageValues = addPage(link, linkAU); // ???
                        if (pageValues != null)
                        {
//                          site.addPage(pageValues); // Проверить в debug - из-за этого дубли в page появляются
                            site.setStatusTime(new Date());
                            siteRepository.save(site);
                            PageWriter pageWriter = new PageWriter(pageValues, linkAU);
                            pageWriter.fork();
                            pageWriterList.add(pageWriter);
                        }

                    } else {} // Нужна ли какая-либо реакция ???

                    lock.readLock().unlock();
                }

                if(Thread.currentThread().isInterrupted())
                {
                    pageWriterList.clear();
                }

                for (PageWriter pageWriter : pageWriterList)
                    {
                        pageWriter.join();
                    }

            }
            catch (InterruptedException e)
                {
                    System.err.println("В классе PageWriter методе compute сработал InterruptedException / RuntimeException(e) ///1 " + e.getMessage() + " ///2 " + e.getStackTrace() + " ///3 " + e.getSuppressed() + " ///4 " + e.getCause() + " ///5 " + e.getLocalizedMessage() + " ///6 " + e.getClass() + " ///7 на странице:  " + page.getPath());
                    Thread.currentThread().interrupt(); // ?
                }
            catch (IOException e)
                {
                    System.err.println("В классе PageWriter методе compute сработал IOException / RuntimeException(e) ///1 " + e.getMessage() + " ///2 " + e.getStackTrace() + " ///3 " + e.getSuppressed() + " ///4 " + e.getCause() + " ///5 " + e.getLocalizedMessage() + " ///6 " + e.getClass() + " ///7 на странице:  " + page.getPath());
                }
            catch (IllegalArgumentException e)
                {
                    System.err.println("В классе PageWriter методе compute сработал IllegalArgumentException / RuntimeException(e) ///1 " + e.getMessage() + " ///2 " + e.getStackTrace() + " ///3 " + e.getSuppressed() + " ///4 " + e.getCause() + " ///5 " + e.getLocalizedMessage() + " ///6 " + e.getClass() + " ///7 на странице:  " + page.getPath());
                }
            catch (Exception e)
                {
                    System.err.println("В классе PageWriter методе compute сработал Exception / RuntimeException(e) ///1 " + e.getMessage() + " ///2 " + e.getStackTrace() + " ///3 " + e.getSuppressed() + " ///4 " + e.getCause() + " ///5 " + e.getLocalizedMessage() + " ///6 " + e.getClass() + " ///7 на странице:  " + page.getPath());
                }
        } // Закр if(indexing){}
            else     // К закр if(indexing) {} else
                {
                    Thread.currentThread().interrupt();
                    System.out.println("\nPageWriter: Пользователь остановил индексацию, значение isIndexingSiteStarted: " + isIndexingSiteStarted + " , на странице: "+ page.getPath() + "\n" + "Получен запрос в странице " + page.getPath() + " на остановку потока: " + Thread.currentThread().isInterrupted());
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



//                String requestedPage = page.getSite().getUrl() + path;
//                Document pageLink = Jsoup.connect(requestedPage)



//                System.out.println("\nТекст страницы " + page.getPath() + ":\n" + pageLink.text() + "\n");   //*



    /*
    public boolean isNotFindPageRead(String linkR, Site siteR)
        {
            return !pageRepository.existsByPathAndSite(linkR, siteR);
        }
     */



//        isIndexingSiteStarted = site.getStatus().equals(StatusType.INDEXING);



//                throw new RuntimeException(e);



//@Component
//public class PageWriter extends RecursiveTask<Set<Page>>
//public class PageWriter extends RecursiveTask<Boolean>{



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



//    public Page addPage(String link, String linkAU) throws IOException
////    public synchronized Page addPage(String link, String linkAU) throws IOException
//    {
//        Page result =new Page();
//        result.setPath("Не добавляем страницу");
//        result.setSiteId(-1);
//        result.setContent("Не добавляем страницу");
//        result.setCode(-1);
////        Page result = null;
//
//        Page pageValues = new Page();
//        pageValues.setPath(link);
//        pageValues.setSite(site);
//        pageValues.setSiteId(site.getId()); // 16 may
//        // Добавление HTML кода страницы:
//        // pageValues.setContent(documentToString(Jsoup.connect(page.getSite().getUrl()+path).get()));
//        pageValues.setContent(new Date().toString() + " - " + String.valueOf(TransactionSynchronizationManager.isActualTransactionActive()));   // Дописать !!!
//        pageValues.setCode(Jsoup.connect(linkAU)
//                .execute()
//                .statusCode());   //  int code = Jsoup.connect(linkAU).execute().statusCode();
//
//        boolean tx = TransactionSynchronizationManager.isActualTransactionActive();
//
////        pageRepository.save(pageValues);
//
////        if (!pageRepository.existsByPath(link))
//        if (!pageRepository.existsByPathAndSite(link, site) & isIndexingSiteStarted )
////        if (!pageRepository.existsByPathAndSite(link, site))  // Рабочее!!!
//        {
//            //
//            if(Thread.currentThread().isInterrupted()) {
//                try {
//                    throw new InterruptedException();
//                } catch (InterruptedException e) {
//                    System.err.println("PW catch in if addPage: " + page.getPath());
//                    site.setStatus(StatusType.FAILED);
//                    siteRepository.save(site);
//                    System.out.println("PW catch in if addPage: " + page.getPath() + " - Выполнено изменение статуса сайта: " + site.getUrl() + " , на: " + site.getStatus());
//                }
//            } else
//            {
//                pageRepository.save(pageValues);
//                result = pageValues;
//                System.out.println("Добавлена страница: " + pageValues.getPath() + " (" + linkAU + ")");
//            } // if else catch
//
//                /*
//                pageRepository.save(pageValues);
//
//                //
//                //Блок добавления Site в БД:
//                //site.setStatusTime(new Date());
//                //site.addPage(pageValues);
//                //siteRepository.save(site);
//                //
//
//                result = pageValues;
//                System.out.println("Добавлена страница: " + pageValues.getPath() + " (" + linkAU + ")");
//                */
//
//        }
//        return result;
//    }



    /*
    private String documentToString(Document newDoc) throws Exception
    {
        DOMSource domSource = new DOMSource((Node) newDoc);
        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        StringWriter stringWriter = new StringWriter();
        StreamResult streamResult = new StreamResult(stringWriter);
        transformer.transform(domSource, streamResult);
        System.out.println(stringWriter.toString());
        return stringWriter.toString();
    }
     */
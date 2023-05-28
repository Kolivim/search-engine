package searchengine.services;

import com.fasterxml.jackson.databind.cfg.PackageVersion;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.hibernate.SessionFactory;
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
//public class PageWriter extends RecursiveTask<Page>
public class PageWriter extends RecursiveAction {

    @Autowired
    private PageRepository pageRepository;
    @Autowired
    private SiteRepository siteRepository;

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

    private Page page;
    private Site site;
    private volatile boolean indexingStarted;
    private ReadWriteLock lock = new ReentrantReadWriteLock();
    private volatile Page pageFind;
    ReentrantLock isLock = new ReentrantLock();
    String linkAbs = "";
//    public static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/73.0.3683.86 Safari/537.36";
//    public static final String USER_AGENT = "Mozilla/5.0 (Windows NT 6.1; Win64; x64; rv:25.0) Gecko/20100101 Firefox/25.0";
public static final String USER_AGENT = "Mozilla/5.0 (compatible; MJ12bot/v1.4.5; http://www.majestic12.co.uk/bot.php?+)";




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





    public void setIndexingStarted(boolean indexingStarted) {
        this.indexingStarted = indexingStarted;
    }

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


    public PageWriter(Site site) throws IOException
    {
        this.site = site;
        this.indexingStarted = true;
        pageRepository = (PageRepository) SpringUtils.ctx.getBean(PageRepository.class);
        siteRepository = (SiteRepository) SpringUtils.ctx.getBean(SiteRepository.class);

        Page pageValues = new Page();
        pageValues.setPath(site.getUrl());
        pageValues.setSite(site);
        pageValues.setSiteId(site.getId()); // 16 may
        // Добавление HTML кода страницы:
        // pageValues.setContent(documentToString(Jsoup.connect(page.getSite().getUrl()+path).get()));
        pageValues.setContent(new Date().toString() + " - " + String.valueOf(TransactionSynchronizationManager.isActualTransactionActive()));   // Дописать !!!
        pageValues.setCode(Jsoup.connect(site.getUrl())
                .execute()
                .statusCode());   //  int code = Jsoup.connect(linkAU).execute().statusCode();

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
    }

    public boolean isLink(String valueUrl) {
        boolean isLink = true;
        if (valueUrl.contains(".pdf") || valueUrl.contains(".PNG") || valueUrl.contains("#")) {
            isLink = false;
        }
        return isLink;
    }

    public boolean isLinkA(String valueUrl) {
        boolean isLink = true;
        if (valueUrl.contains(".pdf") || valueUrl.contains(".PNG") || valueUrl.contains("#") ||
                !valueUrl.contains("https://")) {
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
        if (valueUrl.contains(parentPage) && !valueUrl.equals(parentPage)) {
            isChildren = true;
        }                                     //

        if (valueUrl.contains(parentPage)) {
            valueUrl = valueUrl.replaceFirst(parentPage, "");
        }     // крайнее
        if (valueUrl.contains("/") && valueUrl.length() == 1) {
            isChildren = false;
        }                         // крайнее

        if (valueUrl.equals("")) {
            isChildren = false;
        }

        return isChildren;
    }

    private String documentToString(Document newDoc) throws Exception {
        DOMSource domSource = new DOMSource((Node) newDoc);
        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        StringWriter stringWriter = new StringWriter();
        StreamResult streamResult = new StreamResult(stringWriter);
        transformer.transform(domSource, streamResult);
        System.out.println(stringWriter.toString());
        return stringWriter.toString();
    }

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


    public boolean isFindPage(String path) {
        boolean isFind = false;
        /*
        Page page = pageRepository.findByPath(path);
        if(pageRepository.findByPath(path) != null)
        */

        Optional<Page> optionalPage = pageRepository.findByPath(path);
        if (optionalPage.isPresent()) {
            System.out.println("\nЗапись с путем: " + path + " уже имеется");
            isFind = true;
        }

        return isFind;
    }

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

        Page result =new Page();
        result.setPath("Не добавляем страницу");
        result.setSiteId(-1);
        result.setContent("Не добавляем страницу");
        result.setCode(-1);
//        Page result = null;

        Page pageValues = new Page();
        pageValues.setPath(link);
        pageValues.setSite(site);
        pageValues.setSiteId(site.getId()); // 16 may
        // Добавление HTML кода страницы:
        // pageValues.setContent(documentToString(Jsoup.connect(page.getSite().getUrl()+path).get()));
        pageValues.setContent(new Date().toString() + " - " + String.valueOf(TransactionSynchronizationManager.isActualTransactionActive()));   // Дописать !!!
        pageValues.setCode(Jsoup.connect(linkAU)
                .execute()
                .statusCode());   //  int code = Jsoup.connect(linkAU).execute().statusCode();

        boolean tx = TransactionSynchronizationManager.isActualTransactionActive();

//        pageRepository.save(pageValues);

//        if (!pageRepository.existsByPath(link))
        if (!pageRepository.existsByPathAndSite(link, site))
            {
                pageRepository.save(pageValues);

                /*
                //Блок добавления Site в БД:
                site.setStatusTime(new Date());
                site.addPage(pageValues);
                siteRepository.save(site);
                */

                result = pageValues;
                System.out.println("Добавлена страница: " + pageValues.getPath() + " (" + linkAU + ")");
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
            List<PageWriter> pageWriterList = new ArrayList<>();
            try
            {
                Thread.sleep(1500);
                String path = page.getPath();
                if (path == null || path ==  "/") {path = "";}
//                String requestedPage = page.getSite().getUrl() + path;
                String requestedPage = linkAbs;

                Document pageLink = Jsoup.connect(linkAbs)
//                Document pageLink = Jsoup.connect(requestedPage)
                        .userAgent(
//                                "Mozilla/5.0 (Windows; U; WindowsNT 5.1; en-US; rv1.8.1.6) Gecko/20070725 Firefox/2.0.0.6"
                                USER_AGENT
                        )
                        .referrer("http://www.google.com")
//                        .ignoreHttpErrors(true)
                        .get();

                Elements fullLinks = pageLink.select("a[href]");
                for (Element valueLink : fullLinks)
                {
                    String linkAU = valueLink.absUrl("href");
                    String link = valueLink.attr("href");



                    boolean isChildren = isChildren(linkAU, requestedPage);
//                    boolean isNotFindPage = !pageRepository.findByPath(link).isPresent();
                    boolean isNotFindPage2 = !pageRepository.existsByPath(link);
                    boolean isNotFindPage3 = !pageRepository.existsByPathAndSite(link, site);

//                    isLock.lock();
//                    try {

                    indexingStarted = !pageRepository.existsByPathAndSite(link, site);

                    lock.readLock().lock();
//                    synchronized (link)
//                    {
                        if (indexingStarted & isNotFindPageRead(link, site) & isNotFindPage3 & isNotFindPage2
//                                & isNotFindPage
                                & isLink(linkAU) & isChildren)
                            {
    //                            pageRepository.isBlock(link);

    //                            Page pageValues = new Page();
    //                            pageValues.setPath(link);
    //                            pageValues.setSite(site);
    //                            // pageValues.setContent(documentToString(Jsoup.connect(page.getSite().getUrl()+path).get()));
    //                            pageValues.setContent("Тут д.б. <HTML> page!!! Дочерний к странице = ");
    //                            pageValues.setCode(Jsoup.connect(linkAU).execute().statusCode());


    //                            pageRepository.findByPathAndSite(pageValues.getPath(), site).ifPresentOrElse(page -> System.out.println("page имеется в БД = " + page),
    //                                                            () -> pageRepository.save(pageValues));

    //                            Page existPage = pageRepository
    //                                    .findByPathAndSite(pageValues.getPath(), pageValues.getSite())
    //                                    .orElseGet(() -> pageRepository.save(pageValues));


    //                            pageFind = pageValues;
    //                            Page existPage1 = pageRepository
    //                                    .findByPathAndSite(pageFind.getPath(), pageFind.getSite())
    //                                    .orElseGet(() -> pageRepository.save(pageFind));

    //                            pageRepository
    //                                    .findByPathForUpdate(pageFind.getPath())
    //                                    .orElseGet(() -> pageRepository.save(pageFind));

    //                            pageRepository.save(pageValues);

    //                            Page pageValues = addPage(linkAbs,linkAbs);
    //                            Page pageValues = addPage(link,linkAbs);

                                Page pageValues = addPage(link,linkAU); // ???
                                if (pageValues != null)
                                    {
//                                        PageWriter pageWriter = new PageWriter(pageValues, pageRepository, siteRepository, linkAU);

//                                        site.addPage(pageValues); // Проверить debug - из-за этого дубли в page появляются
                                        site.setStatusTime(new Date());

                                        siteRepository.save(site);
                                        PageWriter pageWriter = new PageWriter(pageValues, linkAU);
                                        pageWriter.fork();
                                        pageWriterList.add(pageWriter);

                                    }
                              //  Page pageValues = new TestValue().addPageTV(link, linkAbs, site, pageRepository, siteRepository);
    //                            Page pageValues = new Page();



    //                            site.setStatusTime(new Date());
    //                            site.addPage(pageValues);
    //                            siteRepository.save(site);

    //                            PageWriter pageWriter = new PageWriter(pageValues, pageRepository, siteRepository, linkAU);
    //                            new TestValue().addPageTVS(link, linkAbs, pageWriter);
    //                            pageWriter.fork();
    //                            pageWriterList.add(pageWriter);

                            } else {}
                    lock.readLock().unlock();
//                    }

//                } finally {isLock.unlock();}


                }



                for (PageWriter pageWriter : pageWriterList)
                    {
                        pageWriter.join();
                    }

            } catch (InterruptedException e) {
                System.err.println("В классе PageWriter методе compute сработал InterruptedException / RuntimeException(e) ///1 " + e.getMessage() + " ///2 " + e.getStackTrace() + " ///3 " + e.getSuppressed() + " ///4 " + e.getCause() + " ///5 " + e.getLocalizedMessage() + " ///6 " + e.getClass());
                throw new RuntimeException(e);

            } catch (IOException e) {
                System.err.println("В классе PageWriter методе compute сработал IOException / RuntimeException(e) ///1 " + e.getMessage() + " ///2 " + e.getStackTrace() + " ///3 " + e.getSuppressed() + " ///4 " + e.getCause() + " ///5 " + e.getLocalizedMessage() + " ///6 " + e.getClass());
//                throw new RuntimeException(e);
            }

            // New, 26 may
            catch (IllegalArgumentException e) {
                System.err.println("В классе PageWriter методе compute сработал IllegalArgumentException / RuntimeException(e) ///1 " + e.getMessage() + " ///2 " + e.getStackTrace() + " ///3 " + e.getSuppressed()+ " ///4 " + e.getCause() + " ///5 " + e.getLocalizedMessage()+ " ///6 " + e.getClass());
//                throw new RuntimeException(e);
            } catch (Exception e) {
                System.err.println("В классе PageWriter методе compute сработал Exception / RuntimeException(e) ///1 " + e.getMessage() + " ///2 " + e.getStackTrace() + " ///3 " + e.getSuppressed()+ " ///4 " + e.getCause() + " ///5 " + e.getLocalizedMessage()+ " ///6 " + e.getClass());
//                throw new RuntimeException(e);
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

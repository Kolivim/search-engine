package searchengine.services;

import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import searchengine.model.Site;
import searchengine.model.StatusType;

import java.util.concurrent.Callable;
import java.util.concurrent.ForkJoinPool;

@NoArgsConstructor
public class StartIndexing implements Callable<Boolean>
{


    private SiteRepository siteRepository; // 1
    Site siteDB;
    public StartIndexing(Site siteDB){this.siteDB = siteDB;}


    @Override
    public Boolean call() throws Exception
    {
        siteRepository = (SiteRepository) SpringUtils.ctx.getBean(SiteRepository.class); // 1
        new ForkJoinPool().invoke(new PageWriter(siteDB));
        System.out.println("\nСайт " + siteDB.getUrl() + " - заверешение метода SIndexing и возврат boolean call()");
        siteDB.setStatus(StatusType.INDEXED);
        siteRepository.save(siteDB);
        return true;
    }



    /*
    private SiteRepository siteRepository;

    private PageRepository pageRepository;

    @Autowired
    public StartIndexing(SiteRepository siteRepository,PageRepository pageRepository) {this.siteRepository = siteRepository;this.pageRepository = pageRepository;}
    */

    //
    /*
//    @Autowired
    private PageWriter pageWriter;
    //    @Autowired
    public StartIndexing(PageWriter pageWriter) { this.pageWriter = pageWriter;}
     */
    //

}   // Закрывающая класс скобка - нужна!!!

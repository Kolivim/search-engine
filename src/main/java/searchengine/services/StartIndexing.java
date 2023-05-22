package searchengine.services;

import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import searchengine.model.Site;

import java.util.concurrent.Callable;
import java.util.concurrent.ForkJoinPool;

@NoArgsConstructor
public class StartIndexing implements Callable<Boolean>
{

    private SiteRepository siteRepository;

    private PageRepository pageRepository;

    @Autowired
    public StartIndexing(SiteRepository siteRepository) {this.siteRepository = siteRepository;}
    @Autowired
    public StartIndexing(PageRepository pageRepository) {this.pageRepository = pageRepository;}


    Site siteDB;
    public StartIndexing(Site siteDB){this.siteDB = siteDB;}


    @Override
    public Boolean call() throws Exception
    {

        /*
        for (searchengine.model.Site siteDB : siteRepository.findAll())
        {
            new ForkJoinPool().invoke(new PageWriter(siteDB, pageRepository, siteRepository));
        }
        */

        /*
        Iterable<searchengine.model.Site> siteIterable = siteRepository.findAll();
        for (searchengine.model.Site siteDB : siteIterable)
            {
                new ForkJoinPool().invoke(new PageWriter(siteDB, pageRepository, siteRepository));
            }
        */


        new ForkJoinPool().invoke(new PageWriter(siteDB, pageRepository, siteRepository));

        return true;
    }
}

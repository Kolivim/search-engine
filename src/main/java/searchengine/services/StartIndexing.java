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
    private SiteRepository siteRepository;
    Site siteDB;
    public StartIndexing(Site siteDB)
    {
        this.siteDB = siteDB;
    }

    @Override
    public Boolean call() throws Exception
    {
        siteRepository = (SiteRepository) SpringUtils.ctx.getBean(SiteRepository.class);
        new ForkJoinPool().invoke(new PageWriter(siteDB));
        System.out.println
                ("\nСайт " + siteDB.getUrl() + " - заверешение метода SIndexing и возврат boolean call()"
                        + " , количество страниц по сайту = " + siteDB.getPages().size()                                    // * (Debug)
                );
        siteDB.setStatus(StatusType.INDEXED);
        siteRepository.save(siteDB);
        return true;
    }
}
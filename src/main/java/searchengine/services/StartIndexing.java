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
    private Site siteDB;    // p
    private ForkJoinPool forkJoinPool = new ForkJoinPool(); // p

    public StartIndexing(Site siteDB)
    {
        this.siteDB = siteDB;
    }

    @Override
    public Boolean call() throws Exception
    {
        siteRepository = (SiteRepository) SpringUtils.ctx.getBean(SiteRepository.class);
//        new ForkJoinPool().invoke(new PageWriter(siteDB));
        forkJoinPool.invoke(new PageWriter(siteDB));
        System.out.println
                ("\nСайт " + siteDB.getUrl() + " - заверешение метода SIndexing и возврат boolean call()"
                        + " , количество страниц по сайту = " + siteDB.getPages().size()                                    // * (Debug)
                );
        siteDB.setStatus(StatusType.INDEXED);
        siteRepository.save(siteDB);


        // 29
        if (Thread.currentThread().isInterrupted())
            {
                System.out.println("Вызвана остановка потока в методе call() класса StartIndexing для сайта: " + siteDB.getUrl());
                return false;
            }
        //

        return true;
    }

    public void cancel()
    {
        forkJoinPool.shutdownNow();
        System.out.println("Вызван метод cancel() в классе StartIndexing"); // *
    }
}
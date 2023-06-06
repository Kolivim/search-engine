package searchengine.services;

import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import searchengine.model.Site;
import searchengine.model.StatusType;

import java.util.concurrent.Callable;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;

@NoArgsConstructor
public class StartIndexing implements Callable<Boolean>
{
    private IndexingService indexingService;
    private SiteRepository siteRepository;
    private Site siteDB;    // p
    private ForkJoinPool forkJoinPool = new ForkJoinPool(); // p
    private boolean isIndexingStarted = true;

    public StartIndexing(Site siteDB)
    {
        this.siteDB = siteDB;
        indexingService = (IndexingService) SpringUtils.ctx.getBean(IndexingServiceImpl.class);
    }
    public void setIndexingStarted(boolean indexingStarted) {
        isIndexingStarted = indexingStarted;
    }

    @Override
    public Boolean call() throws Exception
    {
        siteRepository = (SiteRepository) SpringUtils.ctx.getBean(SiteRepository.class);
//        new ForkJoinPool().invoke(new PageWriter(siteDB));
        forkJoinPool.invoke(new PageWriter(siteDB));
        System.err.println ("\n\nКласс StartIndexing, сайт " + siteDB.getUrl() + " - после forkJoinPool.invoke(new PageWriter(siteDB)) класса SIndexing"
                        + " , количество страниц по сайту = " + siteDB.getPages().size());  // * (Debug)

        //
        while (forkJoinPool.getActiveThreadCount()!=0){ Thread.sleep(1500);}
        //

        // 29
        if (Thread.currentThread().isInterrupted())
            {
                siteDB.setStatus(StatusType.FAILED);
                siteRepository.save(siteDB);
                System.err.println("\nВыполнено изменение статуса сайта: " + siteDB.getUrl() + " , на: " + siteDB.getStatus());
                System.err.println("\n\n\nВызвана остановка потока в методе call() класса StartIndexing для сайта: " + siteDB.getUrl() +
                        " ,выполнено изменение статуса сайта: " + siteDB.getUrl() + " , на: " + siteDB.getStatus());
                return false;
            }
        //

        isIndexingStarted = indexingService.getIndexingStarted();
        if(isIndexingStarted)
        {
            siteDB.setStatus(StatusType.INDEXED);
            siteRepository.save(siteDB);
            System.err.println("\nВыполнено изменение статуса сайта: " + siteDB.getUrl() + " , на: " + siteDB.getStatus());
        }
        //

        System.err.println("\nStartIndexing / call() у сайта " + siteDB.getUrl() + " - forkJoinPool.getActiveThreadCount()=" + forkJoinPool.getActiveThreadCount());

        // june 11
        /*
        boolean isTerminatedPool = forkJoinPool.isTerminated();
        while (!forkJoinPool.isTerminated()) { System.err.println("\n\n\nВ классе SIndexing методе call результат forkJoinPool.isTerminated() isTerminatedPool: " + forkJoinPool.isTerminated());};
        boolean isTerminatedPoolIng = forkJoinPool.isTerminating();
        while (!forkJoinPool.isTerminating()) { System.err.println("\n\n\nВ классе SIndexing методе call результат forkJoinPool.isTerminating() isTerminatedPoolIng: " + forkJoinPool.isTerminating());};
        System.err.println("\n\n\nВ классе SIndexing методе call результат forkJoinPool.isTerminated() isTerminatedPool: " + isTerminatedPool +
                    " , В классе SIndexing методе call результат forkJoinPool.isTerminating() isTerminatedPoolIng: " + isTerminatedPoolIng);
        */
        //

        // june 11
        /*
        try
        {
            boolean isTerminatedFJP = forkJoinPool.awaitTermination(1, TimeUnit.HOURS);
            System.err.println("\n\n\nВ классе SIndexing методе call результат forkJoinPool.awaitTermination isTerminatedFJP: " + isTerminatedFJP);
        } catch (InterruptedException e)
        {
            System.err.println("В классе SIndexing методе call вызове forkJoinPool.awaitTermination сработал InterruptedException(e) ///1 " +
                    e.getMessage() + " ///2 " + e.getStackTrace() + " ///3 " + e.getSuppressed() + " ///4 " + e.getCause() + " ///5 " +
                    e.getLocalizedMessage() + " ///6 " + e.getClass() + " ///7 на сайте:  " + siteDB.getUrl());
        }
        */
        //

        System.out.println ("\nСайт " + siteDB.getUrl() + " - Конец метода call() и возвращение true класса StartIndexing"
                + " , количество страниц по сайту = " + siteDB.getPages().size());  // * (Debug)
        return true;
    }

    public void cancel()
    {
        System.out.println("Вызван метод cancel() в классе StartIndexing у сайта: " + siteDB.getUrl()); // *
        isIndexingStarted = false;
        System.err.println("\nStartIndexing / cancel() до forkJoinPool.shutdownNow() у сайта " + siteDB.getUrl() + " - forkJoinPool.getActiveThreadCount()=" + forkJoinPool.getActiveThreadCount());
        //
        forkJoinPool.shutdownNow();
        //
        System.err.println("\nStartIndexing / cancel() после forkJoinPool.shutdownNow() у сайта " + siteDB.getUrl() + " - forkJoinPool.getActiveThreadCount()=" + forkJoinPool.getActiveThreadCount());
        //
        // 11 june
        try
        {
            boolean isTerminatedFJP = forkJoinPool.awaitTermination(1, TimeUnit.DAYS);
            System.out.println("В классе SIndexing методе cancel() результат forkJoinPool.awaitTermination isTerminatedFJP: " + isTerminatedFJP);
        } catch (InterruptedException e)
            {
                System.err.println("В классе SIndexing методе cancel вызове forkJoinPool.awaitTermination сработал InterruptedException(e) ///1 " + e.getMessage() + " ///2 " + e.getStackTrace() + " ///3 " + e.getSuppressed() + " ///4 " + e.getCause() + " ///5 " + e.getLocalizedMessage() + " ///6 " + e.getClass() + " ///7 на сайте:  " + siteDB.getUrl());
            }
        //

        System.out.println("Завершен метод cancel() в классе StartIndexing у сайта: " + siteDB.getUrl()); // *
    }
}
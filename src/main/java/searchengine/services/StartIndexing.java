package searchengine.services;

import lombok.NoArgsConstructor;

import lombok.extern.slf4j.Slf4j;
import searchengine.model.Site;
import searchengine.model.StatusType;
import searchengine.repository.SiteRepository;

import java.util.concurrent.Callable;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;

@Slf4j
@NoArgsConstructor
public class StartIndexing implements Callable<Boolean> {
    private IndexingService indexingService;
    private SiteRepository siteRepository;
    private Site siteDB;
    private ForkJoinPool forkJoinPool = new ForkJoinPool();
    private boolean isIndexingStarted = true;

    public StartIndexing(Site siteDB) {
        this.siteDB = siteDB;
        indexingService = (IndexingService) SpringUtils.ctx.getBean(IndexingServiceImpl.class);
    }

    public void setIndexingStarted(boolean indexingStarted) {
        isIndexingStarted = indexingStarted;
    }

    @Override
    public Boolean call() throws Exception {
        siteRepository = (SiteRepository) SpringUtils.ctx.getBean(SiteRepository.class);

        forkJoinPool.invoke(new PageWriter(siteDB));
        log.debug("В методе call() - сайт: {} после после forkJoinPool.invoke(new PageWriter(siteDB)) " +
                "класса SIndexing, количество страниц по сайту: {}", siteDB.getUrl(), siteDB.getPages().size());

        while (forkJoinPool.getActiveThreadCount() != 0) {
            Thread.sleep(1500);
        }

        if (Thread.currentThread().isInterrupted()) {
            siteDB.setStatus(StatusType.FAILED);
            siteDB.setLastError("Индексация остановлена пользователем");
            siteRepository.save(siteDB);
            log.info("В методе call() - Вызвана остановка потока для сайта: {}, выполнено изменение статуса " +
                    "сайта на {}", siteDB.getUrl(), siteDB.getStatus());
            return false;
        }

        isIndexingStarted = indexingService.getIndexingStarted();
        if (isIndexingStarted) {
            siteDB.setStatus(StatusType.INDEXED);
            siteRepository.save(siteDB);
            log.info("В методе call() - Выполнено изменение статуса сайта: {} на {}",
                    siteDB.getUrl(), siteDB.getStatus());
        }

        log.info("Завершение метода call() - Сайт : {} - возвращение true у класса StartIndexing, " +
                        "количество страниц по сайту = {}, forkJoinPool.getActiveThreadCount() = {}",
                siteDB.getUrl(), siteDB.getPages().size(), forkJoinPool.getActiveThreadCount());

        return true;
    }

    public void cancel() {
        log.info("Вызван метод cancel() у сайта: {}", siteDB.getUrl());
        isIndexingStarted = false;

        log.debug("В методе cancel() - до forkJoinPool.shutdownNow() у сайта: {}, " +
                "forkJoinPool.getActiveThreadCount() = {}", siteDB.getUrl(), forkJoinPool.getActiveThreadCount());
        forkJoinPool.shutdownNow();
        log.debug("В методе cancel() - после forkJoinPool.shutdownNow() у сайта: {}, " +
                "forkJoinPool.getActiveThreadCount() = {}", siteDB.getUrl(), forkJoinPool.getActiveThreadCount());

        try {
            boolean isTerminatedFJP = forkJoinPool.awaitTermination(1, TimeUnit.DAYS);
            log.debug("В методе cancel() - у сайта: {} результат forkJoinPool.awaitTermination = {}, " +
                            "forkJoinPool.getActiveThreadCount() = {}",
                    siteDB.getUrl(), isTerminatedFJP, forkJoinPool.getActiveThreadCount());

        } catch (InterruptedException e) {
            log.error("В методе cancel() - при вызове forkJoinPool.awaitTermination сработал " +
                    "InterruptedException: {}, у сайта: {}", e, siteDB.getUrl());
        }

        log.info("Завершен метод cancel() у сайта: {}", siteDB.getUrl());
    }
}
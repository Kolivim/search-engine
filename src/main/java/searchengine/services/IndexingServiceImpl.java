package searchengine.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.model.StatusType;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;

import java.util.*;
import java.util.concurrent.*;

@Slf4j
@Service
public class IndexingServiceImpl implements IndexingService {
    private SiteRepository siteRepository;
    private PageRepository pageRepository;
    private LemmatizationService lemmatizationService;
    boolean isIndexingStarted = false;
    private final SitesList sites;
    private ResultCheckerParse resultCheckerExample;
    private Stack<RunnableFuture<Boolean>> taskList = new Stack<>();
    private List<StartIndexing> listStartIndexing = new ArrayList<>();
    private ExecutorService executor;

    @Autowired
    public IndexingServiceImpl(SiteRepository siteRepository, PageRepository pageRepository,
                               SitesList sites, LemmatizationService lemmatizationService) {
        this.sites = sites;
        this.siteRepository = siteRepository;
        this.pageRepository = pageRepository;
        this.lemmatizationService = lemmatizationService;
    }

    @Override
    public boolean startIndexing() {
        log.info("Запущен метод startIndexing");
        if (!isIndexingStarted) {
            isIndexingStarted = true;

            removeSites();
            saveSites();

            Iterable<searchengine.model.Site> siteIterable = siteRepository.findAll();
            for (searchengine.model.Site siteDB : siteIterable) {
                StartIndexing value = new StartIndexing(siteDB);
                RunnableFuture<Boolean> futureValue = new FutureTask<>(value);
                listStartIndexing.add(value);
                taskList.add(futureValue);
                log.info("Выполнено добавление сайта: {} в FJP/Future", siteDB);
            }

            int countThread = sites.getSites().size()+1;
            executor = Executors.newFixedThreadPool(countThread);
            taskList.forEach(executor::execute);

            resultCheckerExample = new ResultCheckerParse(taskList);
            executor.execute(resultCheckerExample);
            executor.shutdown();

            log.info("Завершение выполнившегося метода startIndexing в классе IndexingServiceImp");
            return true;
        } else {
            log.error("ISImpl - Ошибка: Индексация уже запущена");
            return false;
        }
    }

    public void removeSites() {
        for (Site site : sites.getSites()) {
            log.info("Запуск метода removeSites(), передан SitesList: {}", sites);
            Iterable<searchengine.model.Site> siteIterable = siteRepository.findAll();
            for (searchengine.model.Site siteDB : siteIterable) {
                if (site.getUrl().equals(siteDB.getUrl())) {
                    lemmatizationService.deleteSiteIndexAndLemma(siteDB);
                    siteRepository.delete(siteDB);
                    log.info("Выполнено удаление сайта: {}", site.getUrl());
                }
            }
        }
    }

    public void saveSites() {
        for (Site site : sites.getSites()) {
            searchengine.model.Site siteDB = new searchengine.model.Site();
            siteDB.setName(site.getName());
            siteDB.setUrl(site.getUrl());
            siteDB.setStatus(StatusType.INDEXING);
            siteDB.setStatusTime(new Date());
            siteRepository.save(siteDB);
            log.info("Выполнено сохранение в БД сайта: {}", site);
        }
    }

    @Override
    public boolean stopIndexing() {
        log.info("Запущен метод stopIndexing");
        if (isIndexingStarted) {
            setSitesStatus(StatusType.INDEXING, StatusType.FAILED, "Индексация остановлена пользователем");
            isIndexingStarted = false;
            resultCheckerExample.setIndexingStopped(true);
            for (StartIndexing value : listStartIndexing) {
                value.cancel();
            }
            log.info("В методе stopIndexing() - выполнена передача значения true сеттеру setIndexingStopped");
            List<Runnable> notExecuted = executor.shutdownNow();
            log.info("Завершение метода stopIndexing(), значение isIndexingStarrted: {}, " +
                    "лист невыполненных задач: {}", isIndexingStarted, notExecuted);
            return true;
        } else {
            return false;
        }
    }

    public void setSitesStatus(StatusType statusTypeBefore, StatusType statusTypeAfter, String info) {
        Iterable<searchengine.model.Site> siteIterable = siteRepository.findAll();
        for (searchengine.model.Site siteDB : siteIterable) {
            if (siteDB.getStatus().equals(statusTypeBefore)) {
                siteDB.setStatus(statusTypeAfter);
                siteDB.setLastError(info);
                siteRepository.save(siteDB);
                log.info("В методе setSitesStatus() выполнено присвоение сайту {} статуса индексации: {}",
                        siteDB, siteDB.getStatus());
            }
        }
    }

    @Override
    public void setIndexingStarted(boolean indexingStarted) {
        isIndexingStarted = indexingStarted;
    }

    @Override
    public boolean getIndexingStarted() {
        return isIndexingStarted;
    }
}
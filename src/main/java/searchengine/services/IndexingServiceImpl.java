package searchengine.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.model.StatusType;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;

@Service
public class IndexingServiceImpl implements IndexingService
{
//    @Autowired
    private SiteRepository siteRepository;
    //    @Autowired
    private PageRepository pageRepository;

    @Autowired
    public IndexingServiceImpl(SiteRepository siteRepository, PageRepository pageRepository, SitesList sites) {this.siteRepository = siteRepository;this.pageRepository = pageRepository;
//        this.startIndexing = startIndexing;
        this.sites = sites;
    }

    // Flag from indexing status in API
    boolean isIndexingStarted = false;
    private final SitesList sites;
    ResultCheckerParse resultCheckerExample;    //1
    Stack<RunnableFuture<Boolean>> taskList = new Stack<>(); //29
    ExecutorService executor;   //29

    @Override
    public void startIndexing()
    {
        // Блок Б1
        System.out.println("\nЗапущен метод startIndexing");
        isIndexingStarted = true;

        // Блок Б2 с удалением сайта из таблицы sites
        for (Site site:sites.getSites())
        {
            System.out.println(site);
            Iterable<searchengine.model.Site> siteIterable = siteRepository.findAll();
            for (searchengine.model.Site siteDB : siteIterable)
                {
                    if(site.getUrl().equals(siteDB.getUrl()))
                        {
                        siteRepository.delete(siteDB);
                        System.out.println("\nВыполнено удаление сайта: " + site.getUrl());
                        }
                }
        }

        // Блок Б3.2/2
        for (Site site:sites.getSites())
        {
            searchengine.model.Site siteDB = new searchengine.model.Site();
            siteDB.setName(site.getName());
            siteDB.setUrl(site.getUrl());
            siteDB.setStatus(StatusType.INDEXING);
            siteDB.setStatusTime(new Date());
            siteRepository.save(siteDB);

            System.out.println("Выполнено сохранение сайта " + site);   //*
        }

//        Stack<RunnableFuture<Boolean>> taskList = new Stack<>();  //29
        Iterable<searchengine.model.Site> siteIterable = siteRepository.findAll();
        for (searchengine.model.Site siteDB : siteIterable)
        {
            ///*
            //Вар.1
            RunnableFuture<Boolean> futureValue = new FutureTask<>(new StartIndexing(siteDB));
            taskList.add(futureValue);
            System.out.println("\nВыполнено добавление сайта " + siteDB + " в FJP/Future");
            //*/

            /*
            // Вар.2
            RunnableFuture<Boolean> futureValue = null;
            try
            {
                futureValue = new FutureTask<>(new ForkJoinPool().invoke(new PageWriter(siteDB)));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            taskList.add(futureValue);
            System.out.println("\nВыполнено добавление сайта " + siteDB + " в FJP/Future");
            */
        }

//        ExecutorService executor = Executors.newFixedThreadPool(4); // //29
        executor = Executors.newFixedThreadPool(4); // TODO : Внести количество потоков с привязкой к количеству сайтов
        taskList.forEach(executor::execute);

//        ResultCheckerParse resultCheckerExample = new ResultCheckerParse(taskList);   //1 рабочее
        resultCheckerExample = new ResultCheckerParse(taskList);  //1 рабочее
//        resultCheckerExample = new ResultCheckerParse();

        executor.execute(resultCheckerExample);

        executor.shutdown();

//        isIndexingStarted = false; // Завершать нужно по завершению потоков - RCP

        System.out.println("\nЗавершение метода startIndexing в классе IndexingServiceImpl");
    }

    @Override
    public void stopIndexing()
    {
        System.out.println("\nЗапущен метод stopIndexing в классе IndexingServiceImpl");
        if(isIndexingStarted)
            {
                resultCheckerExample.setIndexingStopped(true);
                System.out.println("\nВыполнена передача значения true сеттеру setIndexingStopped");
                List<Runnable> notExecuted = executor.shutdownNow();
                System.out.println("\nЛист невыполненных задач: " + notExecuted);
            }
        isIndexingStarted = false;

    }
    @Override
    public void setIndexingStarted(boolean indexingStarted) {isIndexingStarted = indexingStarted;}  //28
    @Override
    public boolean getIndexingStarted(){return isIndexingStarted;}

}

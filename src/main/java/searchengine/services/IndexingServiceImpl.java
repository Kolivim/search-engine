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

    boolean isIndexingStarted = false;    // Flag from indexing status in API
    private final SitesList sites;
    ResultCheckerParse resultCheckerExample;    //1
    Stack<RunnableFuture<Boolean>> taskList = new Stack<>(); // 29
    List<StartIndexing> listStartIndexing = new ArrayList<>(); //    5.06
    ExecutorService executor;   //29

    @Autowired
    public IndexingServiceImpl(SiteRepository siteRepository, PageRepository pageRepository, SitesList sites)
    {
        this.sites = sites;
        this.siteRepository = siteRepository;
        this.pageRepository = pageRepository;
//        this.startIndexing = startIndexing;
    }

    @Override
    public boolean startIndexing()
    {
        // Блок Б1
        System.out.println("\nЗапущен метод startIndexing");

        if(!isIndexingStarted)
        {
            isIndexingStarted = true;

            // Блок Б2 с удалением сайта из таблицы sites
            for (Site site : sites.getSites()) {
                System.out.println(site);
                Iterable<searchengine.model.Site> siteIterable = siteRepository.findAll();
                for (searchengine.model.Site siteDB : siteIterable) {
                    if (site.getUrl().equals(siteDB.getUrl())) {
                        siteRepository.delete(siteDB);
                        System.out.println("\nВыполнено удаление сайта: " + site.getUrl());
                    }
                }
            }

            // Блок Б3.2/2
            for (Site site : sites.getSites()) {
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

//            try {
//                ForkJoinTask<?> result = new ForkJoinTask<?>().adapt((Runnable) new PageWriter(siteDB));
//            } catch (IOException e) {
//                throw new RuntimeException(e);
//            }

            /* Рабочий!!!
            //Вар.1
            RunnableFuture<Boolean> futureValue = new FutureTask<>(new StartIndexing(siteDB));
            taskList.add(futureValue);
            System.out.println("\nВыполнено добавление сайта " + siteDB + " в FJP/Future");
            */

                // /*
                //Вар.3
                StartIndexing value = new StartIndexing(siteDB);
                RunnableFuture<Boolean> futureValue = new FutureTask<>(value);
                listStartIndexing.add(value);
                taskList.add(futureValue);
                System.out.println("\nВыполнено добавление сайта " + siteDB + " в FJP/Future");
//            */

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

            System.out.println("\nЗавершение выполнившегося метода startIndexing в классе IndexingServiceImpl");    //*
            return true;
        } else
            {
                System.out.println("\nISImpl - Ошибка: Индексация уже запущена");   //*
                return false;
            }
    }

    @Override
    public boolean stopIndexing()
    {
        System.out.println("\nЗапущен метод stopIndexing в классе IndexingServiceImpl");
        if(isIndexingStarted)
            {
                // Блок на пробу с присвоением indexingStatus SiteDB
//               /*
               Iterable<searchengine.model.Site> siteIterable = siteRepository.findAll();
                for (searchengine.model.Site siteDB : siteIterable)
                {
                    if(siteDB.getStatus().equals(StatusType.INDEXING))
                    {
                        siteDB.setStatus(StatusType.FAILED);
                        siteRepository.save(siteDB);
                        System.out.println("\nВ классе IndexingServiceImpl в методе stopIndexing() выполнено изменение статуса сайта: " + siteDB.getUrl() + " , на: " + siteDB.getStatus());
                    }
                }
//                */

                // June 11
                isIndexingStarted = false;
                //
                //
                resultCheckerExample.setIndexingStopped(true);
                //
                for (StartIndexing value : listStartIndexing)
                {
                    value.cancel();
                }  //  5.06
                //

                System.out.println("\nКласс IndexingServImp метод stopIndexing - выполнена передача значения true сеттеру setIndexingStopped");
                List<Runnable> notExecuted = executor.shutdownNow();

                // 11 june
//                try
//                {
//                    executor.awaitTermination(1, TimeUnit.DAYS);
//                } catch (InterruptedException e)
//                {
//                    System.err.println("В классе IndexingЫукмШьзд методе compute сработал InterruptedException(e) ///1 " + e.getMessage() + " ///2 " + e.getStackTrace() + " ///3 " + e.getSuppressed() + " ///4 " + e.getCause() + " ///5 " + e.getLocalizedMessage() + " ///6 " + e.getClass() + " ///7 на .....:  ");
//                }
                //

                System.out.println("\nЛист невыполненных задач: " + notExecuted);

                //
//                isIndexingStarted = false;  // перенести внутрь остановки ???
                System.out.println("\nВ классе IndexingServImpl завершение метода stopIndexing(), значение isIndexingStarrted=" + isIndexingStarted);
                //

                return true;
            } else {return false;}
    }

    @Override
    public void setIndexingStarted(boolean indexingStarted) {isIndexingStarted = indexingStarted;}  //28
    @Override
    public boolean getIndexingStarted(){return isIndexingStarted;}

}

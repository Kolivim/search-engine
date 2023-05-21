package searchengine.services;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.apache.catalina.mbeans.DataSourceUserDatabaseMBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.support.SimpleJpaRepository;
import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.model.Page;
import searchengine.model.StatusType;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;

@Service
@RequiredArgsConstructor
public class IndexingServiceImpl implements IndexingService
{
    @Autowired
    private SiteRepository siteRepository;
    @Autowired
    private PageRepository pageRepository;

    boolean indexingStarted = false; // Нужна ли?

    private final SitesList sites;
//    private final SimpleJpaRepository simpleJpaRepository; 16 мая - ЧТО ЭТО???

    //    private ArrayList<PageWriter> pageWriterList = new ArrayList<>();
    @Override
    public void startIndexing()
    {
//        pageWriterList.clear();

        // Блок Б1
        System.out.println("\nЗапущен метод startIndexing");
        indexingStarted = true;
        // Проверка связи с таблицей sites и pages:
        /*
        Iterable<searchengine.model.Site> siteIterables = siteRepository.findAll();
        for (searchengine.model.Site siteDB : siteIterables)
        {
            System.out.println("\nСайт с ДБ: " + siteDB.getUrl());
        }

        Iterable<searchengine.model.Page> pageIterables = pageRepository.findAll();
        for (searchengine.model.Page page : pageIterables)
        {
            System.out.println("\nСтраница сайта с ДБ: " + page.getPath() + ", сайт номер: " + page.getSiteId());
        }
        */

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

        /*
        // Блок Б3: многопоточного запуска обхода сайтов  - НЕи рабочий
        ExecutorService executorService = Executors.newFixedThreadPool(sites.getSites().size()); // Executors.newSingleThreadExecutor();
        List<Future<searchengine.model.Site>> callFuture = new ArrayList<>();


        for (Site site:sites.getSites())
        {
            searchengine.model.Site siteDB = new searchengine.model.Site();
            callFuture.add(
                    CompletableFuture.supplyAsync(
                            () ->
                            {
                                siteDB.setName(site.getName());
                                siteDB.setUrl(site.getUrl());
                                siteDB.setStatus(StatusType.INDEXING);
                                siteDB.setStatusTime(new Date()); // + siteRepository.save(siteDB); ???
                                System.out.println("Site обход в ES без добавления в БД: " + site.getName() + " - " + new Date());
                                return siteDB;
                            },
                            executorService
                    ));
        }

        for(Future<searchengine.model.Site> valueFuture : callFuture)
        {
            try {
                siteRepository.save(valueFuture.get());

                PageWriter pageWriterValue = new PageWriter(pageRepository, siteRepository);
                pageWriterValue.startPageSearch(valueFuture.get());
                System.out.println("Class IndServImpl: Возврат page в ForkJoinPool в ES: " + pageWriterValue.toString());
//              //  Page page = new ForkJoinPool().invoke(new PageWriter(valueFuture.get(), pageRepository, siteRepository));
//              //  System.out.println("Class IndServImpl: Возврат page в ForkJoinPool в ES: " + page.toString());

            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            } catch (ExecutionException e) {
                throw new RuntimeException(e);
            }

        }
        executorService.shutdown();
        //
        */

        // Блок Б3.2 с new Thread - Рабочий
/*        new Thread(()->{
            for (int i = 1; i <= sites.getSites().size(); i++)
            {
                for (Site site:sites.getSites())
                {
                    searchengine.model.Site siteDB = new searchengine.model.Site();
                    siteDB.setName(site.getName());
                    siteDB.setUrl(site.getUrl());
                    siteDB.setStatus(StatusType.INDEXING);
                    siteDB.setStatusTime(new Date()); // + siteRepository.save(siteDB); ??
                    siteRepository.save(siteDB);

                    //PageWriter pageWriterValue = new PageWriter(pageRepository, siteRepository); // норм
                    PageWriter pageWriterValue = new PageWriter(pageRepository, siteRepository);
                    pageWriterValue.setPageWriter(pageWriterValue);
                    pageWriterValue.startPageSearch(siteDB);

//                    pageWriterList.add(pageWriterValue);

                    System.out.println("Class IndServImpl: Возврат page в ForkJoinPool в new Thread: " + pageWriterValue.toString());
                }
            }
        }).start();*/

        // Блок Б3.2/1 с new Thread - Рабочий
        for (Site site:sites.getSites())
            {
                searchengine.model.Site siteDB = new searchengine.model.Site();
                siteDB.setName(site.getName());
                siteDB.setUrl(site.getUrl());
                siteDB.setStatus(StatusType.INDEXING);
                siteDB.setStatusTime(new Date());
                siteRepository.save(siteDB);

                System.out.println("Выполнено сохранение сайта " + site);
            }

        new Thread(()->
            {
                for (searchengine.model.Site siteDB:siteRepository.findAll())
                    {

                        /*
                        Page pageSearch = new Page();
                        pageSearch.setSite(siteDB);
                        pageSearch.setSiteId(siteDB.getId());
                        pageSearch.setPath(siteDB.getUrl()); // 16 мая 23
                        new ForkJoinPool().invoke(new PageWriter(pageSearch, pageRepository, siteRepository, siteDB.getUrl()));
                        */

                        try {
                            new ForkJoinPool().invoke(new PageWriter(siteDB, pageRepository, siteRepository));
                        } catch (IOException e) {
//                            throw new RuntimeException(e);
                            System.err.println("Cработал в IndexingServiceImpl в new Thread(()-> : IOException / RuntimeException(e)" + e.getMessage());
                        }
                        //                    pageWriterList.add(pageWriterValue);

                                System.out.println("Class IndServImpl: Возврат page в ForkJoinPool в new Thread: по " + siteDB);
                    }

            }).start();




        // Блок Б3 В1: Воднопоточном режиме - рабочий
        /*
        for (Site site:sites.getSites())
        {
            searchengine.model.Site siteDB = new searchengine.model.Site();
            siteDB.setName(site.getName());
            siteDB.setUrl(site.getUrl());
            siteDB.setStatus(StatusType.INDEXING);
            siteDB.setStatusTime(new Date());
            siteRepository.save(siteDB);
            System.out.println("Время добавления сайта " + site.getName() + " - " + new Date());

            // Блок Б4: С FJP по сайтам
//            Page pageValues = new Page();
//            pageValues.setSite(siteDB);
//            Set<Page> mapSite = new ForkJoinPool().invoke(new PageWriter(siteDB)); //Запуск потоков
//            Page page = new ForkJoinPool().invoke(new PageWriter(siteDB)); //Запуск потоков
            Page page = new ForkJoinPool().invoke(new PageWriter(siteDB, pageRepository, siteRepository)); //Запуск потоков
//            pageRepository.save(page); // Сохранение в БД !!!
            System.out.println("Class IndServImpl: Возврат page в ForkJoinPool : " + page.toString());
        }
        */

    }

    @Override
    public void stopIndexing()
    {
        System.out.println("\nЗапущен метод stopIndexing");
        indexingStarted = false;

        /*
        for(PageWriter pageWriterValue : pageWriterList)
            {
              pageWriterValue.setIndexingStarted(indexingStarted);
            }
        */
    }

    @Override
    public boolean getIndexingStarted(){return indexingStarted;}

}

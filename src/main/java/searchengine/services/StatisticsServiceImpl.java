package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.dto.statistics.DetailedStatisticsItem;
import searchengine.dto.statistics.StatisticsData;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.dto.statistics.TotalStatistics;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class StatisticsServiceImpl implements StatisticsService
{
    //
    @Autowired
    private final SiteRepository siteRepository;
    @Autowired
    private final PageRepository pageRepository;
    @Autowired
    private final IndexingService indexingService;
    @Autowired
    private final LemmaRepository lemmaRepository;
    //

    private final Random random = new Random();
    private final SitesList sites;



    @Override
    public StatisticsResponse getStatistics()
    {
        String[] statuses = { "INDEXED", "FAILED", "INDEXING" };
        String[] errors =
                {
                "Ошибка индексации: главная страница сайта не доступна",
                "Ошибка индексации: сайт не доступен",
                ""
                };

        /*
        pageRepository = (PageRepository) SpringUtils.ctx.getBean(PageRepository.class);
        siteRepository = (SiteRepository) SpringUtils.ctx.getBean(SiteRepository.class);
        indexingService = (IndexingService) SpringUtils.ctx.getBean(IndexingServiceImpl.class);
        lemmaRepository = (LemmaRepository) SpringUtils.ctx.getBean(LemmaRepository.class);
        */

        List<searchengine.model.Site> sitesDataBase = siteRepository.findAll(); // List<Site> sitesList = sites.getSites();
        System.out.println("В классе StatisticsServiceImpl() получен список сайтов из БД: " + sitesDataBase);   // *

        TotalStatistics total = new TotalStatistics();
        total.setSites(sitesDataBase.size());     //total.setSites(sites.getSites().size());
        total.setIndexing(indexingService.getIndexingStarted());  //total.setIndexing(true);

        List<DetailedStatisticsItem> detailed = new ArrayList<>();
        for(searchengine.model.Site site : sitesDataBase)   //for(int i = 0; i < sitesList.size(); i++)
        {
            DetailedStatisticsItem item = new DetailedStatisticsItem(); //Site site = sitesList.get(i);
            item.setName(site.getName());
            item.setUrl(site.getUrl());

            int pages = pageRepository.countBySiteId(site.getId());     //int pages = random.nextInt(1_000);  int i = pages;    // Random now
            int lemmas = lemmaRepository.countBySiteId(site.getId());   //int lemmas = pages * random.nextInt(1_000);

            item.setPages(pages);
            item.setLemmas(lemmas);

            item.setStatus(site.getStatus().toString());    // item.setStatus(statuses[i % 3]);    //  RandomImpl now

            //
            if (site.getLastError() == null)
                {
                    item.setError("");
                } else
                    {
                        item.setError(site.getLastError());
                    }
                //item.setError(site.getLastError());   // TODO: Проверить нужна ли проверка на null ??? //
                //item.setError(errors[i % 3]);       //  RandomImpl now
            //

            item.setStatusTime(site.getStatusTime().getTime());   //item.setStatusTime(System.currentTimeMillis() - (random.nextInt(10_000)));
            total.setPages(total.getPages() + pages);
            total.setLemmas(total.getLemmas() + lemmas);
            detailed.add(item);
        }


        StatisticsResponse response = new StatisticsResponse();

        StatisticsData data = new StatisticsData();
        data.setTotal(total);
        data.setDetailed(detailed);
        response.setStatistics(data);

        response.setResult(true);

        return response;
    }
}

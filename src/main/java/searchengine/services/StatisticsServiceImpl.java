package searchengine.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import searchengine.dto.statistics.DetailedStatisticsItem;
import searchengine.dto.statistics.StatisticsData;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.dto.statistics.TotalStatistics;
import searchengine.repository.LemmaRepository;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class StatisticsServiceImpl implements StatisticsService {

    @Autowired
    private final SiteRepository siteRepository;
    @Autowired
    private final PageRepository pageRepository;
    @Autowired
    private final IndexingService indexingService;
    @Autowired
    private final LemmaRepository lemmaRepository;

    @Override
    public StatisticsResponse getStatistics() {

        List<searchengine.model.Site> sitesDataBase = siteRepository.findAll();
        log.info("В методе getStatistics() - получен список сайтов из БД: {}", sitesDataBase);

        TotalStatistics total = new TotalStatistics();
        total.setSites(sitesDataBase.size());
        total.setIndexing(indexingService.getIndexingStarted());

        List<DetailedStatisticsItem> detailed = new ArrayList<>();

        getDetailedStatistics(total, detailed, sitesDataBase);

        StatisticsResponse response = new StatisticsResponse();

        StatisticsData data = new StatisticsData();
        data.setTotal(total);
        data.setDetailed(detailed);

        response.setStatistics(data);
        response.setResult(true);

        return response;
    }

    public void getDetailedStatistics(TotalStatistics total, List<DetailedStatisticsItem> detailed,
                                      List<searchengine.model.Site> sitesDataBase) {
        for (searchengine.model.Site site : sitesDataBase) {
            int pages = pageRepository.countBySiteId(site.getId());
            int lemmas = lemmaRepository.countBySiteId(site.getId());

            DetailedStatisticsItem item = new DetailedStatisticsItem();
            item.setName(site.getName());
            item.setUrl(site.getUrl());
            item.setPages(pages);
            item.setLemmas(lemmas);
            item.setStatus(site.getStatus().toString());
            if (site.getLastError() == null) {
                item.setError("");
            } else {
                item.setError(site.getLastError());
            }
            item.setStatusTime(site.getStatusTime().getTime());
            detailed.add(item);

            total.setPages(total.getPages() + pages);
            total.setLemmas(total.getLemmas() + lemmas);
        }

    }
}

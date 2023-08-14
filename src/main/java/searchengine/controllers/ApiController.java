package searchengine.controllers;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import searchengine.dto.snippets.SnippetsResponse;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;
import searchengine.services.*;

import java.util.Map;
import java.util.TreeMap;

@Slf4j
@RestController
@RequestMapping("/api")
public class ApiController {

    @Autowired
    private SiteRepository siteRepository;
    @Autowired
    private PageRepository pageRepository;
    @Autowired
    private MessageService messageService;
    private Map<String, String> messageList = new TreeMap<>();
    private final IndexingService indexingService;
    private final StatisticsService statisticsService;
    private final LemmatizationService lemmatizationService;
    private final SearchService searchService;

    public ApiController(IndexingService indexingService, StatisticsService statisticsService,
                         LemmatizationService lemmatizationService, SearchService searchService) {
        this.indexingService = indexingService;
        this.statisticsService = statisticsService;
        this.lemmatizationService = lemmatizationService;
        this.searchService = searchService;
    }

    @GetMapping("/statistics")
    public ResponseEntity<StatisticsResponse> statistics() {
        return ResponseEntity.ok(statisticsService.getStatistics());
    }

    @GetMapping(value = "/startIndexing")
    public ResponseEntity<Map<String, String>> startIndexing() {
        boolean isIndexing = indexingService.startIndexing();
        if (isIndexing) {
            return messageService.startIndexingOk();
        } else {
            return messageService.startIndexingError();
        }
    }

    @GetMapping(value = "/stopIndexing")
    public ResponseEntity<Map<String, String>> stopIndexing() {
        boolean isIndexing = indexingService.stopIndexing();
        if (isIndexing) {
            return messageService.stopIndexingOk();
        } else {
            return messageService.stopIndexingError();
        }
    }

    @PostMapping(value = "/indexPage", consumes = {MediaType.APPLICATION_FORM_URLENCODED_VALUE})
    public ResponseEntity<Map<String, String>> indexPage(@RequestParam Map<String, String> map) {
        String url = map.get("url");
        boolean isExistPage = lemmatizationService.indexPage(url);
        if (isExistPage) {
            return messageService.indexPageOk();
        } else {
            return messageService.indexPageError();
        }
    }

    @GetMapping(value = "/search")
    public ResponseEntity<?> search(String query, int offset, int limit, String site) {
        log.info("Запуск метода search() - передано значение: \"{}\" , offset = {} , " +
                "limit = {}, site = {}", query, offset, limit, site);
        SnippetsResponse responce = searchService.startSearch(query, offset, limit, site);
        boolean isSearchSuccess = responce.isResult();
        if (isSearchSuccess) {
            return ResponseEntity.ok(responce);
        } else {
            return messageService.searchError(responce);
        }
    }
}
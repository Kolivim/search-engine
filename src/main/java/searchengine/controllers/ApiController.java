package searchengine.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import searchengine.dto.snippets.SnippetsResponce;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.services.*;

import java.util.Map;
import java.util.TreeMap;

@RestController
@RequestMapping("/api")
public class ApiController {

    @Autowired
    private SiteRepository siteRepository;  // My

    @Autowired
    private PageRepository pageRepository;  // My

    @Autowired
    private MessageService messageService;  // My

    private Map<String,String> messageList = new TreeMap<>();   // My - Убрать после переписи StartIndexing()
    private final IndexingService indexingService;  // My
    private final StatisticsService statisticsService;
    private final LemmatizationService lemmatizationService; // LEMMA STAGE
    private final SearchService searchService; // SEARCH STAGE

    public ApiController(IndexingService indexingService, StatisticsService statisticsService,
                         LemmatizationService lemmatizationService, SearchService searchService)
    {
        this.indexingService = indexingService; // My
        this.statisticsService = statisticsService;
        this.lemmatizationService = lemmatizationService; // LEMMA STAGE
        this.searchService = searchService;
    }

    @GetMapping("/statistics")
    public ResponseEntity<StatisticsResponse> statistics()
    {
        return ResponseEntity.ok(statisticsService.getStatistics());
    }

    /// My ///
    @GetMapping(value = "/startIndexing" /* , produces = MediaType.APPLICATION_JSON_VALUE */ )
    public ResponseEntity<Map<String,String>> startIndexing()
    {
        StatisticsResponse response = statisticsService.getStatistics();    // Убрать м.б.?
        boolean isIndexing = indexingService.startIndexing();
        if (isIndexing)
            {return messageService.startIndexingOk();}
                else
                    {return messageService.startIndexingError();}
    }

    @GetMapping(value = "/stopIndexing")
    public ResponseEntity<Map<String,String>> stopIndexing()
    {
        StatisticsResponse response = statisticsService.getStatistics();    // Убрать отсюда?
        boolean isIndexing = indexingService.stopIndexing();
        if (isIndexing)
            {return messageService.stopIndexingOk();}
                else
                    {return messageService.stopIndexingError();}
    }

    @PostMapping(value = "/indexPage", consumes = {MediaType.APPLICATION_FORM_URLENCODED_VALUE}/*, consumes = {MediaType.APPLICATION_JSON_VALUE}*//*, produces = "application/json;charset=UTF-8"*/)   // TODO: Проверить метод !!!
    public ResponseEntity<Map<String,String>> indexPage(@RequestParam Map<String, String> map)   // String url
    {
        String url = map.get("url");
        boolean isExistPage = lemmatizationService.indexPage(url);
        if (isExistPage)
            {return messageService.indexPageOk();}
                else
                    {return messageService.indexPageError();}
    }

    @GetMapping(value = "/search")
    public ResponseEntity<?> search(String query, int offset, int limit, String site)   // ResponseEntity<Map<String,String>> ; ResponseEntity<SnippetsResponce>;  @RequestParam(value="query", required=false, defaultValue="World")  //   public ResponseEntity<Map<String,String>> search(@RequestParam(value="query") String query, @RequestParam(value="offset") int offset, @RequestParam(value="limit") int limit)   // @RequestParam(value="query", required=false, defaultValue="World")
    {
        System.out.println("Передано значение: \"" + query + "\" , offset = " + offset + " , limit = " + limit + " , site = " + site);    // *

        //  4 jule:
        SnippetsResponce responce = searchService.startSearch(query, offset, limit, site);
        boolean isSearchSuccess = responce.isResult();
        if (isSearchSuccess)
            {return ResponseEntity.ok(responce);}
                else
                    {return messageService.searchError();}
        //


        //
        /*
        boolean isSearchSuccess = searchService.startSearch(query, offset, limit, site);
        if (isSearchSuccess)
            {return messageService.searchOk();}
                else
                    {return messageService.searchError();}
         */
                //

    }

}

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

//    @PostMapping(value = "/indexPage/{path}", consumes = {MediaType.APPLICATION_JSON_VALUE})   // TODO: Проверить метод !!!
//    public ResponseEntity<Map<String,String>> indexPage(@PathVariable String path)
//    {
//        boolean isExistPage = lemmatizationService.indexPage(path);
//        if (isExistPage)
//        {return messageService.indexPageOk();}
//        else
//        {return messageService.indexPageError();}
//    }


//    public ResponseEntity<ErrorMessage> startIndexing(){
//        messageList.clearMessageList();
//        messageList.addMessage("result","true");
//        return new ResponseEntity<>(messageList, HttpStatus.OK);
//    }


//    public @ResponseBody startIndexing()
//    { return "result:true";}

//        public String  startIndexing() {return "Work";}



//    public ResponseEntity<?> startIndexing() {
////        StatisticsResponse response = statisticsService.getStatistics();
////        if (!response.isResult())
////            {
////            }
////        return response.isResult()? ResponseEntity.ok(null):
////                ResponseEntity.status(HttpStatus.valueOf("Индексация уже запущена")).body(null); //badRequest ошибка запроса
//
//
////     public Map<String, String>  startIndexing()
////    {
////        Map<String, String> message = new TreeMap<>();
////        StatisticsResponse response = statisticsService.getStatistics();
////        if (response.isResult()) {
////            message.put("result", "true");
////        } else {
////            message.put("result", "false");
////            message.put("error", "Индексация уже запущена");
////        }
////        return message;
////    }



/*
    /// My ///
    @GetMapping(value = "/startIndexing"                            // Переписать аналогично StopIndexing()
//            , produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<Map<String,String>> startIndexing() {
        StatisticsResponse response = statisticsService.getStatistics();    // Убрать м.б.?
//        response.setResult(false); // Для проверки
        if (//response.isResult()
                !indexingService.getIndexingStarted()
        )
        {
            //
            indexingService.startIndexing();
            //
            messageList.put("result", "true");
            return new ResponseEntity<Map<String,String>>(messageList, HttpStatus.OK);
        }
        else
        {
            messageList.put("result", "false");
            messageList.put("error", "Индексация уже запущена");
            return new ResponseEntity<Map<String,String>>(messageList, HttpStatus.valueOf(505));
        }
    }
    */
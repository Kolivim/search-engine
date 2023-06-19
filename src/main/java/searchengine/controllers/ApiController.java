package searchengine.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import searchengine.dto.statistics.ErrorMessage;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.model.Page;
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

    public ApiController(IndexingService indexingService, StatisticsService statisticsService, LemmatizationService lemmatizationService)
    {
        this.indexingService = indexingService; // My
        this.statisticsService = statisticsService;
        this.lemmatizationService = lemmatizationService; // LEMMA STAGE
    }


    @GetMapping("/statistics")
    public ResponseEntity<StatisticsResponse> statistics()
    {
        return ResponseEntity.ok(statisticsService.getStatistics());
    }


    /// My ///
    @GetMapping(value = "/startIndexing"                            // Переписать аналогично StopIndexing()
//            , produces = MediaType.APPLICATION_JSON_VALUE
                )
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
package searchengine.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import searchengine.dto.statistics.ErrorMessage;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.services.IndexingService;
import searchengine.services.PageRepository;
import searchengine.services.SiteRepository;
import searchengine.services.StatisticsService;

import java.util.Map;
import java.util.TreeMap;

@RestController
@RequestMapping("/api")
public class ApiController {

    /// My ///
    @Autowired
    private SiteRepository siteRepository;

    @Autowired
    private PageRepository pageRepository;


    private Map<String,String> messageList = new TreeMap<>();
    private final IndexingService indexingService;

    /// ///

    private final StatisticsService statisticsService;

    public ApiController(
//            ErrorMessage messageList, /// My ///
            IndexingService indexingService, /// My ///
            StatisticsService statisticsService) {
//        this.messageList = messageList; /// My ///
        this.indexingService = indexingService; /// My ///
        this.statisticsService = statisticsService;
    }


    @GetMapping("/statistics")
    public ResponseEntity<StatisticsResponse> statistics()
    {
        return ResponseEntity.ok(statisticsService.getStatistics());
    }

    /// My ///

    @GetMapping(
            value = "/startIndexing"
//          , produces = MediaType.APPLICATION_JSON_VALUE
            )
    public ResponseEntity<Map<String,String>> startIndexing()
    {
        StatisticsResponse response = statisticsService.getStatistics();
//        response.setResult(false); // Для проверки
        if (
//              response.isResult()
                !indexingService.getIndexingStarted()
            )
        {
            // TODO: some code for start indexing
            indexingService.startIndexing();
            // Some message JSON to Front
            messageList.put("result", "true");
            return new ResponseEntity<Map<String,String>>(messageList, HttpStatus.OK);
            } else {
                    messageList.put("result", "false");
                    messageList.put("error", "Индексация уже запущена");
                    return new ResponseEntity<Map<String,String>>(messageList, HttpStatus.valueOf(505));
                    }
    }

    @GetMapping(value = "/stopIndexing")
    public ResponseEntity<Map<String,String>> stopIndexing() {
        System.out.println("\nСтарт метода: /stopIndexing в классе ApiController\n"); // *
        StatisticsResponse response = statisticsService.getStatistics();
        System.out.println("\nПроверка условия в методе /stopIndexing в классе ApiController ДО if: indexingService.getIndexingStarted() " + indexingService.getIndexingStarted() + "\n"); // *
//        response.setResult(false); // Для проверки
        if (
//                !response.isResult()
                indexingService.getIndexingStarted()
            ) {
            System.out.println("\nПроверка в методе /stopIndexing в классе ApiController - indexingService.getIndexingStarted(): " + indexingService.getIndexingStarted() + "\n"); // *
            // TODO: some code for start indexing
            indexingService.stopIndexing();
            //
            messageList.put("result", "true");
            return new ResponseEntity<Map<String,String>>(messageList, HttpStatus.OK);
        } else {
            messageList.put("result", "false");
            messageList.put("error", "Индексация не запущена");
            return new ResponseEntity<Map<String,String>>(messageList, HttpStatus.valueOf(505));
        }
    }



//    public ResponseEntity<ErrorMessage> startIndexing(){
//        messageList.clearMessageList();
//        messageList.addMessage("result","true");
//        return new ResponseEntity<>(messageList, HttpStatus.OK);
//    }


//    public @ResponseBody startIndexing()
//    { return "result:true";}

//        public String  startIndexing() {return "Work";}

}

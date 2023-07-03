package searchengine.services;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.TreeMap;

@Service
public class MessageService
{
    private Map<String,String> messageList = new TreeMap<>();

    public ResponseEntity<Map<String,String>> startIndexingOk()
    {
        messageList.clear();
        messageList.put("result", "true");
        return new ResponseEntity<Map<String,String>>(messageList, HttpStatus.OK);
    }

    public ResponseEntity<Map<String,String>> startIndexingError()
    {
        messageList.clear();
        messageList.put("result", "false");
        messageList.put("error", "Индексация уже запущена");
        return new ResponseEntity<Map<String,String>>(messageList, HttpStatus.valueOf(505));
    }

    public ResponseEntity<Map<String,String>> stopIndexingOk()
    {
        messageList.clear();
        messageList.put("result", "true");
        return new ResponseEntity<Map<String,String>>(messageList, HttpStatus.OK);
    }

    public ResponseEntity<Map<String,String>> stopIndexingError()
    {
        messageList.clear();
        messageList.put("result", "false");
        messageList.put("error", "Индексация не запущена");
        return new ResponseEntity<Map<String,String>>(messageList, HttpStatus.valueOf(505));
    }

    public ResponseEntity<Map<String,String>> indexPageOk()
    {
        messageList.clear();
        messageList.put("result", "true");
        return new ResponseEntity<Map<String,String>>(messageList, HttpStatus.OK);
    }

    public ResponseEntity<Map<String,String>> indexPageError()
    {
        messageList.clear();
        messageList.put("result", "false");
        messageList.put("error", "Данная страница находится за пределами сайтов, указанных в конфигурационном файле");
        return new ResponseEntity<Map<String,String>>(messageList, HttpStatus.valueOf(503));
    }

    public ResponseEntity<Map<String,String>> searchOk()    // TODO: Переписать на возврат требуемого значения
    {
        messageList.clear();
        messageList.put("result", "true");
        return new ResponseEntity<Map<String,String>>(messageList, HttpStatus.OK);
    }

    public ResponseEntity<Map<String,String>> searchError()    // TODO: Переписать на возврат требуемого значения
    {
        messageList.clear();
        messageList.put("result", "false");
        messageList.put("error", "Задан пустой поисковый запрос");
        messageList.put("error", "Индексация сайта не завершена");
        return new ResponseEntity<Map<String,String>>(messageList, HttpStatus.valueOf(501));
    }
}

package searchengine.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import searchengine.dto.snippets.SnippetsResponce;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

@Slf4j
@Service
public class MessageService {
    private Map<String, String> messageList = new LinkedHashMap<>();

    public ResponseEntity<Map<String, String>> startIndexingOk() {
        messageList.clear();
        messageList.put("result", "true");
        return new ResponseEntity<Map<String, String>>(messageList, HttpStatus.OK);
    }

    public ResponseEntity<Map<String, String>> startIndexingError() {
        messageList.clear();
        messageList.put("result", "false");
        messageList.put("error", "Индексация уже запущена");
        return new ResponseEntity<Map<String, String>>(messageList, HttpStatus.valueOf(505));
    }

    public ResponseEntity<Map<String, String>> stopIndexingOk() {
        messageList.clear();
        messageList.put("result", "true");
        return new ResponseEntity<Map<String, String>>(messageList, HttpStatus.OK);
    }

    public ResponseEntity<Map<String, String>> stopIndexingError() {
        messageList.clear();
        messageList.put("result", "false");
        messageList.put("error", "Индексация не запущена");
        return new ResponseEntity<Map<String, String>>(messageList, HttpStatus.valueOf(505));
    }

    public ResponseEntity<Map<String, String>> indexPageOk() {
        messageList.clear();
        messageList.put("result", "true");
        return new ResponseEntity<Map<String, String>>(messageList, HttpStatus.OK);
    }

    public ResponseEntity<Map<String, String>> indexPageError() {
        messageList.clear();
        messageList.put("result", "false");
        messageList.put("error", "Данная страница находится за пределами сайтов, указанных в конфигурационном файле");
        return new ResponseEntity<Map<String, String>>(messageList, HttpStatus.valueOf(503));
    }

    public ResponseEntity<Map<String, String>> searchOk() {
        messageList.clear();
        messageList.put("result", "true");
        return new ResponseEntity<Map<String, String>>(messageList, HttpStatus.OK);
    }

    public ResponseEntity<Map<String, String>> searchError(SnippetsResponce responce) {
        messageList.clear();

        messageList.put("result", "false");

        switch (responce.getCount()) {
            case -1:
                messageList.put("error", "Индексация сайта не завершена");
                break;
            case -2:
                messageList.put("error", "Задан пустой поисковый запрос");
                break;
            default:
                log.error("В методе searchError() - получена ошибка: передано не отслеживаемое значение");
                break;
        }

        return new ResponseEntity<Map<String, String>>(messageList, HttpStatus.valueOf(501));
    }
}

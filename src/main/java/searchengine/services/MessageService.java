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

}

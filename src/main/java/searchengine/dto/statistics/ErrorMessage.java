package searchengine.dto.statistics;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;
import java.util.TreeMap;

@Data
@NoArgsConstructor
public class ErrorMessage {
    private Map<String, String> messageList = new TreeMap<>();

    public ErrorMessage(Map<String, String> messageList) {
        this.messageList = messageList;
    }

    public void addMessage(String key, String value) {
        messageList.put(key, value);
    }

    public void clearMessageList() {
        messageList.clear();
    }

}

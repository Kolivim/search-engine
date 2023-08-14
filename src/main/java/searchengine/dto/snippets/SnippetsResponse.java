package searchengine.dto.snippets;

import lombok.Data;

import java.util.List;

@Data
public class SnippetsResponse {
    private boolean result;
    private int count;
    private List<DetailedSnippetsItem> data;

    public SnippetsResponse(List<DetailedSnippetsItem> data) {
        this.data = data;
        this.count = data.size();
        this.result = true;
    }

    public SnippetsResponse() {
    }
}

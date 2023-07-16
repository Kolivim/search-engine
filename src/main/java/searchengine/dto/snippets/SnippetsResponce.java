package searchengine.dto.snippets;

import lombok.Data;
import searchengine.dto.statistics.StatisticsData;

import java.util.List;

@Data
public class SnippetsResponce {
    private boolean result;
    private int count;
    private List<DetailedSnippetsItem> data;

    public SnippetsResponce(List<DetailedSnippetsItem> data) {
        this.data = data;
        this.count = data.size();
        this.result = true;
    }

    public SnippetsResponce() {
    }
}

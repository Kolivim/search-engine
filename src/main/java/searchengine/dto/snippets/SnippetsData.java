package searchengine.dto.snippets;

import lombok.Data;
import searchengine.dto.statistics.DetailedStatisticsItem;
import searchengine.dto.statistics.TotalStatistics;

import java.util.List;

@Data
public class SnippetsData
{
    private int count;
    private List<DetailedSnippetsItem> detailed;

    public SnippetsData(List<DetailedSnippetsItem> detailed)
    {
        this.detailed = detailed;
        this.count = detailed.size();
    }
}

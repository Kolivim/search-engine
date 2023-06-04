package searchengine.services;

import java.util.concurrent.Callable;

public interface IndexingService
{
    public void startIndexing();
    public boolean stopIndexing();

    void setIndexingStarted(boolean indexingStarted);

    public boolean getIndexingStarted();
}

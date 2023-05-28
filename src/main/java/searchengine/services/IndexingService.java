package searchengine.services;

import java.util.concurrent.Callable;

public interface IndexingService
{
    public void startIndexing();
    public void stopIndexing();

    void setIndexingStarted(boolean indexingStarted);

    public boolean getIndexingStarted();
}

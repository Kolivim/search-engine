package searchengine.services;

import java.util.concurrent.Callable;

public interface IndexingService
{
    public void startIndexing();
    public void stopIndexing();
    public boolean getIndexingStarted();
}

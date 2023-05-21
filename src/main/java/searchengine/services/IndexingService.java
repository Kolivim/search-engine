package searchengine.services;

public interface IndexingService
{
    public void startIndexing();
    public void stopIndexing();
    public boolean getIndexingStarted();
}

package searchengine.services;

import searchengine.model.Page;

import java.util.HashMap;

public interface LemmatizationService
{
    public boolean indexPage(String path);
    public void indexNewPage(Page page);

}
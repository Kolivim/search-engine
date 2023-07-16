package searchengine.services;

import searchengine.model.Page;
import searchengine.model.Site;

import java.io.IOException;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Set;

public interface LemmatizationService {
    public boolean indexPage(String path);

    public void indexNewPage(Page page);

    public void deleteSiteIndexAndLemma(Site site);

    public HashMap<String, Integer> splitLemmasText(String text) throws IOException;

    public LinkedHashMap<Page, String> getSnippet(LinkedHashMap<Page, Float> sortedAbsoluteRelevancePages,
                                                  Set<String> lemmasList);
}
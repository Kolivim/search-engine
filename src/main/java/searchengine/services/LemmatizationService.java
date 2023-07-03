package searchengine.services;

import searchengine.model.Lemma;
import searchengine.model.Page;
import searchengine.model.Site;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Set;

public interface LemmatizationService
{
    public boolean indexPage(String path);
    public void indexNewPage(Page page);
    public void deleteSiteIndexAndLemma(Site site);
    public HashMap<String, Integer> splitLemmasText(String text) throws IOException;    // SEARCH STAGE 24 june
    public void getSnippet(LinkedHashMap<Page, Float> sortedAbsoluteRelevancePages, /*ArrayList<Lemma> lemmas,*/ Set<String> lemmasList);   // SEARCH STAGE 30 june
}
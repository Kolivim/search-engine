package searchengine.services;

import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;

import searchengine.dto.snippets.DetailedSnippetsItem;
import searchengine.dto.snippets.SnippetsResponce;

import searchengine.model.*;


import java.io.IOException;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class SearchService {
    private LemmatizationService lemmatizationService;
    private SiteRepository siteRepository;
    private PageRepository pageRepository;
    private LemmaRepository lemmaRepository;
    private IndexRepository indexRepository;
    private static final int MAX_LEMMA_FREQUENCY = 10000;

    public SearchService(LemmatizationService lemmatizationService,
                         PageRepository pageRepository, SiteRepository siteRepository,
                         LemmaRepository lemmaRepository, IndexRepository indexRepository) {
        this.lemmatizationService = lemmatizationService;
        this.siteRepository = siteRepository;
        this.pageRepository = pageRepository;
        this.lemmaRepository = lemmaRepository;
        this.indexRepository = indexRepository;
    }

    public SnippetsResponce getEmptyQueryResponce(String query) {
        log.info("В методе startSearch()->getEmptyQueryResponce() - В запросе передана пустая строка: {}", query);
        SnippetsResponce response = new SnippetsResponce();
        response.setResult(false);
        response.setCount(-2);
        log.info("В методе startSearch()->getEmptyQueryResponce() ошибка №2 - Отправляем SnippetsResponce: [\n{}\n]",
                response);
        return response;
    }

    public SnippetsResponce getUnIndexedSitesResponce() {
        SnippetsResponce response = new SnippetsResponce();
        response.setResult(false);
        response.setCount(-1);
        log.info("В методе getUnIndexedSitesResponce() ошибка №1 - Отправляем SnippetsResponce: [\n{}\n]", response);
        return response;
    }

    public SnippetsResponce getSingleSiteQueryResponce(String query, int offset, int limit, String site) {
        log.info("В методе startSearch()->getSingleSiteQueryResponce() - Получен запрос по поиску " +
                "на единственной странице: {}", site);
        if (isSiteIndexed(site)) {
            log.info("В методе startSearch()->getSingleSiteQueryResponce() - Сайт: {} - проиндексирован, выполняем " +
                    "поиск лемм по запросу: {}", site, query);
            SnippetsResponce response = searchOnSite(query, offset, limit, site);
            log.info("В методе startSearch()->getSingleSiteQueryResponce() - Вывод SnippetsResponce responce: " +
                    "\n{}\n - для запроса: {}", response, query);
            return response;
        } else {
            log.info("В методе startSearch()->getSingleSiteQueryResponce() - Сайт: {} - НЕ проиндексирован, ошибка! " +
                    "Поиск не выполняется по запросу: {}", site, query);

            return getUnIndexedSitesResponce();
        }
    }

    public SnippetsResponce getAllSitesQueryResponce(String query, int offset, int limit, String site) {
        if (isSiteIndexed(site)) {
            log.info("В методе startSearch() - Сайты проиндексированы, выполняем поиск лемм по запросу: {}", query);

            ArrayList<SnippetsResponce> responceSites = new ArrayList<>();
            Iterable<searchengine.model.Site> siteIterable = siteRepository.findAll();
            for (searchengine.model.Site siteDB : siteIterable) {
                SnippetsResponce responceSite = searchOnSite(query, offset, limit, siteDB.getUrl());
                responceSites.add(responceSite);
            }

            ArrayList<DetailedSnippetsItem> dataSites = new ArrayList<>();
            for (SnippetsResponce responceSite : responceSites) {
                if (responceSite.isResult()) {
                    dataSites.addAll(responceSite.getData());
                }
            }

            Collections.sort(dataSites, Comparator.comparing(DetailedSnippetsItem::getRelevance));
            Collections.reverse(dataSites);
            log.debug("В методе startSearch() - \nВывод отсортированного по убыванию relevance " +
                    "ArrayList<DetailedSnippetsItem> dataSites:\n {}", dataSites);

            SnippetsResponce responceAllSites = new SnippetsResponce(dataSites);
            log.info("В методе startSearch() - \nВывод SnippetsResponce responceAllSites: {}", responceAllSites);

            return responceAllSites;
        } else {
            log.info("В методе startSearch() - В списке есть НЕ проиндексированный(е) сайт(ы): {} - ошибка! " +
                    "Поиск не выполняется: {}", site, query);
            return getUnIndexedSitesResponce();
        }
    }

    public SnippetsResponce startSearch(String query, int offset, int limit, String site) {

        if (query.isEmpty() || query.isBlank()) {
            return getEmptyQueryResponce(query);
        }
        if (site != null) {
            return getSingleSiteQueryResponce(query, offset, limit, site);
        } else {
            log.info("В методе startSearch() - Получен запрос по поиску на всех страницах:");
            return getAllSitesQueryResponce(query, offset, limit, site);
        }
    }

    public SnippetsResponce searchOnSite(String query, int offset, int limit, String site) {
        try {
            HashMap<String, Integer> splitLemmasText = lemmatizationService.splitLemmasText(query);
            Set<String> lemmasList = splitLemmasText.keySet();
            log.info("В методе searchOnSite() - Получен текст лемм поискового запроса: {}", lemmasList);

            ArrayList<Lemma> lemmas = getLemma(lemmasList, site);
            log.info("В методе searchOnSite() - Получен леммы: {}", lemmas);

            ArrayList<Page> lemmaPages = getPagesSearchList(lemmas, site);
            log.info("В методе searchOnSite() - Получено страницы с леммами, в количестве: {} :\n{}",
                    lemmaPages.size(), lemmaPages);

            return getListWithLemmas(lemmaPages, lemmasList);


        } catch (IOException e) {
            log.info("В методе searchOnSite() - Сработал catch: {} для запроса: {}", e, query);
        }

        log.error("В методе searchOnSite() дошли до \"return null\" в конце метода - Ошибка!");
        return null;
    }

    public SnippetsResponce getEmptyListLemmas() {
        List<DetailedSnippetsItem> detailed = new ArrayList<>();
        SnippetsResponce response = new SnippetsResponce(detailed);
        response.setResult(true);
        log.info("В методе searchOnSite() - Отправляем SnippetsResponce с пустым списком страниц: [\n {} \n]",
                response);
        return response;
    }

    public SnippetsResponce getListWithLemmas(ArrayList<Page> lemmaPages, Set<String> lemmasList) {

        if (!lemmaPages.isEmpty()) {

            HashMap<Page, Float> relevancePages = getRelevancePages(lemmaPages);
            log.info("В методе searchOnSite() - Получены страницы с леммами и их relativeRelevance: {}",
                    relevancePages);

            relevancePages = getAbsoluteRelevancePages(relevancePages);
            log.info("В методе searchOnSite() - Получены страницы с леммами и их absoluteRelevance: {}",
                    relevancePages);

            LinkedHashMap<Page, Float> sortedAbsoluteRelevancePages =
                    getSortedAbsoluteRelevancePages(relevancePages);
            log.info("В методе searchOnSite() - Получены отсортированные страницы с леммами и " +
                    "их absoluteRelevance: {}", sortedAbsoluteRelevancePages);

            LinkedHashMap<Page, String> sortedAbsoluteRelevancePagesSnippet =
                    lemmatizationService.getSnippet(sortedAbsoluteRelevancePages, lemmasList);

            log.debug("В методе searchOnSite() - Получен из метода getSnippetDTO() list:\n [ {} ]",
                    getSnippetDTO(sortedAbsoluteRelevancePages, sortedAbsoluteRelevancePagesSnippet));

            SnippetsResponce response = new SnippetsResponce(getSnippetDTO(sortedAbsoluteRelevancePages,
                    sortedAbsoluteRelevancePagesSnippet));

            log.info("В методе searchOnSite() - Отправляем SnippetsResponce: [\n{}\n]", response);

            return response;

        } else {
            log.info("В методе searchOnSite() - Получен пустой список страниц с леммами (подсчет " +
                    "релевантности не проводим): {}", lemmaPages);

            return getEmptyListLemmas();
        }
    }

    public List<DetailedSnippetsItem> getSnippetDTO(LinkedHashMap<Page, Float> sortedAbsoluteRelevancePages,
                                                    LinkedHashMap<Page, String> sortedAbsoluteRelevancePagesSnippet) {
        List<DetailedSnippetsItem> detailed = new ArrayList<>();
        for (Page pageSearch : sortedAbsoluteRelevancePages.keySet()) {
            DetailedSnippetsItem item = new DetailedSnippetsItem();
            item.setRelevance(sortedAbsoluteRelevancePages.get(pageSearch));
            item.setUri(pageSearch.getPath());
            item.setSnippet(sortedAbsoluteRelevancePagesSnippet.get(pageSearch));

            Document doc = Jsoup.parse(pageSearch.getContent());
            item.setTitle(doc.title());

            item.setSite(pageSearch.getSite().getUrl());
            item.setSiteName(pageSearch.getSite().getName());

            detailed.add(item);
        }
        return detailed;
    }

    public LinkedHashMap<Page, Float> getSortedAbsoluteRelevancePages(HashMap<Page, Float> relevancePages) {
        Map<Page, Float> sortedMap = relevancePages.entrySet().stream()
                .sorted(Comparator.comparingDouble(e -> -e.getValue()))
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (a, b) -> {
                            throw new AssertionError();
                        },
                        LinkedHashMap::new
                ));
        return (LinkedHashMap<Page, Float>) sortedMap;
    }

    public HashMap<Page, Float> getAbsoluteRelevancePages(HashMap<Page, Float> relevancePages) {
        Float maxRelevance = getMaxRelevancePages(relevancePages);
        for (Page searchLemmaPage : relevancePages.keySet()) {
            Float relativeRelevance = relevancePages.get(searchLemmaPage);
            Float absoluteRelevance = relativeRelevance / maxRelevance;
            relevancePages.replace(searchLemmaPage, absoluteRelevance);
        }
        return relevancePages;
    }

    public Float getMaxRelevancePages(HashMap<Page, Float> relevancePages) {
        Page maxRelevancePages = Collections.max(
                relevancePages.entrySet(),
                new Comparator<Map.Entry<Page, Float>>() {
                    @Override
                    public int compare(Map.Entry<Page, Float> o1, Map.Entry<Page, Float> o2) {
                        return o1.getValue() > o2.getValue() ? 1 : -1;
                    }
                }).getKey();

        log.info("В методе getMaxRelevancePages() - Получена Страница с максимальной относительной релевантностью: " +
                "{}, равной = {}", maxRelevancePages, relevancePages.get(maxRelevancePages));

        return relevancePages.get(maxRelevancePages);
    }

    public HashMap<Page, Float> getRelevancePages(ArrayList<Page> lemmaPages) {
        HashMap<Page, Float> relevancePages = new HashMap<>();
        for (Page lemmaPage : lemmaPages) {
            relevancePages.put(lemmaPage, getRelativeRelevancePage(lemmaPage));
        }
        return relevancePages;
    }

    public Float getRelativeRelevancePage(Page lemmaPage) {
        float relativeRelevance = 0;
        ArrayList<Index> indexesLemmaPage = (ArrayList<Index>) indexRepository.findAllByPageId(lemmaPage.getId());
        for (Index index : indexesLemmaPage) {
            relativeRelevance = relativeRelevance + index.getRank();
        }
        return relativeRelevance;
    }

    public ArrayList<Page> getPagesSearchList(ArrayList<Lemma> lemmas, String site) {
        Site siteSearch = siteRepository.findByUrl(site);
        ArrayList<Page> lemmasPages = new ArrayList<>();
        boolean isStartSearch = true;

        log.info("В методе getPagesSearchList() - Получен список лемм: {}", lemmas);

        for (Iterator lemmaIterator = lemmas.iterator(); lemmaIterator.hasNext(); ) {
            if (isStartSearch) {
                Lemma lemma = (Lemma) lemmaIterator.next();
                lemmasPages = getPageWithLemmas(lemma, siteSearch, lemmasPages);

                log.info("В методе getPagesSearchList() - При поиске леммы:{} - получено страниц с леммами = {} :\n {}",
                        lemmas, lemmasPages.size(), lemmasPages);

                if (lemmasPages.isEmpty()) {
                    isStartSearch = false;
                }

            }
            lemmaIterator.remove();
        }

        return lemmasPages;
    }

    public ArrayList<Page> getPageWithLemmas(Lemma lemma, Site site, ArrayList<Page> pagesWithLemmas) {
        int lemmmaFindId = lemma.getId();
        int siteId = site.getId();
        List<Page> pages;

        if (pagesWithLemmas.isEmpty()) {
            pages = pageRepository.findAllBySiteId(siteId);
        } else {
            pages = pagesWithLemmas;
        }

        ArrayList<Page> pagesWithLemma = new ArrayList<>();
        for (Page page : pages) {
            int pageId = page.getId();
            Optional<Index> indexLemmaSiteSearch = indexRepository.findByPageIdAndLemmaId(pageId, lemmmaFindId);
            if (indexLemmaSiteSearch.isPresent()) {
                pagesWithLemma.add(page);
                log.info("В методе getPageWithLemma() - При поиске леммы: {} - добавлена страница: {} сайта: {}",
                        lemma, page.getPath(), page.getSiteId());
            }
        }
        return pagesWithLemma;
    }

    public ArrayList<Lemma> getLemma(Set<String> lemmasList, String site) {
        Site siteSearch = siteRepository.findByUrl(site);
        ArrayList<Lemma> lemmas = new ArrayList<>();

        for (String lemma : lemmasList) {
            Lemma savedLemma;
            Optional lemmaOptional = lemmaRepository.findByLemmaAndSiteId(lemma, siteSearch.getId());
            if (lemmaOptional.isPresent()) {
                savedLemma = (Lemma) lemmaOptional.get();
                if (savedLemma.getFrequency() < MAX_LEMMA_FREQUENCY) {
                    lemmas.add(savedLemma);
                }
            } else {
                return new ArrayList<Lemma>();
            }
        }
        Collections.sort(lemmas, (Comparator<Lemma>) (o1, o2) -> o1.getFrequency() - o2.getFrequency());
        return lemmas;
    }

    public boolean isSiteIndexed(String site) {
        if (site != null) {
            Site siteSearch = siteRepository.findByUrl(site);

            if (siteSearch.getStatus().equals(StatusType.INDEXED))
            {
                return true;
            } else {
                return false;
            }
        } else {
            Iterable<Site> allSites = siteRepository.findAll();
            for (searchengine.model.Site siteDB : allSites) {
                boolean isStopSearch = siteDB.getStatus().equals(StatusType.FAILED) ||
                        siteDB.getStatus().equals(StatusType.INDEXING);
                if (isStopSearch) {
                    log.info("В методе isSiteIndexed() - Поиск остановлен, индексация сайта не завершена: {}",
                            siteDB.getUrl());
                    return false;
                }
                return true;
            }

        }
        return true;
    }

    public ArrayList<Page> getPageWithLemma(Lemma lemma, Site site) {
        int lemmmaFindId = lemma.getId();

        int siteId = site.getId();
        List<Page> pages = pageRepository.findAllBySiteId(siteId);

        ArrayList<Page> pagesWithLemma = new ArrayList<>();
        for (Page page : pages) {
            int pageId = page.getId();
            Optional<Index> indexLemmaSiteSearch = indexRepository.findByPageIdAndLemmaId(pageId, lemmmaFindId);
            if (indexLemmaSiteSearch.isPresent()) {
                pagesWithLemma.add(page);
                log.info("В методе getPageWithLemma() - При поиске леммы: {} - добавлена страница: {} сайта: {}",
                        lemma, page.getPath(), page.getSiteId());
            }
        }
        return pagesWithLemma;
    }
}


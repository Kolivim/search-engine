package searchengine.services;

import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.jsoup.Jsoup;
import org.jsoup.safety.Cleaner;
import org.jsoup.safety.Safelist;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.model.Index;
import searchengine.model.Lemma;
import searchengine.model.Page;
import searchengine.model.Site;
import searchengine.repository.IndexRepository;
import searchengine.repository.LemmaRepository;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

@Slf4j
@Service
public class LemmatizationServiceImpl implements LemmatizationService {
    private SiteRepository siteRepository;
    private PageRepository pageRepository;
    private LemmaRepository lemmaRepository;
    private IndexRepository indexRepository;
    private String[] servicePartsText = {"ПРЕДЛ", "МЕЖД", "СОЮЗ"};
    private LuceneMorphology luceneMorph = new RussianLuceneMorphology();
    private static final int SNIPPET_LENGTH = 240;

    @Autowired
    public LemmatizationServiceImpl(PageRepository pageRepository, SiteRepository siteRepository,
                                    LemmaRepository lemmaRepository,
                                    IndexRepository indexRepository) throws IOException {
        this.siteRepository = siteRepository;
        this.pageRepository = pageRepository;
        this.lemmaRepository = lemmaRepository;
        this.indexRepository = indexRepository;

    }

    @Override
    public boolean indexPage(String path) {
        log.info("Запущен метод indexPage()");
        HashMap<String, String> pathInfo = getPathAndSiteUrl(path);
        String pagePath = pathInfo.get("pagePath");
        String siteUrl = pathInfo.get("siteUrl");

        if (siteRepository.existsByUrl(siteUrl)) {
            log.info("В методе indexPage() получен \"true\" в проверке на существование переданного пути сайта: {}",
                    path);
            Site site = siteRepository.findByUrl(siteUrl);
            List<Integer> deletesLemmaId = deleteIndexes(pagePath, site);
            deleteLemmaOnPage(deletesLemmaId, site);

            PageWriter removedPage = new PageWriter();
            Page page = removedPage.removedOrAddPage(pagePath, path, site);

            log.info("Завершение метода indexPage(), для переданного пути сайта: {} получен новый pageId = {}",
                    path, page.getId());
            return true;
        } else {
            log.info("В методе indexPage() получен \"false\" в проверке на существование переданного " +
                    "пути сайта: {} - данная страница находится за пределами сайтов, " +
                    "указанных в конфигурационном файле", path);
            return false;
        }
    }

    public void deleteLemmaOnPage(List<Integer> lemmasId, Site site) {
        log.info("Начало метода deleteLemmaOnPage(), к удалению переданы lemmaId: {} , в количестве = {}, " +
                "для сайта = {}", lemmasId, lemmasId.size(), site);
        for (Integer lemmaId : lemmasId) {
            Lemma lemmaOnDeletedPage;
            Optional lemmaOptional = lemmaRepository.findByIdAndSiteId(lemmaId, site.getId());
            if (lemmaOptional.isPresent()) {
                lemmaOnDeletedPage = (Lemma) lemmaOptional.get();
                if (lemmaOnDeletedPage.getFrequency() > 1) {
                    lemmaOnDeletedPage.frequencyLemmaDecr();
                    lemmaRepository.save(lemmaOnDeletedPage);
                } else {
                    lemmaRepository.delete(lemmaOnDeletedPage);
                }
            } else {
                log.error("Ошибка - леммаID : {} для сайта {} - не существует", lemmaId, site.getUrl());
            }
        }
    }

    public List<Integer> deleteIndexes(String pagePath, Site site) {
        List<Integer> deletesLemmaId = new ArrayList<>();
        Page deletedPage = pageRepository.findByPathAndSite(pagePath, site);
        List<Index> deletedIndexes = indexRepository.findAllByPageId(deletedPage.getId());
        log.info("В методе deleteIndexes(), к удалению следующие Index: {} , в количестве = {}, ",
                deletedIndexes, deletedIndexes.size());
        for (Index deletedIndex : deletedIndexes) {
            deletesLemmaId.add(deletedIndex.getLemmaId());
            indexRepository.delete(deletedIndex);
        }
        return deletesLemmaId;
    }

    @Override
    public void deleteSiteIndexAndLemma(Site site) {
        Iterable<Page> deletedPagesIterable = pageRepository.findAllBySiteId(site.getId());
        for (Page deletedPage : deletedPagesIterable) {
            List<Index> deletedIndexes = indexRepository.findAllByPageId(deletedPage.getId());
            log.info("В методе deleteSiteIndexAndLemma(), к удалению следующие Index: {} , в количестве = {}, ",
                    deletedIndexes, deletedIndexes.size());
            for (Index deletedIndex : deletedIndexes) {
                indexRepository.delete(deletedIndex);
            }
        }

        deleteLemmaOnSite(site);
    }

    public void deleteLemmaOnSite(Site site) {
        log.info("Начало метода deleteLemmaOnSite(), у следующего сайта: {}", site.getUrl());
        Iterable<Lemma> deletedLemmasInSiteIterable = lemmaRepository.findAllBySiteId(site.getId());
        for (Lemma deleteLemma : deletedLemmasInSiteIterable) {
            lemmaRepository.delete(deleteLemma);
        }
    }

    @Override
    public void indexNewPage(Page page) {
        addLemmas(page);
        log.info("Завершение метода indexNewPage(), у сайта: {}, для пути страницы: {}",
                page.getSite().getUrl(), page.getPath());
    }

    private void addLemmas(Page indexingPage) {
        try {
            HashMap<String, Integer> splitLemmasText = splitLemmasText(getClearHTML(indexingPage.getContent()));

            log.info("В методе addLemmas() к добавлению получены Lemma в количестве: {}, для страницы: {}",
                    splitLemmasText.size(), indexingPage.getPath());
            int indexCountAdd = 0;

            for (String lemma : splitLemmasText.keySet()) {
                Lemma savedLemma;
                Optional lemmaOptional = lemmaRepository.findByLemmaAndSiteId(lemma, indexingPage.getSiteId());
                if (lemmaOptional.isPresent()) {
                    savedLemma = (Lemma) lemmaOptional.get();
                    savedLemma.frequencyLemmaIncr();
                } else {
                    savedLemma = new Lemma(indexingPage.getSiteId(), lemma, 1);
                }

                log.trace("В методе addLemmas() к сохранению получена savedLemma: {}, для страницы: {}",
                        savedLemma, indexingPage.getPath());
                lemmaRepository.save(savedLemma);

                Index savedIndex = new Index(indexingPage.getId(), savedLemma.getId(),
                        splitLemmasText.get(lemma));
                log.trace("В методе addLemmas() к сохранению получен savedIndex: {}, для страницы: {}",
                        savedIndex, indexingPage.getPath());
                indexCountAdd++;
                indexRepository.save(savedIndex);
            }
            log.trace("В методе addLemmas() итого к сохранению получено и посчитано в -count- savedIndex " +
                    "в количестве: {}, для страницы: {}", indexCountAdd, indexingPage.getPath());
        } catch (IOException e) {
            log.error("В методе addLemmas() сработал IOException(e) со следующими параметрами:" +
                            " ///1: {}, ///2: {}, ///3: {}, ///4: {}, ///5: {}, ///6: {}",
                    e.getMessage(), e.getStackTrace(), e.getSuppressed(),
                    e.getCause(), e.getLocalizedMessage(), e.getClass());
        }
    }

    @Override
    public LinkedHashMap<Page, String> getSnippet(LinkedHashMap<Page, Float> sortedAbsoluteRelevancePages,
                                                  Set<String> lemmasList) {
        log.info("Начало метода getSnippet() - передан sortedAbsoluteRelevancePages страниц " +
                "с их абсолютной релевантностью: \n[ {} ]", sortedAbsoluteRelevancePages);

        LinkedHashMap<Page, String> sortedAbsoluteRelevancePagesSnippet = new LinkedHashMap<>();

        for (Page pageSearch : sortedAbsoluteRelevancePages.keySet()) {
            ArrayList<String> contentPage = getContentPage(pageSearch, lemmasList);
            int countLemmaOnPage = getCountLemmaOnPage(contentPage);
            log.info("В методе getSnippet() - получен Arraylist текста страницы с выделенной леммой : {} " +
                    "\n\tДля страницы: {}", contentPage, pageSearch.getPath());
            String contentPageSnippet = getSnippetPage(contentPage, countLemmaOnPage);
            log.info("В методе getSnippet() в результате вызова getSnippetPage() - " +
                    "получен contentPageSnippet текста страницы с выделенным snippet: {} " +
                    "\n\tДля страницы: {}", contentPageSnippet, pageSearch.getPath());
            sortedAbsoluteRelevancePagesSnippet.put(pageSearch, contentPageSnippet);
        }
        log.info("Заверщение метода getSnippet() - " +
                "получен LinkedHashMap<Page, String> sortedAbsoluteRelevancePagesSnippet, страниц с текстом " +
                "выделенных snippet: \n[ {} ]\n", sortedAbsoluteRelevancePagesSnippet);
        return sortedAbsoluteRelevancePagesSnippet;
    }

    public Integer getCountLemmaOnPage(ArrayList<String> contentPage) {
        int countLemmaOnPage = 0;
        for (String word : contentPage) {
            if (isLemma(word)) {
                countLemmaOnPage++;
            }
        }
        return countLemmaOnPage;
    }

    public ArrayList<String> getContentPage(Page pageSearch, Set<String> lemmasList) {
        String[] splitText = getContentPageArray(pageSearch);
        StringBuilder builder = new StringBuilder();
        ArrayList<String> contentPage = new ArrayList<>();
        for (int i = 0; i < splitText.length; i++) {
            String unrefinedWord = getClearWord(splitText[i]);
            if (!unrefinedWord.isBlank() & !unrefinedWord.isEmpty()) {
                log.debug("В методе getContentPage() - полученный unrefinedWord прошел проверку isBlank() и isEmpty()");
                String lemma = getLemma(unrefinedWord);
                if (lemmasList.contains(lemma)) {
                    builder.append(" ");
                    contentPage.add(builder.toString());
                    builder = new StringBuilder();
                    String lemmaString = "<b>".concat(splitText[i]).concat("</b>");
                    contentPage.add(lemmaString);

                    log.debug("В методе getContentPage() - получено совпадение текста страницы с леммой: {}, " +
                            "имеющей вид на сайте: {}, имеющей индекс в массиве = {}", lemma, splitText[i], i);
                } else {
                    builder.append(splitText[i]);
                    builder.append(" ");
                }

            } else {
                builder.append(splitText[i]);
                builder.append(" ");
            }
        }
        if (!builder.isEmpty()) {
            contentPage.add(builder.toString());
        }

        return contentPage;
    }

    public String[] getContentPageArray(Page pageSearch) {
        String content = getClearHTML(pageSearch.getContent());
        content = content.trim();
        String[] splitText = content.split("\\s+");
        log.trace("В методе getContentPageArray() - получен текст страницы для: {}, имеющий вид: {}", pageSearch,
                splitText);
        return splitText;
    }

    public String getClearWord(String unrefinedWord) {
        log.debug("В методе getClearWord() - получен текст слова unrefinedWord: {}", unrefinedWord);
        unrefinedWord = unrefinedWord.replaceAll("([^а-яА-Я\\s])", "").trim()
                .toLowerCase(new Locale("ru", "RU"));
        log.debug("В методе getClearWord() - полученный unrefinedWord преобразован в: {}", unrefinedWord);
        return unrefinedWord;
    }

    public String partSnippetBeforeLemma(String partContentPage, int conditionalLengthLemmas, int lengthPartlemmas) {
        int indexStringStop = partContentPage.length() - 1;
        int indexStringStart = indexStringStop - lengthPartlemmas;
        String partSnippet = " ...".concat(partContentPage.substring(indexStringStart, indexStringStop));
        return partSnippet;
    }

    public String partSnippetAfterLemma(String partContentPage, int conditionalLengthLemmas, int lengthPartlemmas) {
        int indexStringStop = lengthPartlemmas;
        int indexStringStart = 0;
        String partSnippet = partContentPage.substring(indexStringStart, indexStringStop).concat(" ...");
        return partSnippet;
    }

    public PartSnippet getEndPartSnippetLeftLemma(ArrayList<String> contentPage, PartSnippet partSnippetPage3) {
        int index = contentPage.size() - 1;

        PartSnippet partSnippetPage = partSnippetPage3;
        ArrayList<Integer> indexSearchList = partSnippetPage.getIndexSearchList();
        String snippetPartPage = partSnippetPage.getPartSnippet();
        int conditionalLengthLemmas = partSnippetPage.getConditionalLengthLemmas();

        if (isLemma(contentPage.get(index))) {
            int partIndexBefore = index - 1;
            if (!isLemma(contentPage.get(partIndexBefore))) {
                if (indexSearchList.contains(partIndexBefore)) {
                    if (contentPage.get(partIndexBefore).length() >
                            (conditionalLengthLemmas + conditionalLengthLemmas / 2)) {
                        snippetPartPage = snippetPartPage.concat(partSnippetBeforeLemma(
                                contentPage.get(partIndexBefore), conditionalLengthLemmas, conditionalLengthLemmas));
                    } else {
                        String partSnippet = contentPage.get(partIndexBefore);
                        snippetPartPage = snippetPartPage.concat(partSnippet);
                        indexSearchList.remove((Integer) partIndexBefore);
                    }

                } else {
                    log.info("В методе getEndPartSnippet() - полученный элемент snippet находится за пределами " +
                            "диапазона записи сниппета, index: {}", partIndexBefore);
                }
            } else {
                log.info("В методе getEndPartSnippet() - полученный элемент snippet: {} является леммой",
                        contentPage.get(partIndexBefore));
            }
        }

        partSnippetPage.setPartSnippet(snippetPartPage);
        partSnippetPage.setIndexSearchList(indexSearchList);
        return partSnippetPage;
    }

    public PartSnippet getEndPartElseSnippetLeftLemma(ArrayList<String> contentPage, PartSnippet partSnippetPage1) {
        PartSnippet partSnippetPage = new PartSnippet(partSnippetPage1);
        ArrayList<Integer> indexSearchList = partSnippetPage.getIndexSearchList();
        String snippetPartPage = partSnippetPage.getPartSnippet();
        int conditionalLengthLemmas = partSnippetPage.getConditionalLengthLemmas();

        int indexPrePreEnd = contentPage.size() - 3;
        if (!isLemma(contentPage.get(indexPrePreEnd))) {
            if (contentPage.get(indexPrePreEnd).length() > (conditionalLengthLemmas)) {
                snippetPartPage = snippetPartPage.concat(partSnippetBeforeLemma(contentPage.get(indexPrePreEnd),
                        conditionalLengthLemmas, conditionalLengthLemmas / 2));
            } else {
                String partSnippet = contentPage.get(indexPrePreEnd);
                snippetPartPage = snippetPartPage.concat(partSnippet);
                indexSearchList.remove((Integer) indexPrePreEnd);
            }
        }

        int indexEndLemma = contentPage.size() - 2;
        String partSnippet1 = contentPage.get(indexEndLemma);
        snippetPartPage = snippetPartPage.concat(" ").concat(partSnippet1).concat(" ");
        indexSearchList.remove((Integer) indexEndLemma);

        int indexEnd1 = contentPage.size() - 1;
        if (contentPage.get(indexEnd1).length() > (conditionalLengthLemmas / 2)) {
            snippetPartPage = snippetPartPage.concat(partSnippetAfterLemma(contentPage.get(indexEnd1),
                    conditionalLengthLemmas, conditionalLengthLemmas / 2));
            indexSearchList.remove((Integer) indexEnd1);
        } else {
            String partSnippet = contentPage.get(indexEnd1);
            snippetPartPage = snippetPartPage.concat(partSnippet);
            indexSearchList.remove((Integer) indexEnd1);
        }

        partSnippetPage.setPartSnippet(snippetPartPage);
        partSnippetPage.setIndexSearchList(indexSearchList);
        return partSnippetPage;
    }

    public PartSnippet getEndPartSnippet(ArrayList<String> contentPage, PartSnippet partSnippetPage) {
        ArrayList<Integer> indexSearchList = partSnippetPage.getIndexSearchList();

        int index = contentPage.size() - 1;

        if (isLemma(contentPage.get(index))) {

            PartSnippet psP1 = getEndPartSnippetLeftLemma(contentPage, partSnippetPage);
            partSnippetPage.addPartSnippet(psP1);

            if (indexSearchList.contains(index)) {
                String partSnippet = contentPage.get(index);
                partSnippetPage.addStringPartSnippet(partSnippet, index);
            } else {
                log.info("В методе getEndPartSnippet() - полученный элемент snippet находится за пределами " +
                        "диапазона записи сниппета, index: {}", index);
            }

        } else {
            PartSnippet psP = getEndPartElseSnippetLeftLemma(contentPage, partSnippetPage);
            partSnippetPage.addPartSnippet(psP);
        }

        log.info("В методе getEndPartSnippet() - получен полный элемент EndSnippet : {}", partSnippetPage);
        return partSnippetPage;
    }


    public PartSnippet getStartPartSnippet(ArrayList<String> contentPage, PartSnippet partSnippetPage) {
        ArrayList<Integer> indexSearchList = partSnippetPage.getIndexSearchList();
        if (indexSearchList.size() > 0) {

            int index = 0;
            if (isLemma(contentPage.get(index)) & indexSearchList.contains(index)) {
                PartSnippet startPartSnippetLemmaFirst = getStartPartSnippetLemmaFirst(contentPage, partSnippetPage);
            } else {
                PartSnippet startPartSnippetLemmaPost = getStartPartSnippetLemmaPost(contentPage, partSnippetPage);
            }
        } else {
            log.error("В методе getStartPartSnippet() - Ошибка либо индексы перебраны в snippetEnd()");
        }
        return partSnippetPage;
    }

    public PartSnippet getPartSnippetBeforeLemma(ArrayList<String> contentPage,
                                                 PartSnippet partSnippetPage3, int index) {
        PartSnippet partSnippetPage = partSnippetPage3;
        ArrayList<Integer> indexSearchList = partSnippetPage.getIndexSearchList();
        String snippetPartPage = partSnippetPage.getPartSnippet();
        int conditionalLengthLemmas = partSnippetPage.getConditionalLengthLemmas();

        if (indexSearchList.contains(index)) {
            if (contentPage.get(index).length() > (conditionalLengthLemmas)) {
                String partSnippet = partSnippetBeforeLemma(contentPage.get(index),
                        conditionalLengthLemmas, conditionalLengthLemmas / 2);
                snippetPartPage = snippetPartPage.concat(partSnippet);
            } else {
                String partSnippet = contentPage.get(index);
                snippetPartPage = snippetPartPage.concat(partSnippet);
            }
            indexSearchList.remove((Integer) index);
        }
        partSnippetPage.setPartSnippet(snippetPartPage);
        partSnippetPage.setIndexSearchList(indexSearchList);
        return partSnippetPage;
    }

    public PartSnippet getPartSnippetAfterLemma(ArrayList<String> contentPage,
                                                PartSnippet partSnippetPage3, int index) {
        PartSnippet partSnippetPage = partSnippetPage3;
        ArrayList<Integer> indexSearchList = partSnippetPage.getIndexSearchList();
        String snippetPartPage = partSnippetPage.getPartSnippet();
        int conditionalLengthLemmas = partSnippetPage.getConditionalLengthLemmas();

        if (!isLemma(contentPage.get(index)) & indexSearchList.contains(index)) {
            if (contentPage.get(index).length() > conditionalLengthLemmas) {
                String partSnippet = partSnippetAfterLemma(contentPage.get(index),
                        conditionalLengthLemmas, conditionalLengthLemmas / 2);
                snippetPartPage = snippetPartPage.concat(partSnippet);
            } else {
                String partSnippet = contentPage.get(index);
                snippetPartPage = snippetPartPage.concat(partSnippet);
                indexSearchList.remove((Integer) index);
            }
        }

        partSnippetPage.setPartSnippet(snippetPartPage);
        partSnippetPage.setIndexSearchList(indexSearchList);
        return partSnippetPage;
    }

    public PartSnippet getStartPartSnippetLemmaPost(ArrayList<String> contentPage, PartSnippet partSnippetPage) {
        ArrayList<Integer> indexSearchList = partSnippetPage.getIndexSearchList();
        String snippetPartPage = partSnippetPage.getPartSnippet();
        boolean isExistsLemma = false;

        int index = 0;
        PartSnippet partSnippetBeforeLemma = getPartSnippetBeforeLemma(contentPage, partSnippetPage, index);

        int indexPost = 1;
        if (indexSearchList.contains(indexPost)) {
            String partSnippet1 = contentPage.get(indexPost);
            partSnippetPage.addStringPartSnippet(partSnippet1, indexPost);
            isExistsLemma = true;
        }

        int indexPostPost = 2;
        PartSnippet partSnippetAfterLemma = getPartSnippetAfterLemma(contentPage, partSnippetPage, indexPostPost);

        if (!isExistsLemma) {
            partSnippetPage.setPartSnippet(snippetPartPage);
        }

        return partSnippetPage;
    }

    public PartSnippet getStartPartSnippetLemmaFirst(ArrayList<String> contentPage, PartSnippet partSnippetPage) {
        ArrayList<Integer> indexSearchList = partSnippetPage.getIndexSearchList();
        int conditionalLengthLemmas = partSnippetPage.getConditionalLengthLemmas();
        String snippetStart = partSnippetPage.getPartSnippet();
        boolean isExistsLemma = false;

        int index = 0;
        snippetStart = snippetStart.concat(contentPage.get(index));
        indexSearchList.remove((Integer) index);
        isExistsLemma = true;

        int indexPost = 1;
        if (indexSearchList.contains(indexPost)) {
            if (contentPage.get(indexPost).length() > (conditionalLengthLemmas + conditionalLengthLemmas / 2)) {
                snippetStart = snippetStart.concat(partSnippetAfterLemma(contentPage.get(indexPost),
                        conditionalLengthLemmas, conditionalLengthLemmas));
            } else {
                snippetStart = snippetStart.concat(contentPage.get(indexPost));
                indexSearchList.remove((Integer) indexPost);
            }
        }

        if (!isExistsLemma) {
            snippetStart = "";
        }

        partSnippetPage.setPartSnippet(snippetStart);
        partSnippetPage.setIndexSearchList(indexSearchList);

        return partSnippetPage;
    }

    public PartSnippet getMiddlePartSnippetFull(ArrayList<String> contentPage, PartSnippet partSnippetPage) {
        ArrayList<Integer> indexSearchList = partSnippetPage.getIndexSearchList();
        ArrayList<Integer> searchList = new ArrayList<>();
        searchList.addAll(indexSearchList);

        for (Integer index : searchList) {
            if (isLemma(contentPage.get(index))) {
                getMiddlePartSnippet(contentPage, partSnippetPage, index);
            }
        }
        return partSnippetPage;
    }

    public String getSnippetPage(ArrayList<String> contentPage, int countLemmaOnPage) {
        int indexEndContentPage = (contentPage.size() - 1);
        ArrayList<Integer> lemmasIndexSearch = getIndexSearchList(0, indexEndContentPage);

        if (countLemmaOnPage < 1) {
            log.error("В методе getSnippetPage() - Получен countLemmaOnPage < 1 - леммы не найдены, " +
                    "ошибка поиска лемм, countLemmaOnPage: {}", countLemmaOnPage);
            return null;
        }

        String snippetPage = "";
        int conditionalLengthLemmas = getConditionalLengthLemmas(contentPage, countLemmaOnPage);

        String snippetEnd = "";
        PartSnippet partSnippetP = new PartSnippet(snippetEnd, conditionalLengthLemmas, lemmasIndexSearch);
        PartSnippet endPageSnippet = getEndPartSnippet(contentPage, partSnippetP);
        snippetEnd = endPageSnippet.getPartSnippet();
        lemmasIndexSearch = endPageSnippet.getIndexSearchList();

        String snippetStart = "";
        PartSnippet partSnippetStartValue = new PartSnippet(snippetStart, conditionalLengthLemmas, lemmasIndexSearch);
        PartSnippet partSnippetStart = getStartPartSnippet(contentPage, partSnippetStartValue);
        snippetStart = partSnippetStart.getPartSnippet();
        lemmasIndexSearch = partSnippetStart.getIndexSearchList();

        String snippetMiddle = "";
        PartSnippet partSnippetMiddleValue = new PartSnippet(snippetMiddle, conditionalLengthLemmas, lemmasIndexSearch);
        PartSnippet partSnippetMiddle = getMiddlePartSnippetFull(contentPage, partSnippetMiddleValue);
        snippetMiddle = partSnippetMiddle.getPartSnippet();

        log.info("Завершение метода getSnippet() - Получены String сниппетов " +
                "страницы: \nStart: {}, \nMiddle: {},\nEnd: {}, ", snippetStart, snippetMiddle, snippetEnd);

        snippetPage = snippetPage.concat(snippetStart).concat(" ").concat(snippetMiddle)
                .concat(" ").concat(snippetEnd);

        return snippetPage;
    }

    public ArrayList<Integer> getIndexSearchList(int excludedIndexStart, int excludedIndexEnd) {
        ArrayList<Integer> indexSearchList = new ArrayList<>();
        for (int i = excludedIndexStart; i <= excludedIndexEnd; i++) {
            indexSearchList.add(i);
        }
        return indexSearchList;
    }


    public PartSnippet getMiddlePartSnippet(ArrayList<String> contentPage, PartSnippet partSnippetPage, Integer index) {

        ArrayList<Integer> indexSearchList = partSnippetPage.getIndexSearchList();
        String snippetPartPage = partSnippetPage.getPartSnippet();
        boolean isExistsLemma = false;

        int indexBefore = index - 1;
        PartSnippet partSnippetBeforeLemma = getPartSnippetBeforeLemma(contentPage, partSnippetPage, indexBefore);


        if (indexSearchList.contains(index)) {
            String partSnippet1 = contentPage.get(index);
            partSnippetPage.addStringPartSnippet(partSnippet1, index);
            isExistsLemma = true;
        }

        partSnippetPage.setIndexSearchList(indexSearchList);
        int indexAfter = index + 1;
        PartSnippet partSnippetAfterLemma = getPartSnippetAfterLemma(contentPage, partSnippetPage, indexAfter);

        if (!isExistsLemma) {
            partSnippetPage.setPartSnippet(snippetPartPage);
        }

        return partSnippetPage;
    }

    public boolean isLemma(String text) {
        boolean isLemma = false;
        if (text.contains("<b>")) {
            isLemma = true;
        }
        return isLemma;
    }

    public int getConditionalLengthLemmas(ArrayList<String> contentPage, int countLemmaOnPage) {
        int lengthLemmas = getLengthLemmas(contentPage);
        int conditionalLengthLemmas = (SNIPPET_LENGTH - lengthLemmas) / countLemmaOnPage;
        if (conditionalLengthLemmas < 0) {
            conditionalLengthLemmas = 0;
        }
        return conditionalLengthLemmas;
    }

    public int getLengthLemmas(ArrayList<String> contentPage) {
        int lengthLemmas = 0;
        for (String text : contentPage) {
            if (isLemma(text)) {
                lengthLemmas += text.length();
            }
        }
        return lengthLemmas;
    }

    private HashMap<String, String> getPathAndSiteUrl(String path) {
        HashMap<String, String> pathInfo = new HashMap<String, String>();
        try {
            URL url = new URL(path);
            pathInfo.put("pagePath", url.getPath());
            String siteURL = url.getProtocol();
            siteURL = siteURL + "://";
            siteURL = siteURL + url.getHost();
            pathInfo.put("siteUrl", siteURL);
            log.info("Завершение метода getPathAndSiteUrl() - Получены pathInfo: {}", pathInfo);
        } catch (MalformedURLException e) {
            log.error("В методе getPathAndSiteUrl() - сработал MalformedURLException(e): {}, {}, " +
                    "на переданной странице: {}", e.getMessage(), e.getStackTrace(), path);
        }
        return pathInfo;
    }

    public HashMap<String, Integer> splitLemmasText(String text) throws IOException {
        HashMap<String, Integer> splitLemmasText = new HashMap<String, Integer>();
        String[] splitText = arrayContainsRussianUnrefinedWords(text);
        for (Iterator<String> splitTextIterator = Arrays.stream(splitText).iterator(); splitTextIterator.hasNext(); ) {
            String unrefinedWord = splitTextIterator.next();

            if (unrefinedWord.isBlank() || unrefinedWord.isEmpty()) {
                continue;
            }

            if (isRussianServicePartsText(unrefinedWord)) {
                continue;
            }

            String lemma = getLemma(unrefinedWord);
            if (splitLemmasText.containsKey(lemma)) {
                Integer count = splitLemmasText.get(lemma);
                count++;
                splitLemmasText.replace(lemma, count);
            } else {
                splitLemmasText.put(lemma, 1);
            }
        }
        return splitLemmasText;
    }

    private String[] arrayContainsRussianUnrefinedWords(String text) {
        return text.toLowerCase(new Locale("ru", "RU"))
                .replaceAll("([^а-я\\s])", " ")
                .trim()
                .split("\\s+");
    }

    @SneakyThrows
    private boolean isRussianServicePartsText(String unrefinedWord) {
        boolean isServicePartsText = false;
        List<String> wordMorphInfos = luceneMorph.getMorphInfo(unrefinedWord);
        for (String wordMorphInfo : wordMorphInfos) {
            if (isServicePartsText) {
                continue;
            }
            wordMorphInfo = wordMorphInfo.replaceAll("([^А-Я\\s])", " ");
            if (wordMorphInfo.contains(servicePartsText[0]) || wordMorphInfo.contains(servicePartsText[1])
                    || wordMorphInfo.contains(servicePartsText[2])) {
                isServicePartsText = true;
            }
        }
        return isServicePartsText;
    }

    @SneakyThrows
    private String getLemma(String unrefinedWord) {
        List<String> wordMorphsInfo = luceneMorph.getMorphInfo(unrefinedWord);
        String wordMorphInfo = wordMorphsInfo.get(0);
        String lemma = wordMorphInfo.substring(0, wordMorphInfo.indexOf("|"));
        return lemma;
    }

    private String getClearHTML(String pageHTML) {
        Cleaner cleaner = new Cleaner(Safelist.basic());
        String clearPage = cleaner.clean(Jsoup.parse(pageHTML)).text();
        return clearPage;
    }

    @Getter
    @Setter
    class PartSnippet {
        private String partSnippet = "";
        int conditionalLengthLemmas;
        private ArrayList<Integer> indexSearchList = new ArrayList<>();

        public PartSnippet(String partSnippet, int conditionalLengthLemmas, ArrayList<Integer> indexSearchList) {
            this.partSnippet = partSnippet;
            this.conditionalLengthLemmas = conditionalLengthLemmas;
            this.indexSearchList = indexSearchList;
        }

        public PartSnippet(PartSnippet partSnippetOld) {
            this.partSnippet = partSnippetOld.getPartSnippet();
            this.conditionalLengthLemmas = partSnippetOld.getConditionalLengthLemmas();
            this.indexSearchList = partSnippetOld.getIndexSearchList();
        }

        public void addPartSnippet(PartSnippet halfPartSnippet) {
            partSnippet = partSnippet.concat(halfPartSnippet.getPartSnippet());
            indexSearchList = halfPartSnippet.getIndexSearchList();
        }

        public void addStringPartSnippet(String stringPartSnippetPartSnippet, int indexRemove) {
            partSnippet = partSnippet.concat(" ").concat(stringPartSnippetPartSnippet).concat(" ");
            indexSearchList.remove((Integer) indexRemove);
        }

        @Override
        public String toString() {
            return "PartSnippet:\n{" +
                    "partSnippet = " + partSnippet +
                    "\n indexSearchList = " + indexSearchList +
                    '}';
        }
    }
}


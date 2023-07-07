package searchengine.services;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;
import org.xml.sax.InputSource;
import searchengine.dto.snippets.DetailedSnippetsItem;
import searchengine.dto.snippets.SnippetsData;
import searchengine.dto.snippets.SnippetsResponce;
import searchengine.dto.statistics.DetailedStatisticsItem;
import searchengine.dto.statistics.StatisticsData;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.model.*;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.StringReader;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class SearchService
{
    private LemmatizationService lemmatizationService;

    private SiteRepository siteRepository;
    private PageRepository pageRepository;
    private LemmaRepository lemmaRepository;
    private IndexRepository indexRepository;
    private static final int MAX_LEMMA_FREQUENCY = 10000;

    public SearchService(LemmatizationService lemmatizationService,
                         PageRepository pageRepository, SiteRepository siteRepository,
                         LemmaRepository lemmaRepository, IndexRepository indexRepository)
    {
        this.lemmatizationService = lemmatizationService;

        this.siteRepository = siteRepository;
        this.pageRepository = pageRepository;
        this.lemmaRepository = lemmaRepository;
        this.indexRepository = indexRepository;
    }
    public SnippetsResponce startSearch(String query, int offset, int limit, String site)   // boolean
    {
//        SnippetsResponce response = new SnippetsResponce();
//        response.setResult(false);  // TODO: Проверка на ошибку - далее меняем на осмысленный ответ, при его получении !!!

        if(query.isEmpty() || query.isBlank())
            {
                System.out.println("В запросе передана пустая строка: " + query); // *
                SnippetsResponce response = new SnippetsResponce();
                response.setResult(false);
                response.setCount(-2);  // Эмуляция ответа ошибки с №2 - сайт(ы) не проиндексированы
                System.out.println("\nВ методе startSearch() ошибка №2 - отправляем SnippetsResponce: [\n" + response + "\n]");  // *
                return response;
            }

        if (site != null)
        {
            System.out.println("Получен запрос по поиску на единственной странице: " + site); // *
            if (isSiteIndexed(site))    // TODO: Исправить методпроверки isSiteIndexed() - стоит заглушка для возврата "true" !!!
            {
                System.out.println("Сайт: " + site + " - проиндексирован, выполняем поиск лемм по запросу: " + query); // *
//                response = searchOnSite(query, offset, limit, site);
                SnippetsResponce response = searchOnSite(query, offset, limit, site);
                System.out.println("\nВывод SnippetsResponce responce:\n" + response + " , для запроса: " + query); // *
                return response;
            }  else    // TODO: Написать код для передачи ошибки !!!
                {
                    System.out.println("Сайт: " + site + " - НЕ проиндексирован, ОШИБКА !!! поиск не выполняется: " + query); // *
                    SnippetsResponce response = new SnippetsResponce();
                    response.setResult(false);
                    response.setCount(-1);  // Эмуляция ответа ошибки с №1 - сайт(ы) не проиндексированы
                    System.out.println("\nВ методе startSearch() ошибка №1 - отправляем SnippetsResponce: [\n" + response + "\n]");  // *
                    return response;
                }
        }   else
                {
                    System.out.println("Получен запрос по поиску на всех страницах"); // *

                    // TODO: Исправить - Раскомментировать после отладки кода !!!
                    if (/*isSiteIndexed(site)*/ true)
                    {
                        System.out.println("Сайты проиндексированы, выполняем поиск лемм по запросу: " + query); // *

                        // TODO: Здесь ищем леммы по всем сайтам, 5 jule:
                        ArrayList<SnippetsResponce> responceSites = new ArrayList<>();
                        Iterable<searchengine.model.Site> siteIterable = siteRepository.findAll();
                        for (searchengine.model.Site siteDB : siteIterable)
                        {
                            SnippetsResponce responceSite = searchOnSite(query, offset, limit, siteDB.getUrl());
                            responceSites.add(responceSite);
                        }

                        // Здесь создаем новый объединенный SnippetsResponce и выполнить в нем сортировку заново:

                        //S_start
                        ArrayList<DetailedSnippetsItem> dataSites = new ArrayList<>();
                        for(SnippetsResponce responceSite : responceSites)
                        {
                            if(responceSite.isResult())
                            {
                                dataSites.addAll(responceSite.getData());
                            }
                        }

                        // TODO: Выполнить здесь сортировку полученного ArrayList<DetailedSnippetsItem> dataSites:
                        Collections.sort(dataSites, Comparator.comparing(DetailedSnippetsItem::getRelevance));
                        Collections.reverse(dataSites);
                        System.out.println("\nВывод отсортированного по убыванию relevance ArrayList<DetailedSnippetsItem> dataSites:\n" + dataSites); // *
                        //

                        SnippetsResponce responceAllSites = new SnippetsResponce(dataSites);
                        System.out.println("\nВывод SnippetsResponce responceAllSites:\n" + responceAllSites); // *
                        //S_end

                        return responceAllSites;

                        //
                    } else    // TODO: Написать код для передачи ошибки !!!
                        {
                            System.out.println("В списке есть НЕ проиндексированный(е) сайт(ы): " + site + " - ОШИБКА !!! поиск не выполняется: " + query); // *
                            SnippetsResponce response = new SnippetsResponce();
                            response.setResult(false);
                            response.setCount(-1);  // Эмуляция ответа ошибки с №1 - сайт(ы) не проиндексированы
                            System.out.println("\nВ методе startSearch() ошибка №1 - отправляем SnippetsResponce: [\n" + response + "\n]");  // *
                            return response;
                        }
                    //
                }
        //return responce;    // TODO: Добавить возврат false при ошибке - как все варианты if-else пройду и пропишу в них return по идее можно будет убрать !!!
    }

    public SnippetsResponce searchOnSite(String query, int offset, int limit, String site) // void
    {
        try
        {
            HashMap<String, Integer> splitLemmasText = lemmatizationService.splitLemmasText(query); // TODO: Здесь теряются повторы слов - нужно ли исправить чтобы не терялись?
            Set<String> lemmasList = splitLemmasText.keySet();
            System.out.println("Текст лемм поискового запроса: " + lemmasList); // *

            ArrayList<Lemma> lemmas = getLemma(lemmasList, site);   // Почему дебагер отображает 0 элементов в lemmas, когда SOUT выводит значения лемм ???
            System.out.println("В методе searchOnSite() - Получены леммы: " + lemmas); // *

            ArrayList<Page> lemmaPages = getPagesSearchList(lemmas,site);
            System.out.println("В методе searchOnSite() - Получено количество страниц с леммами: " + lemmaPages.size() + " :\n" + lemmaPages); // *

            // TODO: Вынести весь блок проверки с подсчетом релевантности в отдельный метод !!!
            if (!lemmaPages.isEmpty())
            {
                //Подсчет релевантности: TODO:добавить получение сниппетов
                HashMap<Page, Float> relevancePages = getRelevancePages(lemmaPages);
                System.out.println("В методе searchOnSite() - Получены страницы с леммами и их relativeRelevance: " + relevancePages); // *
                relevancePages = getAbsoluteRelevancePages(relevancePages);
                System.out.println("В методе searchOnSite() - Получены страницы с леммами и их absoluteRelevance: " + relevancePages); // *
                LinkedHashMap<Page, Float> sortedAbsoluteRelevancePages = getSortedAbsoluteRelevancePages(relevancePages);
                System.out.println("В методе searchOnSite() - Получены отсортированные страницы с леммами и их absoluteRelevance: " + sortedAbsoluteRelevancePages); // *
                //

                // Получение сниппетов:
                LinkedHashMap<Page, String> sortedAbsoluteRelevancePagesSnippet = lemmatizationService.getSnippet(sortedAbsoluteRelevancePages, /*lemmas,*/ lemmasList);
                //

                // TODO: Создание и сбор в связанный список DTO по snippet:
                System.out.println("\nВ методе searchOnSite() получен из метода getSnippetDTO() ArrayList<DetailedSnippetsItem>:\n [ " + getSnippetDTO(sortedAbsoluteRelevancePages, sortedAbsoluteRelevancePagesSnippet) + " ]");  // *

                //
                SnippetsResponce response = new SnippetsResponce(getSnippetDTO(sortedAbsoluteRelevancePages, sortedAbsoluteRelevancePagesSnippet)); // new SnippetsResponce() - ОК !!!
                //SnippetsData data = new SnippetsData(getSnippetDTO(sortedAbsoluteRelevancePages, sortedAbsoluteRelevancePagesSnippet));
//                data.setDetailed(getSnippetDTO(sortedAbsoluteRelevancePages, sortedAbsoluteRelevancePagesSnippet));
//                data.setCount();
                //response.setSnippets(data);   // ОК !!!
                //response.setResult(true);   // TODO: Может сделать проверкой ввиде ответа метода ???

                System.out.println("\nВ методе searchOnSite() отправляем SnippetsResponce: [\n" + response + "\n]");  // *

                return response;
                //

                //
            } else
                {   // TODO: Исправить ответ согласно ТЗ!!! Может имеет смысл в startSearch() выполнять проверки по response.setResult(false) и выдавать ответ в API ???
                    System.out.println("В методе searchOnSite() - Получен пустой список страниц с леммами (подсчет релевантности не проводим): " + " :\n" + lemmaPages); // *
                    SnippetsResponce response = new SnippetsResponce();
                    //SnippetsData data = new SnippetsData(new ArrayList<>());
                    //response.setSnippets(data);   // ОК !!!
                    response.setResult(false);
                    System.out.println("\nВ методе searchOnSite() отправляем SnippetsResponce: [\n" + response + "\n]");  // *
                    return response;
                }
            //

        } catch (IOException e)
        {
            System.err.println("Сработал catch в методе startSearch в классе SearchService: " + e);
        }

        System.out.println("\nВ методе searchOnSite() дошли до \"return null\" в конце метода - ОШИБКА !!!");  // *
        return null;   // TODO: Определиться какой ответ нужен и исправить ??? Проверить не получаю ли ошибку из-за возврата null - м.б. что-то другое вида SnippetsResponce возвращать ???
    }

    public List<DetailedSnippetsItem> getSnippetDTO(LinkedHashMap<Page, Float> sortedAbsoluteRelevancePages, LinkedHashMap<Page, String> sortedAbsoluteRelevancePagesSnippet)
    {
        List<DetailedSnippetsItem> detailed = new ArrayList<>();
        for(Page pageSearch : sortedAbsoluteRelevancePages.keySet())
        {
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

    public LinkedHashMap<Page, Float> getSortedAbsoluteRelevancePages(HashMap<Page, Float> relevancePages)
    {
        Map<Page, Float> sortedMap = relevancePages.entrySet().stream()
                 .sorted(Comparator.comparingDouble(e -> -e.getValue()))                                                // "(e -> e.getValue())" - в порядке возрастания
                 .collect(Collectors.toMap(
                         Map.Entry::getKey,
                         Map.Entry::getValue,
                         (a, b) -> { throw new AssertionError(); },
                         LinkedHashMap::new
                 ));

//*        System.out.println("В методе getSortedAbsoluteRelevancePages() - Получены отсортированные страницы с леммами и их absoluteRelevance: " +
//*                            sortedMap + "\n Построчно:"); // *
//*        sortedMap.entrySet().forEach(System.out::println); // *


        return (LinkedHashMap<Page, Float>) sortedMap;
    }

    public HashMap<Page, Float> getAbsoluteRelevancePages(HashMap<Page, Float> relevancePages)  // TODO: Проверить метод - не проверял 28!!!
    {
        Float maxRelevance = getMaxRelevancePages(relevancePages);
        for (Page searchLemmaPage : relevancePages.keySet())
        {
            Float relativeRelevance = relevancePages.get(searchLemmaPage);
            Float absoluteRelevance = relativeRelevance / maxRelevance;
            relevancePages.replace(searchLemmaPage, absoluteRelevance);
        }

        // TODO: Может здесь же и отсортировать?

        return relevancePages;
    }

    public Float getMaxRelevancePages(HashMap<Page, Float> relevancePages)
    {
        Page maxRelevancePages = Collections.max(
                relevancePages.entrySet(),
                new Comparator<Map.Entry<Page, Float>>()
                {
                    @Override
                    public int compare(Map.Entry<Page, Float> o1, Map.Entry<Page, Float> o2)
                    {
                        return o1.getValue() > o2.getValue()? 1:-1;
                    }
                }).getKey();
        System.out.println("Страница с максимальной относительной релевантностью: " + maxRelevancePages +
                            " , равной = " + relevancePages.get(maxRelevancePages));    // *
        return relevancePages.get(maxRelevancePages);
    }

    public HashMap<Page, Float> getRelevancePages(ArrayList<Page> lemmaPages)
    {
        HashMap<Page, Float> relevancePages = new HashMap<>();
        for (Page lemmaPage : lemmaPages)
        {
            relevancePages.put(lemmaPage, getRelativeRelevancePage(lemmaPage));
        }
        return relevancePages;
    }

    public Float getRelativeRelevancePage(Page lemmaPage)   // TODO: Проверить метод 27 !!!
        {
            float relativeRelevance = 0;
            ArrayList<Index> indexesLemmaPage = (ArrayList<Index>) indexRepository.findAllByPageId(lemmaPage.getId());
            for(Index index : indexesLemmaPage)
            {
                relativeRelevance = relativeRelevance + index.getRank();
            }
            return relativeRelevance;
        }

    public ArrayList<Page> getPagesSearchList(ArrayList<Lemma> lemmas, String  site)    // TODO: Исправить возврат непустых списков при отсутствующих леммах на странице
    {
        /**/Site siteSearch = siteRepository.findByUrl(site);
        ArrayList<Page> lemmasPages = new ArrayList<>();
        boolean isStartSearch = true;
        //Iterable<Lemma> lemmaPagesIterable = lemmas;

        ///*
        //
//        while (lemmas.size() != 0)
//        {
//            for (Iterator<Lemma> lemmaIterator = lemmas.iterator(); lemmaIterator.hasNext(); )
//            {
//                Lemma lemma = lemmaIterator.next();
            for (Iterator lemmaIterator = lemmas.iterator(); lemmaIterator.hasNext(); )
            {
                if(isStartSearch)
                {
                    Lemma lemma = (Lemma) lemmaIterator.next();

                    System.out.println("В классе SearchService в методе getPagesSearchList() - Получен список лемм: " + lemmas);   // *
                    lemmasPages = getPageWithLemmas(lemma, siteSearch, lemmasPages);
                    System.out.println("В классе SearchService в методе getPagesSearchList() при поиске леммы: " + lemma +
                            " - Получено количество в списке страниц: " + lemmasPages.size() +
                            " :\n" + lemmasPages);  // *
                    //lemmaIterator.remove(); TODO: Перенес за проверку "if(isStartSearch)"

                    // TODO: Проба остановить повторный круг запроса по леммам к сайту в getPageWithLemmas
                    if (lemmasPages.isEmpty()) {
                        isStartSearch = false;
                    }
                    //
                }
                lemmaIterator.remove();
            }
//        }
        //
       // */

        /*
        //
        for(Lemma lemma : lemmaPagesIterable)
        {
            System.out.println("В классе SearchService в методе getSnippet() - Получен список лемм: " + lemmas);   // *
            lemmaPages = getPageWithLemma(lemma, siteSearch);
            //lemmaPagesIterable;
            System.out.println("В классе SearchService в методе getSnippet() при поиске леммы: " + lemma +
                    " -  - Получен список страниц: " + lemmaPages);  // *
        }
        //
        */

        return lemmasPages;
    }

    /*
        public ArrayList<Page> getSnippet(ArrayList<Lemma> lemmas, String  site)
    {
        Site siteSearch = siteRepository.findByUrl(site);

        ArrayList<Page> lemmaPages = getPageWithLemma(lemmas.get(0), siteSearch);

        return lemmaPages;
}
    */

    public ArrayList<Page> getPageWithLemmas(Lemma lemma, Site  site, ArrayList<Page> pagesWithLemmas)
    {
        int lemmmaFindId = lemma.getId();
        int siteId = site.getId();
        List<Page> pages;

        if(pagesWithLemmas.isEmpty())
            {pages = pageRepository.findAllBySiteId(siteId);}
                else {pages = pagesWithLemmas;}

        ArrayList<Page> pagesWithLemma = new ArrayList<>();
        for (Page page : pages)
        {
            int pageId  = page.getId();
            Optional<Index> indexLemmaSiteSearch = indexRepository.findByPageIdAndLemmaId(pageId,lemmmaFindId);
            if(indexLemmaSiteSearch.isPresent())
            {
                pagesWithLemma.add(page);

                System.out.println("В классе SearchService в методе  getPageWithLemma() при поиске леммы: " + lemma +
                        " - добавлена страница: " + page.getPath() + " / [" + page.getSiteId() + "]");
            }
        }
        return pagesWithLemma;
    }

    /*
    public ArrayList<Page> getPageSitesWithLemmas(Lemma lemma, ArrayList<Page> pagesWithLemmas)
    {
        int lemmmaFindId = lemma.getId();
        int siteId = site.getId();
        List<Page> pages;

        if(pagesWithLemmas.isEmpty())
        {pages = pageRepository.findAllBySiteId(siteId);}
        else {pages = pagesWithLemmas;}

        ArrayList<Page> pagesWithLemma = new ArrayList<>();
        for (Page page : pages)
        {
            int pageId  = page.getId();
            Optional<Index> indexLemmaSiteSearch = indexRepository.findByPageIdAndLemmaId(pageId,lemmmaFindId);
            if(indexLemmaSiteSearch.isPresent())
            {
                pagesWithLemma.add(page);

                System.out.println("В классе SearchService в методе  getPageWithLemma() при поиске леммы: " + lemma +
                        " - добавлена страница: " + page.getPath() + " / [" + page.getSiteId() + "]");
            }
        }
        return pagesWithLemma;
    }
    */

    public ArrayList<Lemma> getLemma(Set<String> lemmasList, String  site)
    {
        Site siteSearch = siteRepository.findByUrl(site);
        ArrayList<Lemma> lemmas = new ArrayList<>();    //  Set<Lemma> lemmasA = new HashSet<>();

        for(String lemma : lemmasList)
        {
            Lemma savedLemma;
            Optional lemmaOptional = lemmaRepository.findByLemmaAndSiteId(lemma, siteSearch.getId());
            if(lemmaOptional.isPresent())
            {
                savedLemma = (Lemma) lemmaOptional.get();
                if(savedLemma.getFrequency() < MAX_LEMMA_FREQUENCY)
                {
                    lemmas.add(savedLemma);
                }
            } else {return new ArrayList<Lemma>();}   // TODO: Проверить не ломает ли код
        }
        Collections.sort(lemmas, (Comparator<Lemma>) (o1, o2) -> o1.getFrequency() - o2.getFrequency());
        return lemmas;
    }

    public boolean isSiteIndexed(String site)
    {
        if(site != null)
        {
            Site siteSearch = siteRepository.findByUrl(site);
     /**/   if(siteSearch.getStatus().equals(StatusType.FAILED))    // TODO: Заменить в product на "StatusType.INDEXED"
            {
                return true;
            } else
                {return false;}
        } else
            {
                // TODO: Блок для кода проверки всех сайтов на INDEXED: - проверить логику !!!
                Iterable<Site> allSites = siteRepository.findAll();
                for (searchengine.model.Site siteDB : allSites)
                {
                    boolean isStopSearch = siteDB.getStatus().equals(StatusType.FAILED) || siteDB.getStatus().equals(StatusType.INDEXING);
     /**/           if(isStopSearch)
                    {
                        System.out.println("Поиск остановлен, индексация сайта не завершена: " + siteDB.getUrl()); // *
                        return false;
                    }
                    return true;
                }
                //
            }
        return true;    // Проверить - не ломает ли? В логике не нужен !!!
    }

    public ArrayList<Page> getPageWithLemma(Lemma lemma, Site  site)
    {
        int lemmmaFindId = lemma.getId();

        int siteId = site.getId();
        List<Page> pages = pageRepository.findAllBySiteId(siteId);

        ArrayList<Page> pagesWithLemma = new ArrayList<>();
        for (Page page : pages)
        {
            int pageId  = page.getId();
            Optional<Index> indexLemmaSiteSearch = indexRepository.findByPageIdAndLemmaId(pageId,lemmmaFindId);
            if(indexLemmaSiteSearch.isPresent())
            {
                pagesWithLemma.add(page);

                System.out.println("В классе SearchService в методе  getPageWithLemma() при поиске леммы: " + lemma +
                        " - добавлена страница: " + page.getPath() + " / [" + page.getSiteId() + "]");
            }
        }
        return pagesWithLemma;
    }
}

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

//
//  Collections.sort() с компаратором
            /*        Collections.sort(list, new Comparator<Company>() {
            @Override
            public int compare(Company o1, Company o2) {
                return o1.getPrice() - o2.getPrice();
            }
            });
            */
            /*
            Collections.sort(list, (o1, o2) -> o1.getPrice() - o2.getPrice());
            */
            /*
            Comparator comparator = (elem1, elem2) ->
            {
                return elem1.getId().compareTo(elem2.getId());
            };
            */
//



// System.out.println("Получены леммы: [ " + /*lemmasIterable + */ " ]");
//        //Iterable<String> lemmasListIterable = lemmasList;
//        //Iterable<Lemma> lemmasIterable = lemmaRepository.findAllByLemma(lemmasListIterable);
//        //lemmaRepository.findByLemmaAndSiteId("1",1).forEach(lemmas::add);



    /*
    public ArrayList<Page> getPagesSearchList(ArrayList<Lemma> lemmas, String  site)    // TODO: Исправить возврат непустых списков при отсутсвтующих леммах на странице
    {
        Site siteSearch = siteRepository.findByUrl(site);
        ArrayList<Page> lemmasPages = new ArrayList<>();
        boolean isStopSearch = false;
        //Iterable<Lemma> lemmaPagesIterable = lemmas;

        for (Iterator lemmaIterator = lemmas.iterator(); lemmaIterator.hasNext(); )
        {

            Lemma lemma = (Lemma) lemmaIterator.next();

            System.out.println("В классе SearchService в методе getPagesSearchList() - Получен список лемм: " + lemmas);   // *
            lemmasPages = getPageWithLemmas(lemma, siteSearch, lemmasPages);
            System.out.println("В классе SearchService в методе getPagesSearchList() при поиске леммы: " + lemma +
                    " - Получено количество в списке страниц: " + lemmasPages.size() +
                    " :\n" + lemmasPages);  // *
            lemmaIterator.remove();

            // TODO: Проба остановить повторный круг запроса по леммам к сайту в getPageWithLemmas
            if(lemmasPages.isEmpty()) {isStopSearch = true; }
            //
        }

        return lemmasPages;
    }
    */



    /*
    public static Document getDocumentPage(String content) throws Exception
    {
        DocumentBuilder builder = getDocumentBuilder();
        Document document = builder.parse(new InputSource(new StringReader(content)));
        return document;
    }

    public static DocumentBuilder getDocumentBuilder() throws ParserConfigurationException
    {
        DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
        builderFactory.setNamespaceAware(true);
        DocumentBuilder builder = builderFactory.newDocumentBuilder();
        return builder;
    }
    */

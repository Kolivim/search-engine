package searchengine.services;

import lombok.SneakyThrows;
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

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.stream.IntStream;

@Service
public class LemmatizationServiceImpl implements LemmatizationService
{
    private SiteRepository siteRepository;
    private PageRepository pageRepository;
    private LemmaRepository lemmaRepository;
    private IndexRepository indexRepository;

    private String[] servicePartsText = {"ПРЕДЛ", "МЕЖД", "СОЮЗ"};
    private HashMap<String, Integer> lemmasText = new HashMap<String, Integer>();
    private LuceneMorphology luceneMorph = new RussianLuceneMorphology();   // TODO: После отладки вынести в класс !!! M
    private static final int SNIPPET_LENGHT = 240;
    @Autowired
    public LemmatizationServiceImpl(PageRepository pageRepository, SiteRepository siteRepository,
                                    LemmaRepository lemmaRepository, IndexRepository indexRepository) throws IOException
    {
        this.siteRepository = siteRepository;
        this.pageRepository = pageRepository;

        // 19 june:
        this.lemmaRepository = lemmaRepository;
        this.indexRepository = indexRepository;
        //
    }

    @Override
    public boolean indexPage(String path)
    {
        HashMap<String, String> pathInfo = getPathAndSiteUrl(path);
        String pagePath = pathInfo.get("pagePath");
        String siteUrl = pathInfo.get("siteUrl");

        if(siteRepository.existsByUrl(siteUrl))
        {
            System.out.println(path + " : В классе LSImpl в методе indexPage вход в метод после true в проверке if на существование сайта");
            Site site = siteRepository.findByUrl(siteUrl);
            Integer siteId = site.getId();

            // TODO: Здесь удаляем данные в index и после нее в lemma
//            Page deletedPage = pageRepository.findByPathAndSite(pagePath, site);//            List<Index> deletedIndexes = indexRepository.findAllByPageId(deletedPage.getId());//            System.out.println("В классе LSImpl в методе indexPage к удалению следующие Index: " + deletedIndexes); // *//            for(Index deletedIndex : deletedIndexes)//                {//                    indexRepository.delete(deletedIndex);//                }
            List<Integer> deletesLemmaId = deleteIndexes(pagePath, site);
            //

            //  TODO: Здесь удаляем в lemma (после index!!!)
            deleteLemmaOnPage(deletesLemmaId, site);
            //

            PageWriter removedPage = new PageWriter();
            Page page = removedPage.removedOrAddPage(pagePath, path, site);
            Integer pageId = page.getId();

            // TODO: Здесь добавляем данные в index и lemma
//            addLemmas(page); // По идее в ___Page page = removedPage.removedOrAddPage(pagePath, path, site)___ добавляется
            //

            System.out.println(path + " : В классе LSImpl завершение метода indexPage, новый pageId = " + pageId + " , для пути: " + path);// *
            return true;

        }  else
            {
                System.out.println(path + " : В классе LSImpl в методе indexPage вход в метод после false в проверке if на существование сайта"); // *
                System.out.println(path + " : Данная страница находится за пределами сайтов, указанных в конфигурационном файле"); // *
                return false;
            }
    }

    public void deleteLemmaOnPage(List<Integer> lemmasId, Site site)
    {
        System.out.println("В классе LSImpl в методе indexPage/deleteLemmaOnPage к удалению следующие lemmaId, в количестве = " + lemmasId.size() + " : " + lemmasId); // *
        for(Integer lemmaId : lemmasId)
        {
            Lemma lemmaOnDeletedPage;
            Optional lemmaOptional = lemmaRepository.findByIdAndSiteId(lemmaId, site.getId());
            if (lemmaOptional.isPresent())
            {
                lemmaOnDeletedPage = (Lemma) lemmaOptional.get();
                if (lemmaOnDeletedPage.getFrequency() > 1)
                {
                    lemmaOnDeletedPage.frequencyLemmaDecr();
                    lemmaRepository.save(lemmaOnDeletedPage);
                } else
                    {
                        lemmaRepository.delete(lemmaOnDeletedPage);
                    }
            } else
            {
                System.err.println("Ошибка - леммаID/сайт: " + lemmaId + " / " + site.getUrl() + " - не существует");
            }
        }
    }

    public List<Integer> deleteIndexes(String pagePath, Site site)   // TODO: Нужна ли проверка на существование перед удвлением ???
    {
        List<Integer> deletesLemmaId = new ArrayList<>();
        Page deletedPage = pageRepository.findByPathAndSite(pagePath, site);
        List<Index> deletedIndexes = indexRepository.findAllByPageId(deletedPage.getId());
        System.out.println("В классе LSImpl в методе indexPage/deleteIndexes к удалению следующие Index, в количестве = " + deletedIndexes.size() + " :\n" + deletedIndexes); // *
        for(Index deletedIndex : deletedIndexes)
        {
            deletesLemmaId.add(deletedIndex.getLemmaId());
            indexRepository.delete(deletedIndex);
        }
        return deletesLemmaId;
    }

    @Override
    public void deleteSiteIndexAndLemma(Site site)
    {
        // TODO: Здесь удаляем Index:
        Iterable<Page> deletedPagesIterable = pageRepository.findAllBySiteId(site.getId());
        for (Page deletedPage : deletedPagesIterable)
        {
            List<Index> deletedIndexes = indexRepository.findAllByPageId(deletedPage.getId());
            System.out.println("В классе LSImpl в методе deleteSiteIndexAndLemma к удалению следующие Index, в количестве = " + deletedIndexes.size() + " :\n" + deletedIndexes); // *
            for(Index deletedIndex : deletedIndexes)
            {
                indexRepository.delete(deletedIndex);
            }
        }
        //

        // TODO: Здесь удаляем Lemma:
        deleteLemmaOnSite(site);
        //
    }

    public void deleteLemmaOnSite(Site site)
    {
        System.out.println("В классе LSImpl начало метода deleteLemmaOnSite у сайта: " + site.getUrl()); // *
        Iterable<Lemma> deletedLemmasInSiteIterable = lemmaRepository.findAllBySiteId(site.getId());
        for(Lemma deleteLemma: deletedLemmasInSiteIterable)
        {
            lemmaRepository.delete(deleteLemma);
        }
    }

    @Override
    public void indexNewPage(Page page)
    {
            addLemmas(page);
            System.out.println(page.getPath() + " , сайта" + page.getSite().getUrl() + " : В классе LSImpl в методе indexNewPage завершение метода"); // *
    }

    private void addLemmas(Page indexingPage)
    {
        try
            {
                HashMap<String, Integer> splitLemmasText = splitLemmasText(getClearHTML(indexingPage.getContent()));

                // *
                System.out.println("В классе LSImpl методе addLemmas к добавлению получена Lemma в количестве: " + splitLemmasText.size()); // *
                int indexCountAdd = 0;  // *
                //

                // Перебираем HashMap и добавляем Lemma, проверяя наличие lemma по site
                for(String lemma : splitLemmasText.keySet())
                    {
                        Lemma savedLemma;
                        Optional lemmaOptional = lemmaRepository.findByLemmaAndSiteId(lemma, indexingPage.getSiteId());
                        if(lemmaOptional.isPresent())
                        {
                            savedLemma = (Lemma) lemmaOptional.get();
                            savedLemma.frequencyLemmaIncr();
                        } else
                            {
                                savedLemma = new Lemma(indexingPage.getSiteId(), lemma, 1);
                            }
                        System.out.println("В классе LSImpl методе addLemmas к сохранению получена savedLemma: " + savedLemma); // *
                        lemmaRepository.save(savedLemma);

                        // Добавление index в таблицу index:
                        Index savedIndex = new Index(indexingPage.getId(), savedLemma.getId(), splitLemmasText.get(lemma));

                        //
                        System.out.println("В классе LSImpl методе addLemmas к сохранению получен savedIndex: " + savedIndex); // *
                        indexCountAdd++;    // *
                        //

                        indexRepository.save(savedIndex);
                        //
                    }

                //
                System.out.println("В классе LSImpl методе addLemmas итого к сохранению получено и посчитано в -count- savedIndex в количестве: " + indexCountAdd); // *
                //

                //
            } catch (IOException e)
                {
                    System.err.println("В классе LSImpl методе addLemmas сработал IOException(e) ///1 " + e.getMessage() + " ///2 " + e.getStackTrace() + " ///3 " + e.getSuppressed() + " ///4 " + e.getCause() + " ///5 " + e.getLocalizedMessage() + " ///6 " + e.getClass());
                }
    }

    @Override
    public LinkedHashMap<Page, String> getSnippet(LinkedHashMap<Page, Float> sortedAbsoluteRelevancePages, /*ArrayList<Lemma> lemmas,*/ Set<String> lemmasList)
    {
        System.out.println("В методе getSnippet() класса LemmServImpl - Передан sortedAbsoluteRelevancePages страниц с их абсолютной релевантностью: \n[ " + sortedAbsoluteRelevancePages + " ]"); // *

        LinkedHashMap<Page, ArrayList<String>> sortedPagesAndSnippet = new LinkedHashMap<>();

        LinkedHashMap<Integer, Integer> contentIndexAndLength = new LinkedHashMap<>();

        LinkedHashMap<Page, String> sortedAbsoluteRelevancePagesSnippet = new LinkedHashMap<>();

        for(Page pageSearch : sortedAbsoluteRelevancePages.keySet())
        {
            ArrayList<Integer> lemmasIndex = new ArrayList<Integer>();
            String content = getClearHTML(pageSearch.getContent());
            content = content.trim();

            String[] splitText = content.split("\\s+");

            System.out.println("В методе getSnippet() класса LemmServImpl - Получен текст страницы: " +
                    pageSearch + " : " + splitText); // *

            //
            StringBuilder builder = new StringBuilder();
            ArrayList<String> contentPage = new ArrayList<>();
            //

            for (int i = 0; i < splitText.length; i++)
            {
                String unrefinedWord = splitText[i];
//*                System.out.println("В методе getSnippet() класса LemmServImpl - Получен unrefinedWord: " + unrefinedWord); // *
                unrefinedWord = unrefinedWord.replaceAll("([^а-яА-Я\\s])", "").trim().toLowerCase(new Locale("ru", "RU"));    // Заменить на введенную новую переменную для проверки - ниже она же добавляется
//*                System.out.println("В методе getSnippet() класса LemmServImpl - Полученный unrefinedWord преобразован в: " + unrefinedWord); // *
                if(!unrefinedWord.isBlank() & !unrefinedWord.isEmpty())
                {
//*                    System.out.println("В методе getSnippet() класса LemmServImpl - Преобразованный unrefinedWord: " + unrefinedWord + " - прошел проверку \"if(!unrefinedWord.isBlank() & !unrefinedWord.isEmpty())\" и передан к получению из него леммы"); // *
                    String lemma = getLemma(unrefinedWord);
                    if (lemmasList.contains(lemma))   // (lemmasString.contains(lemma))
                    {
                        builder.append(" "); // builder.append(" "); Для создания пробела перед леммой - проверить нужно ли ???
                        contentPage.add(builder.toString()); // Проверить сработает ли - если что вынести в отдельную переменную
                        builder = new StringBuilder();
                        String lemmaString = "<b>".concat(splitText[i]).concat("</b>");
                        contentPage.add(lemmaString);  //  (splitText[i])

                        lemmasIndex.add(i);

                        System.out.println("\nВ методе getSnippet() класса LemmServImpl - Получено совпадение текста страницы с леммой: " +
                                lemma + " , имеющей вид на сайте: " + splitText[i] + " , имеющей индекс в массиве = " + i); // *
                    } else {
                        builder.append(splitText[i]);
                        builder.append(" ");
                    }

                } else // Дописываем в текст страницы цифровые / знаковые и иные не удовлетворяющие условию if() символы
                    {
                        builder.append(splitText[i]);
                        builder.append(" ");
                    }
            }
            if(!builder.isEmpty()) {contentPage.add(builder.toString());}
            //

            // TODO: Здесь вызываем getSnippetPage(), в котором в т.ч. оборачиваем в <div> и добавляем результат в сортированный список:
            int countLemmaOnPage = lemmasIndex.size(); // TODO: Добавить проверку найдены ли леммы - если их  - пропускаем подсчет и выводим ошибку поиска лемм на странице
            System.out.println("В методе getSnippet() класса LemmServImpl - Получен list текста страницы с выделенной леммой: " + contentPage); // *
            String contentPageSnippet = getSnippetPage(contentPage, lemmasList,countLemmaOnPage, lemmasIndex);   // TODO: Передать вырезанный сниппет страницы дальше
            System.out.println("В методе getSnippet() класса LemmServImpl - Получен contentPageSnippet текста страницы с выделенным snippet: " + contentPageSnippet); // *
            //

            //  TODO: Здесь добавляем полученный сниппет страницы в MAP:
            sortedAbsoluteRelevancePagesSnippet.put(pageSearch, contentPageSnippet);
            //
        }

        System.out.println("В методе getSnippet() класса LemmServImpl - Получен sortedAbsoluteRelevancePagesSnippet страниц с текстом выделенных snippet: \n[ " + sortedAbsoluteRelevancePagesSnippet + " ]"); // *

        return sortedAbsoluteRelevancePagesSnippet;
    }

    public String getSnippetPage(ArrayList<String> contentPage, Set<String> lemmasList, int countLemmaOnPage, ArrayList<Integer> lemmasIndex) // TODO: Переписать с выделением в отдельные методы до 30 строк длиной !!! Проанализировать как убрать повтор кода !!!
    {
        //
        int indexEndContentPage = (contentPage.size()-1);
        ArrayList<Integer> lemmasIndexSearch = getIndexSearchList(0, indexEndContentPage);  //  3 june
        //lemmasIndexSearch.addAll(lemmasIndex);  //  3 june
        //

        if (countLemmaOnPage < 1)
        {
            System.err.println("В методе getSnippetPage() класса LemmServImpl - Получен countLemmaOnPage < 1 - леммы не найдены, ошибка поиска лемм: " + countLemmaOnPage); // *
            return null;
        }

        //
        /*
        int lengthLemmas = getLengthLemmas(contentPage);
        int conditionalLengthLemmas = (SNIPPET_LENGHT - lengthLemmas) / countLemmaOnPage;  // Не заложил удлиннение лемм в сниппете на троеточие с двух сторон !!!
         */
        //

        String snippetPage = "";
        int conditionalLengthLemmas = getConditionalLengthLemmas(contentPage, countLemmaOnPage);
        int contentLength = SNIPPET_LENGHT;

        // Блок с концом:
        String snippetEnd = "";
        int excludedIndexEnd = 0;
        if(isLemma(contentPage.get(contentPage.size()-1))) // Лемма последняя на странице ДА/НЕТ
        {
            int indexPreEnd = contentPage.size() - 2;
            if (!isLemma(contentPage.get(indexPreEnd)))
            {
                if (contentPage.get(indexPreEnd).length() > (conditionalLengthLemmas + conditionalLengthLemmas / 2))
                {
                    String partSnippet = "...".concat(contentPage.get(indexPreEnd).substring(contentPage.get(indexPreEnd).length() - conditionalLengthLemmas, contentPage.get(indexPreEnd).length()));
                    snippetEnd = snippetEnd.concat(partSnippet);
                    contentLength -= partSnippet.length();
                } else
                {
                    String partSnippet = " ...".concat(contentPage.get(indexPreEnd));
                    snippetEnd = snippetEnd.concat(partSnippet);
                    contentLength -= partSnippet.length();
                    excludedIndexEnd++;
                    lemmasIndexSearch.remove((Integer)indexPreEnd);  //  3 june
                }
            }
            int indexEndLemma = contentPage.size() - 1;
            snippetEnd = snippetEnd.concat(contentPage.get(contentPage.size()-1));
            contentLength -= contentPage.get(indexEndLemma).length();
            excludedIndexEnd++;
            lemmasIndexSearch.remove((Integer)indexEndLemma);  //  3 june

        } else
            {
                //
                int indexPrePreEnd = contentPage.size() - 3;
                if(!isLemma(contentPage.get(indexPrePreEnd)))
                {
                    if (contentPage.get(indexPrePreEnd).length() > (conditionalLengthLemmas))
                    {
                        int indexStringStop = contentPage.get(indexPrePreEnd).length() - 1;
                        int indexStringStart = indexStringStop - conditionalLengthLemmas / 2;
                        String partSnippet = "...".concat(contentPage.get(indexPrePreEnd).substring(indexStringStart, indexStringStop));
                        snippetEnd = snippetEnd.concat(partSnippet);
                        contentLength -= partSnippet.length();
                    } else
                        {
                            String partSnippet = contentPage.get(indexPrePreEnd);
                            snippetEnd = snippetEnd.concat(partSnippet);
                            contentLength -= partSnippet.length();
                            excludedIndexEnd++;
                            lemmasIndexSearch.remove((Integer)indexPrePreEnd);  //  3 june
                        }
                }
                //

                //
                int indexEndLemma = contentPage.size() - 2;
                String partSnippet1 = contentPage.get(indexEndLemma);
                snippetEnd = snippetEnd.concat(" ").concat(partSnippet1).concat(" ");
                contentLength -= partSnippet1.length();
                excludedIndexEnd++;
                lemmasIndexSearch.remove((Integer)indexEndLemma);  //  3 june
                //

                //
                int indexEnd = contentPage.size() - 1;
                if(contentPage.get(indexEnd).length() > (conditionalLengthLemmas/2))
                {
                    String partSnippet = contentPage.get(indexEnd).substring(0,conditionalLengthLemmas/2).concat("...");
                    snippetEnd = snippetEnd.concat(partSnippet);
                    contentLength -= partSnippet.length();
                    excludedIndexEnd++;
                    lemmasIndexSearch.remove((Integer) indexEnd);  //  3 june
                } else
                    {
                        String partSnippet = contentPage.get(indexEnd);
                        snippetEnd = snippetEnd.concat(partSnippet);
                        contentLength -= partSnippet.length();
                        excludedIndexEnd++;
                        lemmasIndexSearch.remove((Integer)indexEnd);  //  3 june
                    }
                //
            }
        //

        int indexEnd = (contentPage.size()-1) - excludedIndexEnd;   // Верно ли ???
        boolean isExistIndex = indexEnd > 0;

        // Блок со стартом:
        String snippetStart = "";
        Integer excludedIndexStart = 0; // int excludedIndexStart = 0;
        if(isExistIndex)
        {
            int index = 0;
            if(isLemma(contentPage.get(index)) & lemmasIndexSearch.contains(index)) // Лемма первая на странице
            {
                snippetStart = snippetStart.concat(contentPage.get(index));
                contentLength -= contentPage.get(index).length();
                excludedIndexStart++;
                lemmasIndexSearch.remove((Integer)index);

                //
                int indexPost = 1;
                if(lemmasIndexSearch.contains(indexPost))   // 3 june v2
                {
                    if (contentPage.get(indexPost).length() > (conditionalLengthLemmas + conditionalLengthLemmas / 2))
                    {
                        snippetStart = snippetStart.concat(contentPage.get(indexPost).substring(0, conditionalLengthLemmas)).concat("...");
                        contentLength -= conditionalLengthLemmas;
                    } else
                        {
                            snippetStart = snippetStart.concat(contentPage.get(indexPost));
                            contentLength -= contentPage.get(indexPost).length();
                            excludedIndexStart++;
                            lemmasIndexSearch.remove((Integer) indexPost);
                        }
                }
                //
            } else
                {
                    //
                    int indexFirst = 0;
                    if(lemmasIndexSearch.contains(indexFirst))   // 3 june v2
                    {
                        if(contentPage.get(indexFirst).length() > (conditionalLengthLemmas/2))
                        {
                            int indexStringStop = contentPage.get(indexFirst).length() - 1;
                            int indexStringStart = indexStringStop - conditionalLengthLemmas/2;
                            String partSnippet = "...".concat(contentPage.get(indexFirst).substring(indexStringStart,indexStringStop));
                            snippetStart = snippetStart.concat(partSnippet);
                            contentLength -= partSnippet.length();
                            excludedIndexStart++;
                            lemmasIndexSearch.remove((Integer)indexFirst);
                        } else
                            {
                                String partSnippet = contentPage.get(indexFirst);
                                snippetStart = snippetStart.concat(partSnippet);
                                contentLength -= partSnippet.length();
                                excludedIndexStart++;
                                lemmasIndexSearch.remove((Integer)indexFirst);
                            }
                    }
                    //

                    //
                    int indexPost = 1;
                    if(lemmasIndexSearch.contains(indexPost))   // 3 june v2
                    {
                        String partSnippet1 = contentPage.get(indexPost);   // Основной элемент
                        snippetStart = snippetStart.concat(" ").concat(partSnippet1).concat(" ");
                        contentLength -= partSnippet1.length();
                        excludedIndexStart++;
                        lemmasIndexSearch.remove((Integer) indexPost);
                    }
                    //

                    //
                    int indexPostPost = 2;
                    if(!isLemma(contentPage.get(indexPostPost)) & lemmasIndexSearch.contains(indexPostPost))
                    {
                        if (contentPage.get(indexPostPost).length() > (conditionalLengthLemmas + conditionalLengthLemmas / 2))
                        {
                            String partSnippet = contentPage.get(indexPostPost).substring(0, conditionalLengthLemmas / 2).concat("...");
                            snippetStart = snippetStart.concat(partSnippet);
                            contentLength -= partSnippet.length();
                        } else
                            {
                                String partSnippet = contentPage.get(indexPostPost);
                                snippetStart = snippetStart.concat(partSnippet);
                                contentLength -= partSnippet.length();
                                excludedIndexStart++;
                                lemmasIndexSearch.remove((Integer)indexPostPost);
                            }
                    }
                    //
                }
        } else
            {
                System.out.println("Ошибка либо индексы перебраны в snippetEnd!!! startSnippet");
            }
        //

        int indexStart = excludedIndexStart;   // Верно ли ???
        boolean isExistIndexStart = indexStart < 0;

        // Поиск частей сниппетов посередине:
        int[] arrIndexMiddle = IntStream.iterate(excludedIndexStart, i -> i + 1).limit(excludedIndexEnd).toArray();
        ArrayList<Integer> indexSearchList = getIndexSearchList(indexStart, indexEnd);  //ArrayList<Integer> l = new ArrayList<>(); //ArrayList<Integer> list = (ArrayList<Integer>) Collections.singletonList(arrIndexMiddle);

        String snippetMiddle = "";
        for (int i = indexStart; i <= indexEnd; i++)
        {
            if(isLemma(contentPage.get(i)))
            {
                //
                //snippetMiddle = snippetMiddle.concat(" ").concat(getMiddlePartSnippet(contentPage, conditionalLengthLemmas, i,  indexSearchList));
                //
                ArrayList<String> snippetPartPageWithRemoveIndex = getMiddlePartSnippet(contentPage, conditionalLengthLemmas, i,  indexSearchList);
                String partSnippetMiddle = snippetPartPageWithRemoveIndex.get(0);
                snippetMiddle = snippetMiddle.concat(" ").concat(partSnippetMiddle);
                Integer indexRemove = Integer.valueOf(snippetPartPageWithRemoveIndex.get(1));
                if(indexRemove > 0)
                    {
                        indexSearchList.remove(indexRemove);
                    }
                //
            }
        }
        //

        //Объединение сниппетов:
        System.out.println("В методе getSnippet() класса LemmServImpl - Получены String сниппетов страницы: \nStart: " + snippetStart +
                "\nMiddle: " + snippetMiddle + "\nEnd: " + snippetEnd); // *
        //

        snippetPage = snippetPage.concat(snippetStart).concat(" ").concat(snippetMiddle).concat(" ").concat(snippetEnd);

        return snippetPage;
    }

    public ArrayList<Integer> getIndexSearchList(int excludedIndexStart, int excludedIndexEnd)
    {
        ArrayList<Integer> indexSearchList = new ArrayList<>();
        for (int i = excludedIndexStart; i <= excludedIndexEnd; i++)
        {
            indexSearchList.add(i);
        }
        return indexSearchList;
    }

    public ArrayList<String> getMiddlePartSnippet(ArrayList<String> contentPage, int conditionalLengthLemmas, int index,  ArrayList<Integer> indexSearchList)
    {
        ArrayList<String> snippetPartPageWithRemoveIndex = new ArrayList<>();
        String removeIndex = "0";
        int snippetPartPageLength = 0;
        String snippetPartPage = "";
        if(isLemma(contentPage.get(index)))
        {
            // Слева от леммы текст:
            int partIndexBefore = index - 1;
            if(!isLemma(contentPage.get(partIndexBefore)))
            {
                if (indexSearchList.contains(partIndexBefore))
                {
                    if (contentPage.get(partIndexBefore).length() > (conditionalLengthLemmas / 2))
                    {
                        int indexStringStop = contentPage.get(partIndexBefore).length() - 1;
                        int indexStringStart = indexStringStop - conditionalLengthLemmas / 2;
                        String partSnippet = "...".concat(contentPage.get(partIndexBefore).substring(indexStringStart, indexStringStop));
                        snippetPartPage = snippetPartPage.concat(partSnippet);
                        snippetPartPageLength += partSnippet.length();
                    } else
                        {
                            String partSnippet = contentPage.get(partIndexBefore);
                            snippetPartPage = snippetPartPage.concat(partSnippet);
                            snippetPartPageLength += partSnippet.length();
                            // excludedIndexStart++;    // Метод подается в полный перебор середины страницы - отмечать уже пройденные части не нужно
                        }
                } else
                    {
                        System.out.println("Элемент snippet находится за пределами диапазона записи сниппета"); // *
                    }
            }  else
                {
                    System.out.println("Элемент" + contentPage.get(partIndexBefore) +  " - является леммой"); // *
                }
            //

            //  Сама лемма:
            String partSnippet1 = contentPage.get(index);   // Основной элемент
            snippetPartPage = snippetPartPage.concat(" ").concat(partSnippet1).concat(" ");
            snippetPartPageLength += partSnippet1.length();
            // excludedIndexStart++;    // Метод подается в полный перебор середины страницы - отмечать уже пройденные части не нужно

            // Справа от леммы текст:
            int partIndexAfter = index + 1;
            if(!isLemma(contentPage.get(partIndexAfter)))
            {
                if (indexSearchList.contains(partIndexAfter))
                {
                    if (contentPage.get(partIndexAfter).length() > (conditionalLengthLemmas / 2))
                    {
                        int indexStringStart = 0;
                        int indexStringStop = conditionalLengthLemmas / 2;
                        String partSnippet = contentPage.get(partIndexAfter).substring(indexStringStart, indexStringStop).concat("...");
                        snippetPartPage = snippetPartPage.concat(partSnippet);
                        snippetPartPageLength += partSnippet.length();
                    } else
                        {
                            String partSnippet = contentPage.get(partIndexAfter);
                            snippetPartPage = snippetPartPage.concat(partSnippet);
                            snippetPartPageLength += partSnippet.length();
                            removeIndex = Integer.toString (partIndexAfter);
                            // excludedIndexStart++;    // Метод подается в полный перебор середины страницы - отмечать уже пройденные части не нужно
                        }
                } else
                    {
                        System.out.println("Элемент snippet находится за пределами диапазона записи сниппета"); // *
                    }
            } else
                {
                    System.out.println("Элемент" + contentPage.get(partIndexAfter) +  " - является леммой"); // *
                }
            //
        }
        //return snippetPartPage;
        snippetPartPageWithRemoveIndex.add(snippetPartPage);
        snippetPartPageWithRemoveIndex.add(removeIndex);
        return snippetPartPageWithRemoveIndex;
    }

    public boolean isLemma(String text)
    {
        boolean isLemma = false;
        if(text.contains("<b>"))
        {
            isLemma = true;
        }
        return isLemma;
    }

    public int getConditionalLengthLemmas(ArrayList<String> contentPage, int countLemmaOnPage)
    {
        int lengthLemmas = getLengthLemmas(contentPage);
        int conditionalLengthLemmas = (SNIPPET_LENGHT - lengthLemmas) / countLemmaOnPage;  // Не заложил удлиннение лемм в сниппете на троеточие с двух сторон !!!
        return conditionalLengthLemmas;
    }

    public int getLengthLemmas(ArrayList<String> contentPage)
    {
        int lengthLemmas = 0;
        for (String text : contentPage)
        {
            if(isLemma(text))
            {
                lengthLemmas += text.length();
            }
        }
        return lengthLemmas;
    }

    public ArrayList<String> getLemmasList(ArrayList<Lemma> lemmas)
    {
        ArrayList<String> lemmasString = new ArrayList<>();
        for(Lemma lemma : lemmas) {lemmasString.add(lemma.getLemma());}
        return lemmasString;
    }

    private HashMap<String, String> getPathAndSiteUrl(String path)
    {
        HashMap<String, String> pathInfo = new HashMap<String, String>();
        try
        {
            URL url = new URL(path);
            pathInfo.put("pagePath", url.getPath());
            String siteURL = url.getProtocol();
            siteURL = siteURL + "://";
            siteURL = siteURL + url.getHost();
            pathInfo.put("siteUrl", siteURL);
            System.out.println(pathInfo);  // *
        } catch (MalformedURLException e)
            {
                System.err.println("В классе LSImpl методе indexPage сработал MalformedURLException(e) ///1 " + e.getMessage() + " ///2 " + e.getStackTrace() + " ///3 " + e.getSuppressed() + " ///4 " + e.getCause() + " ///5 " + e.getLocalizedMessage() + " ///6 " + e.getClass() + " ///7 на переданной странице:  " + path);
            }
        return pathInfo;
    }

    public HashMap<String, Integer> splitLemmasText(String text) throws IOException
    {
        HashMap<String, Integer> splitLemmasText = new HashMap<String, Integer>();
        String[] splitText = arrayContainsRussianUnrefinedWords(text);
        for (Iterator<String> splitTextIterator = Arrays.stream(splitText).iterator(); splitTextIterator.hasNext(); )
        {
            String unrefinedWord = splitTextIterator.next();
            if(isRussianServicePartsText(unrefinedWord))
                {
                    continue; // TODO: Проверить срабатывает ли сброс прохождения дальнейшего кода в цикле ???
                }
            String lemma = getLemma(unrefinedWord);
            if(splitLemmasText.containsKey(lemma))
                {
                    Integer count = splitLemmasText.get(lemma);
                    count++;
                    splitLemmasText.replace(lemma, count);
                }  else {splitLemmasText.put(lemma, 1);}
        }
        return splitLemmasText;
    }

    private String[] arrayContainsRussianUnrefinedWords(String text)
    {
        return text.toLowerCase(new Locale("ru", "RU"))
                .replaceAll("([^а-я\\s])", " ")
                .trim()
                .split("\\s+");
    }

    @SneakyThrows
    private boolean isRussianServicePartsText(String unrefinedWord) // TODO: Найти описание библиотеки и заменить
    {
        boolean isServicePartsText= false;
        List<String> wordMorphInfos = luceneMorph.getMorphInfo(unrefinedWord);
        for (String wordMorphInfo : wordMorphInfos)
        {
            if(isServicePartsText) {continue;}
            wordMorphInfo = wordMorphInfo.replaceAll("([^А-Я\\s])", " ");
            if (wordMorphInfo.contains(servicePartsText[0]) || wordMorphInfo.contains(servicePartsText[1]) || wordMorphInfo.contains(servicePartsText[2]))
            {
                isServicePartsText = true;
            };
        }
        return isServicePartsText;
    }

    @SneakyThrows
    private String getLemma(String unrefinedWord)
    {
        List<String> wordMorphsInfo = luceneMorph.getMorphInfo(unrefinedWord);
        String wordMorphInfo = wordMorphsInfo.get(0);
        String lemma = wordMorphInfo.substring(0,wordMorphInfo.indexOf("|"));
        return lemma;
    }

    private String getClearHTML(String pageHTML)
    {
        Cleaner cleaner = new Cleaner(Safelist.basic());
        String clearPage = cleaner.clean(Jsoup.parse(pageHTML)).text();    // .wholeText();
        //System.out.println(clearPage);  // *
        return clearPage;
    }

    public boolean indexPageAndSite(String path)
    {
        HashMap<String, String> pathInfo = getPathAndSiteUrl(path);
        String pagePath = pathInfo.get("pagePath");
        String siteUrl = pathInfo.get("siteUrl");
        Site site = siteRepository.findByUrl(siteUrl);
        Integer siteId = site.getId();
        if(pageRepository.existsByPathAndSiteId(pagePath, siteId))
        {
            System.out.println("В классе LSImpl в методе indexPage вход в метод после проверки if на существование страницы и сайта - успешно");
            return true;
        } else {return false;}
    }

}

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

            // pageRepository.findById(id).orElseGet(() -> pageRepository.save(new Page));



//
/*
            contentIndexAndLength.put(-1, 0); // Добавляем индекс "-1" и значение количества символов для "хвоста" страницы, после крайней леммы
                    int sumCountSymbol = 0;
                    for (int i = 0; i < splitText.length; i++)
        {
        String unrefinedWord = splitText[i];
        String lemma = getLemma(unrefinedWord.toLowerCase(new Locale("ru", "RU")));

        sumCountSymbol += unrefinedWord.length();   // Пробел между словми не учтен !!!

        if(lemmasString.contains(lemma))
        {
                        int startLemma;
                        int finishLemma;

        contentIndexAndLength.put(i, sumCountSymbol);
        sumCountSymbol = 0;
        lemmasIndex.add(i); // Итог блока

        System.out.println("В методе getSnippet() класса LemmServImpl - Получено совпадение текста страницы с леммой: " +
        lemma + " , имеющей вид на сайте: " + unrefinedWord + " , имеющей индекс в массиве = " + i); // *
        }

        contentIndexAndLength.replace(-1, sumCountSymbol);
        }
*/
//



//
//            for (Iterator<String> splitTextIterator = Arrays.stream(splitText).iterator(); splitTextIterator.hasNext(); )
//        {
//        String unrefinedWord = splitTextIterator.next();
//        String lemma = getLemma(unrefinedWord.toLowerCase(new Locale("ru", "RU")));
//        if(lemmasString.contains(lemma))
//        {
//
//        System.out.println("В методе getSnippet() класса LemmServImpl - Получено совпадение текста страницы с леммой: " +
//        lemma + " , имеющей вид на сайте: " + unrefinedWord); // *
//        }
//        }
//



//        LuceneMorphology luceneMorph = new RussianLuceneMorphology();   //TODO: После отладки вынести в класс !!!



                      /* TODO: Проверки по идее не нужны, т.к. каждую страницу индексиреум целиком ???
                        Optional indexOptional = indexRepository.findByPageIdAndLemmaId(indexingPage.getId(), savedLemma.getId());  // .orElseGet(() -> pageRepository.save(new Page));
                        if(indexOptional.isPresent())
                        {
                            savedIndex = (Index) indexOptional.get();
                            savedIndex.frequencyIndexIncr();
                        } else
                        {
                            savedIndex = new Index(indexingPage.getId(), savedLemma.getId(), 1);
                        }
                        */



        //    private void addIndex(Page indexingPage)
        //    {
        //
        //    }



            /* Вынес в PageWriter:
            if(pageRepository.existsByPathAndSiteId(pagePath, siteId))
            {
                System.out.println(path + " : В классе LSImpl в методе indexPage вход в метод после true в проверке if на существование страницы");
                // TODO: Вынести в отдельный метод и все что возвращает true сделать через тернальный оператор + Добавить логику на удаление страницы и переиндексацию !!!
                PageWriter removedPage = new PageWriter();
                removedPage.setSite(site);
                Integer pageId = removedPage.removePage(pagePath, path, site);
                //
                return true;
            } else
                {
                    System.out.println(path + " : В классе LSImpl в методе indexPage вход в метод после false в проверке if на существование страницы");
                    // TODO: Добавить логику на создание страницы и индексацию !!!

                    return true;
                }
            */



/*
    @Override
    public boolean indexPage(String path)
    {

        HashMap<String, String> pathInfo = getPathAndSiteUrl(path);
        String pagePath = pathInfo.get("pagePath");
        String siteUrl = pathInfo.get("siteUrl");
        Site site = siteRepository.findByUrl(siteUrl);
        Integer siteId = site.getId();
        if(pageRepository.existsByPathAndSiteId(pagePath, siteId))
        {
            /
            System.out.println("В классе LSImpl в методе indexPage вход в метод после проверки if на существование страницы и сайта");

            return true;
        } else {return false;}
    }
*/



//    LuceneMorphology luceneMorph =
//        new RussianLuceneMorphology();
//    List<String> wordBaseForms =
//            luceneMorph.getMorphInfo("или");
//    wordBaseForms.forEach(System.out::println);



//    String clearPage = Jsoup.clean(pageHTML, Safelist.none());
//            System.out.println(clearPage);



/*
    @SneakyThrows
    private boolean isLemmaExists(String unrefinedWord)
    {
        LuceneMorphology luceneMorph = new RussianLuceneMorphology();

        boolean isPurifiedWord = false;
        List<String> wordMorphsInfo = luceneMorph.getMorphInfo(unrefinedWord);
        for (String wordMorphInfo : wordMorphsInfo)
        {
            if(isPurifiedWord) {continue;}

            //
            String purifiedWord = wordMorphInfo.substring(0,wordMorphInfo.indexOf("|"));
            System.out.println(wordMorphInfo + " - очищенное значение: " + purifiedWord);

            if (false)
            {
                isPurifiedWord = true;
            };
            //
        }
        return isPurifiedWord;
    }
*/



//
//            List<String> wordBaseForms = luceneMorph.getMorphInfo(unrefinedWord);
//            List<String> wordMorphInfo = luceneMorph.getMorphInfo(unrefinedWord);
//            System.out.println("Слово: " + unrefinedWord + " - " + wordBaseForms + " - MorphInfo:" + wordMorphInfo);  // *
//



    // Old homework:

    //
    /*
    public static String splitTextIntoWords(String text)
    {
 //       text = text.replaceAll("[^а-яА-Я\\sa-zA-Z]", "");


        String[] formatText = text.split("\\s+");


        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < formatText.length - 1; i++)
        {
            stringBuilder.append(formatText[i]).append("\n");
        }
        stringBuilder.append(formatText[formatText.length - 1]);
        text  = stringBuilder.toString();


        return text;

    }
     */
    //


/*
    //
    public static String sequentialWordsNumbers(String text){
        StringBuilder sWN = new StringBuilder();

        int index = text.indexOf(" ");
        int value = 0;

        if (text.isBlank()) {
            return "";
        }
        else while (index >= 0) {
//          else while (!text.isBlank()) {
            value++;
            sWN.append("(").append(Integer.toString(value)).append(") ").append(text.substring(0,index + 1));
            text = text.substring(index + 1);
            index = text.indexOf(" ");
        };

        if (text.indexOf(" ") == -1) {
            value++;
            sWN.append("(").append(Integer.toString(value)).append(") ").append(text);
            return sWN.toString();
        }

        return sWN.toString();
    }
    //
*/



    /*
    @SneakyThrows
    private boolean isRussianServicePartsText(List<String> wordsMorphInfo)
    {
        LuceneMorphology luceneMorph = new RussianLuceneMorphology();

        boolean isServicePartsText= false;
        List<String> wordMorphInfo = luceneMorph.getMorphInfo(unrefinedWord);
        for (String wordMorphInfo : wordsMorphInfo)
        {
            if(isServicePartsText) {continue;}
            wordMorphInfo = wordMorphInfo.replaceAll("([^А-Я\\s])", " ");
            if (wordMorphInfo.contains(servicePartsText[0]) || wordMorphInfo.contains(servicePartsText[1]) || wordMorphInfo.contains(servicePartsText[2]))
            {
                isServicePartsText = true;
            };
        }
        return isServicePartsText;
    }
    */

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////


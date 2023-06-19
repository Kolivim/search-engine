package searchengine.services;

import lombok.SneakyThrows;
import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.jsoup.Jsoup;
import org.jsoup.safety.Cleaner;
import org.jsoup.safety.Safelist;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.config.SitesList;
import searchengine.model.Site;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

@Service
public class LemmatizationServiceImpl implements LemmatizationService
{
    private SiteRepository siteRepository;
    private PageRepository pageRepository;

    private String[] servicePartsText = {"ПРЕДЛ", "МЕЖД", "СОЮЗ"};
    private HashMap<String, Integer> lemmasText = new HashMap<String, Integer>();
    private LuceneMorphology luceneMorph = new RussianLuceneMorphology();   // TODO: После отладки вынести в класс !!! M
    @Autowired
    public LemmatizationServiceImpl(PageRepository pageRepository, SiteRepository siteRepository) throws IOException
    {
        this.siteRepository = siteRepository;
        this.pageRepository = pageRepository;
    }  // TODO: После отладки вынести в класс !!! M

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

            //

            PageWriter removedPage = new PageWriter();
            Integer pageId = removedPage.removeOrAddPage(pagePath, path, site);
            // TODO: Здесь добавляем данные в index и lemma

            //
            System.out.println(path + " : В классе LSImpl завершение метода indexPage, новый pageId = " + pageId + " , для пути: " + path);// *
            return true;

        }  else
            {
                System.out.println(path + " : В классе LSImpl в методе indexPage вход в метод после false в проверке if на существование сайта"); // *
                System.out.println(path + " : Данная страница находится за пределами сайтов, указанных в конфигурационном файле");
                return false;
            }
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
            System.out.println("В классе LSImpl в методе indexPage вход в метод после проверки if на существование страницы и сайта");
            return true;
        } else {return false;}
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
//        LuceneMorphology luceneMorph = new RussianLuceneMorphology();   // TODO: После отладки вынести в класс !!!

        HashMap<String, Integer> splitLemmasText = new HashMap<String, Integer>();
        String[] splitText = arrayContainsRussianUnrefinedWords(text);
        for (Iterator<String> splitTextIterator = Arrays.stream(splitText).iterator(); splitTextIterator.hasNext(); )
        {
            String unrefinedWord = splitTextIterator.next();
            if(isRussianServicePartsText(unrefinedWord)){continue;}
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
//        LuceneMorphology luceneMorph = new RussianLuceneMorphology();   //TODO: После отладки вынести в класс !!!

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
//        LuceneMorphology luceneMorph = new RussianLuceneMorphology();   //TODO: После отладки вынести в класс !!!

        List<String> wordMorphsInfo = luceneMorph.getMorphInfo(unrefinedWord);
        String wordMorphInfo = wordMorphsInfo.get(0);
        String lemma = wordMorphInfo.substring(0,wordMorphInfo.indexOf("|"));
        return lemma;
    }

    private String getClearHTML(String pageHTML)
    {
        Cleaner cleaner = new Cleaner(Safelist.basic());
        String clearPage = cleaner.clean(Jsoup.parse(pageHTML)).text();    // .wholeText();
        System.out.println(clearPage);
        return clearPage;
    }
}

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

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


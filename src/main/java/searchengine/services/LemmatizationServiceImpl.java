package searchengine.services;

import lombok.SneakyThrows;
import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.jsoup.Jsoup;
import org.jsoup.safety.Cleaner;
import org.jsoup.safety.Safelist;
import searchengine.model.Site;

import java.io.IOException;
import java.util.*;

public class LemmatizationServiceImpl implements LemmatizationService
{
    private String[] servicePartsText = {"ПРЕДЛ", "МЕЖД", "СОЮЗ"};
    HashMap<String, Integer> lemmasText = new HashMap<String, Integer>();
    LuceneMorphology luceneMorph = new RussianLuceneMorphology();   // TODO: После отладки вынести в класс !!! M

    public LemmatizationServiceImpl() throws IOException {}   // TODO: После отладки вынести в класс !!! M

    @Override
    public boolean indexPage()
    {
        return false;
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


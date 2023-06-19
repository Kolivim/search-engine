package searchengine.services;

import lombok.SneakyThrows;
import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.jsoup.Jsoup;
import org.jsoup.safety.Cleaner;
import org.jsoup.safety.Safelist;
import org.springframework.boot.SpringApplication;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import searchengine.Application;
import searchengine.model.Page;
import searchengine.model.Site;

import javax.persistence.LockModeType;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

public class TestValue
{
    @SneakyThrows
    public static void main(String[] args)
    {
        /*
        // Блок 1:
        LuceneMorphology luceneMorph =
                new RussianLuceneMorphology();
        List<String> wordBaseForms =
                luceneMorph.getNormalForms("лес");
        wordBaseForms.forEach(System.out::println);
        //
        */

        // Блок 2:
        /*
        String[] servicePartsText = {"ПРЕДЛ", "МЕЖД", "СОЮЗ"};

        String text = "повторное появление леопарда в Осетии позволяет предположить, что леопард постоянно обитает в некоторых районах Северного Кавказа.";

        LuceneMorphology luceneMorph = new RussianLuceneMorphology();
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


        splitLemmasText.forEach((key, value) -> {System.out.print("\n" + key + " : " + value);});
        //splitLemmasText.forEach(System.out::println);

        for (Iterator<String> splitTextIterator = Arrays.stream(splitText).iterator(); splitTextIterator.hasNext(); )
        {
            String unrefinedWord = splitTextIterator.next();
            List<String> wordMorphsInfo = luceneMorph.getMorphInfo(unrefinedWord);
            System.out.println("\n");
            System.out.println(unrefinedWord + " - " + wordMorphsInfo);
        }
        */
        //

        /*
        String html = getPageHTML();
        getClearHTML(html);
        */

        String path = "https://skillbox.ru/otzyvy/";
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

            String pagePath = pathInfo.get("pagePath");
            String siteUrl = pathInfo.get("siteUrl");
            System.out.println(url + " - siteUrl: " + siteUrl + " , pagePath: " + pagePath);  // *

        } catch (MalformedURLException e)
        {
            System.err.println("В классе LSImpl методе indexPage сработал MalformedURLException(e) ///1 " + e.getMessage() + " ///2 " + e.getStackTrace() + " ///3 " + e.getSuppressed() + " ///4 " + e.getCause() + " ///5 " + e.getLocalizedMessage() + " ///6 " + e.getClass() + " ///7 на переданной странице:  " + path);
        }

        }


        private static String[] arrayContainsRussianUnrefinedWords(String text)
        {
            return text.toLowerCase(new Locale("ru", "RU"))
                    .replaceAll("([^а-я\\s])", " ")
                    .trim()
                    .split("\\s+");
        }

        @SneakyThrows
        private static boolean isRussianServicePartsText(String unrefinedWord) // TODO: Найти описание библиотеки и заменить
        {
            String[] servicePartsText = {"ПРЕДЛ", "МЕЖД", "СОЮЗ"};

            LuceneMorphology luceneMorph = new RussianLuceneMorphology();   //TODO: После отладки вынести в класс !!!

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
    private static boolean isLemmaExists(String unrefinedWord)
    {
        LuceneMorphology luceneMorph = new RussianLuceneMorphology();   //TODO: После отладки вынести в класс !!!

        boolean isPurifiedWord = false;
        List<String> wordMorphsInfo = luceneMorph.getMorphInfo(unrefinedWord);
        for (String wordMorphInfo : wordMorphsInfo)
        {
            if(isPurifiedWord) {continue;}

            // TODO: Переписать проверки - с другого блока !!!

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

    @SneakyThrows
    private static String getLemma(String unrefinedWord)
    {
        LuceneMorphology luceneMorph = new RussianLuceneMorphology();   //TODO: После отладки вынести в класс !!!

        List<String> wordMorphsInfo = luceneMorph.getMorphInfo(unrefinedWord);
        String wordMorphInfo = wordMorphsInfo.get(0);
        String lemma = wordMorphInfo.substring(0,wordMorphInfo.indexOf("|"));
        System.out.println(wordMorphInfo + " - очищенное значение: " + lemma);
        return lemma;
    }

    private static String getClearHTML(String pageHTML)
    {
        //
//        String clearPage = Jsoup.clean(pageHTML, Safelist.none());  // Safelist.none()
//        System.out.println(clearPage);
        //

        Cleaner cleaner = new Cleaner(Safelist.basic());
        String clearPage = cleaner.clean(Jsoup.parse(pageHTML)).text();    // .wholeText();
        System.out.println(clearPage);

        return clearPage;
    }

    private static String getPageHTML()
    {
        String pageHTMLage = "\n" +
                "<!DOCTYPE html>\n" +
                "<html lang=\"ru\">\n" +
                "<head>\n" +
                "  <meta charset=\"utf-8\">\n" +
                "  <meta http-equiv=\"X-UA-Compatible\" content=\"IE=edge\">\n" +
                "  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">\n" +
                "  <!-- The above 3 meta tags *must* come first in the head; any other head content must come *after* these tags -->\n" +
                "  <title>Галерея искусств Niko</title>  <meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\" />\n" +
                "<meta name=\"robots\" content=\"index, follow\" />\n" +
                "<meta name=\"keywords\" content=\"Галерея искусств Niko\" />\n" +
                "<meta name=\"description\" content=\"Галерея искусств Niko\" />\n" +
                "<link href=\"/bitrix/cache/css/s1/addeo/kernel_main/kernel_main.css?168436335244417\" type=\"text/css\"  rel=\"stylesheet\" />\n" +
                "<link href=\"/bitrix/cache/css/s1/addeo/page_8854fb4ef25f0739949f02b1152afdbf/page_8854fb4ef25f0739949f02b1152afdbf.css?1684363352870\" type=\"text/css\"  rel=\"stylesheet\" />\n" +
                "<link href=\"/bitrix/cache/css/s1/addeo/template_4653b6c2543cdce0987861f7796b4634/template_4653b6c2543cdce0987861f7796b4634.css?16843633522973\" type=\"text/css\"  data-template-style=\"true\"  rel=\"stylesheet\" />\n" +
                "<script type=\"text/javascript\">if(!window.BX)window.BX={message:function(mess){if(typeof mess=='object') for(var i in mess) BX.message[i]=mess[i]; return true;}};</script>\n" +
                "<script type=\"text/javascript\">(window.BX||top.BX).message({'JS_CORE_LOADING':'Загрузка...','JS_CORE_NO_DATA':'- Нет данных -','JS_CORE_WINDOW_CLOSE':'Закрыть','JS_CORE_WINDOW_EXPAND':'Развернуть','JS_CORE_WINDOW_NARROW':'Свернуть в окно','JS_CORE_WINDOW_SAVE':'Сохранить','JS_CORE_WINDOW_CANCEL':'Отменить','JS_CORE_WINDOW_CONTINUE':'Продолжить','JS_CORE_H':'ч','JS_CORE_M':'м','JS_CORE_S':'с','JSADM_AI_HIDE_EXTRA':'Скрыть лишние','JSADM_AI_ALL_NOTIF':'Показать все','JSADM_AUTH_REQ':'Требуется авторизация!','JS_CORE_WINDOW_AUTH':'Войти','JS_CORE_IMAGE_FULL':'Полный размер'});</script>\n" +
                "<script type=\"text/javascript\">(window.BX||top.BX).message({'LANGUAGE_ID':'ru','FORMAT_DATE':'DD.MM.YYYY','FORMAT_DATETIME':'DD.MM.YYYY HH:MI:SS','COOKIE_PREFIX':'BITRIX_SM','SERVER_TZ_OFFSET':'10800','SITE_ID':'s1','SITE_DIR':'/','USER_ID':'','SERVER_TIME':'1686892700','USER_TZ_OFFSET':'0','USER_TZ_AUTO':'Y','bitrix_sessid':'65d7e0d8588f8b36b0bc2122fe74f557'});</script>\n" +
                "\n" +
                "\n" +
                "<script type=\"text/javascript\" src=\"/bitrix/cache/js/s1/addeo/kernel_main/kernel_main.js?1684363352269063\"></script>\n" +
                "<script type=\"text/javascript\" src=\"//www.google.com/recaptcha/api.js\"></script>\n" +
                "<script type=\"text/javascript\">BX.setJSList(['/bitrix/js/main/core/core.js?168436311173480','/bitrix/js/main/core/core_ajax.js?168436311221031','/bitrix/js/main/json/json2.min.js?16843631133467','/bitrix/js/main/core/core_ls.js?16843631127365','/bitrix/js/main/session.js?16843631132511','/bitrix/js/main/core/core_window.js?168436311274754','/bitrix/js/main/core/core_popup.js?168436311229812','/bitrix/js/main/core/core_date.js?168436311234241','/bitrix/js/main/utils.js?168436311319858','/bitrix/components/bitrix/map.yandex.view/templates/.default/script.js?16843626041540']); </script>\n" +
                "<script type=\"text/javascript\">BX.setCSSList(['/bitrix/js/main/core/css/core.css?16843631122854','/bitrix/js/main/core/css/core_popup.css?168436311229699','/bitrix/js/main/core/css/core_date.css?16843631129657','/bitrix/components/bitrix/map.yandex.system/templates/.default/style.css?1684362604666','/bitrix/templates/addeo/components/bitrix/menu/top/style.css?1480508527490','/bitrix/templates/addeo/components/bitrix/form/forma_request/bitrix/form.result.new/.default/style.css?1543576936666','/bitrix/templates/addeo/components/bitrix/form/forma_request_en/bitrix/form.result.new/.default/style.css?1554127419666']); </script>\n" +
                "\n" +
                "\n" +
                "<script type=\"text/javascript\" src=\"/bitrix/cache/js/s1/addeo/page_27a727aff7a47f8f8886d089ee11f709/page_27a727aff7a47f8f8886d089ee11f709.js?16843633521897\"></script>\n" +
                "<script type=\"text/javascript\">var _ba = _ba || []; _ba.push([\"aid\", \"75edb33fc1ac88e4b848dce98874d580\"]); _ba.push([\"host\", \"www.nikoartgallery.com\"]); (function() {var ba = document.createElement(\"script\"); ba.type = \"text/javascript\"; ba.async = true;ba.src = (document.location.protocol == \"https:\" ? \"https://\" : \"http://\") + \"bitrix.info/ba.js\";var s = document.getElementsByTagName(\"script\")[0];s.parentNode.insertBefore(ba, s);})();</script>\n" +
                "\n" +
                "\n" +
                "\n" +
                "  <!-- Bootstrap -->\n" +
                "  <link href=\"/bitrix/templates/addeo/css/bootstrap.css\" rel=\"stylesheet\">\n" +
                "  <link href=\"/bitrix/templates/addeo/css/animate.css\" rel=\"stylesheet\">\n" +
                "  <link href=\"/bitrix/templates/addeo/css/owl.carousel.css\" rel=\"stylesheet\">\n" +
                "  <link href=\"/bitrix/templates/addeo/css/lightbox.css\" rel=\"stylesheet\">\n" +
                "<link href=\"/bitrix/templates/addeo/css/orders.css\" rel=\"stylesheet\">\n" +
                "  <link href=\"/bitrix/templates/addeo/css1/style.css\" rel=\"stylesheet\">\n" +
                "  <!--<link href=\"/bitrix/templates/addeo/css/style.css\" rel=\"stylesheet\">-->\n" +
                "  <link href=\"/bitrix/templates/addeo/css/media.css\" rel=\"stylesheet\">\n" +
                "<link href=\"/bitrix/templates/addeo/css/font-awesome.css\" rel=\"stylesheet\">\n" +
                "\n" +
                "\n" +
                "  <!-- HTML5 shim and Respond.js for IE8 support of HTML5 elements and media queries -->\n" +
                "  <!-- WARNING: Respond.js doesn't work if you view the page via file:// -->\n" +
                "    <!--[if lt IE 9]>\n" +
                "      <script src=\"https://oss.maxcdn.com/html5shiv/3.7.2/html5shiv.min.js\"></script>\n" +
                "      <script src=\"https://oss.maxcdn.com/respond/1.4.2/respond.min.js\"></script>\n" +
                "      <![endif]-->\n" +
                "    </head>\n" +
                "    <body>\n" +
                "\n" +
                "\n" +
                "    <div id=\"panel\"></div>\n" +
                "    \n" +
                "      <header>\n" +
                "        <div class=\"row\">\n" +
                "          <div class=\"top\">\n" +
                "            <div class=\"container\">\n" +
                "              <p class=\"top-left\">\n" +
                "              \t  \t              \t\tБронирование <a href=\"/arenda/\">залов галереи</a> по телефонам <span>+7 (985) 998 97 95 / +7 (499) 253 86 07</span>\t              \n" +
                "\n" +
                "              </p>\n" +
                "\n" +
                "\n" +
                "\n" +
                "\n" +
                "\n" +
                "                            \t\t<p class=\"top-right\"><a href=\"/en/\">In English</a></p>\n" +
                "              \n" +
                "\t\t\t\t<div class=\"head_social\">\n" +
                "<a target=\"_blank\" href=\"https://vk.com/public200017253 \"><i class=\"fa fa-vk\"></i></a>\n" +
                "        <!-- asocial 2\n" +
                "<a target=\"_blank\" href=\"https://www.instagram.com/niko_art_gallery/ \"><i class=\"fa fa-instagram\"></i></a>\n" +
                "<a target=\"_blank\" href=\"https://www.facebook.com/pg/artgalleryniko\"><i class=\"fa fa-facebook\"></i></a>\n" +
                "       \t\t\t-->\n" +
                "       \t\t\t</div>\n" +
                "\n" +
                "            </div>\n" +
                "          </div>\n" +
                "          <div class=\"top-mobile\">\n" +
                "<div class=\"container\">\n" +
                "            <p class=\"top-left\">\n" +
                "              \t\t              \t\tБронирование <a href=\"/arenda/\">залов галереи</a> по телефонам <span>+7 (985) 998 97 95 / +7 (499) 253 86 07</span>\t                            </p>\n" +
                "\n" +
                "\t\t\t</div>\n" +
                "          </div>\n" +
                "        </div>\n" +
                "        <div class=\"row\">\n" +
                "          <div class=\"bottom\">\n" +
                "            <div class=\"container\">\n" +
                "              <div class=\"mobile-menu\">\n" +
                "                <span class=\"icon-bar\"></span>\n" +
                "                <span class=\"icon-bar\"></span>\n" +
                "                <span class=\"icon-bar\"></span>\n" +
                "              </div>\n" +
                "              <div class=\"logo\"><a href=\"/\"><img src=\"/bitrix/templates/addeo/img/Logotip.svg\" alt=\"\"></a></div>\n" +
                "              <h1>\n" +
                "                            \t\tКреативное пространство и галерея Н.Б. Никогосяна              \n" +
                "              </h1>\n" +
                "              \n" +
                "<ul>\n" +
                "\n" +
                "\t\t\t<li><a href=\"/gallery/\">Галерея</a></li>\n" +
                "\t\t\n" +
                "\t\t\t<li><a href=\"/author/\">О художнике</a></li>\n" +
                "\t\t\n" +
                "\t\t\t<li><a href=\"/afisha/\">Aфиша</a></li>\n" +
                "\t\t\n" +
                "\t\t\t<li><a href=\"/arenda/\">Аренда залов</a></li>\n" +
                "\t\t\n" +
                "\t\t\t<li><a href=\"/news/\">Новости</a></li>\n" +
                "\t\t\n" +
                "\t\t\t<li><a href=\"/contacts/\">Контакты</a></li>\n" +
                "\t\t\n" +
                "\n" +
                "</ul>\n" +
                "            </div>\n" +
                "          </div>\n" +
                "        </div>\n" +
                "      </header>\n" +
                "\n" +
                "      <div class=\"mobile-menu-wrap\">\n" +
                "        <div class=\"container\">\n" +
                "          <img class=\"close-mobile\" src=\"/bitrix/templates/addeo/img/close-mobile.png\" alt=\"\">\n" +
                "          \n" +
                "<ul>\n" +
                "\n" +
                "\t\t\t<li><a href=\"/gallery/\">Галерея</a></li>\n" +
                "\t\t\n" +
                "\t\t\t<li><a href=\"/author/\">О художнике</a></li>\n" +
                "\t\t\n" +
                "\t\t\t<li><a href=\"/afisha/\">Aфиша</a></li>\n" +
                "\t\t\n" +
                "\t\t\t<li><a href=\"/arenda/\">Аренда залов</a></li>\n" +
                "\t\t\n" +
                "\t\t\t<li><a href=\"/news/\">Новости</a></li>\n" +
                "\t\t\n" +
                "\t\t\t<li><a href=\"/contacts/\">Контакты</a></li>\n" +
                "\t\t\n" +
                "\n" +
                "</ul>\n" +
                "         \t <p class=\"top-left\">\n" +
                "              \t\t              \t\tБронирование <a href=\"/arenda/\">залов галереи</a> по телефонам <span>+7 (985) 998 97 95 / +7 (499) 253 86 07</span>\t                            </p>\n" +
                "\t\t\t<div class=\"mobile__social_mod\">\n" +
                "\t\t\t\t              \t\t<p class=\"top-right\"><a href=\"/en/\">In English</a></p>\n" +
                "              \n" +
                "\t\t\t\t<div class=\"head_social\">\n" +
                "\n" +
                "\t\t\t\t\t<a target=\"_blank\" href=\"https://vk.com/public200017253 \"><i class=\"fa fa-vk\"></i></a>\n" +
                "\t\t\t\t<!-- asocial 1\n" +
                "\t\t\t\t\t<a target=\"_blank\" href=\"https://www.instagram.com/niko_art_gallery/ \"><i class=\"fa fa-instagram\"></i></a>\n" +
                "\t\t\t\t\t<a target=\"_blank\" href=\"https://www.facebook.com/pg/artgalleryniko\"><i class=\"fa fa-facebook\"></i></a>\n" +
                "                -->\n" +
                "       \t\t\t</div>\n" +
                "\n" +
                "\t\t\t</div>\n" +
                "        </div>\n" +
                "      </div>\n" +
                "\n" +
                "\n" +
                "<div class=\"modal-wrap modal-wrap-ru\">\n" +
                "        <div class=\"modal-order\">\n" +
                "          <img class=\"close\" src=\"/bitrix/templates/addeo/img/close-2.png\" alt=\"\">\n" +
                "          <div id=\"comp_1b9c614ab5bf22d5d095729762d2400b\">\n" +
                "<style>\n" +
                "\t.g-recaptcha {\n" +
                "\t\tmargin: 10px 0 20px;\n" +
                "\t}\n" +
                "</style>\n" +
                "\n" +
                "<script src=\"https://www.google.com/recaptcha/api.js\" async defer></script>\n" +
                "\n" +
                "\n" +
                "<form name=\"SIMPLE_FORM_3\" action=\"/\" method=\"POST\" enctype=\"multipart/form-data\"><input type=\"hidden\" name=\"bxajaxid\" id=\"bxajaxid_1b9c614ab5bf22d5d095729762d2400b_8BACKi\" value=\"1b9c614ab5bf22d5d095729762d2400b\" /><input type=\"hidden\" name=\"AJAX_CALL\" value=\"Y\" /><script type=\"text/javascript\">\n" +
                "function _processform_8BACKi(){\n" +
                "\tvar obForm = top.BX('bxajaxid_1b9c614ab5bf22d5d095729762d2400b_8BACKi').form;\n" +
                "\ttop.BX.bind(obForm, 'submit', function() {BX.ajax.submitComponentForm(this, 'comp_1b9c614ab5bf22d5d095729762d2400b', true)});\n" +
                "\ttop.BX.removeCustomEvent('onAjaxSuccess', _processform_8BACKi);\n" +
                "}\n" +
                "if (top.BX('bxajaxid_1b9c614ab5bf22d5d095729762d2400b_8BACKi'))\n" +
                "\t_processform_8BACKi();\n" +
                "else\n" +
                "\ttop.BX.addCustomEvent('onAjaxSuccess', _processform_8BACKi);\n" +
                "</script><input type=\"hidden\" name=\"sessid\" id=\"sessid\" value=\"65d7e0d8588f8b36b0bc2122fe74f557\" /><input type=\"hidden\" name=\"WEB_FORM_ID\" value=\"3\" />\n" +
                "\n" +
                "\n" +
                "\t<h3>Оформление заявки</h3>\n" +
                "\n" +
                "\t<div>\n" +
                "\n" +
                "\t\t\n" +
                "\n" +
                "\t</div>\n" +
                "\n" +
                "\t\t\t<p>Оставьте свой номер и мы вам перезвоним, чтобы проконсультировать вас по возникшим вопросам</p>\n" +
                "\t\t\t<p><font class=\"notetext\"></font></p>\n" +
                "\n" +
                "\t\n" +
                "<br />\n" +
                "\n" +
                "\t\n" +
                "\t\t\t\t\n" +
                "\t\t\t\t\n" +
                "\t\t<div class=\"input-wrap\"><input type=\"text\" placeholder=\"Ваше имя\" name=\"form_text_13\" value=\"\" size=\"0\" /><font color='red'><span class='form-required starrequired'>*</span></font></div>\n" +
                "\n" +
                "\t\n" +
                "\t\t\t\t\n" +
                "\t\t\t\t\n" +
                "\t\t<div class=\"input-wrap\"><input type=\"text\" placeholder=\"E-mail\" name=\"form_email_31\" value=\"\" size=\"0\" /></div>\n" +
                "\n" +
                "\t\n" +
                "\t\t\t\t\n" +
                "\t\t\t\t\n" +
                "\t\t<div class=\"input-wrap\"><input type=\"text\" placeholder=\"+7 (___)-___-__-__\" name=\"form_text_14\" value=\"\" size=\"0\" /><font color='red'><span class='form-required starrequired'>*</span></font></div>\n" +
                "\n" +
                "\t\n" +
                "\t\t\t\t\n" +
                "\t\t\t\t\n" +
                "\t\t<div class=\"input-wrap\"><select  class=\"inputselect\"  name=\"form_dropdown_SIMPLE_QUESTION_699\" id=\"form_dropdown_SIMPLE_QUESTION_699\"><option value=\"15\">4-й этаж Галереи Нико</option><option value=\"16\">5-й этаж Галереи Нико</option><option value=\"18\">Я не знаю</option></select></div>\n" +
                "\n" +
                "\t\n" +
                "\t\t\t\t\n" +
                "\t\t\t\t\n" +
                "\t\t<div class=\"input-wrap\"><textarea name=\"form_textarea_17\" cols=\"40\" rows=\"5\" placeholder=\"Объясните в двух словах суть мероприятия\"></textarea></div>\n" +
                "\n" +
                "\t\t\t<b>Защита от автоматического заполнения</b>\n" +
                "\t\t<script>Recaptchafree.reset();</script>\n" +
                "\n" +
                "\n" +
                "\t\t\t<input type=\"hidden\" name=\"captcha_sid\" value=\"01b73a2dc426fe674576a7bc82a4f314\" /><img src=\"/bitrix/tools/captcha.php?captcha_sid=01b73a2dc426fe674576a7bc82a4f314\" width=\"180\" height=\"40\" />\n" +
                "\n" +
                "\t\t\t<!-- Введите символы с картинки<font color='red'><span class='form-required starrequired'>*</span></font> -->\n" +
                "\t\t\t<input type=\"text\" name=\"captcha_word\" size=\"30\" maxlength=\"50\" value=\"\" class=\"inputtext\" />\n" +
                "\n" +
                "\n" +
                "\t\t\t\t<input  type=\"submit\" name=\"web_form_submit\" value=\"Отправить\" />\n" +
                "\t\t\t\t<!-- \t\t\t\t&nbsp;<input type=\"reset\" value=\"Сбросить\" /> -->\n" +
                "\n" +
                "<!-- <p>\n" +
                "<font color='red'><span class='form-required starrequired'>*</span></font> - обязательные поля</p> -->\n" +
                "</form></div>            <p class=\"descr\">Или звоните нам по телефону\n" +
                "            <span>\n" +
                "            \t<p>\n" +
                " <span style=\"color: #acacac;\">+7 (985) 998 97 95</span><br>\n" +
                "\t<span style=\"color: #acacac;\">\n" +
                "\t+7 (499) 253 86 07 </span>\n" +
                "</p>\n" +
                "<span style=\"color: #acacac;\"> </span><br>            </span>\n" +
                "            </p>\n" +
                "          </form>\n" +
                "        </div>\n" +
                "      </div>\n" +
                "\n" +
                "\n" +
                "\n" +
                "<div class=\"modal-wrap modal-wrap-en\">\n" +
                "        <div class=\"modal-order\">\n" +
                "          <img class=\"close\" src=\"/bitrix/templates/addeo/img/close-2.png\" alt=\"\">\n" +
                "          <div id=\"comp_d9871c672723c30ce8d2aa612f316115\">\n" +
                "\n" +
                "\n" +
                "<form name=\"SIMPLE_FORM_3_WOBFl\" action=\"/\" method=\"POST\" enctype=\"multipart/form-data\"><input type=\"hidden\" name=\"bxajaxid\" id=\"bxajaxid_d9871c672723c30ce8d2aa612f316115_Ar8Szp\" value=\"d9871c672723c30ce8d2aa612f316115\" /><input type=\"hidden\" name=\"AJAX_CALL\" value=\"Y\" /><script type=\"text/javascript\">\n" +
                "function _processform_Ar8Szp(){\n" +
                "\tvar obForm = top.BX('bxajaxid_d9871c672723c30ce8d2aa612f316115_Ar8Szp').form;\n" +
                "\ttop.BX.bind(obForm, 'submit', function() {BX.ajax.submitComponentForm(this, 'comp_d9871c672723c30ce8d2aa612f316115', true)});\n" +
                "\ttop.BX.removeCustomEvent('onAjaxSuccess', _processform_Ar8Szp);\n" +
                "}\n" +
                "if (top.BX('bxajaxid_d9871c672723c30ce8d2aa612f316115_Ar8Szp'))\n" +
                "\t_processform_Ar8Szp();\n" +
                "else\n" +
                "\ttop.BX.addCustomEvent('onAjaxSuccess', _processform_Ar8Szp);\n" +
                "</script><input type=\"hidden\" name=\"sessid\" id=\"sessid_1\" value=\"65d7e0d8588f8b36b0bc2122fe74f557\" /><input type=\"hidden\" name=\"WEB_FORM_ID\" value=\"5\" />\n" +
                "\n" +
                "\n" +
                "\t<h3>Request</h3>\n" +
                "\t\n" +
                "\t\t\t<p>Leave your number and we will call you back to advise you on any questions</p>\n" +
                "\t\t\t\n" +
                "\t\n" +
                "<br />\n" +
                "\n" +
                "\t\n" +
                "\t\t\t\t\n" +
                "\t\t\t\t\n" +
                "\t\t\t<div class=\"input-wrap\"><input type=\"text\" placeholder=\"Your name\" name=\"form_text_25\" value=\"\" size=\"0\" /><font color='red'><span class='form-required starrequired'>*</span></font></div>\n" +
                "\n" +
                "\t\n" +
                "\t\t\t\t\n" +
                "\t\t\t\t\n" +
                "\t\t\t<div class=\"input-wrap\"><input type=\"text\" placeholder=\"Phone\" name=\"form_text_26\" value=\"\" size=\"0\" /><font color='red'><span class='form-required starrequired'>*</span></font></div>\n" +
                "\n" +
                "\t\n" +
                "\t\t\t\t\n" +
                "\t\t\t\t\n" +
                "\t\t\t<div class=\"input-wrap\"><select  class=\"inputselect\"  name=\"form_dropdown_SIMPLE_QUESTION_699\" id=\"form_dropdown_SIMPLE_QUESTION_699\"><option value=\"27\">The Fourth Floor of the Niko Gallery</option><option value=\"28\">The Third floor of the Niko Gallery</option><option value=\"29\">I dont know</option></select></div>\n" +
                "\n" +
                "\t\n" +
                "\t\t\t\t\n" +
                "\t\t\t\t\n" +
                "\t\t\t<div class=\"input-wrap\"><textarea name=\"form_textarea_30\" cols=\"40\" rows=\"5\" placeholder=\"Descriptions\"></textarea></div>\n" +
                "\n" +
                "\t\n" +
                "\t\t\t<b>Защита от автоматического заполнения</b>\n" +
                "\t\t\t<script>Recaptchafree.reset();</script>\n" +
                "\t\t\t\n" +
                "\n" +
                "\t\t\t<input type=\"hidden\" name=\"captcha_sid\" value=\"09f68e0491e67a3a8f074e5801058115\" /><img src=\"/bitrix/tools/captcha.php?captcha_sid=09f68e0491e67a3a8f074e5801058115\" width=\"180\" height=\"40\" />\n" +
                "\n" +
                "\t\t\t<!-- Введите символы с картинки<font color='red'><span class='form-required starrequired'>*</span></font> -->\n" +
                "\t\t\t<input type=\"text\" name=\"captcha_word\" size=\"30\" maxlength=\"50\" value=\"\" class=\"inputtext\" />\n" +
                "\n" +
                "\n" +
                "\t\t\t\t<input  type=\"submit\" name=\"web_form_submit\" value=\"Send\" />\n" +
                "\n" +
                "<!-- <p>\n" +
                "<font color='red'><span class='form-required starrequired'>*</span></font> - обязательные поля</p> -->\n" +
                "</form></div>            <p class=\"descr\">Or call us at\n" +
                "            <span>\n" +
                "            \t+7 (985) 998 97 95<br>+7 (499) 253 86 07            </span>\n" +
                "            </p>\n" +
                "          </form>\n" +
                "        </div>\n" +
                "      </div>\n" +
                "\n" +
                "<section id=\"slider-top\">\n" +
                "  \t\t<div class=\"owl-top\">\n" +
                "  \t\t\t\n" +
                "  \t\t\t\n" +
                "  \t\t\t  \t\t\t  \t\t\t  \t\t\t\t\n" +
                "\t\t\t\t<div class=\"item\" style=\"background-image: url(/upload/iblock/73c/73ca0456674705ddb2a749d46cf65792.png);\">\n" +
                "\t  \t\t\t\t\t\t  \t\t\t\t<div class=\"container\">\n" +
                "\t\t  \t\t\t\t\t<div class=\"owl-top-descr\">\n" +
                "\t\t  \t\t\t\t\t\t<a href=\"/afisha/\"><h2>Креативное пространство и галерея Н.Б. Никогосяна</h2></a>\n" +
                "\t\t  \t\t\t\t\t\t<p></p>\n" +
                "\t\t  \t\t\t\t\t</div>\n" +
                "\t\t  \t\t\t\t</div>\n" +
                "\t\t  \t\t\t\t\t  \t\t\t\t\t  \t\t\t</div>\n" +
                "  \t\t\t  \t\t\t  \t\t\t\t\n" +
                "\t\t\t\t<div class=\"item\" style=\"background-image: url(/upload/iblock/0da/0dabca39ed4a742c2857d6c53e8b85c6.jpg);\">\n" +
                "\t  \t\t\t\t\t  \t\t\t</div>\n" +
                "  \t\t\t  \t\t\t  \t\t\t\t\n" +
                "\t\t\t\t<div class=\"item\" style=\"background-image: url(/upload/iblock/3e8/3e80a65d6dda9c625e4615380e61724b.png);\">\n" +
                "\t  \t\t\t\t\t  \t\t\t</div>\n" +
                "  \t\t\t  \t\t\t  \t\t\t\t\n" +
                "\t\t\t\t<div class=\"item\" style=\"background-image: url(/upload/iblock/a3c/a3c3e0cbc4d4e224aa06ba1fc70ab83a.jpg);\">\n" +
                "\t  \t\t\t\t\t  \t\t\t</div>\n" +
                "  \t\t\t  \t\t\t  \t\t\t\t\n" +
                "\t\t\t\t<div class=\"item\" style=\"background-image: url(/upload/iblock/5f4/5f42a6d8136e9ab9cd44efcf08456d12.jpg);\">\n" +
                "\t  \t\t\t\t\t  \t\t\t</div>\n" +
                "  \t\t\t\n" +
                "  \t\t</div>\n" +
                "  \t</section>\n" +
                "\n" +
                "  \t<section id=\"descr\">\n" +
                "  \t\t<div class=\"container\">\n" +
                "  \t\t\t<h2 class=\"wow fadeIn\" data-wow-delay=\"0.2s\">Галерея Нико — это музей работ классика советского и постсоветского искусства академика Н.Б. Никогосяна и креативное пространство в центре Москвы, открытое для творческих идей.</h2>\n" +
                "<p class=\"wow fadeIn\" data-wow-delay=\"0.4s\">\n" +
                "\tГалерея «Нико» - это, в первую очередь, галерея-музей признанного мастера скульптуры и живописи <br>\n" +
                "\t<a href=\"/author/\">Н.Б. Никогосяна</a>&nbsp;(1918-2018), чьи произведения украшают главные музеи нашей страны, такие как Третьяковская галерея, Русский музей, собрание РОСИЗО и многие другие, музеи стран бывших республик СССР и многочисленные частные коллекции по всем миру.\n" +
                "</p>\n" +
                "<p class=\"wow fadeIn\" data-wow-delay=\"0.6s\">\n" +
                "\tВ нашей галерее собраны скульптурные, живописные и графические произведения Н.Б. Никогосяна, созданные более, чем за 70 лет активной творческой жизни, а также богатый архивный материал. <br>\n" +
                "\t<br>\n" +
                "\t<a href=\"/gallery/\">Подробнее</a>\n" +
                "</p>\n" +
                "  \t\t\t<!--<p>Простейшая архитектурная конструкция, известная с <span>эпохи неолита</span>. С древних времен и до наших дней применяется во всех зданиях, перекрытых плоской или двускатной крышей.<br><br><a href=\"#\">Подробнее</a></p>--> \n" +
                "  \t\t</div>\n" +
                "  \t</section>\n" +
                "\n" +
                "  \t<section id=\"whatwedo\">\n" +
                "  \t\t<div class=\"container\">\n" +
                "  \t\t\t<h2>Деятельность галереи</h2>\n" +
                "  \t\t\t\n" +
                "  \t\t\t\n" +
                "  \t\t\t\n" +
                "  \t\t\t  \t\t\t\t\t\t\t<a href=\"/gallery/\">\n" +
                "\t\t\t\t\t<div class=\"whatwedo-block wow fadeIn\" data-wow-delay=\"0.2s\">\n" +
                "\t\t  \t\t\t\t<img src=\"/upload/iblock/92e/92e2f8774758880a88a1c63625b99f96.png\" alt=\"\">\n" +
                "\t\t  \t\t\t\t<h3>Галерея работ Н.Б.Никогосяна</h3>\n" +
                "\t\t  \t\t\t\t<p>Постоянная экспозиция</p>\n" +
                "\t\t\t\t\t</div>\n" +
                "\t\t\t\t</a>\n" +
                "\t\t\t\t  \t\t\t\t\t\t\t<a href=\"/arenda/\">\n" +
                "\t\t\t\t\t<div class=\"whatwedo-block wow fadeIn\" data-wow-delay=\"0.4s\">\n" +
                "\t\t  \t\t\t\t<img src=\"/upload/iblock/5da/5da52c737266ed467027526cc18055a5.png\" alt=\"\">\n" +
                "\t\t  \t\t\t\t<h3>Организация выставок и концертов</h3>\n" +
                "\t\t  \t\t\t\t<p>Аренда зала под репетиции музыкальных коллективов.</p>\n" +
                "\t\t\t\t\t</div>\n" +
                "\t\t\t\t</a>\n" +
                "\t\t\t\t  \t\t\t\t\t\t\t<a href=\"/arenda/\">\n" +
                "\t\t\t\t\t<div class=\"whatwedo-block wow fadeIn\" data-wow-delay=\"0.6s\">\n" +
                "\t\t  \t\t\t\t<img src=\"/upload/iblock/e00/e008d30653e522a2c9623f32b53efc2d.png\" alt=\"\">\n" +
                "\t\t  \t\t\t\t<h3>Аренда зала под Ваши проекты</h3>\n" +
                "\t\t  \t\t\t\t<p>проведение корпоративов, презентаций, банкетов</p>\n" +
                "\t\t\t\t\t</div>\n" +
                "\t\t\t\t</a>\n" +
                "\t\t\t\t  \t\t\t\n" +
                "  \t\t</div>\n" +
                "  \t</section>\n" +
                "\n" +
                "  \t<section id=\"gallery\">\n" +
                "  \t\t<div class=\"row\">\n" +
                "  \t\t\t  \t\t\t  \t\t\t<a href=\"/upload/iblock/294/2946180878c43a16dace612586c4bca3.png\" data-lightbox=\"gallery\">\n" +
                "  \t\t\t\t<div class=\"gallery-block\" style=\"background-image: url(/upload/iblock/d75/d75aa22184c4a0fb6850788274dbbe81.png);\"></div>\n" +
                "  \t\t\t</a>\n" +
                "  \t\t\t  \t\t\t<a href=\"/upload/iblock/4a4/4a4c74f24e803b15887c4c2b4ee62134.JPG\" data-lightbox=\"gallery\">\n" +
                "  \t\t\t\t<div class=\"gallery-block\" style=\"background-image: url(/upload/iblock/8ff/8ff9415b2787f8b988e769241a1f3a80.JPG);\"></div>\n" +
                "  \t\t\t</a>\n" +
                "  \t\t\t  \t\t\t<a href=\"/upload/iblock/66e/66e360372a2fb2aa13ce189afc31d2e0.JPG\" data-lightbox=\"gallery\">\n" +
                "  \t\t\t\t<div class=\"gallery-block\" style=\"background-image: url(/upload/iblock/d3a/d3a1abddda58cbd9df3f9a620beb3ef0.JPG);\"></div>\n" +
                "  \t\t\t</a>\n" +
                "  \t\t\t  \t\t\t<a href=\"/upload/iblock/a0f/a0f9218750c0691c062faca5ae1a069b.JPG\" data-lightbox=\"gallery\">\n" +
                "  \t\t\t\t<div class=\"gallery-block\" style=\"background-image: url(/upload/iblock/5f3/5f32957423672ea5da8995f7e9243fb5.JPG);\"></div>\n" +
                "  \t\t\t</a>\n" +
                "  \t\t\t  \t\t\t<a href=\"/upload/iblock/10a/10aa7226843afc00528da7dfda93102e.png\" data-lightbox=\"gallery\">\n" +
                "  \t\t\t\t<div class=\"gallery-block\" style=\"background-image: url(/upload/iblock/a39/a396751520790defb940b7ef90a29f16.png);\"></div>\n" +
                "  \t\t\t</a>\n" +
                "  \t\t\t  \t\t\t<a href=\"/upload/iblock/215/2151bfa8d6472ac7aa5af2a67174d199.jpg\" data-lightbox=\"gallery\">\n" +
                "  \t\t\t\t<div class=\"gallery-block\" style=\"background-image: url(/upload/iblock/fd0/fd093262d8295c8217f5cc7d79eb13e0.jpg);\"></div>\n" +
                "  \t\t\t</a>\n" +
                "  \t\t\t  \t\t\t<a href=\"/upload/iblock/c53/c530222a57ef136b4eba16731f0e1934.png\" data-lightbox=\"gallery\">\n" +
                "  \t\t\t\t<div class=\"gallery-block\" style=\"background-image: url(/upload/iblock/23b/23bdbc39bf2265f0d515873ec9e5e4d8.png);\"></div>\n" +
                "  \t\t\t</a>\n" +
                "  \t\t\t  \t\t\t<a href=\"/upload/iblock/268/268c32ed2757c9ee092264a94cebda88.jpg\" data-lightbox=\"gallery\">\n" +
                "  \t\t\t\t<div class=\"gallery-block\" style=\"background-image: url(/upload/iblock/121/12168276c39b29b110695d5fddbd7102.jpg);\"></div>\n" +
                "  \t\t\t</a>\n" +
                "  \t\t\t</div>\t\n" +
                "  \t</section>\n" +
                "\n" +
                "  \t<section id=\"author\">\n" +
                "  \t\t<h2>О художнике</h2>\n" +
                "\n" +
                "  \t\t<div class=\"author-wrap\">\n" +
                "  \t\t\t<div class=\"container\">\n" +
                "\t\t\t\t<h3 class=\"wow fadeIn\" data-wow-delay=\"0.2s\">\"Мне нравится писать людей, показывать их уникальность. Я не устаю вглядываться, не устаю находить в человеке новое. Я люблю людей.\"</h3>\n" +
                "<p class=\"wow fadeIn\" data-wow-delay=\"0.4s\">\n" +
                "\tН.Б. Никогосян яркий представитель советской художественной школы, ученик А.Т. Матвеева и достойный современник М. Сарьяна, С.Т. Коненкова, С. Лебедевой, А.А.Осмеркина и многих других. <br>\n" +
                "\t<br>\n" +
                "\t В 90-е годы перестроил здание своей скульптурной мастерской (в которой создавались его монументальные работы) и организовал галерею на верхнем этаже. <br>\n" +
                "\t<br>\n" +
                " <a href=\"/author/\">Подробнее</a>\n" +
                "</p> \n" +
                "  \t\t\t\t\n" +
                "  \t\t\t</div>\n" +
                "  \t\t\t<div class=\"author-img-wrap\"></div>\n" +
                "  \t\t</div>\n" +
                "  \t</section>\n" +
                "\n" +
                "  \t<section id=\"news\">\n" +
                "  \t\t<div class=\"container\">\n" +
                "  \t\t\t<h2>Новости и события</h2>\n" +
                "  \t\t\t<a class=\"all-news\" href=\"/news/\">Смотреть все</a>\n" +
                "  \t\t\t  \t\t\t  \t\t\t\t<a href=\"/news/all/vystavka-albiny-voronkovoy-zvuki-i-kraski-vselennoy/\">\n" +
                "\t  \t\t\t\t<div class=\"news-block wow fadeIn\" data-wow-delay=\"0.2s\">\n" +
                "\t  \t\t\t\t\t<div class=\"news-img-wrap\"><img src=\"/upload/iblock/c33/c33e301df3c508ca366ed6b0ec4eb0fb.jpeg\" alt=\"\"></div>\n" +
                "\t  \t\t\t\t\t<h3>Выставка Альбины Воронковой &quot;Звуки и краски вселенной&quot;</h3>\n" +
                "\t  \t\t\t\t\t\n" +
                "<p>  </p>\n" +
                "\t  \t\t\t\t</div>\n" +
                "\t  \t\t\t</a>\n" +
                "\t  \t\t\t  \t\t\t  \t\t\t\t<a href=\"/news/vystavki/noch-muzeev-2023/\">\n" +
                "\t  \t\t\t\t<div class=\"news-block wow fadeIn\" data-wow-delay=\"0.4s\">\n" +
                "\t  \t\t\t\t\t<div class=\"news-img-wrap\"><img src=\"/upload/iblock/198/198a21c1a1d896ade6e7d11cb756808c.jpg\" alt=\"\"></div>\n" +
                "\t  \t\t\t\t\t<h3>Ночь музеев 2023</h3>\n" +
                "\t  \t\t\t\t\t\n" +
                "<p>  </p>\n" +
                "\t  \t\t\t\t</div>\n" +
                "\t  \t\t\t</a>\n" +
                "\t  \t\t\t  \t\t\t  \t\t\t\t<a href=\"/news/all/maryam-aslamazyan-predchuvstvie-vesny/\">\n" +
                "\t  \t\t\t\t<div class=\"news-block wow fadeIn\" data-wow-delay=\"0.6s\">\n" +
                "\t  \t\t\t\t\t<div class=\"news-img-wrap\"><img src=\"/upload/iblock/c55/c5580957fc7f24d8d910a07275b37fe6.jpeg\" alt=\"\"></div>\n" +
                "\t  \t\t\t\t\t<h3>Мариам Асламазян. «Предчувствие весны»</h3>\n" +
                "\t  \t\t\t\t\t\n" +
                "<p>  </p>\n" +
                "\t  \t\t\t\t</div>\n" +
                "\t  \t\t\t</a>\n" +
                "\t  \t\t\t  \t\t\t  \t\t\t\n" +
                "  \t\t</div>\n" +
                "  \t</section>\n" +
                "\n" +
                "  \t<section id=\"contacts\">\n" +
                "  \t\t\n" +
                "<script type=\"text/javascript\">\n" +
                "function BX_SetPlacemarks_MAP_DP2PV1n8UY(map)\n" +
                "{\n" +
                "\tif(typeof window[\"BX_YMapAddPlacemark\"] != 'function')\n" +
                "\t{\n" +
                "\t\t/* If component's result was cached as html,\n" +
                "\t\t * script.js will not been loaded next time.\n" +
                "\t\t * let's do it manualy.\n" +
                "\t\t*/\n" +
                "\n" +
                "\t\t(function(d, s, id)\n" +
                "\t\t{\n" +
                "\t\t\tvar js, bx_ym = d.getElementsByTagName(s)[0];\n" +
                "\t\t\tif (d.getElementById(id)) return;\n" +
                "\t\t\tjs = d.createElement(s); js.id = id;\n" +
                "\t\t\tjs.src = \"/bitrix/components/bitrix/map.yandex.view/templates/.default/script.js\";\n" +
                "\t\t\tbx_ym.parentNode.insertBefore(js, bx_ym);\n" +
                "\t\t}(document, 'script', 'bx-ya-map-js'));\n" +
                "\n" +
                "\t\tvar ymWaitIntervalId = setInterval( function(){\n" +
                "\t\t\t\tif(typeof window[\"BX_YMapAddPlacemark\"] == 'function')\n" +
                "\t\t\t\t{\n" +
                "\t\t\t\t\tBX_SetPlacemarks_MAP_DP2PV1n8UY(map);\n" +
                "\t\t\t\t\tclearInterval(ymWaitIntervalId);\n" +
                "\t\t\t\t}\n" +
                "\t\t\t}, 300\n" +
                "\t\t);\n" +
                "\n" +
                "\t\treturn;\n" +
                "\t}\n" +
                "\n" +
                "\tvar arObjects = {PLACEMARKS:[],POLYLINES:[]};\n" +
                "\tarObjects.PLACEMARKS[arObjects.PLACEMARKS.length] = BX_YMapAddPlacemark(map, {'LON':'37.575696932986','LAT':'55.768554470311','TEXT':'Большой Тишинский пер., д.19, строение 1\\n'});\n" +
                "}\n" +
                "</script>\n" +
                "<div class=\"bx-yandex-view-layout\">\n" +
                "\t<div class=\"bx-yandex-view-map\">\n" +
                "\t\t<script>\n" +
                "\t\t\tvar script = document.createElement('script');\n" +
                "\t\t\tscript.src = 'https://api-maps.yandex.ru/2.0/?load=package.full&mode=release&lang=ru-RU&wizard=bitrix';\n" +
                "\t\t\t(document.head || document.documentElement).appendChild(script);\n" +
                "\t\t\tscript.onload = function () {\n" +
                "\t\t\t\tthis.parentNode.removeChild(script);\n" +
                "\t\t\t};\n" +
                "\t\t</script>\n" +
                "\t\t<script type=\"text/javascript\">\n" +
                "\n" +
                "if (!window.GLOBAL_arMapObjects)\n" +
                "\twindow.GLOBAL_arMapObjects = {};\n" +
                "\n" +
                "function init_MAP_DP2PV1n8UY()\n" +
                "{\n" +
                "\tif (!window.ymaps)\n" +
                "\t\treturn;\n" +
                "\n" +
                "\tif(typeof window.GLOBAL_arMapObjects['MAP_DP2PV1n8UY'] !== \"undefined\")\n" +
                "\t\treturn;\n" +
                "\n" +
                "\tvar node = BX(\"BX_YMAP_MAP_DP2PV1n8UY\");\n" +
                "\tnode.innerHTML = '';\n" +
                "\n" +
                "\tvar map = window.GLOBAL_arMapObjects['MAP_DP2PV1n8UY'] = new ymaps.Map(node, {\n" +
                "\t\tcenter: [55.768573219878, 37.576162564406],\n" +
                "\t\tzoom: 17,\n" +
                "\t\ttype: 'yandex#map'\n" +
                "\t});\n" +
                "\n" +
                "\tif (map.behaviors.isEnabled(\"scrollZoom\"))\n" +
                "\t\tmap.behaviors.disable(\"scrollZoom\");\n" +
                "\tmap.behaviors.enable(\"dblClickZoom\");\n" +
                "\tmap.behaviors.enable(\"drag\");\n" +
                "\tif (map.behaviors.isEnabled(\"rightMouseButtonMagnifier\"))\n" +
                "\t\tmap.behaviors.disable(\"rightMouseButtonMagnifier\");\n" +
                "\tmap.controls.add('zoomControl');\n" +
                "\tmap.controls.add('smallZoomControl');\n" +
                "\tmap.controls.add('miniMap');\n" +
                "\tmap.controls.add('scaleLine');\n" +
                "\tif (window.BX_SetPlacemarks_MAP_DP2PV1n8UY)\n" +
                "\t{\n" +
                "\t\twindow.BX_SetPlacemarks_MAP_DP2PV1n8UY(map);\n" +
                "\t}\n" +
                "}\n" +
                "\n" +
                "(function bx_ymaps_waiter(){\n" +
                "\tif(typeof ymaps !== 'undefined')\n" +
                "\t\tymaps.ready(init_MAP_DP2PV1n8UY);\n" +
                "\telse\n" +
                "\t\tsetTimeout(bx_ymaps_waiter, 100);\n" +
                "})();\n" +
                "\n" +
                "\n" +
                "/* if map inits in hidden block (display:none)\n" +
                "*  after the block showed\n" +
                "*  for properly showing map this function must be called\n" +
                "*/\n" +
                "function BXMapYandexAfterShow(mapId)\n" +
                "{\n" +
                "\tif(window.GLOBAL_arMapObjects[mapId] !== undefined)\n" +
                "\t\twindow.GLOBAL_arMapObjects[mapId].container.fitToViewport();\n" +
                "}\n" +
                "\n" +
                "</script>\n" +
                "<div id=\"BX_YMAP_MAP_DP2PV1n8UY\" class=\"bx-yandex-map\" style=\"height: 500px; width: 100%;\">загрузка карты...</div>\t</div>\n" +
                "</div>\n" +
                "\n" +
                "  \t\t<div class=\"container\">\n" +
                "        <div class=\"contacts-shadow\"></div>\n" +
                "  \t\t\t<div class=\"contacts-left\">\n" +
                "  \t\t\t\t<h2>Контакты</h2>\n" +
                "  \t\t\t\t<p>\n" +
                "\tМосква,<br>\n" +
                "\tБольшой Тишинский пер., д.19, стр. 1. <br>\n" +
                "\t<br>\n" +
                "\tЧасы работы:<br>\n" +
                "\tкаждый день, 14–19 (по договорённости)<br>\n" +
                "\t<br>\n" +
                "\t+7 (985) 998 97 95<br>\n" +
                "\t+7 (499) 253 86 07<br>\n" +
                "\t<a href=\"#\">info@nikoartgallery.com</a>\n" +
                "</p> \n" +
                "  \t\t\t</div>\n" +
                "  \t\t\t<div class=\"contacts-right\">\n" +
                "  \t\t\t\t<div class=\"owl-contacts\">\n" +
                "  \t\t\t\t\t\n" +
                "  \t\t\t\t\t  \t\t\t\t\t  \t\t\t\t\t\t<div class=\"item\">\n" +
                "\t  \t\t\t\t\t\t<img src=\"/upload/iblock/303/303a4f962733e2dad7a2e559a7383587.jpg\" alt=\"\">\n" +
                "\t  \t\t\t\t\t</div>\n" +
                "  \t\t\t\t\t  \t\t\t\t\t\t<div class=\"item\">\n" +
                "\t  \t\t\t\t\t\t<img src=\"/upload/iblock/14f/14f60b1f6dc054fd60d6e99fb858214d.jpg\" alt=\"\">\n" +
                "\t  \t\t\t\t\t</div>\n" +
                "  \t\t\t\t\t  \t\t\t\t\t\n" +
                "  \t\t\t\t\t\n" +
                "  \t\t\t\t</div>\n" +
                "  \t\t\t</div>\n" +
                "  \t\t</div>\n" +
                "\n" +
                "  \t</section>\n" +
                "\n" +
                "\n" +
                "      <footer>\n" +
                "        <div class=\"container\">\n" +
                "          <div class=\"row\">\n" +
                "            <div class=\"footer-top\">\n" +
                "\t\t\t\t<a href=\"/\"><img src=\"/bitrix/templates/addeo/img/logo.png\" alt=\"\"></a>\n" +
                "              \n" +
                "<ul>\n" +
                "\n" +
                "\t\t\t<li><a href=\"/gallery/\">Галерея</a></li>\n" +
                "\t\t\n" +
                "\t\t\t<li><a href=\"/author/\">О художнике</a></li>\n" +
                "\t\t\n" +
                "\t\t\t<li><a href=\"/afisha/\">Aфиша</a></li>\n" +
                "\t\t\n" +
                "\t\t\t<li><a href=\"/arenda/\">Аренда залов</a></li>\n" +
                "\t\t\n" +
                "\t\t\t<li><a href=\"/news/\">Новости</a></li>\n" +
                "\t\t\n" +
                "\t\t\t<li><a href=\"/contacts/\">Контакты</a></li>\n" +
                "\t\t\n" +
                "\n" +
                "</ul>\n" +
                "            </div>\n" +
                "          </div>\n" +
                "          <div class=\"row\">\n" +
                "            <div class=\"footer-bottom\">\n" +
                "              <p>\n" +
                "              \t\n" +
                "              \t \t              \t<span style=\"color: #acacac;\">Большой Тишинский пер., д.19, строение 1. </span><br>\n" +
                "<span style=\"color: #acacac;\"> </span><span style=\"color: #acacac;\">\n" +
                "ежедневно с 14 до 19 (посещение до договоренности)</span> \n" +
                "\t                            </p>\n" +
                "              <p class=\"phone\">\n" +
                "              \t<p>\n" +
                " <span style=\"color: #acacac;\">+7 (985) 998 97 95</span><br>\n" +
                "\t<span style=\"color: #acacac;\">\n" +
                "\t+7 (499) 253 86 07 </span>\n" +
                "</p>\n" +
                "<span style=\"color: #acacac;\"> </span><br> \n" +
                "              </p>\n" +
                "                            \t<p class=\"addeo\">дизайн и разработка - <a target=\"_blank\" href=\"http://addeo.ru\">Addeo.ru</a><br>Фото: <a href=\"mailto:sysoi@mail.ru\">Борис Сысоев</a></p>\n" +
                "              \t\t\t</div>\n" +
                "          </div>\n" +
                "        </div>\n" +
                "      </footer>\n" +
                "\n" +
                "\n" +
                "      <!-- jQuery (necessary for Bootstrap's JavaScript plugins) -->\n" +
                "      <script src=\"https://ajax.googleapis.com/ajax/libs/jquery/1.11.3/jquery.min.js\"></script>\n" +
                "      <!-- Include all compiled plugins (below), or include individual files as needed -->\n" +
                "      <script src=\"/bitrix/templates/addeo/js/bootstrap.min.js\"></script>\n" +
                "      <script src=\"/bitrix/templates/addeo/js/owl.carousel.js\"></script>\n" +
                "      <script src=\"/bitrix/templates/addeo/js/lightbox.js\"></script>\n" +
                "      <script src=\"/bitrix/templates/addeo/js/wow.min.js\"></script>\n" +
                "   \t\t<script>\n" +
                "      \t\tnew WOW().init();\n" +
                "    \t</script>\n" +
                "    \t<script src=\"/bitrix/templates/addeo/js/jquery.stellar.js\"></script>\n" +
                "      <script src=\"/bitrix/templates/addeo/js/script.js\"></script>\n" +
                "      \n" +
                "      \n" +
                "      \n" +
                "      <!— Yandex.Metrika counter —> \n" +
                "<script type=\"text/javascript\"> \n" +
                "(function (d, w, c) { \n" +
                "(w[c] = w[c] || []).push(function() { \n" +
                "try { \n" +
                "w.yaCounter44647948 = new Ya.Metrika({ \n" +
                "id:44647948, \n" +
                "clickmap:true, \n" +
                "trackLinks:true, \n" +
                "accurateTrackBounce:true, \n" +
                "webvisor:true \n" +
                "}); \n" +
                "} catch(e) { } \n" +
                "}); \n" +
                "\n" +
                "var n = d.getElementsByTagName(\"script\")[0], \n" +
                "s = d.createElement(\"script\"), \n" +
                "f = function () { n.parentNode.insertBefore(s, n); }; \n" +
                "s.type = \"text/javascript\"; \n" +
                "s.async = true; \n" +
                "s.src = \"https://mc.yandex.ru/metrika/watch.js\"; \n" +
                "\n" +
                "if (w.opera == \"[object Opera]\") { \n" +
                "d.addEventListener(\"DOMContentLoaded\", f, false); \n" +
                "} else { f(); } \n" +
                "})(document, window, \"yandex_metrika_callbacks\"); \n" +
                "</script> \n" +
                "<noscript><div><img src=\"https://mc.yandex.ru/watch/44647948\" style=\"position:absolute; left:-9999px;\" alt=\"\" /></div></noscript> \n" +
                "<!— /Yandex.Metrika counter —>\n" +
                "      \n" +
                "    </body>\n" +
                "    </html> ";

        return pageHTMLage;
    }

}


/*
    @Lock(value = LockModeType.OPTIMISTIC_FORCE_INCREMENT)
    @Transactional(
//                    transactionManager = "entityManagerFactoryT",
            propagation = Propagation.REQUIRES_NEW,
            isolation = Isolation.SERIALIZABLE
    )
    public void addPageTVS(String link, String linkAU, PageWriter pageWriter) throws IOException
//    public synchronized Page addPage(String link, String linkAU) throws IOException
    {
        pageWriter.addPage(link, linkAU);
    }

    @Lock(value = LockModeType.OPTIMISTIC_FORCE_INCREMENT)
    @Transactional(
//                    transactionManager = "entityManagerFactoryT",
            propagation = Propagation.REQUIRES_NEW,
            isolation = Isolation.SERIALIZABLE
    )
    public Page addPageTV(String link, String linkAU, Site site, PageRepository pageRepository, SiteRepository siteRepository) throws IOException
//    public synchronized Page addPage(String link, String linkAU) throws IOException
    {
        Page result = null;
        Page pageValues = new Page();
        pageValues.setPath(link);
        pageValues.setSite(site);
        // Добавление HTML кода страницы:
        // pageValues.setContent(documentToString(Jsoup.connect(page.getSite().getUrl()+path).get()));
        pageValues.setContent(new Date().toString() + " - " + String.valueOf(TransactionSynchronizationManager.isActualTransactionActive()));   // Дописать !!!
        pageValues.setCode(Jsoup.connect(linkAU)
                .execute()
                .statusCode());   //  int code = Jsoup.connect(linkAU).execute().statusCode();

        boolean tx = TransactionSynchronizationManager.isActualTransactionActive();

        if (!pageRepository.existsByPath(link)) {

            pageRepository.save(pageValues); // Сохранение в БД !!!

            //Блок добавления Site в БД:
            site.setStatusTime(new Date());
            site.addPage(pageValues);
            siteRepository.save(site);

            result = pageValues;
        }
        return result;
    }
*/
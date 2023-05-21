package searchengine.services;

import org.jsoup.Jsoup;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import searchengine.model.Page;
import searchengine.model.Site;

import javax.persistence.LockModeType;
import java.io.IOException;
import java.util.Date;

public class TestValue
{
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


}

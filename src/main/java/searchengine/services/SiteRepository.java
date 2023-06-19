package searchengine.services;

import org.springframework.data.repository.CrudRepository;
import searchengine.model.Page;
import searchengine.model.Site;

import javax.xml.parsers.SAXParserFactory;
import java.util.List;

public interface SiteRepository extends CrudRepository<Site,Integer>
{
//    List<Site> findAllByFirstName(String firstName);

    Site findByUrl(String url);
    boolean existsByUrl(String url);

}

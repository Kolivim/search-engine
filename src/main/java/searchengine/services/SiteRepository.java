package searchengine.services;

import org.springframework.data.repository.CrudRepository;
import searchengine.model.Site;

import javax.xml.parsers.SAXParserFactory;

public interface SiteRepository extends CrudRepository<Site,Integer>
{
//    List<Site> findAllByFirstName(String firstName);
}

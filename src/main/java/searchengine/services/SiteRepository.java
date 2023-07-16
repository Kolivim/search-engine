package searchengine.services;

import org.springframework.data.repository.CrudRepository;
import searchengine.model.Site;

import java.util.List;

public interface SiteRepository extends CrudRepository<Site,Integer>
{
    Site findByUrl(String url);
    boolean existsByUrl(String url);
    List<Site> findAll();
}

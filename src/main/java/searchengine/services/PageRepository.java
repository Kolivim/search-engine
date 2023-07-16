package searchengine.services;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import searchengine.model.Page;
import searchengine.model.Site;

import java.util.List;
import java.util.Optional;

@EnableTransactionManagement
@Repository
public interface PageRepository extends CrudRepository<Page, Integer> {
    List<Page> findAllBySiteId(int siteId);

    void deleteAllBySiteId(int siteId);

    Optional<Page> findByPath(String path);

    boolean existsByPath(String path);

    boolean existsByPathAndSite(String path, Site site);

    boolean existsByPathAndSiteId(String path, int siteId);

    void deleteByPathAndSite(String path, Site site);

    void deleteByPathAndSiteId(String path, int siteId);

    Page findByPathAndSite(String path, Site site);

    Integer countBySiteId(int siteId);

}
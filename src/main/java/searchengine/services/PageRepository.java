package searchengine.services;

import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.Page;
import searchengine.model.Site;

import javax.persistence.LockModeType;
import java.util.List;
import java.util.Optional;

//@Component
@EnableTransactionManagement
@Repository
public interface PageRepository extends CrudRepository<Page, Integer>
{
        List<Page> findAllBySiteId(int siteId);

        void deleteAllBySiteId(int siteId);

//        @Lock(value = LockModeType.PESSIMISTIC_READ)
        Optional<Page> findByPath(String path);

//        @Lock(value = LockModeType.OPTIMISTIC)
//        @Transactional
                // So spring : @Transactional ???
        Optional<Page> findByPathAndSite(String path, Site site);

//        Optional<Page> findByPathAndSiteIdForUpdate(String path, Integer siteId);


//        Optional<Page> findByPathForUpdate(String path);

//        Page findByPath(String path);

//        @Retryable(backoff = @Backoff(delay = 1, maxDelay = 100, random = true))
//        @Transactional
//        @Lock(value = LockModeType.OPTIMISTIC)

        boolean existsByPath(String path);

//        boolean existsByPathToShare(String path);

//        @Lock(LockModeType.OPTIMISTIC_FORCE_INCREMENT)
//        @Lock(LockModeType.PESSIMISTIC_READ)          // no transaction is in progress; nested exception is javax.persistence.TransactionRequiredException: no transaction is in progress
        boolean existsByPathAndSite(String path, Site site);

//        boolean existsByPathAndSiteForUpdate(String path, Site site);

//        @Query("SELECT count(path) FROM search_engine.pages GROUP BY path HAVING COUNT(path) > 1")
//        int pathCount(String path);


//        @Query("SELECT page FROM search_engine.pages WHERE `path` = 3 FOR UPDATE")
//        @Query("SELECT page FROM search_engine.pages page WHERE page.path = :link FOR UPDATE")
//        boolean isBlock(@Param("link") String link);








//        @Query("select e from Employees e where e.salary > :salary")
//        List<Employees> findEmployeesWithMoreThanSalary(@Param("salary") Long salary, Sort sort);



}

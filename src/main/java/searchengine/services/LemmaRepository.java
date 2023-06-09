package searchengine.services;

import org.springframework.data.repository.CrudRepository;
import searchengine.model.Index;
import searchengine.model.Lemma;
import searchengine.model.Page;
import searchengine.model.Site;

import java.util.List;
import java.util.Optional;

public interface LemmaRepository  extends CrudRepository<Lemma, Integer>
{
    Optional<Lemma> findByLemmaAndSiteId(String lemma, int siteId);
    Optional<Lemma> findByIdAndSiteId(int id, int siteId);
    void deleteByIdAndSiteId(Integer integer, int siteId);
    List<Lemma> findAllBySiteId(int siteId);
    //List<Lemma> findAllByLemma(Iterable<String> lemma);
    Integer countBySiteId(int siteId); // 6 jule

}

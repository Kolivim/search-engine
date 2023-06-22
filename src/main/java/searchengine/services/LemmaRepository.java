package searchengine.services;

import org.springframework.data.repository.CrudRepository;
import searchengine.model.Lemma;
import searchengine.model.Page;

import java.util.List;
import java.util.Optional;

public interface LemmaRepository  extends CrudRepository<Lemma, Integer>
{
    Optional<Lemma> findByLemmaAndSiteId(String lemma, int siteId);
    Optional<Lemma> findByIdAndSiteId(int id, int siteId);
    void deleteByIdAndSiteId(Integer integer, int siteId);
}

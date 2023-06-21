package searchengine.services;

import org.springframework.data.repository.CrudRepository;
import searchengine.model.Lemma;
import searchengine.model.Page;

import java.util.Optional;

public interface LemmaRepository  extends CrudRepository<Lemma, Integer>
{
    Optional<Lemma> findByLemmaAndSiteId(String lemma, int siteId);
}

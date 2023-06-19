package searchengine.services;

import org.springframework.data.repository.CrudRepository;
import searchengine.model.Lemma;
import searchengine.model.Page;

public interface LemmaRepository  extends CrudRepository<Lemma, Integer>
{
    boolean existsByLemma(String lemma);
}

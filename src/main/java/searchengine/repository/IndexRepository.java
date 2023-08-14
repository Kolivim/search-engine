package searchengine.repository;

import org.springframework.data.repository.CrudRepository;
import searchengine.model.Index;

import java.util.List;
import java.util.Optional;

public interface IndexRepository extends CrudRepository<Index, Integer> {
    Optional<Index> findByPageIdAndLemmaId(int pageId, int lemmaId);

    List<Index> findAllByPageId(int pageId);

    List<Index> findAllByLemmaId(int lemmmaFindId);
}

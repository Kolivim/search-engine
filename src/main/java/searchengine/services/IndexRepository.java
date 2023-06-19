package searchengine.services;

import org.springframework.data.repository.CrudRepository;
import searchengine.model.Index;
import searchengine.model.Page;

public interface IndexRepository  extends CrudRepository<Index, Integer>
{

}

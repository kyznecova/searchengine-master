package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import searchengine.model.LemmaEntity;
import searchengine.model.SiteEntity;

import java.util.ArrayList;


@Repository
public interface LemmaRepository extends JpaRepository<LemmaEntity, Integer> {
    ArrayList<LemmaEntity> findLemmaEntitiesByLemmaEqualsIgnoreCase (String lemma);
    Integer countBySiteId(SiteEntity siteId);

}

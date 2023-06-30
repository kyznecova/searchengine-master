package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import searchengine.model.IndexEntity;
import searchengine.model.PageEntity;
import searchengine.model.SiteEntity;

import java.util.ArrayList;

@Repository
public interface IndexRepository extends JpaRepository<IndexEntity, Integer> {
    ArrayList<IndexEntity> findIndexEntityByLemmaId_LemmaAndPageId_SiteId (String lemma, SiteEntity site);
    ArrayList<IndexEntity> findIndexEntityByPageIdAndLemmaId_Lemma (PageEntity page, String lemma);
}

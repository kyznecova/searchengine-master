package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.PageEntity;



@Transactional
@Repository
public interface PageRepository extends JpaRepository<PageEntity, Integer> {

}

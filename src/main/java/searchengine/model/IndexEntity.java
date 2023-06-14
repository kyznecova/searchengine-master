package searchengine.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "index_entity")
public class IndexEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false)
    private int id;

   /* @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(referencedColumnName = "page_id", name = "page_id")
    private PageEntity pageId;*/
    @Column(name = "page_id")
    private int pageId;

   /* @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(referencedColumnName = "lemma_id", name = "lemma_id")
    private LemmaEntity lemmaId;*/
   @Column(name = "lemma_id")
    private int lemmaId;

    /*@Column(name = "page_id", insertable = false, updatable = false)
    private int pageID;
    @Column(name = "lemma_id", insertable = false, updatable = false)
    private int lemmaID;*/

    @Column(name = "lemma_rank", nullable = false)
    private float rank;
}

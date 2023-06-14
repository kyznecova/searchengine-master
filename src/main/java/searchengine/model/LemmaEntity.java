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
@Table(name = "lemma")
public class LemmaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "lemma_id", nullable = false)
    private int lemmaId;
   /* @ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "site_id", nullable = false)
    private SiteEntity siteId;*/
    @Column(name = "site_id")
    private int siteId;
    @Column(columnDefinition = "VARCHAR(255)", nullable = false)
    private String lemma;
    @Column(nullable = false)
    private int frequency;

   /* @ManyToMany(cascade = CascadeType.ALL)
    @JoinTable(name = "search_index",
            joinColumns = {@JoinColumn(name = "lemma_id")},
            inverseJoinColumns = {@JoinColumn(name = "page_id")})
    private List<PageEntity> pageEntities;*/

/*    @OneToMany(cascade = CascadeType.ALL)
    @JoinColumn(name = "lemma_id")
    private List<IndexEntity> indexEntity;*/


}

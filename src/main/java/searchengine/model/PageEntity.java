package searchengine.model;

import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import searchengine.config.Site;

import javax.persistence.*;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter

@Table(name = "page", indexes = @Index(name = "path_index", columnList = "path, site_id", unique = true))
public class PageEntity{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    @ManyToOne(cascade = CascadeType.MERGE, fetch = FetchType.LAZY)
    @JoinColumn (name = "site_id", referencedColumnName = "id", nullable = false)
    private SiteEntity siteId;
    @Column(columnDefinition = "VARCHAR(768) CHARACTER SET utf8", nullable = false)
    private String path;
    @Column(nullable = false)
    private int code;
    @Column(columnDefinition = "MEDIUMTEXT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci", nullable = false)
    private String content;

    @OneToMany(mappedBy = "pageId")
    private Set<IndexEntity> indexes = new HashSet<>();


}

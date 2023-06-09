package searchengine.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "pages"
//        , indexes =
//        {
//        @Index(name = "path_index", columnList = "path")    // alter table search_engine.pages ADD KEY(path(767)); CREATE INDEX idx_col1 ON my_table (col1(255));
//        }
)
//@NamedNativeQuery(name="Key", query="alter table search_engine.pages ADD KEY(path(767))")
//@org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.READ_WRITE)   // 28.03
//@org.hibernate.annotations.Cache(usage = SharedCacheMode.NONE)
@org.hibernate.annotations.DynamicInsert    // 30.03
//@org.hibernate.annotations.DynamicUpdate    // 30.03
//@org.hibernate.annotations.SelectBeforeUpdate    // 30.03
//@org.hibernate.annotations.OptimisticLocking(
//        type = org.hibernate.annotations.OptimisticLockType.ALL)
public class Page //implements Auditable // implements Serializable
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false
//            , insertable = false, updatable = false
            )
    private int id;

    @Version
    private long version;



    @Column(name = "site_id", nullable = false
            , insertable = false, updatable = false
            )
    private int siteId;

//    @Version
//    @Access(AccessType.PROPERTY)
    @Column(columnDefinition = "TEXT NOT NULL, Index (path(512))" /*, nullable = false*/)
    private String path;
    @Column(nullable = false)
    private int code;
    @Column(columnDefinition = "MEDIUMTEXT", nullable = false)
    private String content;



//    @ManyToOne(cascade = CascadeType.ALL)
    @ManyToOne(cascade = CascadeType.MERGE) // Исправил из-за добавления в БД page в compute()
    @JoinColumn(name = "site_id")
//    @JoinColumns
//            ({
//                    @JoinColumn(name = "sites", referencedColumnName = "id"),
//                    @JoinColumn(name = "pages", referencedColumnName = "site_id")
//            })
    private Site site;

    @Override
    public String toString()
    {
        return "Page{" +
                "id=" + id +
                ", siteId=" + siteId +
                ", path='" + path + '\'' +
                ", code=" + code +
                ", content='" + content + '\'' +
//                ", site=" + site +
                '}';
    }
}

package searchengine.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "pages")
@org.hibernate.annotations.DynamicInsert
public class Page {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false)
    private int id;

    @Version
    private long version;

    @Column(name = "site_id", nullable = false, insertable = false, updatable = false)
    private int siteId;

    @Column(columnDefinition = "TEXT NOT NULL, Index (path(512))")
    private String path;
    @Column(nullable = false)
    private int code;
    @Column(columnDefinition = "MEDIUMTEXT", nullable = false)
    private String content;
    @ManyToOne(cascade = CascadeType.MERGE)
    @JoinColumn(name = "site_id")
    private Site site;

    @Override
    public String toString() {
        return "Page{" +
                "id=" + id +
                ", siteId=" + siteId +
                ", path='" + path + '\'' +
                ", code=" + code +
                ", content[Length]= ['" + content.length() + "]" + '\'' +
                '}';
    }
}

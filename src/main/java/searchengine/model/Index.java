package searchengine.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "\"index\"")
public class Index {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(name = "page_id", nullable = false)
    private int pageId;

    @Column(name = "lemma_id", nullable = false)
    private int lemmaId;

    @Column(name = "\"rank\"", nullable = false)
    private float rank;

    public Index(int pageId, int lemmaId, float rank) {
        this.pageId = pageId;
        this.lemmaId = lemmaId;
        this.rank = rank;
    }

    public void frequencyIndexIncr() {
        this.rank++;
    }

    @Override
    public String toString() {
        return "Index:[" +
                " id = " + id +
                ", pageId = " + pageId +
                ", lemmaId = " + lemmaId +
                ", rank = " + rank +
                " ]";
    }

}

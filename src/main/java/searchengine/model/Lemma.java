package searchengine.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "lemma")
public class Lemma {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(name = "site_id", nullable = false)
    private int siteId;

    @Column(columnDefinition = "VARCHAR(255) NOT NULL, Index (lemma(255))") //(columnDefinition = "VARCHAR(255) NOT NULL") // (columnDefinition = "VARCHAR(255) NOT NULL, Index (lemma(255))")

    private String lemma;

    @Column(nullable = false)
    private int frequency;

    public Lemma(int siteId, String lemma, int frequency) {
        this.siteId = siteId;
        this.lemma = lemma;
        this.frequency = frequency;
    }

    public void frequencyLemmaIncr() {
        this.frequency++;
    }

    public void frequencyLemmaDecr() {
        this.frequency--;
    }

    @Override
    public String toString() {
        return "Lemma:[" +
                " id = " + id +
                ", siteId = " + siteId +
                ", lemma = " + lemma +
                ", frequency = " + frequency +
                " ]";
    }
}

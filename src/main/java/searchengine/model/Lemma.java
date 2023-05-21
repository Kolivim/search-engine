package searchengine.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name="lemma")
public class Lemma
{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(
            name = "site_id", nullable = false
//            , insertable = false, updatable = false
            )
    private int siteId;

    @Column(nullable = false)
    private String lemma;

    @Column(nullable = false)
    private int frequency;





}

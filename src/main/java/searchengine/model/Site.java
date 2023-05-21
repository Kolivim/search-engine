package searchengine.model;


import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Type;

import javax.persistence.*;
import javax.print.attribute.EnumSyntax;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;

import static javax.persistence.EnumType.STRING;

//enum StatusTypeF {INDEXING, INDEXED, FAILED;}

@Getter
@Setter
@Entity
@Table(name = "sites")
public class Site
{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false
//            , insertable = false, updatable = false
            )
    private int id;




//    @Transient
//    String sql = "\"enum(" + StatusType.values().toString() + ")\""; //
//    public String getSql() {return sql;}

    @Enumerated(value = STRING)
//    @Convert(converter = StatusType.class)
    @Column(
//            columnDefinition = "\"enum\""
//            length = 125,
//            columnDefinition = getSql()
//            columnDefinition = "TEXT",
            columnDefinition = "enum('INDEXING', 'INDEXED', 'FAILED')" // Рабочее
            ,
            nullable = false)
//            )

    private StatusType status;
/*  status ENUM('INDEXING', 'INDEXED', 'FAILED') NOT NULL — текущий статус
    полной индексации сайта, отражающий готовность поискового движка осуществлять
    поиск по сайту — индексация или переиндексация в процессе, сайт полностью
    проиндексирован (готов к поиску) либо его не удалось проиндексировать
    (сайт не готов к поиску и не будет до устранения ошибок и перезапуска индексации);  */




    @Column(name = "status_time", nullable = false)
    private Date statusTime;    // SimpleDateFormat
 /*
    status_time DATETIME NOT NULL — дата и время статуса
    (в случае статуса INDEXING дата и время должны обновляться регулярно при добавлении каждой новой страницы в индекс);
*/

    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError;
    /*
    last_error TEXT — текст ошибки индексации или NULL, если её не было;
    */

    @Column(nullable = false)
    private String url;
    /*
    url VARCHAR(255) NOT NULL — адрес главной страницы сайта;
    */



    @Column(nullable = false)
    private String name;
    /*
    name VARCHAR(255) NOT NULL — имя сайта.
    */


    @OneToMany
            (
            cascade = CascadeType.ALL
            , mappedBy = "site"
//                    , fetch = FetchType.EAGER
            )
//    @JoinColumn(name = "site_id")

   /* @JoinColumns
            ({
            @JoinColumn(name = "sites", referencedColumnName = "id"),
            @JoinColumn(name = "pages", referencedColumnName = "site_id")
            })*/

    private List<Page> pages = new ArrayList<>(); /// 3 mart


    public Site()
    {
    }

    @Override
    public String toString() {
        return "Site{" +
                "id=" + id +
                ", status=" + status +
                ", statusTime=" + statusTime +
                ", lastError='" + lastError + '\'' +
                ", url='" + url + '\'' +
                ", name='" + name + '\'' +
//                ", pages Count=" + pages.size() +
                '}';
    }

    public void addPage(Page page)
    {
        pages.add(page);
    }
}

package searchengine.model;


import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static javax.persistence.EnumType.STRING;

@Getter
@Setter
@Entity
@Table(name = "sites")
public class Site {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false)
    private int id;

    /*  status ENUM('INDEXING', 'INDEXED', 'FAILED') NOT NULL — текущий статус
    полной индексации сайта, отражающий готовность поискового движка осуществлять
    поиск по сайту — индексация или переиндексация в процессе, сайт полностью
    проиндексирован (готов к поиску) либо его не удалось проиндексировать
    (сайт не готов к поиску и не будет до устранения ошибок и перезапуска индексации);  */
    @Enumerated(value = STRING)
    @Column(columnDefinition = "enum('INDEXING', 'INDEXED', 'FAILED')", nullable = false)
    private StatusType status;

    /* status_time DATETIME NOT NULL — дата и время статуса
    (в случае статуса INDEXING дата и время должны обновляться регулярно
    при добавлении каждой новой страницы в индекс); */
    @Column(name = "status_time", nullable = false)
    private Date statusTime;

    /* last_error TEXT — текст ошибки индексации или NULL, если её не было; */
    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError;

    /* url VARCHAR(255) NOT NULL — адрес главной страницы сайта; */
    @Column(nullable = false)
    private String url;

    /* name VARCHAR(255) NOT NULL — имя сайта. */
    @Column(nullable = false)
    private String name;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "site")
    private List<Page> pages = new ArrayList<>();

    public Site() {
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
                '}';
    }

    public void addPage(Page page) {
        pages.add(page);
    }
}

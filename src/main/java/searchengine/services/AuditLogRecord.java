package searchengine.services;

import com.sun.istack.NotNull;

import javax.persistence.*;
import java.util.Date;

@Entity
public class AuditLogRecord
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    protected int id;
//    @NotNull
    protected String path;
    @NotNull
    protected String message;
//    @NotNull
    protected Long version;
    @NotNull
    protected Class entityClass;
    @NotNull
    protected Long currentUserId;
//    @NotNull
    protected Long siteId;
    @NotNull
    @Temporal(TemporalType.TIMESTAMP)
    protected Date createdOn = new Date();

    public AuditLogRecord() {
    }

    public AuditLogRecord(String insert, Auditable entity, Long currentUserId) {
        this.message = insert;
        this.currentUserId = currentUserId;
        this.entityClass = entity.getClass();

    }

}

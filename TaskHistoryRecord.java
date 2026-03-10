@Entity
@Table(name = "taskhistory")
public class TaskHistoryRecord {
    @Id
    private long id;
    private String record;

    public TaskHistoryRecord(String record) {
        this.record = record;
    }


    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getRecord() {
        return record;
    }

    public void setRecord(String record) {
        this.record = record;
    }
}

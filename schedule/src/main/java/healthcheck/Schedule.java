package healthcheck;

import javax.persistence.*;
import org.springframework.beans.BeanUtils;
import java.util.List;
import java.util.Date;

@Entity
@Table(name="Schedule_table")
public class Schedule {

    @Id
    @GeneratedValue(strategy=GenerationType.AUTO)
    private Long id;
    private Long scheduleId;
    private Integer availableCount;

    @PostPersist
    public void onPostPersist(){
        ScheduleResistered scheduleResistered = new ScheduleResistered();
        BeanUtils.copyProperties(this, scheduleResistered);
        scheduleResistered.publishAfterCommit();


    }

    @PostUpdate
    public void onPostUpdate(){
        CountModified countModified = new CountModified();
        BeanUtils.copyProperties(this, countModified);
        countModified.publishAfterCommit();


    }


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
    public Long getScheduleId() {
        return scheduleId;
    }

    public void setScheduleId(Long scheduleId) {
        this.scheduleId = scheduleId;
    }
    public Integer getAvailableCount() {
        return availableCount;
    }

    public void setAvailableCount(Integer availableCount) {
        this.availableCount = availableCount;
    }




}

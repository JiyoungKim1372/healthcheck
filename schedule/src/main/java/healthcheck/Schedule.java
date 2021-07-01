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
        
        // 요청처리 쓰레드에서 처리시간이 610 밀리가 넘어서기 시작하여 어느정도 유지되면 CB 회로가 닫히도록 (요청을 빠르게 실패처리, 차단) 설정
        /*
        try {
            System.out.println("#####  schedule service 부하  #####");
            Thread.currentThread().sleep((long) (400 + Math.random() * 220));
            System.out.println("#####  schedule service 부하  #####");
        } catch (InterruptedException e) {
                e.printStackTrace();
        }
        */
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

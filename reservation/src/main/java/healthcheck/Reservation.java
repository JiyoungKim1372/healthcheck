package healthcheck;

import javax.persistence.*;
import org.springframework.beans.BeanUtils;
import java.util.List;
import java.util.Date;

@Entity
@Table(name="Reservation_table")
public class Reservation {

    @Id
    @GeneratedValue(strategy=GenerationType.AUTO)
    private Long id;
    private Long reservationId;
    private Long scheduleId;
    private Integer reservationCount;
    private String reservationStatus;

    @PrePersist
    public void onPrePersist() throws Exception {
        System.out.println("##### Reservation - process : Start #####");
        this.setReservationStatus("Health Check Reserved");
    }

    @PostPersist
    public void onPostPersist() throws Exception {
     

        boolean rslt = ReservationApplication.applicationContext.getBean(healthcheck.external.ScheduleService.class)
        .modifyCount(this.getScheduleId(), this.getReservationCount());


        if(rslt) {
            
            System.out.println("##### Reservation - Result : Success #####");
            this.setReservationStatus("Health Check Reserved");
            Reserved reserved = new Reserved();
            BeanUtils.copyProperties(this, reserved);
            reserved.publishAfterCommit();
            
        }else{
            System.out.println("##### Reservation - Result : Fail - limit exceed #####");
            throw new Exception("Fail:limit exceed");
        }

    }

    @PreRemove
    public void onPreRemove(){
        Canceled canceled = new Canceled();
        BeanUtils.copyProperties(this, canceled);
        canceled.publishAfterCommit();

    }


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
    public Long getReservationId() {
        return reservationId;
    }

    public void setReservationId(Long reservationId) {
        this.reservationId = reservationId;
    }
    public Long getScheduleId() {
        return scheduleId;
    }

    public void setScheduleId(Long scheduleId) {
        this.scheduleId = scheduleId;
    }
    public Integer getReservationCount() {
        return reservationCount;
    }

    public void setReservationCount(Integer reservationCount) {
        this.reservationCount = reservationCount;
    }
    public String getReservationStatus() {
        return reservationStatus;
    }

    public void setReservationStatus(String reservationStatus) {
        this.reservationStatus = reservationStatus;
    }




}

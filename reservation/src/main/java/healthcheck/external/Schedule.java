package healthcheck.external;

public class Schedule {

    private Long id;
    private Long scheduleId;
    private Integer availableCount;

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

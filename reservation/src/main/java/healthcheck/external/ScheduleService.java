
package healthcheck.external;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Date;

//@FeignClient(name="schedule", url="http://schedule:8080")
@FeignClient(name="schedule", url="${api.url.schedule}")
public interface ScheduleService {

    @RequestMapping(method= RequestMethod.GET, path="/chkAndModifyCount")
    public boolean modifyCount(@RequestParam("scheduleId") Long scheduleId,
                              @RequestParam("reservationCount") int reservationCount);

}
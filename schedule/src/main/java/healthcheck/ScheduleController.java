package healthcheck;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

 @RestController
 public class ScheduleController {

        @Autowired
        ScheduleRepository scheduleRepository;

        @RequestMapping(value = "/chkAndModifyCount",
                method = RequestMethod.GET,
                produces = "application/json;charset=UTF-8")

        public boolean modifyCount(HttpServletRequest request, HttpServletResponse response)
                throws Exception {
                System.out.println("##### /schedule/modifyCount  called #####");

                boolean status = false;
                Long scheduleId = Long.valueOf(request.getParameter("scheduleId"));
                int count = Integer.parseInt(request.getParameter("reservationCount"));

                Schedule schedule = scheduleRepository.findByScheduleId(scheduleId);

                if(schedule.getAvailableCount() >= count) {
                        status = true;
                        schedule.setAvailableCount(schedule.getAvailableCount() - count);
                        scheduleRepository.save(schedule);
                }
                return status;

         }

 }

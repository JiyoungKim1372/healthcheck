package healthcheck;

import healthcheck.config.kafka.KafkaProcessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

@Service
public class PolicyHandler{
    @Autowired ScheduleRepository scheduleRepository;

    @StreamListener(KafkaProcessor.INPUT)
    public void wheneverCanceled_IncreaseCount(@Payload Canceled canceled){

        if(canceled.validate()) {

        System.out.println("\n\n##### listener IncreaseCount : " + canceled.toJson() + "\n\n");

        // 예약이 취소되면 예약가능 숫자가 증가한다. //
        Schedule schedule = scheduleRepository.findByScheduleId(Long.valueOf(canceled.getScheduleId()));
        schedule.setAvailableCount(schedule.getAvailableCount()+canceled.getReservationCount());
        scheduleRepository.save(schedule); 

        }
    }

    @StreamListener(KafkaProcessor.INPUT)
    public void whatever(@Payload String eventString){}


}

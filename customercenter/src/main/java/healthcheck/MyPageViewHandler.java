package healthcheck;

import healthcheck.config.kafka.KafkaProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Service
public class MyPageViewHandler {


    @Autowired
    private MyPageRepository myPageRepository;

    @StreamListener(KafkaProcessor.INPUT)
    public void whenReserved_then_CREATE_1 (@Payload Reserved reserved) {
        try {

            if (!reserved.validate()) return;

            // view 객체 생성
            MyPage myPage = new MyPage();
            // view 객체에 이벤트의 Value 를 set 함
            myPage.setReservationId(reserved.getReservationId());
            myPage.setSheduleId(reserved.getScheduleId());
            myPage.setReservationCount(reserved.getReservationCount());
            myPage.setReservationStatus(reserved.getReservationStatus());
            // view 레파지 토리에 save
            myPageRepository.save(myPage);
        
        }catch (Exception e){
            e.printStackTrace();
        }
    }


    @StreamListener(KafkaProcessor.INPUT)
    public void whenCanceled_then_UPDATE_1(@Payload Canceled canceled) {
        try {
            if (!canceled.validate()) return;
                // view 객체 조회
            List<MyPage> myPageList = myPageRepository.findByReservationId(canceled.getReservationId());
            for(MyPage myPage : myPageList){
                // view 객체에 이벤트의 eventDirectValue 를 set 함
                myPage.setReservationId(canceled.getReservationId());
                //myPage.setReservationStatus(canceled.getReservationStatus());
                myPage.setReservationStatus("Reservation Canceled!");
                
                // view 레파지 토리에 save
                myPageRepository.save(myPage);
            }
            
        }catch (Exception e){
            e.printStackTrace();
        }
    }
    @StreamListener(KafkaProcessor.INPUT)
    public void whenDeliveryStarted_then_UPDATE_2(@Payload DeliveryStarted deliveryStarted) {
        try {
            if (!deliveryStarted.validate()) return;
                // view 객체 조회
            List<MyPage> myPageList = myPageRepository.findByReservationId(deliveryStarted.getReservationId());
            for(MyPage myPage : myPageList){
                // view 객체에 이벤트의 eventDirectValue 를 set 함
                myPage.setDeliveryStatus(deliveryStarted.getDeliveryStatus());
                myPage.setDeliveryId(deliveryStarted.getId());
                // view 레파지 토리에 save
                myPageRepository.save(myPage);
            }
            
        }catch (Exception e){
            e.printStackTrace();
        }
    }
    @StreamListener(KafkaProcessor.INPUT)
    public void whenDeliveryCanceled_then_UPDATE_3(@Payload DeliveryCanceled deliveryCanceled) {
        try {
            if (!deliveryCanceled.validate()) return;
                // view 객체 조회
            List<MyPage> myPageList = myPageRepository.findByDeliveryId(deliveryCanceled.getId());
            for(MyPage myPage : myPageList){
                // view 객체에 이벤트의 eventDirectValue 를 set 함
                //myPage.setDeliveryStatus(deliveryCanceled.getDeliveryStatus());
                myPage.setDeliveryStatus("Delivery Canceled!");
                // view 레파지 토리에 save
                myPageRepository.save(myPage);
            }
            
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    @StreamListener(KafkaProcessor.INPUT)
    public void whenCanceled_then_DELETE_1(@Payload Canceled canceled) {
        try {
            if (!canceled.validate()) return;
            // view 레파지 토리에 삭제 쿼리
            myPageRepository.deleteByReservationId(canceled.getReservationId());
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}
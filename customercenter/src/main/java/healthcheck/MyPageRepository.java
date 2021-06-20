package healthcheck;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MyPageRepository extends CrudRepository<MyPage, Long> {

    List<MyPage> findByReservationId(Long reservationId);
    List<MyPage> findByDeliveryId(Long deliveryId);

        void deleteByReservationId(Long reservationId);
}
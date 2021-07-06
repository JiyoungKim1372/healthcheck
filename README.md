# healthcheck (건강검진 예약) 
healthchek reservation 



## 0. 서비스 시나리오


### 기능적 요구사항

```
1. 건강검진 관리자가 예약가능한 스케줄(schedule)을 등록한다.
2. 고객이 고객이 건강검진을 예약한다.
3. 건강검진을 예약하면 건강검진 스케줄의 예약가능한 인원수가 차감된다.
4. 건강검진이 예약되면, 문진표와 주의사항 안내문을 우편 배송한다. 
5. 고객이 건강검진을 취소할 수 있다.  
6. 건강검진 예약이 취소되면 문진표와 안내문 배송이 취소된다.  
7  건강검진 예약이 취소되면 건강검진 스케줄의 예약가능한 인원수가 증가한다.
8. 고객은 모든 건강검진 예약 및 문진표/안내문 배송 진행사항을 볼 수 있다. 
```

### 비기능적 요구사항
```
1. 트랜잭션
    1.1 예약가능한 인원을 초과하면 예약이 되지 않는다. --> Sync 호출
    1.2 예약이 취소되면 문진표/안내문 배송이 취소되고 예약가능한 인원수가 증가한다. --> SAGA
2. 장애격리
    2.1 문진표/안내문 배송이 되지 않아도 예약은 365일 24시간 받을 수 있어야 한다. --> Async (event-driven), Eventual Consistency
    2.2 건강검진 예약이 많아 건강검진 일정(schedule)시스템의 부하가 과중하면 예약을 잠시동안 받지 않고 잠시 후에 예약을 하도록 유도한다. --> Circuit breaker, fallback
3. 성능
    3.1 고객이 상시 예약진행내역을 조회 할 수 있도록 성능을 고려하여 별도의 view(MyPage)로 구성한다. --> CQRS
```



## 1. 분석/설계

### Event Storming 결과
![image](https://user-images.githubusercontent.com/82069747/122672248-869c6d00-d205-11eb-8b2f-e6db51e4e736.png)


### 헥사고날 아키텍처 다이어그램 도출

![image](https://user-images.githubusercontent.com/82069747/124525415-3561bf80-de3a-11eb-8d76-70af4d29552f.png)

## 2. 구현
분석/설계 단계에서 도출된 헥사고날 아키텍처에 따라, 구현한 각 서비스를 로컬에서 실행하는 방법은 아래와 같다 (각각의 포트넘버는 8081 ~ 8084 이다)
```
  cd schedule
  mvn spring-boot:run  
  
  cd reservation
  mvn spring-boot:run  

  cd delivery
  mvn spring-boot:run

  cd customercenter
  mvn spring-boot:run  

```

### 2.1. DDD 의 적용

msaez.io를 통해 구현한 Aggregate 단위로 Entity를 선언 후, 구현을 진행하였다.

Entity Pattern과 Repository Pattern을 적용하기 위해 Spring Data REST의 RestRepository를 적용하였다.

** schedule 서비스의 schedule.java**

```java
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


```

** Schedule 서비스의 PolicyHandler.java**

```java
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
 
```

DDD 적용 후 REST API의 테스트를 통하여 정상적으로 동작하는 것을 확인할 수 있었다.

### 2.2. Polyglot Persistence 구조
schedule, reservation, customercenter 서비스는 H2 DB를 사용하게 구성했고, 
delivery 서비스는 HSQLDB 를 사용하도록 구성되어 있어서, DB 부분을 Polyglot 구조로 동작하도록 처리하였다.


**reservation 서비스의 pom.xml 내 DB 설정부분**

![image](https://user-images.githubusercontent.com/82069747/124422012-8ff90e00-dd9d-11eb-9c95-d02e49c77521.png)


**reservation 서비스 spring boot 기동 로그**

![image](https://user-images.githubusercontent.com/82069747/124418949-8f5d7900-dd97-11eb-9047-e080bfd1b40d.png)


**delivery 서비스의 pom.xml 내 DB 설정부분**

![image](https://user-images.githubusercontent.com/82069747/123743942-fd61f600-d8e8-11eb-8765-d1422e6ab0df.png)



**delivery 서비스 spring boot 기동 로그**

![image](https://user-images.githubusercontent.com/82069747/124418732-1b22d580-dd97-11eb-9714-5f201dd21ea5.png)


### 2.3. Gateway 적용

**gateway > application.yml 설정**

![image](https://user-images.githubusercontent.com/82069747/124408584-7c3fae80-dd81-11eb-81cb-af5208fd67fc.png)

**gateway 테스트**

```
http GET http://gateway:8080/reservations/3
```
![image](https://user-images.githubusercontent.com/82069747/124416471-07c13b80-dd92-11eb-81d7-14f27f22e28b.png)


### 2.4. Saga, CQRS, Correlation, Req/Resp



<구현기능별 요약>
```
[Saga]
- 마이크로 서비스간 통신은 Kafka를 통해 Pub/Sub 통신하도록 구성함. 이를 통해 Event Driven 구조로 각 단계가 진행되도록 함
- 아래 테스트 시나리오의 전 구간 참조

[CQRS]
- customercenter (myPage) 서비스의 경우의 경우, 각 마이크로 서비스로부터 Pub/Sub 구조를 통해 받은 데이터를 이용하여 자체 DB로 View를 구성함.
- 이를 통해 여러 마이크로 서비스에 존재하는 DB간의 Join 등이 필요 없으며, 성능에 대한 이슈없이 빠른 조회가 가능함.

[Correlation]
- 예약을 하게되면 reservation > delivery > MyPage로 예약정보가 Assigned 되고, 
  예약이 취소가 되면 reservationStatus가 Reservation Cancelled, 
  delivery가 취소되면 deliveryStatus가 delivery Cancelled 로 Update 되는 것을 볼 수 있다.
- 또한 Correlation Key를 구현하기 위해 각 마이크로서비스에서 관리하는 데이터의 Id값을 전달받아서 서비스간의 연관 처리를 수행하였다.
- 이 결과로 서로 다른 마이크로 서비스 간에 트랜잭션이 묶여 있음을 알 수 있다.

[Req/Resp]
- schedule 마이크로서비스의 예약가능인원을 초과한 예약 시도시에는, reservation 마이크로서비스에서 예약이 되지 않도록 처리함
- FeignClient 를 이용한 Req/Resp 연동

```

![image](https://user-images.githubusercontent.com/84003381/122410244-b574d200-cfbe-11eb-8b49-3dad0dafe79b.png)


**<구현기능 점검을 위한 테스트 시나리오>**

![image](https://user-images.githubusercontent.com/82069747/123797882-9dd40c80-d921-11eb-865d-c054056fd78e.png)



**1. 관리자가 건강검진 일정정보 등록**

- http POST http://localhost:8081/schedules scheduleId=1 availableCount=100

![image](https://user-images.githubusercontent.com/82069747/124420335-4955e480-dd9a-11eb-8e13-79f26b43ec4e.png)


**2. 고객이 건강검진 예약**

2.1 정상예약 #1

- 10명 예약 (http POST http://reservation:8080/reservations reservationId=1  scheduleId=1 reservationCount=10)

![image](https://user-images.githubusercontent.com/82069747/124420566-a5b90400-dd9a-11eb-9518-12fe123635d9.png)

- 배송상태확인 (http GET http://localhost:8083/deliveries/1)

![image](https://user-images.githubusercontent.com/82069747/124420606-b9646a80-dd9a-11eb-8c4d-94d5ffc30fea.png)

- Mypage확인 (http GET http://localhost:8084/myPages/1)

![image](https://user-images.githubusercontent.com/82069747/124420650-d436df00-dd9a-11eb-8ddb-d83f619ae635.png)

- 예약가능수량 차감 확인 (http GET http://localhost:8081/schedules/1)
![image](https://user-images.githubusercontent.com/82069747/124420765-00526000-dd9b-11eb-9184-c4c6f0fb008d.png)


2.2 정상예약 #2

- 5명 예약 (http POST http://localhost:8082/reservations reservationId=2  scheduleId=1 reservationCount=5)

![image](https://user-images.githubusercontent.com/82069747/124420936-71921300-dd9b-11eb-9379-63ca0abbe0eb.png)

![image](https://user-images.githubusercontent.com/82069747/124421531-9a66d800-dd9c-11eb-8a33-3211cfd75d8c.png)

2.3 예악 가능한 인원을 초과하여 예약시도 시에는 예약이 되지 않도록 처리함

- FeignClient를 이용한 Req/Resp 연동
- 86명 예약 (http POST http://localhost:8082/reservations reservationId=3  scheduleId=1 reservationCount=86)

![image](https://user-images.githubusercontent.com/82069747/124421090-b453eb00-dd9b-11eb-93bc-1ec0b59b82ed.png)



**3. 예약 취소**

- 예약 취소 (http DELETE http://localhost:8082/reservations/1)
- 예약가능 인원 확인 (http GET http://localhost:8081/schedules/1)
![image](https://user-images.githubusercontent.com/82069747/124421254-039a1b80-dd9c-11eb-8b75-be92af416eef.png)

- Mypage확인 (http GET http://localhost:8084/myPages/1)
![image](https://user-images.githubusercontent.com/82069747/124421339-380dd780-dd9c-11eb-948a-93fd37783342.png)

   

## 3. 운영

### 3.1. Deploy


**네임스페이스 만들기**
```
kubectl create ns healthcheck
kubectl get ns
```
![image](https://user-images.githubusercontent.com/82069747/124406516-8a3f0080-dd7c-11eb-8d03-1fea063e8c62.png)


**소스가져오기**
```
git clone https://github.com/JiyoungKim1372/healthcheck.git
```

**빌드하기**
```
cd schedule   
mvn package -Dmaven.test.skip=true
```
![image](https://user-images.githubusercontent.com/82069747/124406325-0f75e580-dd7c-11eb-984c-1418a913140b.png)


**도커라이징: Azure 레지스트리에 도커 이미지 빌드 후 푸시하기**
```
az acr build --registry user03acr --image user03acr.azurecr.io/schedule:v1 .
```
![image](https://user-images.githubusercontent.com/82069747/124406279-dc335680-dd7b-11eb-97bb-6c4f35d88c6b.png)


![image](https://user-images.githubusercontent.com/82069747/124406126-68914980-dd7b-11eb-8d76-cb35ade9dd7b.png)



**컨테이너라이징: 디플로이 생성 확인**
```
kubectl create deploy schedule --image=user03acr.azurecr.io/schedule:v1 -n healthcheck
kubectl get all -n healthcheck
```

![image](https://user-images.githubusercontent.com/82069747/124406769-2ec14280-dd7d-11eb-95d5-082309833203.png)


**컨테이너라이징: 서비스 생성 확인**

```
kubectl create deploy schedule --image=user03acr.azurecr.io/schedule:v1 -n healthcheck
kubectl get all -n healthcheck
```

![image](https://user-images.githubusercontent.com/82069747/124407554-2bc75180-dd7f-11eb-9697-433950d71b1c.png)


**reservation, delivery, customercenter, gateway에도 동일한 작업 반복**
*최종 결과

![image](https://user-images.githubusercontent.com/82069747/124526011-5fb47c80-de3c-11eb-9c83-2eccccc4640e.png)



- deployment.yml을 사용하여 배포 (reservation의 deployment.yml 추가)

![image](https://user-images.githubusercontent.com/82069747/124424752-9047d800-dda2-11eb-8f8e-03d0fcef30f0.png)

- deployment.yml로 서비스 배포

```
kubectl apply -f kubernetes/deployment.yml
```


### 3.2. 동기식 호출 / 서킷 브레이킹 / 장애격리
- 시나리오는 예약(reservation)--> 건강검진스케줄(schedule)의 연결을 RESTful Request/Response 로 연동하여 구현이 되어있고, 예약이 과도할 경우 CB 를 통하여 장애격리.
- Hystrix 설정: 요청처리 쓰레드에서 처리시간이 250 밀리가 넘어서기 시작하여 어느정도 유지되면 CB 회로가 닫히도록 (요청을 빠르게 실패처리, 차단) 설정

```
# circuit breaker 설정 start
feign:
  hystrix:
    enabled: true

hystrix:
  command:
    # 전역설정
    default:
      execution.isolation.thread.timeoutInMilliseconds: 250
# circuit breaker 설정 end
```

![image](https://user-images.githubusercontent.com/82069747/124426018-5d9edf00-dda4-11eb-89b6-1c2f1640dfc2.png)

- 부하테스터 siege 툴을 통한 서킷 브레이커 동작 확인: 동시사용자 100명, 60초 동안 실시
시즈 접속


```
kubectl exec -it pod/siege-d484db9c-2t6ls -c siege -n healthcheck -- /bin/bash
```
- 부하테스트 동시사용자 100명 60초 동안 예약 수행


```
siege -c100 -t60S -r10 -v --content-type "application/json" 'http://reservation:8080/reservations POST {"reservationId": “1”, "scheduleId”:”7”, “reservationCount”:1}’
```
- 부하 발생하여 CB가 발동하여 요청 실패처리하였고, 밀린 부하가 schedule에서 처리되면서 다시 reservation(예약)을 받기 시작함

![image](https://user-images.githubusercontent.com/82069747/124429070-609bce80-dda8-11eb-965f-a9f9a871b5fc.png)


- 레포트결과
![image](https://user-images.githubusercontent.com/82069747/124429292-a8baf100-dda8-11eb-8b17-9eb3644f38e7.png)

- 서킷브레이킹 동작확인완료


### 3.3. Autoscale(HPA)
- 오토스케일 테스트를 위해 리소스 제한설정 함
- reservation/kubernetes/deployment.yml 설정

```
resources:
	limits:
		cpu : 500m
	requests: 
		cpu : 200m
```

- 예약 시스템에 대한 replica 를 동적으로 늘려주도록 HPA 를 설정한다. 설정은 CPU 사용량이 15프로를 넘어서면 replica 를 10개까지 늘려준다

```
kubectl autoscale deploy reservation --min=1 --max=10 --cpu-percent=15 -n healthcheck
```
![image](https://user-images.githubusercontent.com/82069747/124445456-87fb9700-ddba-11eb-90b7-34420629f460.png)

![image](https://user-images.githubusercontent.com/82069747/124445794-d3ae4080-ddba-11eb-98e6-7eec8c9d0ed7.png)


- 부하테스트 동시사용자 1000명 60초 동안 예약조회 수행

```
siege -c1000 -t60S -r10 -v --content-type "application/json" 'http://reservation:8080/reservations'
```


- 오토스케일 모니터링 수행


```
kubectl get deploy reservation -w -n healthcheck
```

![image](https://user-images.githubusercontent.com/82069747/124446071-09ebc000-ddbb-11eb-8085-12d4862754ec.png)


- 부하가 증가됨에 따라서 replica 를 10개 까지 늘어났다가 다시 줄어 드는 것을 확인 가능함 

![image](https://user-images.githubusercontent.com/82069747/124446406-5931f080-ddbb-11eb-83c9-9fa9fd9b0bc8.png)


### 3.4. Self-healing (Liveness Probe)

- schedule 서비스 정상 확인

![image](https://user-images.githubusercontent.com/82069747/124464447-2bef3d80-ddcf-11eb-9777-a9c92bc595b7.png)

- schedule deployment.yml 에 Liveness Probe 옵션 변경하여 계속 실패하여 재기동 되도록 yml 수정
```
          livenessProbe:
            tcpSocket:
              port: 8081
            initialDelaySeconds: 5
            periodSeconds: 5	
```
![image](https://user-images.githubusercontent.com/82069747/124464518-3f9aa400-ddcf-11eb-8d45-97fde31d88c6.png)

-schedule pod에 liveness가 적용된 부분 확인

![image](https://user-images.githubusercontent.com/82069747/124464566-4f19ed00-ddcf-11eb-8ddf-6b7d46a0a0a0.png)

-scheduel 서비스의 liveness가 발동되어 5번 retry 시도 한 부분 확인

![image](https://user-images.githubusercontent.com/82069747/124464604-5ccf7280-ddcf-11eb-96f3-410807e99254.png)


### 3.5. Zero-downtime deploy(Readiness Probe)
- Zero-downtime deploy를 위해 Autoscale 및 CB 설정 제거 
- readiness 옵션이 없는 reservation 배포
- siege로 부하 준비 후 실행 
- siege로 부하 실행 중 reservation 새로운 버전의 이미지로 교체
- readiness 옵션이 없는 경우 배포 중 서비스 요청처리 실패

- deployment.yml에 readiness 옵션제거

![image](https://user-images.githubusercontent.com/82069747/124470971-44635600-ddd7-11eb-84e4-7ede1ed30b5f.png)

- siege 부하 실행 중 reservation 서비스 이미지 교체 (접속 에러 뱔생) 

![image](https://user-images.githubusercontent.com/82069747/124471143-7a083f00-ddd7-11eb-891a-8174d5bda947.png)

![image](https://user-images.githubusercontent.com/82069747/124471313-afad2800-ddd7-11eb-9a0c-a6fc29dcf1ac.png)

- deployment.yml에 readiness 옵션을 추가

![image](https://user-images.githubusercontent.com/82069747/124471367-bf2c7100-ddd7-11eb-9d63-7baeed2973ed.png)


-새로운 버전의 이미지로 교체

```
az acr build --registry user03acr --image user03acr.azurecr.io/reservation:v8 .
kubectl set image deploy reservation reservation=user03acr.azurecr.io/reservation:v8 -n healthcheck
```

- 이미지 변경 전 reservation pod (1개)

![image](https://user-images.githubusercontent.com/82069747/124471534-ef740f80-ddd7-11eb-8070-57aa741269e6.png)


-기존 버전과 새 버전의 reservation pod 공존 중

![image](https://user-images.githubusercontent.com/82069747/124471792-4548b780-ddd8-11eb-9398-8dd77a96c855.png)

-Availability: 100.00 % 확인

![image](https://user-images.githubusercontent.com/82069747/124471848-572a5a80-ddd8-11eb-9e42-726b73029024.png)


### 3.6. Config Map
application.yml 설정

-default부분

![image](https://user-images.githubusercontent.com/82069747/124475661-ec2f5280-dddc-11eb-8809-5b72bcef3c22.png)

-docker 부분

![image](https://user-images.githubusercontent.com/82069747/124475680-f3566080-dddc-11eb-89a7-5480224f9f39.png)

Deployment.yml 설정

![image](https://user-images.githubusercontent.com/82069747/124475714-fe10f580-dddc-11eb-87f2-a78991d1f555.png)

config map 생성 후 조회
```
kubectl create configmap apiurl --from-literal=url=http://schedule:8080 -n healthcheck
```
![image](https://user-images.githubusercontent.com/82069747/124476058-6233b980-dddd-11eb-834e-c7be6f1ed712.png)


- 설정한 url로 예약 호출
```
http POST http://reservation:8080/reservations reservationId=2  scheduleId=1 reservationCount=1
```
![image](https://user-images.githubusercontent.com/82069747/124476559-f0a83b00-dddd-11eb-90e7-bda34c6400a9.png)


-configmap 삭제 후 app 서비스 재시작

```
kubectl delete configmap apiurl -n healthcheck
kubectl get pod/reservation-59b9cd8bcb-x2npx -n healthcheck -o yaml | kubectl replace --force -f-
```

-configmap 삭제된 상태에서 예약 호출
```
http POST http://reservation:8080/reservations reservationId=2  scheduleId=20 reservationCount=1
```

![image](https://user-images.githubusercontent.com/82069747/124476681-17ff0800-ddde-11eb-84b8-678daac3275b.png)




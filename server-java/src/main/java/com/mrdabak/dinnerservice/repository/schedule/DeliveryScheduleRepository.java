package com.mrdabak.dinnerservice.repository.schedule;

import com.mrdabak.dinnerservice.model.DeliverySchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface DeliveryScheduleRepository extends JpaRepository<DeliverySchedule, Long> {

    boolean existsByEmployeeIdAndReturnTimeAfterAndDepartureTimeBefore(Long employeeId,
                                                                       LocalDateTime start,
                                                                       LocalDateTime end);

    List<DeliverySchedule> findByEmployeeIdAndDepartureTimeBetween(Long employeeId,
                                                                   LocalDateTime start,
                                                                   LocalDateTime end);

    List<DeliverySchedule> findByDepartureTimeBetween(LocalDateTime start, LocalDateTime end);

    Optional<DeliverySchedule> findByOrderId(Long orderId);

    void deleteByOrderId(Long orderId);
}


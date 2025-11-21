package com.mrdabak.dinnerservice.repository.order;

import com.mrdabak.dinnerservice.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByUserIdOrderByCreatedAtDesc(Long userId);
    Optional<Order> findByIdAndUserId(Long id, Long userId);
    List<Order> findByStatus(String status);
    
    @Query("SELECT o FROM Order o WHERE o.deliveryTime >= :start AND o.deliveryTime < :end")
    List<Order> findByDeliveryTimeBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}


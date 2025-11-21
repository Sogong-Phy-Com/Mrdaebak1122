package com.mrdabak.dinnerservice.repository;

import com.mrdabak.dinnerservice.model.InventoryReservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface InventoryReservationRepository extends JpaRepository<InventoryReservation, Long> {

    @Query("SELECT COALESCE(SUM(r.quantity), 0) FROM InventoryReservation r " +
            "WHERE r.menuItemId = :menuItemId " +
            "AND r.windowStart = :windowStart")
    Integer sumQuantityByMenuItemIdAndWindowStart(@Param("menuItemId") Long menuItemId,
                                                  @Param("windowStart") LocalDateTime windowStart);

    List<InventoryReservation> findByOrderId(Long orderId);

    void deleteByOrderId(Long orderId);

    @Query("SELECT r FROM InventoryReservation r WHERE r.windowStart >= :start AND r.windowStart < :end")
    List<InventoryReservation> findByWindowStartBetween(@Param("start") LocalDateTime start, 
                                                         @Param("end") LocalDateTime end);
    
    @Query("SELECT r FROM InventoryReservation r WHERE r.consumed = false AND r.expiresAt < :now")
    List<InventoryReservation> findExpiredUnconsumed(@Param("now") LocalDateTime now);
    
    @Query("SELECT r FROM InventoryReservation r WHERE r.consumed = false AND r.orderId = :orderId")
    List<InventoryReservation> findUnconsumedByOrderId(@Param("orderId") Long orderId);
}


package com.jun.notificationservice.repository;

import com.jun.notificationservice.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, String> {
    
    List<Notification> findByUserIdOrderByCreatedAtDesc(Long userId);
    
    Notification findByIdAndUserId(String id, Long userId);
    
    List<Notification> findByUserIdAndIsReadFalse(Long userId);
    
    long countByUserIdAndIsReadFalse(Long userId);
}

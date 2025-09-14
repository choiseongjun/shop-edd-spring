package com.jun.notificationservice.service;

import com.jun.notificationservice.dto.NotificationDto;
import com.jun.notificationservice.dto.NotificationRequest;
import com.jun.notificationservice.entity.Notification;
import com.jun.notificationservice.event.NotificationEvent;
import com.jun.notificationservice.repository.NotificationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class NotificationService {
    
    @Autowired
    private SimpMessagingTemplate messagingTemplate;
    
    @Autowired
    private NotificationRepository notificationRepository;
    
    @KafkaListener(topics = "flash-sale-events", groupId = "notification-service-group")
    public void handleFlashSaleEvent(String message) {
        // 플래시 세일 이벤트 처리
        messagingTemplate.convertAndSend("/topic/flash-sale", message);
    }
    
    @KafkaListener(topics = "order-events", groupId = "notification-service-group") 
    public void handleOrderEvent(String message) {
        // 주문 이벤트 처리
        messagingTemplate.convertAndSend("/topic/order-status", message);
    }
    
    public void sendRealTimeNotification(String topic, Object message) {
        messagingTemplate.convertAndSend(topic, message);
    }
    
    public NotificationDto sendNotification(NotificationRequest request) {
        Notification notification = new Notification();
        notification.setId(UUID.randomUUID().toString());
        notification.setUserId(request.getUserId());
        notification.setTitle(request.getTitle());
        notification.setMessage(request.getMessage());
        notification.setType(request.getType());
        notification.setChannel(request.getChannel());
        notification.setOrderId(request.getOrderId());
        notification.setPaymentId(request.getPaymentId());
        notification.setIsRead(false);
        notification.setCreatedAt(LocalDateTime.now());
        
        Notification saved = notificationRepository.save(notification);
        
        // 실시간 알림 전송
        sendRealTimeNotification("/topic/user-notification", new NotificationDto(saved));
        
        return new NotificationDto(saved);
    }
    
    public List<NotificationDto> getNotificationsByUserId(Long userId) {
        List<Notification> notifications = notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
        return notifications.stream().map(NotificationDto::new).collect(Collectors.toList());
    }
    
    public NotificationDto getNotification(String notificationId, Long userId) {
        Notification notification = notificationRepository.findByIdAndUserId(notificationId, userId);
        return notification != null ? new NotificationDto(notification) : null;
    }
    
    public boolean markAsRead(String notificationId, Long userId) {
        Notification notification = notificationRepository.findByIdAndUserId(notificationId, userId);
        if (notification != null) {
            notification.setIsRead(true);
            notificationRepository.save(notification);
            return true;
        }
        return false;
    }
    
    public int markAllAsRead(Long userId) {
        List<Notification> notifications = notificationRepository.findByUserIdAndIsReadFalse(userId);
        notifications.forEach(n -> n.setIsRead(true));
        notificationRepository.saveAll(notifications);
        return notifications.size();
    }
    
    public long getUnreadCount(Long userId) {
        return notificationRepository.countByUserIdAndIsReadFalse(userId);
    }
    
    public void handleOrderPlaced(Map<String, Object> orderData) {
        Long userId = Long.valueOf(orderData.get("userId").toString());
        String orderId = orderData.get("orderId").toString();
        
        NotificationRequest request = new NotificationRequest();
        request.setUserId(userId);
        request.setTitle("주문이 접수되었습니다");
        request.setMessage("주문번호 " + orderId + "가 성공적으로 접수되었습니다.");
        request.setType(Notification.NotificationType.ORDER_PLACED);
        request.setChannel(Notification.NotificationChannel.IN_APP);
        request.setOrderId(orderId);
        
        sendNotification(request);
    }
    
    public void handlePaymentCompleted(Map<String, Object> paymentData) {
        Long userId = Long.valueOf(paymentData.get("userId").toString());
        String orderId = paymentData.get("orderId").toString();
        
        NotificationRequest request = new NotificationRequest();
        request.setUserId(userId);
        request.setTitle("결제가 완료되었습니다");
        request.setMessage("주문번호 " + orderId + "의 결제가 완료되었습니다.");
        request.setType(Notification.NotificationType.PAYMENT_COMPLETED);
        request.setChannel(Notification.NotificationChannel.IN_APP);
        request.setOrderId(orderId);
        
        sendNotification(request);
    }

    public void createNotification(NotificationEvent event) {
        Notification notification = new Notification();
        notification.setId(event.getNotificationId());
        notification.setUserId(event.getUserId());
        notification.setTitle(event.getTitle());
        notification.setMessage(event.getMessage());
        notification.setType(Notification.NotificationType.valueOf(event.getType()));
        notification.setChannel(Notification.NotificationChannel.IN_APP);

        if (event.getRelatedId() != null) {
            if (event.getType().contains("ORDER")) {
                notification.setOrderId(event.getRelatedId());
            } else if (event.getType().contains("PAYMENT")) {
                notification.setPaymentId(event.getRelatedId());
            }
        }

        notification.setIsRead(false);
        notification.setCreatedAt(LocalDateTime.now());

        notificationRepository.save(notification);

        // 실시간 WebSocket 알림 전송
        sendRealTimeNotification("/topic/user-notification/" + event.getUserId(), new NotificationDto(notification));
    }
}
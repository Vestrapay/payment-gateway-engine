package com.example.gateway.commons.notificatioin.repository;

import com.example.gateway.commons.notificatioin.Notification;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NotificationRepository extends R2dbcRepository<Notification,Long> {
}

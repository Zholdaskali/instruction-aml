package kz.example.instructions.repository;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;

import kz.example.instructions.model.Notification;
public interface NotificationRepository extends JpaRepository<Notification, UUID> {
    Slice<Notification> findAllByRecipient_UsernameOrderByCreatedAtDesc(
        String username,
        Pageable pageable
);
    long countByRecipient_UsernameAndReadAtIsNull(String username);
    java.util.Optional<Notification> findByIdAndRecipient_Username(UUID id, String username);
}

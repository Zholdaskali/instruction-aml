package kz.example.instructions.controller;

import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import kz.example.instructions.model.dto.ApiDtos.NotificationResponse;
import kz.example.instructions.model.dto.ApiDtos.ReadNotificationResponse;
import kz.example.instructions.model.dto.ApiDtos.UnreadCountResponse;
import kz.example.instructions.service.InstructionService;

@RestController
@RequestMapping("api/v1/notifications")
public class NotificationController {
    private final InstructionService service;
    public NotificationController(InstructionService service) { this.service = service; }

    @GetMapping
    public Slice<NotificationResponse> getNotifications(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_REQUEST, "Invalid page or size");
        }
        return service.getNotifications(authentication.getName(),
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
    }

    @GetMapping("/unread-count")
    public UnreadCountResponse unreadCount(Authentication authentication) {
        return service.getUnreadCount(authentication.getName());
    }

    @PatchMapping("/{id}/read")
    public ReadNotificationResponse markRead(@PathVariable UUID id, Authentication authentication) {
        return service.markRead(id, authentication.getName());
    }
}

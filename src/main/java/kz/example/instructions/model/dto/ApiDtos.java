package kz.example.instructions.model.dto;

import java.time.Instant;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class ApiDtos {
    private ApiDtos() {}
//req
    public record CreateInstructionRequest(
            @NotNull UUID caseId,
            @NotNull UUID recipientId,
            @NotBlank @Size(max = 2000) String text
    ) {}

    //resp
    public record CaseResponse(UUID id, String caseNumber, String region) {}
    public record RecipientResponse(UUID id, String username, String region) {}

    public record CreateInstructionResponse(UUID instructionId) {}

    public record NotificationResponse(
            UUID id,
            String caseNumber,
            String text,
            String sender,
            Instant sentAt,
            Instant readAt
    ) {}

    public record UnreadCountResponse(long count) {}
    public record ReadNotificationResponse(UUID id, Instant readAt) {}
}

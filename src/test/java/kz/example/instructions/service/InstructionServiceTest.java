package kz.example.instructions.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import kz.example.instructions.model.AppUser;
import kz.example.instructions.model.CaseFile;
import kz.example.instructions.model.Instruction;
import kz.example.instructions.model.Notification;
import kz.example.instructions.model.Role;
import kz.example.instructions.model.dto.ApiDtos.CreateInstructionRequest;
import kz.example.instructions.model.dto.ApiDtos.CreateInstructionResponse;
import kz.example.instructions.model.dto.ApiDtos.ReadNotificationResponse;
import kz.example.instructions.model.dto.ApiDtos.UnreadCountResponse;
import kz.example.instructions.repository.CaseParticipantRepository;
import kz.example.instructions.repository.CaseRepository;
import kz.example.instructions.repository.InstructionRepository;
import kz.example.instructions.repository.NotificationRepository;
import kz.example.instructions.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class InstructionServiceTest {

    @Mock
    private UserRepository users;

    @Mock
    private CaseRepository cases;

    @Mock
    private CaseParticipantRepository participants;

    @Mock
    private InstructionRepository instructions;

    @Mock
    private NotificationRepository notifications;

    @InjectMocks
    private InstructionService service;

    // 1. Пользователь не может прочитать чужое уведомление
    @Test
    void shouldNotAllowReadingAnotherUsersNotification() {
        UUID notificationId = UUID.randomUUID();

        AppUser anton = new AppUser(
                "anton",
                "password",
                Role.INVESTIGATOR,
                "NORTH"
        );

        when(users.findByUsername("anton"))
                .thenReturn(Optional.of(anton));

        when(notifications.findByIdAndRecipient_Username(notificationId, "anton"))
                .thenReturn(Optional.empty());

        assertThrows(
                ResponseStatusException.class,
                () -> service.markRead(notificationId, "anton")
        );

        verify(notifications, never()).save(any(Notification.class));
    }

    // 2. Указание и уведомление успешно создаются
    @Test
    void shouldSaveInstructionAndNotificationSuccessfully() {
        AppUser supervisor = new AppUser(
                "supervisor",
                "password",
                Role.SUPERVISOR,
                "NORTH"
        );

        AppUser investigator = new AppUser(
                "anton",
                "password",
                Role.INVESTIGATOR,
                "NORTH"
        );

        CaseFile caseFile = new CaseFile("DEMO-001", "NORTH");

        UUID caseId = UUID.randomUUID();
        UUID recipientId = UUID.randomUUID();

        when(users.findByUsername("supervisor"))
                .thenReturn(Optional.of(supervisor));

        when(cases.findByIdAndRegion(caseId, "NORTH"))
                .thenReturn(Optional.of(caseFile));

        when(users.findById(recipientId))
                .thenReturn(Optional.of(investigator));

        when(participants.existsByCaseFileAndUser(caseFile, investigator))
                .thenReturn(true);

        when(instructions.save(any(Instruction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CreateInstructionRequest request = new CreateInstructionRequest(
                caseId,
                recipientId,
                "Проверьте наличие акта № 7"
        );

        CreateInstructionResponse response =
                service.createInstruction(request, "supervisor");

        assertNotNull(response);

        verify(instructions).save(any(Instruction.class));

        verify(notifications).save(argThat(notification ->
                notification.getRecipient().equals(investigator)
        ));
    }

    // 3. Нельзя отправить указание следователю, который не участвует в деле
    @Test
    void shouldRejectInstructionWhenRecipientIsNotCaseParticipant() {
        AppUser supervisor = new AppUser(
                "supervisor",
                "password",
                Role.SUPERVISOR,
                "NORTH"
        );

        AppUser investigator = new AppUser(
                "vera",
                "password",
                Role.INVESTIGATOR,
                "NORTH"
        );

        CaseFile caseFile = new CaseFile("DEMO-001", "NORTH");

        UUID caseId = UUID.randomUUID();
        UUID recipientId = UUID.randomUUID();

        when(users.findByUsername("supervisor"))
                .thenReturn(Optional.of(supervisor));

        when(cases.findByIdAndRegion(caseId, "NORTH"))
                .thenReturn(Optional.of(caseFile));

        when(users.findById(recipientId))
                .thenReturn(Optional.of(investigator));

        when(participants.existsByCaseFileAndUser(caseFile, investigator))
                .thenReturn(false);

        CreateInstructionRequest request = new CreateInstructionRequest(
                caseId,
                recipientId,
                "Проверьте документы"
        );

        assertThrows(
                ResponseStatusException.class,
                () -> service.createInstruction(request, "supervisor")
        );

        verify(instructions, never()).save(any(Instruction.class));
        verify(notifications, never()).save(any(Notification.class));
    }

    // 4. Руководитель не может отправить указание по делу другого региона
    @Test
    void shouldRejectInstructionWhenCaseIsOutsideSupervisorRegion() {
        AppUser supervisor = new AppUser(
                "supervisor",
                "password",
                Role.SUPERVISOR,
                "NORTH"
        );

        UUID caseId = UUID.randomUUID();
        UUID recipientId = UUID.randomUUID();

        when(users.findByUsername("supervisor"))
                .thenReturn(Optional.of(supervisor));

        when(cases.findByIdAndRegion(caseId, "NORTH"))
                .thenReturn(Optional.empty());

        CreateInstructionRequest request = new CreateInstructionRequest(
                caseId,
                recipientId,
                "Проверьте документы"
        );

        assertThrows(
                ResponseStatusException.class,
                () -> service.createInstruction(request, "supervisor")
        );

        verify(instructions, never()).save(any(Instruction.class));
        verify(notifications, never()).save(any(Notification.class));
    }

    // 5. Следователь не может отправлять указания
    @Test
    void shouldRejectInstructionFromInvestigator() {
        AppUser investigator = new AppUser(
                "anton",
                "password",
                Role.INVESTIGATOR,
                "NORTH"
        );

        when(users.findByUsername("anton"))
                .thenReturn(Optional.of(investigator));

        CreateInstructionRequest request = new CreateInstructionRequest(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "Проверьте документы"
        );

        assertThrows(
                ResponseStatusException.class,
                () -> service.createInstruction(request, "anton")
        );

        verify(instructions, never()).save(any(Instruction.class));
        verify(notifications, never()).save(any(Notification.class));
    }

    // 6. При первом прочтении записывается readAt
    @Test
    void shouldSetReadAtWhenNotificationIsRead() {
        AppUser anton = new AppUser(
                "anton",
                "password",
                Role.INVESTIGATOR,
                "NORTH"
        );

        AppUser supervisor = new AppUser(
                "supervisor",
                "password",
                Role.SUPERVISOR,
                "NORTH"
        );

        CaseFile caseFile = new CaseFile("DEMO-001", "NORTH");

        Instruction instruction = new Instruction(
                caseFile,
                supervisor,
                anton,
                "Проверьте документы"
        );

        Notification notification = new Notification(instruction, anton);

        UUID notificationId = UUID.randomUUID();

        when(users.findByUsername("anton"))
                .thenReturn(Optional.of(anton));

        when(notifications.findByIdAndRecipient_Username(notificationId, "anton"))
                .thenReturn(Optional.of(notification));

        ReadNotificationResponse response =
                service.markRead(notificationId, "anton");

        assertNotNull(response.readAt());
        assertEquals(notification.getReadAt(), response.readAt());

        verify(notifications).save(notification);
    }

    // 7. Повторное прочтение не меняет первоначальный readAt
    @Test
    void shouldKeepReadAtUnchangedWhenNotificationIsReadAgain() {
        AppUser anton = new AppUser(
                "anton",
                "password",
                Role.INVESTIGATOR,
                "NORTH"
        );

        AppUser supervisor = new AppUser(
                "supervisor",
                "password",
                Role.SUPERVISOR,
                "NORTH"
        );

        CaseFile caseFile = new CaseFile("DEMO-001", "NORTH");

        Instruction instruction = new Instruction(
                caseFile,
                supervisor,
                anton,
                "Проверьте документы"
        );

        Notification notification = new Notification(instruction, anton);

        // Имитируем, что уведомление уже было прочитано
        notification.markRead();

        var originalReadAt = notification.getReadAt();

        UUID notificationId = UUID.randomUUID();

        when(users.findByUsername("anton"))
                .thenReturn(Optional.of(anton));

        when(notifications.findByIdAndRecipient_Username(notificationId, "anton"))
                .thenReturn(Optional.of(notification));

        ReadNotificationResponse response =
                service.markRead(notificationId, "anton");

        assertEquals(originalReadAt, response.readAt());
        assertEquals(originalReadAt, notification.getReadAt());

        verify(notifications).save(notification);
    }

    // 8. Счётчик возвращает количество непрочитанных уведомлений пользователя
    @Test
    void shouldReturnUnreadCountForCurrentUser() {
        AppUser anton = new AppUser(
                "anton",
                "password",
                Role.INVESTIGATOR,
                "NORTH"
        );

        when(users.findByUsername("anton"))
                .thenReturn(Optional.of(anton));

        when(notifications.countByRecipient_UsernameAndReadAtIsNull("anton"))
                .thenReturn(3L);

        UnreadCountResponse response = service.getUnreadCount("anton");

        assertEquals(3L, response.count());

        verify(notifications)
                .countByRecipient_UsernameAndReadAtIsNull("anton");
    }

    // 9. Руководитель получает только дела своего региона
    @Test
    void shouldReturnCasesOnlyFromSupervisorRegion() {
        AppUser supervisor = new AppUser(
                "supervisor",
                "password",
                Role.SUPERVISOR,
                "NORTH"
        );

        CaseFile caseFile = new CaseFile("DEMO-001", "NORTH");

        when(users.findByUsername("supervisor"))
                .thenReturn(Optional.of(supervisor));

        when(cases.findAllByRegionOrderByCaseNumber("NORTH"))
                .thenReturn(List.of(caseFile));

        var response = service.getCases("supervisor");

        assertEquals(1, response.size());
        assertEquals("DEMO-001", response.get(0).caseNumber());

        verify(cases).findAllByRegionOrderByCaseNumber("NORTH");
    }
}
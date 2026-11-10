package kz.example.instructions.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.web.server.ResponseStatusException;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import kz.example.instructions.model.AppUser;
import kz.example.instructions.model.CaseFile;
import kz.example.instructions.model.CaseParticipant;
import kz.example.instructions.model.Notification;
import kz.example.instructions.model.Role;
import kz.example.instructions.model.dto.ApiDtos.CreateInstructionRequest;
import kz.example.instructions.repository.CaseParticipantRepository;
import kz.example.instructions.repository.CaseRepository;
import kz.example.instructions.repository.InstructionRepository;
import kz.example.instructions.repository.NotificationRepository;
import kz.example.instructions.repository.UserRepository;

@SpringBootTest
@Testcontainers
class InstructionServiceIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> postgres =
        new PostgreSQLContainer<>("postgres:17")
                .withDatabaseName("instructions_test")
                .withUsername("test")
                .withPassword("test");

    @Autowired
    private InstructionService instructionService;

    @Autowired
    private UserRepository users;

    @Autowired
    private CaseRepository cases;

    @Autowired
    private CaseParticipantRepository participants;

    @Autowired
    private InstructionRepository instructions;

    @Autowired
    private NotificationRepository notifications;

    @MockitoSpyBean
    private NotificationRepository notificationsSpy;

    // 1. руководитель не может отправить инструкцию следователю, который не участвует в выбранном деле

    @Test
    void shouldRejectRecipientOutsideCase() {
        TestData data = createTestData();

        AppUser investigator = createUser(Role.INVESTIGATOR, "NORTH");

        CreateInstructionRequest request = new CreateInstructionRequest(
                data.caseFile().getId(),
                investigator.getId(),
                "Проверьте документы"
        );

        long instructionsBefore = instructions.count();
        long notificationsBefore = notifications.count();

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> instructionService.createInstruction(
                        request,
                        data.supervisor().getUsername()
                )
        );

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());

        assertEquals(instructionsBefore, instructions.count());
        assertEquals(notificationsBefore, notifications.count());
    }

    // 2. Руководитель пытается отправить указание по делу другого региона

    @Test
    void shouldRejectCaseFromAnotherRegion() {
        AppUser supervisor = createUser(Role.SUPERVISOR, "NORTH");
        AppUser investigator = createUser(Role.INVESTIGATOR, "SOUTH");

        CaseFile southCase = createCase("SOUTH");

        participants.saveAndFlush(
                new CaseParticipant(southCase, investigator)
        );

        CreateInstructionRequest request = new CreateInstructionRequest(
                southCase.getId(),
                investigator.getId(),
                "Проверьте документы"
        );

        long instructionsBefore = instructions.count();
        long notificationsBefore = notifications.count();

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> instructionService.createInstruction(
                        request,
                        supervisor.getUsername()
                )
        );

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());

        assertEquals(instructionsBefore, instructions.count());
        assertEquals(notificationsBefore, notifications.count());
    }

    // 3. Другой пользователь не может прочитать чужое уведомление

    @Test
    void shouldRejectNotificationAccessByAnotherUser() {
        TestData data = createTestData();

        AppUser anotherInvestigator = createUser(
                Role.INVESTIGATOR,
                "NORTH"
        );

        participants.saveAndFlush(
                new CaseParticipant(data.caseFile(), data.investigator())
        );

        CreateInstructionRequest request = new CreateInstructionRequest(
                data.caseFile().getId(),
                data.investigator().getId(),
                "Проверьте наличие акта № 7"
        );

        instructionService.createInstruction(
                request,
                data.supervisor().getUsername()
        );

        Notification notification = notifications
                .findAllByRecipient_UsernameOrderByCreatedAtDesc(
                        data.investigator().getUsername(),
                        PageRequest.of(0, 10)
                )
                .getContent()
                .get(0);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> instructionService.markRead(
                        notification.getId(),
                        anotherInvestigator.getUsername()
                )
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());

        Notification savedNotification = notifications
                .findById(notification.getId())
                .orElseThrow();

        assertNull(savedNotification.getReadAt());
    }

    // 4. Пользователь без роли SUPERVISOR не может отправлять указания

    @Test
    void shouldRejectNonSupervisorInstruction() {
        TestData data = createTestData();

        CreateInstructionRequest request = new CreateInstructionRequest(
                data.caseFile().getId(),
                data.investigator().getId(),
                "Проверьте документы"
        );

        long instructionsBefore = instructions.count();

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> instructionService.createInstruction(
                        request,
                        data.investigator().getUsername()
                )
        );

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());

        assertEquals(instructionsBefore, instructions.count());
    }

    // 5. Следователь видит только свои уведомления

    @Test
    void shouldReturnOnlyUsersNotifications() {
        TestData first = createTestData();

        TestData second = createTestData();

        participants.save(
                new CaseParticipant(first.caseFile(), first.investigator())
        );

        participants.save(
                new CaseParticipant(second.caseFile(), second.investigator())
        );

        instructionService.createInstruction(
                new CreateInstructionRequest(
                        first.caseFile().getId(),
                        first.investigator().getId(),
                        "Указание для первого следователя"
                ),
                first.supervisor().getUsername()
        );

        instructionService.createInstruction(
                new CreateInstructionRequest(
                        second.caseFile().getId(),
                        second.investigator().getId(),
                        "Указание для второго следователя"
                ),
                second.supervisor().getUsername()
        );

        var firstNotifications = instructionService.getNotifications(
                first.investigator().getUsername(),
                PageRequest.of(0, 10)
        );

        var secondNotifications = instructionService.getNotifications(
                second.investigator().getUsername(),
                PageRequest.of(0, 10)
        );

        assertEquals(1, firstNotifications.getNumberOfElements());
        assertEquals(1, secondNotifications.getNumberOfElements());
    }

    // 6. Руководитель видит только дела своего региона

    @Test
    void shouldReturnOnlyCasesFromSupervisorRegion() {
        AppUser supervisor = createUser(Role.SUPERVISOR, "NORTH");

        CaseFile northCase = createCase("NORTH");
        CaseFile southCase = createCase("SOUTH");

        var result = instructionService.getCases(
                supervisor.getUsername()
        );

        assertTrue(
                result.stream().anyMatch(
                        c -> c.id().equals(northCase.getId())
                )
        );

        assertFalse(
                result.stream().anyMatch(
                        c -> c.id().equals(southCase.getId())
                )
        );
    }

    // фабрика

    private TestData createTestData() {
        AppUser supervisor = createUser(Role.SUPERVISOR, "NORTH");
        AppUser investigator = createUser(Role.INVESTIGATOR, "NORTH");
        CaseFile caseFile = createCase("NORTH");

        return new TestData(supervisor, investigator, caseFile);
    }

    private AppUser createUser(Role role, String region) {
        String username = role.name().toLowerCase()
                + "-"
                + UUID.randomUUID();

        return users.saveAndFlush(
                new AppUser(
                        username,
                        "password",
                        role,
                        region
                )
        );
    }

    private CaseFile createCase(String region) {
        return cases.saveAndFlush(
                new CaseFile(
                        "CASE-" + UUID.randomUUID(),
                        region
                )
        );
    }

    private record TestData(
            AppUser supervisor,
            AppUser investigator,
            CaseFile caseFile
    ) {}
}

package kz.example.instructions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
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
import kz.example.instructions.service.InstructionService;

@SpringBootTest
@Testcontainers
class InstructionIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:17")
                    .withDatabaseName("instructions_test")
                    .withUsername("test")
                    .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

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

    @Test
    void shouldCreateInstructionAndNotificationAndMarkItAsRead() {

        // 1. Создаём supervisor
        AppUser supervisor = users.save(
                new AppUser(
                        "test_supervisor",
                        "password",
                        Role.SUPERVISOR,
                        "NORTH"
                )
        );

        // 2. Создаём investigator
        AppUser investigator = users.save(
                new AppUser(
                        "test_investigator",
                        "password",
                        Role.INVESTIGATOR,
                        "NORTH"
                )
        );

        // 3. Создаём дело
        CaseFile caseFile = cases.save(
                new CaseFile("TEST-001", "NORTH")
        );

        // 4. Добавляем investigator в участники дела
        participants.save(
                new CaseParticipant(caseFile, investigator)
        );

        // 5. Supervisor создаёт поручение
        CreateInstructionRequest request = new CreateInstructionRequest(
                caseFile.getId(),
                investigator.getId(),
                "Проверьте наличие акта № 7"
        );

        var response = instructionService.createInstruction(
                request,
                supervisor.getUsername()
        );

        // 6. Проверяем, что поручение сохранилось
        assertNotNull(response.instructionId());

        assertTrue(
                instructions.findById(response.instructionId()).isPresent()
        );

        // 7. Проверяем, что уведомление сохранилось
        var notificationPage =
                notifications.findAllByRecipient_UsernameOrderByCreatedAtDesc(
                        investigator.getUsername(),
                        PageRequest.of(0, 10)
                );

        assertEquals(1, notificationPage.getContent().size());

        Notification notification = notificationPage.getContent().get(0);

        assertEquals(
                "Проверьте наличие акта № 7",
                instructionService.getNotifications(
                        investigator.getUsername(),
                        PageRequest.of(0, 10)
                ).getContent().get(0).text()
        );

        // 8. Проверяем количество непрочитанных
        assertEquals(
                1,
                instructionService.getUnreadCount(
                        investigator.getUsername()
                ).count()
        );

        // 9. Отмечаем уведомление прочитанным
        var firstRead = instructionService.markRead(
                notification.getId(),
                investigator.getUsername()
        );

        assertNotNull(firstRead.readAt());

        // 10. Непрочитанных больше нет
        assertEquals(
                0,
                instructionService.getUnreadCount(
                        investigator.getUsername()
                ).count()
        );

        // 11. Повторно отмечаем уведомление прочитанным
        var secondRead = instructionService.markRead(
                notification.getId(),
                investigator.getUsername()
        );

        // 12. readAt не должен измениться
        assertEquals(
                firstRead.readAt(),
                secondRead.readAt()
        );
    }
}

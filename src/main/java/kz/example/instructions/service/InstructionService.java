package kz.example.instructions.service;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import kz.example.instructions.model.AppUser;
import kz.example.instructions.model.CaseFile;
import kz.example.instructions.model.Instruction;
import kz.example.instructions.model.Notification;
import kz.example.instructions.model.Role;
import kz.example.instructions.model.dto.ApiDtos.CaseResponse;
import kz.example.instructions.model.dto.ApiDtos.CreateInstructionRequest;
import kz.example.instructions.model.dto.ApiDtos.CreateInstructionResponse;
import kz.example.instructions.model.dto.ApiDtos.NotificationResponse;
import kz.example.instructions.model.dto.ApiDtos.ReadNotificationResponse;
import kz.example.instructions.model.dto.ApiDtos.RecipientResponse;
import kz.example.instructions.model.dto.ApiDtos.UnreadCountResponse;
import kz.example.instructions.repository.CaseParticipantRepository;
import kz.example.instructions.repository.CaseRepository;
import kz.example.instructions.repository.InstructionRepository;
import kz.example.instructions.repository.NotificationRepository;
import kz.example.instructions.repository.UserRepository;

@Service
public class InstructionService {
    
    private final UserRepository usersRepo;
    private final CaseRepository casesRepo;
    private final CaseParticipantRepository participantsRepo;
    private final InstructionRepository instructionsRepo;
    private final NotificationRepository notificationsRepo;

    public InstructionService(UserRepository usersRepo, CaseRepository casesRepo,
                              CaseParticipantRepository participantsRepo,
                              InstructionRepository instructionsRepo,
                              NotificationRepository notificationsRepo) {
        this.usersRepo = usersRepo;
        this.casesRepo = casesRepo;
        this.participantsRepo = participantsRepo;
        this.instructionsRepo = instructionsRepo;
        this.notificationsRepo = notificationsRepo;
    }

    // 1
    @Transactional(readOnly = true)
    public List<CaseResponse> getCases(String username) {
        AppUser supervisor = requireUser(username);
        requireRole(supervisor, Role.SUPERVISOR);
        return casesRepo.findAllByRegionOrderByCaseNumber(supervisor.getRegion()).stream()
                .map(c -> new CaseResponse(c.getId(), c.getCaseNumber(), c.getRegion()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<RecipientResponse> getRecipients(UUID caseId, String username) {
        AppUser supervisor = requireUser(username);
        requireRole(supervisor, Role.SUPERVISOR);
        CaseFile caseFile = casesRepo.findByIdAndRegion(caseId, supervisor.getRegion())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Case not found in your region"));
        return participantsRepo.findEligibleRecipients(caseFile.getId(), supervisor.getRegion()).stream()
                .map(u -> new RecipientResponse(u.getId(), u.getUsername(), u.getRegion()))
                .toList();
    }

    // 2
    @Transactional
    public CreateInstructionResponse createInstruction(CreateInstructionRequest request, String username) {
        AppUser sender = requireUser(username);
        requireRole(sender, Role.SUPERVISOR);

        CaseFile caseFile = casesRepo.findByIdAndRegion(request.caseId(), sender.getRegion())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Case is outside your region"));

        AppUser recipient = usersRepo.findById(request.recipientId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Recipient not found"));

        if (recipient.getRole() != Role.INVESTIGATOR
                || !recipient.getRegion().equals(sender.getRegion())
                || !participantsRepo.existsByCaseFileAndUser(caseFile, recipient)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Recipient must be an investigator participating in this case and region");
        }

        Instruction instruction = instructionsRepo.save(new Instruction(caseFile, sender, recipient, request.text().trim()));
        notificationsRepo.save(new Notification(instruction, recipient));
        return new CreateInstructionResponse(instruction.getId());
    }

    @Transactional(readOnly = true)
    public Slice<NotificationResponse> getNotifications(String username, Pageable pageable) {
        requireRole(requireUser(username), Role.INVESTIGATOR);
        return notificationsRepo.findAllByRecipient_UsernameOrderByCreatedAtDesc(username, pageable)
                .map(n -> new NotificationResponse(
                        n.getId(),
                        n.getInstruction().getCaseFile().getCaseNumber(),
                        n.getInstruction().getText(),
                        n.getInstruction().getSender().getUsername(),
                        n.getInstruction().getSentAt(),
                        n.getReadAt()
                ));
    }

    @Transactional(readOnly = true)
    public UnreadCountResponse getUnreadCount(String username) {
        requireRole(requireUser(username), Role.INVESTIGATOR);
        return new UnreadCountResponse(
                notificationsRepo.countByRecipient_UsernameAndReadAtIsNull(username));
    }

    @Transactional
    public ReadNotificationResponse markRead(UUID notificationId, String username) {
        requireRole(requireUser(username), Role.INVESTIGATOR);
        Notification notification = notificationsRepo.findByIdAndRecipient_Username(notificationId, username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Notification not found"));
        notification.markRead();
        notificationsRepo.save(notification);
        return new ReadNotificationResponse(notification.getId(), notification.getReadAt());
    }



    
    private AppUser requireUser(String username) {
        return usersRepo.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unknown user"));
    }

    private void requireRole(AppUser user, Role role) {
        if (user.getRole() != role) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Insufficient role");
        }
    }
}

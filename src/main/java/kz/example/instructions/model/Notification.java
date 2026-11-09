package kz.example.instructions.model;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "t_notifications")
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "instruction_id", nullable = false, unique = true)
    private Instruction instruction;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "recipient_id")
    private AppUser recipient;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    private Instant readAt;

    protected Notification() {}

    public Notification(Instruction instruction, AppUser recipient) {
        this.instruction = instruction;
        this.recipient = recipient;
        this.createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public Instruction getInstruction() { return instruction; }
    public AppUser getRecipient() { return recipient; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getReadAt() { return readAt; }

    public void markRead() {
        if (this.readAt == null) {
            this.readAt = Instant.now();
        }
    }
}

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
@Table(name = "t_instructions")
public class Instruction {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "case_id")
    private CaseFile caseFile;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id")
    private AppUser sender;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "recipient_id")
    private AppUser recipient;

    @Column(nullable = false, length = 2000)
    private String text;

    @Column(nullable = false, updatable = false)
    private Instant sentAt;

    protected Instruction() {}

    public Instruction(CaseFile caseFile, AppUser sender, AppUser recipient, String text) {
        this.caseFile = caseFile;
        this.sender = sender;
        this.recipient = recipient;
        this.text = text;
        this.sentAt = Instant.now();
    }

    public UUID getId() { return id; }
    public CaseFile getCaseFile() { return caseFile; }
    public AppUser getSender() { return sender; }
    public AppUser getRecipient() { return recipient; }
    public String getText() { return text; }
    public Instant getSentAt() { return sentAt; }
}

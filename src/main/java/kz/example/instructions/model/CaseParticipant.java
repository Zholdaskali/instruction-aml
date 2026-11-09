package kz.example.instructions.model;

import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "t_case_participants",
       uniqueConstraints = @UniqueConstraint(columnNames = {"case_id", "user_id"}))
public class CaseParticipant {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "case_id")
    private CaseFile caseFile;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private AppUser user;

    protected CaseParticipant() {}

    public CaseParticipant(CaseFile caseFile, AppUser user) {
        this.caseFile = caseFile;
        this.user = user;
    }

    public UUID getId() { return id; }
}

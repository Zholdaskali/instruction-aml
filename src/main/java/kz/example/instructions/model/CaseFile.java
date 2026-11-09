package kz.example.instructions.model;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "t_case_files")
public class CaseFile {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String caseNumber;

    @Column(nullable = false)
    private String region;

    protected CaseFile() {}

    public CaseFile(String caseNumber, String region) {
        this.caseNumber = caseNumber;
        this.region = region;
    }

    public UUID getId() { return id; }
    public String getCaseNumber() { return caseNumber; }
    public String getRegion() { return region; }
}

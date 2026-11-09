package kz.example.instructions.repository;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import kz.example.instructions.model.AppUser;
import kz.example.instructions.model.CaseFile;
import kz.example.instructions.model.CaseParticipant;

public interface CaseParticipantRepository extends JpaRepository<CaseParticipant, UUID> {
   
    @Query("select p.user from CaseParticipant p where p.caseFile.id = :caseId and p.user.region = :region and p.user.role = kz.example.instructions.model.Role.INVESTIGATOR order by p.user.username")
    List<AppUser> findEligibleRecipients(@Param("caseId") UUID caseId, @Param("region") String region);

    boolean existsByCaseFileAndUser(CaseFile caseFile, AppUser user);
}

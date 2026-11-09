package kz.example.instructions.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import kz.example.instructions.model.CaseFile;

public interface CaseRepository extends JpaRepository<CaseFile, UUID> {
    List<CaseFile> findAllByRegionOrderByCaseNumber(String region);
    Optional<CaseFile> findByIdAndRegion(UUID id, String region);
}

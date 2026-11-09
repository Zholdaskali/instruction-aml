package kz.example.instructions.repository;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import kz.example.instructions.model.Instruction;

public interface InstructionRepository extends JpaRepository<Instruction, UUID> {}

package kz.example.instructions.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import kz.example.instructions.model.dto.ApiDtos.CaseResponse;
import kz.example.instructions.model.dto.ApiDtos.RecipientResponse;
import kz.example.instructions.service.InstructionService;

@RestController
@RequestMapping("api/v1/cases")
public class CaseController {
    
    private final InstructionService service;

    public CaseController(InstructionService service) { this.service = service; }

    @GetMapping
    public List<CaseResponse> getCases(Authentication authentication) {
        return service.getCases(authentication.getName());
    }

    @GetMapping("/{id}/instruction-recipients")
    public List<RecipientResponse> getRecipients(
        @PathVariable UUID id, 
        Authentication authentication
    ) {
        return service.getRecipients(id, authentication.getName());
    }
}

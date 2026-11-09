package kz.example.instructions.controller;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import kz.example.instructions.model.dto.ApiDtos.CreateInstructionRequest;
import kz.example.instructions.model.dto.ApiDtos.CreateInstructionResponse;
import kz.example.instructions.service.InstructionService;

@RestController
@RequestMapping("api/v1/instructions")
public class InstructionController {

    private final InstructionService service;
    
    public InstructionController(InstructionService service) { this.service = service; }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CreateInstructionResponse create(@Valid @RequestBody CreateInstructionRequest request,
                                            Authentication authentication) {
        return service.createInstruction(request, authentication.getName());
    }
}

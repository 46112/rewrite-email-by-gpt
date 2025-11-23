package com.please.rewritebygpt.controller;

import com.please.rewritebygpt.dto.MailRequest;
import com.please.rewritebygpt.dto.MailResponse;
import com.please.rewritebygpt.service.GptService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/mail")
public class MailController {

    private final GptService gptService;

    public MailController(GptService gptService) {
        this.gptService = gptService;
    }

    @PostMapping("/rewrite")
    public ResponseEntity<MailResponse> rewriteMail(@RequestBody MailRequest request) {
        if (request.getContent() == null || request.getContent().trim().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(MailResponse.builder()
                            .rewrittenContent("")
                            .suggestions(java.util.List.of("메일 내용이 비어있습니다."))
                            .build());
        }

        MailResponse response = gptService.rewriteMail(request);
        return ResponseEntity.ok(response);
    }
    
}

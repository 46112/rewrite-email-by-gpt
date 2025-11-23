package com.please.rewritebygpt.service;

import com.please.rewritebygpt.dto.MailRequest;
import com.please.rewritebygpt.dto.MailResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class GptService {

    private final WebClient webClient;

    @Value("${openai.api-key}")
    private String apiKey;

    @Value("${openai.model}")
    private String model;

    @Value("${openai.api-url}")
    private String apiUrl;

    public GptService(WebClient webClient) {
        this.webClient = webClient;
    }

    public MailResponse rewriteMail(MailRequest request) {
        String systemPrompt = buildSystemPrompt(request);
        String userPrompt = request.getContent();

        Map<String, Object> requestBody = Map.of(
                "model", model,
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userPrompt)
                ),
                "temperature", 0.7
        );

        Map response = webClient.post()
                .uri(apiUrl)
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        return parseResponse(response);
    }

    private String buildSystemPrompt(MailRequest request) {
        String recipientContext = "";
        if (request.getRecipient() != null && !request.getRecipient().isEmpty()) {
            recipientContext = String.format("수신자: %s\n", request.getRecipient());
        }

        String toneContext = "";
        if (request.getTone() != null && !request.getTone().isEmpty()) {
            toneContext = switch (request.getTone().toLowerCase()) {
                case "formal" -> "톤: 격식체, 공손하고 정중한 표현 사용\n";
                case "casual" -> "톤: 캐주얼, 친근하고 편안한 표현 사용\n";
                case "polite" -> "톤: 정중함, 예의 바르고 부드러운 표현 사용\n";
                default -> request.getTone();
            };
        }

        return String.format("""
                당신은 한국어 메일 작성을 도와주는 전문가입니다.
                사용자가 작성한 메일을 다음 기준으로 다듬어주세요:

                1. 맞춤법과 문법 오류 수정
                2. 자연스러운 문장 흐름으로 개선
                3. 적절한 경어 사용
                4. 명확하고 간결한 표현

                %s%s

                응답 형식:
                ---다듬어진 메일---
                (다듬어진 메일 내용)

                ---개선 사항---
                - (개선한 내용 1)
                - (개선한 내용 2)
                ...
                """, recipientContext, toneContext);
    }

    @SuppressWarnings("unchecked")
    private MailResponse parseResponse(Map response) {
        try {
            List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
            Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
            String content = (String) message.get("content");

            String rewrittenContent = "";
            List<String> suggestions = new ArrayList<>();

            if (content.contains("---다듬어진 메일---")) {
                String[] parts = content.split("---다듬어진 메일---");
                if (parts.length > 1) {
                    String afterRewritten = parts[1];
                    if (afterRewritten.contains("---개선 사항---")) {
                        String[] subParts = afterRewritten.split("---개선 사항---");
                        rewrittenContent = subParts[0].trim();
                        if (subParts.length > 1) {
                            String suggestionsText = subParts[1].trim();
                            for (String line : suggestionsText.split("\n")) {
                                line = line.trim();
                                if (line.startsWith("-") || line.startsWith("•")) {
                                    suggestions.add(line.substring(1).trim());
                                } else if (!line.isEmpty()) {
                                    suggestions.add(line);
                                }
                            }
                        }
                    } else {
                        rewrittenContent = afterRewritten.trim();
                    }
                }
            } else {
                rewrittenContent = content;
            }

            return MailResponse.builder()
                    .rewrittenContent(rewrittenContent)
                    .suggestions(suggestions)
                    .build();

        } catch (Exception e) {
            return MailResponse.builder()
                    .rewrittenContent("응답 파싱 중 오류가 발생했습니다.")
                    .suggestions(List.of(e.getMessage()))
                    .build();
        }
    }
}

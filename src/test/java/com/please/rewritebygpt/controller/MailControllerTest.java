package com.please.rewritebygpt.controller;

import com.please.rewritebygpt.dto.MailRequest;
import com.please.rewritebygpt.dto.MailResponse;
import com.please.rewritebygpt.service.GptService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class MailControllerTest {

    @LocalServerPort
    private int port;

    private RestTemplate restTemplate;
    private String baseUrl;

    @MockitoBean
    private GptService gptService;

    @BeforeEach
    void setUp() {
        restTemplate = new RestTemplate();
        baseUrl = "http://localhost:" + port;
    }

    @Test
    @DisplayName("메일 다듬기 API - 정상 요청")
    void rewriteMail_Success() {
        // given
        MailResponse mockResponse = MailResponse.builder()
                .rewrittenContent("교수님, 안녕하세요.\n\n과제 제출이 늦어져 대단히 죄송합니다.")
                .suggestions(List.of("인사말 추가", "문장 구조 개선"))
                .build();

        when(gptService.rewriteMail(any(MailRequest.class))).thenReturn(mockResponse);

        MailRequest request = new MailRequest(
                "교수님 안녕하세요 과제 늦어서 죄송합니다",
                "교수님",
                "formal"
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<MailRequest> entity = new HttpEntity<>(request, headers);

        // when
        ResponseEntity<MailResponse> response = restTemplate.postForEntity(
                baseUrl + "/api/mail/rewrite",
                entity,
                MailResponse.class
        );

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getRewrittenContent()).isNotEmpty();
        assertThat(response.getBody().getSuggestions()).isNotEmpty();
    }

    @Test
    @DisplayName("메일 다듬기 API - 빈 내용 요청시 400 에러")
    void rewriteMail_EmptyContent_BadRequest() {
        // given
        MailRequest request = new MailRequest("", "교수님", "formal");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<MailRequest> entity = new HttpEntity<>(request, headers);

        // when & then
        assertThatThrownBy(() -> restTemplate.exchange(
                baseUrl + "/api/mail/rewrite",
                HttpMethod.POST,
                entity,
                MailResponse.class
        ))
        .isInstanceOf(HttpClientErrorException.BadRequest.class);
    }

    @Test
    @DisplayName("메일 다듬기 API - null 내용 요청시 400 에러")
    void rewriteMail_NullContent_BadRequest() {
        // given
        MailRequest request = new MailRequest(null, "교수님", "formal");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<MailRequest> entity = new HttpEntity<>(request, headers);

        // when & then
        assertThatThrownBy(() -> restTemplate.exchange(
                baseUrl + "/api/mail/rewrite",
                HttpMethod.POST,
                entity,
                MailResponse.class
        ))
        .isInstanceOf(HttpClientErrorException.BadRequest.class);
    }

    @Test
    @DisplayName("메일 다듬기 API - recipient와 tone 없이도 동작")
    void rewriteMail_WithoutOptionalFields() {
        // given
        MailResponse mockResponse = MailResponse.builder()
                .rewrittenContent("테스트 메일 내용입니다.")
                .suggestions(List.of())
                .build();

        when(gptService.rewriteMail(any(MailRequest.class))).thenReturn(mockResponse);

        MailRequest request = new MailRequest("테스트 메일 내용입니다", null, null);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<MailRequest> entity = new HttpEntity<>(request, headers);

        // when
        ResponseEntity<MailResponse> response = restTemplate.postForEntity(
                baseUrl + "/api/mail/rewrite",
                entity,
                MailResponse.class
        );

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getRewrittenContent()).isNotEmpty();
    }
}

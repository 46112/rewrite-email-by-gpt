package com.please.rewritebygpt.service;

import com.please.rewritebygpt.dto.MailRequest;
import com.please.rewritebygpt.dto.MailResponse;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.*;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.assertThat;

class GptServiceTest {

    private static MockWebServer mockWebServer;
    private GptService gptService;

    @BeforeAll
    static void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
    }

    @AfterAll
    static void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @BeforeEach
    void initialize() throws Exception {
        WebClient webClient = WebClient.builder().build();
        gptService = new GptService(webClient);

        // 리플렉션으로 private 필드 설정
        setField(gptService, "apiKey", "test-api-key");
        setField(gptService, "model", "gpt-4o-mini");
        setField(gptService, "apiUrl", mockWebServer.url("/v1/chat/completions").toString());
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    @Test
    @DisplayName("메일 다듬기 - 정상 응답 파싱")
    void rewriteMail_Success() throws InterruptedException {
        // given
        String mockResponse = """
                {
                    "choices": [
                        {
                            "message": {
                                "content": "---다듬어진 메일---\\n교수님, 안녕하세요.\\n\\n과제 제출이 늦어져 대단히 죄송합니다.\\n\\n---개선 사항---\\n- 인사말을 추가했습니다\\n- 문장을 더 공손하게 수정했습니다"
                            }
                        }
                    ]
                }
                """;

        mockWebServer.enqueue(new MockResponse()
                .setBody(mockResponse)
                .addHeader("Content-Type", "application/json"));

        MailRequest request = new MailRequest(
                "교수님 안녕하세요 과제 늦어서 죄송합니다",
                "교수님",
                "formal"
        );

        // when
        MailResponse response = gptService.rewriteMail(request);

        // then
        assertThat(response.getRewrittenContent()).contains("교수님");
        assertThat(response.getSuggestions()).isNotEmpty();

        // 요청 검증
        RecordedRequest recordedRequest = mockWebServer.takeRequest();
        assertThat(recordedRequest.getHeader("Authorization")).isEqualTo("Bearer test-api-key");
        assertThat(recordedRequest.getHeader("Content-Type")).isEqualTo("application/json");
    }

    @Test
    @DisplayName("메일 다듬기 - 개선 사항 없는 응답")
    void rewriteMail_NoSuggestions() {
        // given
        String mockResponse = """
                {
                    "choices": [
                        {
                            "message": {
                                "content": "---다듬어진 메일---\\n이미 잘 작성된 메일입니다."
                            }
                        }
                    ]
                }
                """;

        mockWebServer.enqueue(new MockResponse()
                .setBody(mockResponse)
                .addHeader("Content-Type", "application/json"));

        MailRequest request = new MailRequest("테스트 내용", null, null);

        // when
        MailResponse response = gptService.rewriteMail(request);

        // then
        assertThat(response.getRewrittenContent()).isEqualTo("이미 잘 작성된 메일입니다.");
        assertThat(response.getSuggestions()).isEmpty();
    }

    @Test
    @DisplayName("메일 다듬기 - 구분자 없는 응답 처리")
    void rewriteMail_NoDelimiters() {
        // given
        String mockResponse = """
                {
                    "choices": [
                        {
                            "message": {
                                "content": "다듬어진 메일 내용입니다."
                            }
                        }
                    ]
                }
                """;

        mockWebServer.enqueue(new MockResponse()
                .setBody(mockResponse)
                .addHeader("Content-Type", "application/json"));

        MailRequest request = new MailRequest("테스트", null, null);

        // when
        MailResponse response = gptService.rewriteMail(request);

        // then
        assertThat(response.getRewrittenContent()).isEqualTo("다듬어진 메일 내용입니다.");
    }

    @Test
    @DisplayName("다양한 톤 설정 테스트 - formal")
    void rewriteMail_FormalTone() {
        // given
        String mockResponse = """
                {
                    "choices": [
                        {
                            "message": {
                                "content": "---다듬어진 메일---\\n격식체로 작성된 메일\\n\\n---개선 사항---\\n- 격식체 적용"
                            }
                        }
                    ]
                }
                """;

        mockWebServer.enqueue(new MockResponse()
                .setBody(mockResponse)
                .addHeader("Content-Type", "application/json"));

        MailRequest request = new MailRequest("테스트", "상사", "formal");

        // when
        MailResponse response = gptService.rewriteMail(request);

        // then
        assertThat(response.getRewrittenContent()).contains("격식체");
    }

    @Test
    @DisplayName("다양한 톤 설정 테스트 - casual")
    void rewriteMail_CasualTone() {
        // given
        String mockResponse = """
                {
                    "choices": [
                        {
                            "message": {
                                "content": "---다듬어진 메일---\\n캐주얼하게 작성된 메일\\n\\n---개선 사항---\\n- 친근한 표현 사용"
                            }
                        }
                    ]
                }
                """;

        mockWebServer.enqueue(new MockResponse()
                .setBody(mockResponse)
                .addHeader("Content-Type", "application/json"));

        MailRequest request = new MailRequest("테스트", "친구", "casual");

        // when
        MailResponse response = gptService.rewriteMail(request);

        // then
        assertThat(response.getRewrittenContent()).contains("캐주얼");
    }
}

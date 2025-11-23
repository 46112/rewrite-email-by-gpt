# AI 기반 문장 다듬기 서비스

## 문제 상황

메일을 작성할 때마다 여러 같은 어려움을 겪습니다.

- **맞춤법 검사**: 오타나 문법 오류가 없는지 확인
- **논리 흐름 점검**: 내용이 자연스럽게 이어지는지 확인
- **표현 다듬기**: 수신자에게 적절한 표현인지 확인

현재는 매번 AI에게 전체 초안을 복사/붙여넣기하고, 수신자가 누구인지 설명해야 합니다.
이 과정이 번거롭다고 느꼈습니다.

## 해결 방안

**크롬 확장프로그램 + 백엔드 서버**

네이버 메일 작성 화면에서 확장프로그램 버튼 하나만 클릭하면
1. 현재 작성 중인 메일 내용을 자동으로 추출
2. 백엔드 서버로 전송
3. OPENAI API를 통해 메일을 다듬음
4. 결과를 메일 작성 화면에 자동으로 반영

## 프로젝트 구조

```
rewriteByGpt/
├── src/
│   └── main/
│       ├── java/com/please/rewritebygpt/
│       │   ├── RewriteByGptApplication.java    # 메인 애플리케이션
│       │   ├── controller/
│       │   │   └── MailController.java          # API 엔드포인트
│       │   ├── service/
│       │   │   └── GptService.java              # ChatGPT API 연동
│       │   └── dto/
│       │       ├── MailRequest.java             # 요청 DTO
│       │       └── MailResponse.java            # 응답 DTO
│       └── resources/
│           └── application.yml                   # 설정 파일
└── chrome-extension/                             # (추후 구현) 크롬 확장프로그램
```

## 구현 계획

### 1단계: 백엔드 서버

- [x] Spring Boot 프로젝트 초기 설정
- [x] ChatGPT API 연동 서비스 구현
- [x] 메일 다듬기 REST API 엔드포인트 구현

### 2단계: 크롬 확장프로그램

- [ ] manifest.json 설정
- [ ] 네이버 메일 페이지 콘텐츠 스크립트
- [ ] 메일 내용 추출 로직
- [ ] 백엔드 API 호출 및 결과 반영
- [ ] 확장프로그램 팝업 UI

## API 명세

### POST /api/mail/rewrite

메일 내용을 받아 자연스럽게 수정한 결과를 반환합니다.

**Request Body:**
```json
{
  "content": "메일 본문 내용",
  "recipient": "교수님/상사/동료/친구 등",
  "tone": "formal/casual/polite/기타"
}
```

**Response:**
```json
{
  "rewrittenContent": "다듬어진 메일 본문",
  "suggestions": ["개선 사항 1", "개선 사항 2"]
}
```

## 기술 스택

- **Backend**: Spring Boot 4.0, Java 17
- **AI API**: OpenAI ChatGPT API
- **HTTP Client**: Spring WebClient
- **Chrome Extension**: JavaScript, Chrome Extensions Manifest V3

## 설정 방법

### 1. OpenAI API 키 설정

`application.yml`에 API 키를 설정합니다:

```yaml
openai:
  api-key: test-api-key
```

또는 환경 변수로 설정:
```bash
export OPENAI_API_KEY=test-api-key
```

### 2. 서버 실행

```bash
./gradlew bootRun
```

서버가 `http://localhost:8080`에서 실행됩니다.

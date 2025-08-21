# 실시간 통신 및 알림 시스템

## 프로젝트 개요

* **트랙**: WebSocket 기반 채팅 시스템과 SSE 기반 실시간 알림 시스템을 구축
* **목표**: Spring Security로 보호된 상태여야하고, 모든 테스트는 Postman 또는 콘솔 툴(WebSocket Client) 기준으로 수행
* **테스트 환경**: Postman / WebSocket 콘솔 클라이언트
* **구현 범위**: 로그인, 토큰 재발급, 보호 API, 예외 처리, 실시간 로그인 토큰 발급

## 기술 스택
| 항목        | 기술                   |
| --------- | -------------------- |
| Language  | Java 17              |
| Framework | Spring Boot 3      |
| Database  | MySQL                |
| Security  | Spring Security, JWT |
| Infra     | Docker, AWS EC2      |


## 기능 요약

| 기능             | 경로                       | 메서드    | 인증                                                |
| -------------- | ------------------------ | ------ | ------------------------------------------------- |
| JWT 발급         | `/auth/login`            | `POST` | 필요 없음                                             |
| JWT 재발급        | `/auth/refresh`          | `POST` | Refresh 토큰 필요                                     |
| 실시간 채팅 (STOMP) | `/ws/chat`               | `WS`   | STOMP `CONNECT` 헤더에 `Authorization: Bearer <JWT>` |
| 채팅 이력 조회       | `/chat/history/{roomId}` | `GET`  | JWT 필요                                            |
| 알림 구독(SSE)     | `/sse/subscribe`         | `GET`  | JWT 필요                                            |
| 알림 전송          | `/sse/notify`            | `POST` | JWT 필요                                            |
| 알림 이력 조회       | `/sse/history`           | `GET`  | JWT 필요                                            |

## 배포 주소
https://backendteamb.site

## API 설명

| API | Request Body | Response | 설명 |
| --- | --- | --- | --- |
| **POST /auth/login** | `{"username":"testuser","password":"testpass"}` | `{"success":true,"message":"로그인 성공","data":{"accessToken":"<JWT>","refreshToken":"<JWT>"}}` | 로그인 후 **JWT 발급**(WebSocket/보호 API에 사용) |
| **WS /ws/chat** | STOMP **CONNECT 헤더**: `Authorization: Bearer <JWT>`<br>메시지 전송(SEND): **`/app/chat.send`**<br>Body: `{"roomId":"room-1","message":"hi"}` | `{"roomId":"room-1","sender":"alice","message":"hi","sentAt":"2025-08-14T14:55:00Z"}` | **실시간 채팅**(STOMP). CONNECT에서 JWT 검증 → 전송 시 DB 저장 → 구독자에게 브로드캐스트 |
| **GET /chat/history/{roomId}** | *(Body 없음)*<br>**Header**: `Authorization: Bearer <JWT>` | `[{"id":1,"roomId":"room-1","sender":"alice","message":"hi","sentAt":"2025-08-14T14:55:00Z"}, ...]` | 특정 방의 **채팅 이력 조회**(DB 기반) |
| **GET /sse/subscribe** | *(Header: Authorization: Bearer <JWT>)*              | SSE 연결 스트림                                                                                 | 실시간 알림 구독. 서버가 이벤트 발생 시 푸시      |
| **POST /sse/notify**   | `{"receiverId":1,"title":"알림 제목","message":"알림 내용"}` | `{"id":5,"receiverId":1,"title":"알림 제목","message":"알림 내용","read":false,"createdAt":"..."}` | 특정 사용자에게 알림 전송 (DB 저장 + 실시간 전송) |
| **GET /sse/history**   | *(Header: Authorization: Bearer <JWT>)*              | `[{"id":5,"title":"알림 제목","message":"알림 내용",...}, ...]`                                    | 내 알림 이력 조회 (최근 100개)            |

## 실시간 통신 흐름

1. **로그인 → 토큰 발급**
  * 클라이언트 → `POST /auth/login`
  * 서버 → Access/Refresh 토큰 발급

2. **웹소켓 채팅**
  * 클라이언트 → `wss://backendteamb.site/ws/chat` STOMP CONNECT
  * CONNECT 헤더: `Authorization: Bearer <accessToken>`
  * SUBSCRIBE: `/topic/chat.{roomId}`
  * SEND: `/app/chat.send`
  * 서버: 토큰 검증 → DB 저장 → 구독자에게 메시지 전송

3. **채팅 이력 조회**
  * `GET /chat/history/{roomId}` 호출 → DB에서 메시지 반환

4. **알림 구독 (SSE)**
  * 클라이언트(EventSource) → `GET /sse/subscribe`
  * 서버: `SseEmitter` 생성 후 연결 유지

5. **알림 전송**
  * `POST /sse/notify` → DB 저장 + 연결된 모든 Emitter로 전송

6. **알림 이력 조회**
  * `GET /sse/history` → 최근 100개 알림 반환

## 포스트맨
https://www.postman.com/backend-team-b/workspace/websocket/collection/46095284-63155a74-6114-408b-a997-acc748c5977d?action=share&source=copy-link&creator=46095284
## 기술 오류
https://velog.io/@cse23_ewha/Websocket-%ED%8A%B8%EB%9F%AC%EB%B8%94%EC%8A%88%ED%8C%85-%EC%A0%9C%EB%AF%B8%EB%82%98%EC%9D%B4%EC%99%88
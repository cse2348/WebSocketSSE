# 실시간 통신 및 알림 시스템

## 프로젝트 개요

* **트랙**: WebSocket 기반 채팅 시스템과 SSE 기반 실시간 알림 시스템을 구축
* **목표**: Spring Security로 보호된 상태여야하고, 모든 테스트는 Postman 또는 콘솔 툴(WebSocket Client) 기준으로 수행
* **구현 범위**: 로그인, 토큰 재발급, 보호 API, 예외 핸들링까지 완성

## 기술 스택
| 항목        | 기술                   |
| --------- | -------------------- |
| Language  | Java 17              |
| Framework | Spring Boot 3      |
| Database  | MySQL                |
| Security  | Spring Security, JWT |
| Infra     | Docker, AWS EC2      |


## 기능 요약

| 기능 | 경로 | 메서드 | 인증 |
|---|---|---|---|
| JWT 발급 | `/auth/login` | `POST` | 필요 없음 |
| 실시간 채팅 (STOMP) | `/ws/chat` | `WS` | STOMP `CONNECT` 헤더에 `Authorization: Bearer <JWT>` |
| 채팅 이력 조회 | `/chat/history/{roomId}` | `GET` | 요청 헤더 `Authorization: Bearer <JWT>` |

## 배포 주소
https://backendteamb.site

## API 설명

| API | Request Body | Response | 설명 |
| --- | --- | --- | --- |
| **POST /auth/login** | `{"username":"testuser","password":"testpass"}` | `{"success":true,"message":"로그인 성공","data":{"accessToken":"<JWT>","refreshToken":"<JWT>"}}` | 로그인 후 **JWT 발급**(WebSocket/보호 API에 사용) |
| **WS /ws/chat** | STOMP **CONNECT 헤더**: `Authorization: Bearer <JWT>`<br>메시지 전송(SEND): **`/app/chat.send`**<br>Body: `{"roomId":"room-1","message":"hi"}` | `{"roomId":"room-1","sender":"alice","message":"hi","sentAt":"2025-08-14T14:55:00Z"}` | **실시간 채팅**(STOMP). CONNECT에서 JWT 검증 → 전송 시 DB 저장 → 구독자에게 브로드캐스트 |
| **GET /chat/history/{roomId}** | *(Body 없음)*<br>**Header**: `Authorization: Bearer <JWT>` | `[{"id":1,"roomId":"room-1","sender":"alice","message":"hi","sentAt":"2025-08-14T14:55:00Z"}, ...]` | 특정 방의 **채팅 이력 조회**(DB 기반) |


## 실시간 통신 흐름

1. 로그인 → 토큰 받기
  클라이언트가 POST /auth/login에 {"username","password"} 전송
  서버가 accessToken(+ refreshToken) 발급
  클라이언트는 accessToken을 저장

2. 웹소켓 연결 & 채팅 주고받기
  클라이언트가 wss://.../ws/chat로 STOMP 연결 시도
  STOMP CONNECT 헤더에 Authorization: Bearer <accessToken> 첨부
  연결 성공 후, 방에 구독(/topic/chat.{roomId})
  메시지 보낼 때 발행(/app/chat.send, body: {roomId, message})
  서버는 토큰 검증 → 메시지 DB 저장 → 해당 방 구독자들에게 브로드캐스트

3. 채팅 이력 조회
  클라이언트가 GET /chat/history/{roomId} 호출
  헤더에 Authorization: Bearer <accessToken> 첨부
  서버가 DB에서 해당 방 메시지 목록을 반환

## 기타(참고url,기술오류 등등)
https://www.postman.com/backend-team-b/workspace/websocket/collection/46095284-63155a74-6114-408b-a997-acc748c5977d?action=share&source=copy-link&creator=46095284

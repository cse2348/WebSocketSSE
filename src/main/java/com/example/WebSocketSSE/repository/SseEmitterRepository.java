package com.example.WebSocketSSE.repository;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Component // 스프링 빈 등록
public class SseEmitterRepository {

    // 사용자별로 연결된 SseEmitter 목록을 저장 (동시성 안전)
    private final Map<Long, List<SseEmitter>> userEmitters = new ConcurrentHashMap<>();

    // 사용자에게 새로운 SseEmitter 추가
    public SseEmitter add(Long userId, SseEmitter emitter) {
        // userId별 emitter 리스트를 가져오거나 새로 생성 후 emitter 추가
        userEmitters.computeIfAbsent(userId, k -> new CopyOnWriteArrayList<>()).add(emitter);

        // emitter 생명주기 이벤트 등록: 끊어지면 자동 제거
        emitter.onCompletion(() -> remove(userId, emitter)); // 정상 종료 시
        emitter.onTimeout(() -> remove(userId, emitter));    // 타임아웃 시
        emitter.onError(e -> remove(userId, emitter));       // 에러 발생 시

        return emitter; // 최종 등록된 emitter 반환
    }

    // 특정 userId의 emitter 목록 조회
    public List<SseEmitter> get(Long userId) {
        return userEmitters.getOrDefault(userId, List.of()); // 없으면 빈 리스트 반환
    }

    // 특정 userId의 emitter 하나 제거
    public void remove(Long userId, SseEmitter emitter) {
        List<SseEmitter> list = userEmitters.get(userId);
        if (list != null) {
            list.remove(emitter); // emitter 제거
            if (list.isEmpty()) userEmitters.remove(userId); // 더 없으면 전체 엔트리 삭제
        }
    }
}

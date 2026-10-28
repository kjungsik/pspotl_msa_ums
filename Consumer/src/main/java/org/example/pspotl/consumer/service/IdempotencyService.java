package org.example.pspotl.consumer.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class IdempotencyService {

    private final StringRedisTemplate redisTemplate;

    // 중복 검사 키 유지 시간 (24시간)
    private static final Duration KEY_TTL = Duration.ofHours(24);

    /**
     * 최초 처리 메시지인지 검증 (Deduplication)
     * @param messageId 유니크 메시지 ID
     * @return true: 최초 수신 메시지 (처리 진행) / false: 이미 처리된 중복 메시지 (스킵)
     */
    public boolean isFirstRequest(String messageId) {
        String redisKey = "processed:msg:" + messageId;

        // SET key value NX EX 86400 (Atomic 처리)
        Boolean isAbsent = redisTemplate.opsForValue()
                .setIfAbsent(redisKey, "PROCESSED", KEY_TTL);

        if (Boolean.FALSE.equals(isAbsent)) {
            log.warn("[Deduplication] 중복 메시지 감지: messageId={}", messageId);
            return false;
        }

        return true;
    }
}
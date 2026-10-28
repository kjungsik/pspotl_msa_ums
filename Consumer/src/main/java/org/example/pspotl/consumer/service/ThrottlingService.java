package org.example.pspotl.consumer.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Collections;

@Slf4j
@Service
@RequiredArgsConstructor
public class ThrottlingService {

    private final StringRedisTemplate redisTemplate;

    // 초당 5,000건 제한 설정 (과제 요구사항 기준)
    private static final long LIMIT_PER_SECOND = 5000;

    /**
     * Lua Script: Atomic하게 TPS 카운팅 및 TTL(만료시간) 설정
     * - Key가 존재하지 않으면 1로 생성하고 TTL 2초 부여
     * - Key가 존재하면 카운트 +1 증가
     * - 현재 카운트 반환
     */
    private static final String THROTTLE_LUA_SCRIPT =
            "local current = redis.call('INCR', KEYS[1]) " +
                    "if tonumber(current) == 1 then " +
                    "redis.call('EXPIRE', KEYS[1], 2) " +
                    "end " +
                    "return current";

    /**
     * 초당 처리량(TPS) 초과 여부 검사
     * @param channelName 발송 채널 (예: APP_PUSH, ALIMTALK)
     * @return true: 허용 / false: 차단(Throttled)
     */
    public boolean isAllowed(String channelName) {
        // 현재 초 단위 타임스탬프 기반 Key 생성 (예: rate:limiter:APP_PUSH:1723650000)
        String currentSecond = String.valueOf(System.currentTimeMillis() / 1000);
        String redisKey = "rate:limiter:" + channelName + ":" + currentSecond;

        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setScriptText(THROTTLE_LUA_SCRIPT);
        script.setResultType(Long.class);

        // Lua Script 실행
        Long currentTps = redisTemplate.execute(script, Collections.singletonList(redisKey));

        if (currentTps != null && currentTps > LIMIT_PER_SECOND) {
            log.warn("[Throttling] 채널 {} 초당 임계치 초과! Current TPS: {}/{}", channelName, currentTps, LIMIT_PER_SECOND);
            return false;
        }

        return true;
    }
}
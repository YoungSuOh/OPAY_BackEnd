package com.opay.domain.search.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PopularKeywordService {

    private static final String POPULAR_KEYWORDS_KEY = "search:popular:keywords";

    private final StringRedisTemplate redisTemplate;

    @Value("${opay.search.popular-keywords-limit:10}")
    private int limit;

    public void recordSearch(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) return;
        String normalized = keyword.trim().toLowerCase();
        try {
            redisTemplate.opsForZSet().incrementScore(POPULAR_KEYWORDS_KEY, normalized, 1);
        } catch (Exception e) {
            log.debug("Redis popular keyword record failed: {}", e.getMessage());
        }
    }

    public List<String> getPopularKeywords() {
        try {
            Set<ZSetOperations.TypedTuple<String>> set = redisTemplate.opsForZSet()
                    .reverseRangeWithScores(POPULAR_KEYWORDS_KEY, 0, limit - 1);
            if (set == null) return List.of();
            return set.stream()
                    .map(ZSetOperations.TypedTuple::getValue)
                    .filter(v -> v != null)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.debug("Redis popular keyword fetch failed: {}", e.getMessage());
            return List.of();
        }
    }
}

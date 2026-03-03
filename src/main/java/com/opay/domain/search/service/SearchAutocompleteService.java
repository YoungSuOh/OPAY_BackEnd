package com.opay.domain.search.service;

import com.opay.domain.search.document.ProductDocument;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class SearchAutocompleteService {

    private static final String AUTOCOMPLETE_KEY_PREFIX = "search:autocomplete:";
    private static final int AUTOCOMPLETE_LIMIT = 10;

    private final StringRedisTemplate redisTemplate;
    private final ElasticsearchOperations elasticsearchOperations;

    @Value("${opay.search.autocomplete-cache-ttl:600}")
    private int cacheTtlSeconds;

    public List<String> getSuggestions(String prefix) {
        if (prefix == null || prefix.trim().length() < 2) {
            return List.of();
        }
        String normalized = prefix.trim().toLowerCase();
        String cacheKey = AUTOCOMPLETE_KEY_PREFIX + normalized;

        try {
            List<String> cached = redisTemplate.opsForList().range(cacheKey, 0, -1);
            if (cached != null && !cached.isEmpty()) {
                return cached;
            }
        } catch (Exception e) {
            log.debug("Redis autocomplete cache miss: {}", e.getMessage());
        }

        try {
            List<String> suggestions = searchFromElasticsearch(normalized);
            if (!suggestions.isEmpty()) {
                try {
                    redisTemplate.opsForList().rightPushAll(cacheKey, suggestions);
                    redisTemplate.expire(cacheKey, cacheTtlSeconds, TimeUnit.SECONDS);
                } catch (Exception e) {
                    log.debug("Redis autocomplete cache write failed: {}", e.getMessage());
                }
            }
            return suggestions;
        } catch (Exception e) {
            log.warn("Autocomplete search failed: {}", e.getMessage());
            return List.of();
        }
    }

    private List<String> searchFromElasticsearch(String prefix) {
        Query query = NativeQuery.builder()
                .withQuery(q -> q
                        .matchPhrasePrefix(m -> m
                                .field("name")
                                .query(prefix)
                                .maxExpansions(10)
                        )
                )
                .withPageable(PageRequest.of(0, AUTOCOMPLETE_LIMIT))
                .build();

        SearchHits<ProductDocument> hits = elasticsearchOperations.search(query, ProductDocument.class);
        Set<String> seen = new HashSet<>();
        List<String> result = new ArrayList<>();
        for (SearchHit<ProductDocument> hit : hits) {
            String name = hit.getContent().getName();
            if (name != null && !seen.contains(name)) {
                seen.add(name);
                result.add(name);
                if (result.size() >= AUTOCOMPLETE_LIMIT) break;
            }
        }
        return result;
    }
}

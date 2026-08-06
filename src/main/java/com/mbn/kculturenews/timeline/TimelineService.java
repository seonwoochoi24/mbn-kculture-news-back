package com.mbn.kculturenews.timeline;

import com.mbn.kculturenews.article.Article;
import com.mbn.kculturenews.article.ArticleRepository;
import com.mbn.kculturenews.article.ArticleStatus;
import com.mbn.kculturenews.translation.OpenAiProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

@Service
public class TimelineService {

    private static final Logger log = LoggerFactory.getLogger(TimelineService.class);
    private static final int MAX_ARTICLES = 10;
    private static final int MIN_EVENTS = 3;
    private static final int MAX_EVENTS = 5;
    private static final DateTimeFormatter RESPONSE_DATE = DateTimeFormatter.ofPattern("yy.MM.dd");
    private static final ZoneId KOREA = ZoneId.of("Asia/Seoul");
    private static final String CACHE_VERSION = "timeline-events-v2";

    private final ArticleRepository articleRepository;
    private final ArticleTimelineCacheRepository cacheRepository;
    private final TimelineAiClient aiClient;
    private final OpenAiProperties properties;
    private final ObjectMapper objectMapper;
    private final Map<String, Object> generationLocks = new ConcurrentHashMap<>();

    public TimelineService(
            ArticleRepository articleRepository,
            ArticleTimelineCacheRepository cacheRepository,
            TimelineAiClient aiClient,
            OpenAiProperties properties,
            ObjectMapper objectMapper
    ) {
        this.articleRepository = articleRepository;
        this.cacheRepository = cacheRepository;
        this.aiClient = aiClient;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    public TimelineResponse create(List<Long> requestedIds) {
        List<Long> ids = requestedIds == null
                ? List.of()
                : requestedIds.stream().distinct().sorted().toList();
        if (ids.isEmpty() || ids.size() > MAX_ARTICLES || ids.stream().anyMatch(id -> id == null || id < 1)) {
            throw new InvalidTimelineRequestException("articleIds는 1~10개의 유효한 기사 ID여야 합니다.");
        }

        Map<Long, Article> articlesById = new LinkedHashMap<>();
        articleRepository.findAllById(ids).stream()
                .filter(article -> article.getStatus() == ArticleStatus.PUBLISHED)
                .forEach(article -> articlesById.put(article.getArticleId(), article));
        if (articlesById.size() != ids.size()) {
            throw new InvalidTimelineRequestException("존재하지 않거나 공개되지 않은 articleId가 포함되어 있습니다.");
        }

        List<TimelineArticleInput> inputs = ids.stream()
                .map(articlesById::get)
                .map(this::toInput)
                .toList();
        validateInputLength(inputs);

        String cacheKey = cacheKey(inputs);
        TimelineResponse cached = readCache(cacheKey);
        if (cached != null) {
            return cached;
        }

        Object lock = generationLocks.computeIfAbsent(cacheKey, ignored -> new Object());
        try {
            synchronized (lock) {
                cached = readCache(cacheKey);
                if (cached != null) {
                    return cached;
                }
                TimelineResponse response = generate(inputs, articlesById);
                if (!response.items().isEmpty()) {
                    writeCache(cacheKey, response);
                }
                return response;
            }
        } finally {
            generationLocks.remove(cacheKey, lock);
        }
    }

    private TimelineResponse generate(List<TimelineArticleInput> inputs, Map<Long, Article> articlesById) {
        try {
            List<TimelineEvent> validated = validate(aiClient.extract(inputs), articlesById);
            if (validated.isEmpty()) {
                validated = fallbackEvents(articlesById);
            }
            return toResponse(validated);
        } catch (RuntimeException exception) {
            log.warn("AI 타임라인 생성에 실패해 빈 결과를 반환합니다.", exception);
            return TimelineResponse.empty();
        }
    }

    private TimelineArticleInput toInput(Article article) {
        String content = article.getContent();
        if (content == null || content.isBlank() || article.getPublishedAt() == null) {
            throw new InvalidTimelineRequestException("본문 또는 작성일이 없는 기사는 타임라인에 사용할 수 없습니다.");
        }
        return new TimelineArticleInput(
                article.getArticleId(),
                article.getTitle(),
                content,
                article.getPublishedAt().atZone(KOREA).toLocalDate()
        );
    }

    private void validateInputLength(List<TimelineArticleInput> inputs) {
        int length = inputs.stream().mapToInt(article ->
                article.title().length() + article.content().length() + 40
        ).sum();
        if (length > properties.getMaxInputChars()) {
            throw new InvalidTimelineRequestException(
                    "선택한 기사 본문이 AI 입력 제한 " + properties.getMaxInputChars() + "자를 초과했습니다. 기사 수를 줄여주세요."
            );
        }
    }

    private List<TimelineEvent> validate(List<TimelineEvent> candidates, Map<Long, Article> articlesById) {
        List<TimelineEvent> result = new ArrayList<>();
        Set<String> duplicateKeys = new HashSet<>();
        Set<String> similarKeys = new HashSet<>();
        for (TimelineEvent candidate : candidates) {
            Article article = articlesById.get(candidate.articleId());
            String eventTitle = candidate.eventTitle() == null ? "" : candidate.eventTitle().trim();
            if (article == null || eventTitle.isBlank() || eventTitle.length() > 120) {
                continue;
            }
            if (!dateAppearsInArticle(candidate.date(), article)) {
                continue;
            }
            String duplicateKey = candidate.date() + "|" + eventTitle + "|" + candidate.articleId();
            String similarKey = candidate.date() + "|" + normalizeEventTitle(eventTitle);
            if (duplicateKeys.add(duplicateKey) && similarKeys.add(similarKey)) {
                result.add(new TimelineEvent(candidate.date(), eventTitle, candidate.articleId()));
            }
        }
        result.sort(Comparator.comparing(TimelineEvent::date).thenComparingLong(TimelineEvent::articleId));
        return result.stream().limit(MAX_EVENTS).toList();
    }

    private List<TimelineEvent> fallbackEvents(Map<Long, Article> articlesById) {
        return articlesById.values().stream()
                .filter(article -> article.getPublishedAt() != null)
                .sorted(Comparator
                        .comparing(Article::getPublishedAt)
                        .thenComparing(Article::getArticleId))
                .limit(MAX_EVENTS)
                .map(article -> new TimelineEvent(
                        article.getPublishedAt().atZone(KOREA).toLocalDate(),
                        article.getTitle(),
                        article.getArticleId()
                ))
                .toList();
    }

    private String normalizeEventTitle(String eventTitle) {
        return eventTitle.replaceAll("[^0-9A-Za-z가-힣]", "").toLowerCase();
    }

    private boolean dateAppearsInArticle(LocalDate date, Article article) {
        if (date == null) {
            return false;
        }
        String text = article.getTitle() + "\n" + article.getContent();
        String year = String.valueOf(date.getYear());
        String month = String.valueOf(date.getMonthValue());
        String day = String.valueOf(date.getDayOfMonth());
        boolean fullDate = Pattern.compile(
                "(?<!\\d)" + year + "\\s*(?:년|[-./])\\s*0?" + month
                        + "\\s*(?:월|[-./])\\s*0?" + day + "\\s*일?"
        ).matcher(text).find();
        if (fullDate) {
            return true;
        }
        LocalDate publishedDate = article.getPublishedAt().atZone(KOREA).toLocalDate();
        return publishedDate.getYear() == date.getYear() && Pattern.compile(
                "(?<!\\d)0?" + month + "\\s*(?:월|[-./])\\s*0?" + day + "\\s*일"
        ).matcher(text).find();
    }

    private TimelineResponse toResponse(List<TimelineEvent> events) {
        List<TimelineItemResponse> items = events.stream()
                .map(event -> new TimelineItemResponse(
                        event.articleId(), event.eventTitle(), RESPONSE_DATE.format(event.date())
                ))
                .toList();
        return new TimelineResponse(items);
    }

    private TimelineResponse readCache(String cacheKey) {
        return cacheRepository.findByCacheKey(cacheKey).map(cache -> {
            try {
                return objectMapper.readValue(cache.getResponseJson(), TimelineResponse.class);
            } catch (Exception exception) {
                cacheRepository.delete(cache);
                return null;
            }
        }).orElse(null);
    }

    private void writeCache(String cacheKey, TimelineResponse response) {
        try {
            String json = objectMapper.writeValueAsString(response);
            cacheRepository.saveAndFlush(new ArticleTimelineCache(cacheKey, json, Instant.now()));
        } catch (DataIntegrityViolationException ignored) {
            // A concurrent request already cached the same article set.
        } catch (Exception exception) {
            throw new IllegalStateException("타임라인 캐시 저장에 실패했습니다.", exception);
        }
    }

    private String cacheKey(List<TimelineArticleInput> inputs) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(CACHE_VERSION.getBytes(StandardCharsets.UTF_8));
            digest.update(properties.getModel().getBytes(StandardCharsets.UTF_8));
            for (TimelineArticleInput input : inputs) {
                digest.update(objectMapper.writeValueAsBytes(input));
            }
            return java.util.HexFormat.of().formatHex(digest.digest());
        } catch (Exception exception) {
            throw new IllegalStateException("타임라인 캐시 키 생성에 실패했습니다.", exception);
        }
    }
}

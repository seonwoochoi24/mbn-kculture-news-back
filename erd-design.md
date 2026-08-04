# AI 기반 한류 뉴스 서비스 ERD 설계

## 1. 설계 범위와 원칙

- 기사 원본과 언어별 현지화 결과를 분리한다.
- AI 산출물은 `상태`, `모델`, `프롬프트 버전`, `생성 시각`을 기록하여 재생성 및 장애 대응이 가능하게 한다.
- 댓글 원문은 한 번만 저장하고, 번역은 언어별 캐시 테이블에 저장한다.
- 검색 타임라인은 검색 조건별 캐시로 관리한다. 실시간 생성과 배치 사전 생성 방식을 모두 지원한다.
- 장소와 상품은 초기에는 공통 `promotion` 모델로 통합한다. 속성이 크게 늘어나면 타입별 상세 테이블로 분리할 수 있다.
- 댓글과 좋아요는 계정 없이 브라우저에 발급된 익명 토큰으로 처리하고, 서버에는 토큰 해시만 저장한다.
- 최근 검색어는 명세대로 LocalStorage에만 저장하며 서버 DB에는 저장하지 않는다.
- IP 원문은 저장하지 않는다. 언어 감지 결과가 분석에 필요하면 익명화된 국가 코드와 선택 언어만 별도 로그 시스템에 보관한다.
- 기사는 시스템 수집·생성 데이터이므로 업로더 또는 작성자 계정과 연결하지 않는다.
- 연예 뉴스의 기본 수집원은 MBN 공식 RSS `https://www.mbn.co.kr/rss/enter/`로 고정한다.
- RSS는 신규 기사 발견에 사용하고, 사용자 검색은 수집된 기사를 Elasticsearch/OpenSearch에서 조회한다.
- 관계형 데이터베이스는 MySQL을 사용하고 모든 일시 값은 UTC 기준 `DATETIME(6)`으로 저장한다.
- 한글·일본어·중국어·이모지 저장을 위해 데이터베이스와 연결 문자셋은 `utf8mb4`로 통일한다.

## 2. 전체 ERD

```mermaid
erDiagram
    ANONYMOUS_VISITOR {
        binary(16) visitor_id PK
        varchar token_hash UK
        varchar status
        datetime(6) first_seen_at
        datetime(6) last_seen_at
    }

    LANGUAGE {
        varchar language_code PK
        varchar language_name
        boolean is_active
    }

    ARTICLE {
        bigint article_id PK
        varchar content_type
        varchar source_name
        varchar source_url
        varchar external_guid UK
        varchar source_category
        varchar original_language_code FK
        datetime(6) published_at
        datetime(6) collected_at
        varchar status
        datetime(6) created_at
        datetime(6) updated_at
    }

    ARTICLE_LOCALIZATION {
        bigint article_localization_id PK
        bigint article_id FK
        varchar language_code FK
        varchar title
        text body
        varchar translation_status
        varchar model_name
        varchar prompt_version
        datetime(6) generated_at
    }

    ARTICLE_MEDIA {
        bigint media_id PK
        bigint article_id FK
        varchar media_type
        varchar url
        varchar variant
        int sort_order
        json metadata
    }

    AI_SUMMARY {
        bigint summary_id PK
        bigint article_id FK
        varchar language_code FK
        varchar generation_status
        varchar model_name
        varchar prompt_version
        datetime(6) generated_at
    }

    AI_SUMMARY_LINE {
        bigint summary_line_id PK
        bigint summary_id FK
        smallint line_no
        text content
    }

    CARD_NEWS {
        bigint card_news_id PK
        bigint article_id FK
        varchar language_code FK
        varchar generation_status
        varchar model_name
        varchar prompt_version
        datetime(6) generated_at
    }

    CARD_NEWS_SLIDE {
        bigint slide_id PK
        bigint card_news_id FK
        int slide_no
        varchar image_url
        varchar headline
        text body
    }

    CULTURAL_TERM {
        bigint term_id PK
        varchar canonical_term
        varchar source_language_code FK
        varchar status
    }

    CULTURAL_TERM_LOCALIZATION {
        bigint term_localization_id PK
        bigint term_id FK
        varchar language_code FK
        varchar display_term
        text description
        varchar model_name
        datetime(6) generated_at
    }

    ARTICLE_TERM_OCCURRENCE {
        bigint occurrence_id PK
        bigint article_localization_id FK
        bigint term_id FK
        int start_offset
        int end_offset
        varchar anchor_text
    }

    COMMENT {
        bigint comment_id PK
        bigint article_id FK
        binary(16) visitor_id FK
        varchar original_language_code FK
        varchar nickname
        text content
        bigint like_count
        varchar status
        bigint version
        datetime(6) created_at
        datetime(6) updated_at
    }

    COMMENT_TRANSLATION {
        bigint comment_translation_id PK
        bigint comment_id FK
        varchar language_code FK
        text translated_content
        varchar translation_status
        varchar model_name
        datetime(6) generated_at
    }

    COMMENT_LIKE {
        bigint comment_id PK,FK
        binary(16) visitor_id PK,FK
        datetime(6) created_at
    }

    PROMOTION {
        bigint promotion_id PK
        varchar promotion_type
        varchar image_url
        varchar link_url
        decimal(10,7) latitude
        decimal(10,7) longitude
        varchar status
        datetime(6) starts_at
        datetime(6) ends_at
    }

    PROMOTION_LOCALIZATION {
        bigint promotion_localization_id PK
        bigint promotion_id FK
        varchar language_code FK
        varchar name
        text description
    }

    ARTICLE_PROMOTION {
        bigint article_id PK,FK
        bigint promotion_id PK,FK
        int sort_order
        decimal(8,6) relevance_score
    }

    SEARCH_SNAPSHOT {
        bigint search_snapshot_id PK
        varchar normalized_keyword
        varchar category
        varchar language_code FK
        varchar generation_mode
        varchar generation_status
        datetime(6) expires_at
        datetime(6) created_at
    }

    SEARCH_RESULT {
        bigint search_snapshot_id PK,FK
        bigint article_id PK,FK
        int rank_order
        decimal(8,6) relevance_score
    }

    TIMELINE_EVENT {
        bigint timeline_event_id PK
        bigint search_snapshot_id FK
        date event_date
        varchar generation_status
        int sort_order
    }

    TIMELINE_EVENT_LOCALIZATION {
        bigint timeline_event_localization_id PK
        bigint timeline_event_id FK
        varchar language_code FK
        varchar title
        text description
    }

    TIMELINE_EVENT_ARTICLE {
        bigint timeline_event_id PK,FK
        bigint article_id PK,FK
        boolean is_primary
        int sort_order
    }

    LANGUAGE ||--o{ ARTICLE : original_language
    ARTICLE ||--o{ ARTICLE_LOCALIZATION : localized_as
    LANGUAGE ||--o{ ARTICLE_LOCALIZATION : written_in
    ARTICLE ||--o{ ARTICLE_MEDIA : has
    ARTICLE ||--o{ AI_SUMMARY : summarized_as
    LANGUAGE ||--o{ AI_SUMMARY : written_in
    AI_SUMMARY ||--|{ AI_SUMMARY_LINE : contains
    ARTICLE ||--o{ CARD_NEWS : rendered_as
    LANGUAGE ||--o{ CARD_NEWS : written_in
    CARD_NEWS ||--|{ CARD_NEWS_SLIDE : contains
    LANGUAGE ||--o{ CULTURAL_TERM : source_language
    CULTURAL_TERM ||--o{ CULTURAL_TERM_LOCALIZATION : localized_as
    LANGUAGE ||--o{ CULTURAL_TERM_LOCALIZATION : written_in
    ARTICLE_LOCALIZATION ||--o{ ARTICLE_TERM_OCCURRENCE : marks
    CULTURAL_TERM ||--o{ ARTICLE_TERM_OCCURRENCE : appears_as
    ARTICLE ||--o{ COMMENT : receives
    ANONYMOUS_VISITOR ||--o{ COMMENT : writes
    LANGUAGE ||--o{ COMMENT : original_language
    COMMENT ||--o{ COMMENT_TRANSLATION : translated_as
    LANGUAGE ||--o{ COMMENT_TRANSLATION : written_in
    COMMENT ||--o{ COMMENT_LIKE : receives
    ANONYMOUS_VISITOR ||--o{ COMMENT_LIKE : creates
    ARTICLE ||--o{ ARTICLE_PROMOTION : recommends
    PROMOTION ||--o{ ARTICLE_PROMOTION : attached_to
    PROMOTION ||--o{ PROMOTION_LOCALIZATION : localized_as
    LANGUAGE ||--o{ PROMOTION_LOCALIZATION : written_in
    LANGUAGE ||--o{ SEARCH_SNAPSHOT : requested_in
    SEARCH_SNAPSHOT ||--o{ SEARCH_RESULT : contains
    ARTICLE ||--o{ SEARCH_RESULT : appears_in
    SEARCH_SNAPSHOT ||--o{ TIMELINE_EVENT : produces
    TIMELINE_EVENT ||--o{ TIMELINE_EVENT_LOCALIZATION : localized_as
    LANGUAGE ||--o{ TIMELINE_EVENT_LOCALIZATION : written_in
    TIMELINE_EVENT ||--|{ TIMELINE_EVENT_ARTICLE : references
    ARTICLE ||--o{ TIMELINE_EVENT_ARTICLE : referenced_by
```

## 3. 핵심 테이블 설명

| 영역 | 테이블 | 역할 |
|---|---|---|
| 익명 방문자 | `anonymous_visitor` | 계정 없이 댓글 소유권과 중복 좋아요를 식별하는 익명 브라우저 토큰 관리 |
| 언어 | `language` | 서비스가 지원하는 ISO 639-1 언어 코드 관리 |
| 기사 | `article` | MBN 연예 RSS에서 수집한 원문 URL·GUID·발행 시각과 콘텐츠 타입 관리 |
| 현지화 | `article_localization` | 언어별 제목·본문·AI 번역 이력 관리 |
| 미디어 | `article_media` | 썸네일, 원본 영상, 자막 제거 영상 등 관리 |
| AI 요약 | `ai_summary`, `ai_summary_line` | 언어별 3줄 요약과 생성 상태 관리 |
| 카드뉴스 | `card_news`, `card_news_slide` | 기사·언어별 카드뉴스 덱과 슬라이드 관리 |
| 문화 툴팁 | `cultural_term`, `cultural_term_localization`, `article_term_occurrence` | 용어 사전, 언어별 설명, 본문 내 노출 위치 관리 |
| 댓글 | `comment`, `comment_translation`, `comment_like` | 익명 댓글 원문, 번역 캐시, 방문자별 좋아요 관리 |
| 추천 | `promotion`, `promotion_localization`, `article_promotion` | 장소·상품과 기사 간 N:M 추천 관계 관리 |
| 검색 | `search_snapshot`, `search_result` | 동일 검색 조건의 결과 및 AI 응답 캐시 관리 |
| 타임라인 | `timeline_event`, `timeline_event_localization`, `timeline_event_article` | 날짜별 사건과 연관 기사 연결 |

## 4. 주요 제약조건

### 유니크 제약

- `article_localization (article_id, language_code)`
- `article (source_url)`
- `article (source_name, external_guid)` (`external_guid`가 존재할 때)
- `ai_summary (article_id, language_code)`
- `ai_summary_line (summary_id, line_no)` 및 `line_no BETWEEN 1 AND 3`
- `card_news (article_id, language_code)`
- `card_news_slide (card_news_id, slide_no)`
- `cultural_term_localization (term_id, language_code)`
- `comment_translation (comment_id, language_code)`
- `anonymous_visitor (token_hash)`
- `comment_like (comment_id, visitor_id)`
- `promotion_localization (promotion_id, language_code)`
- `article_promotion (article_id, promotion_id)`
- `search_result (search_snapshot_id, article_id)`
- `timeline_event_localization (timeline_event_id, language_code)`
- `timeline_event_article (timeline_event_id, article_id)`

### 체크 제약

- `article.content_type IN ('LONG_FORM', 'NEWS', 'SHORTS')`
- `article.status IN ('DRAFT', 'PUBLISHED', 'DELETED')`
- `promotion.promotion_type IN ('PLACE', 'PRODUCT')`
- AI 상태값은 공통으로 `PENDING`, `PROCESSING`, `COMPLETED`, `FAILED` 사용
- `comment.content`는 트림 후 1~500자
- `article_term_occurrence.start_offset >= 0`
- `article_term_occurrence.end_offset > start_offset`
- `promotion.starts_at IS NULL OR promotion.ends_at IS NULL OR starts_at < ends_at`

## 5. 권장 인덱스

```sql
CREATE INDEX idx_article_search_base
    ON article (status, content_type, published_at DESC);

CREATE UNIQUE INDEX uk_article_source_url
    ON article (source_url);

CREATE UNIQUE INDEX uk_article_source_guid
    ON article (source_name, external_guid);

CREATE INDEX idx_article_localization_title
    ON article_localization (language_code, title);

CREATE INDEX idx_comment_article_created
    ON comment (article_id, status, created_at DESC);

CREATE INDEX idx_promotion_active_period
    ON promotion (status, starts_at, ends_at);

CREATE INDEX idx_search_snapshot_cache
    ON search_snapshot
       (normalized_keyword, category, language_code, expires_at DESC);

CREATE INDEX idx_timeline_snapshot_date
    ON timeline_event (search_snapshot_id, event_date, sort_order);
```

실제 통합 검색은 MySQL 인덱스만으로 제한하지 않고 Elasticsearch/OpenSearch를 사용한다. DB의 `article_id`를 검색 문서의 식별자로 사용하고, MySQL을 최종 원장으로 둔다.

## 6. 수집 및 API와 테이블 매핑

### MBN 연예 RSS 수집

수집 주소는 프로토콜을 포함한 `https://www.mbn.co.kr/rss/enter/`를 사용한다.

1. 서버 스케줄러가 3~5분 간격으로 RSS XML을 조회한다.
2. RSS 항목의 `guid`와 정규화한 원문 URL로 기존 기사 여부를 확인한다.
3. 신규 항목만 `article`에 `source_name='MBN'`, `source_category='ENTERTAINMENT'`로 저장한다.
4. 전체 본문은 MBN 내부 콘텐츠 API를 우선 사용해 확보한다. RSS 설명문만으로 AI 요약을 만들지 않는다.
5. 저장 성공 후 번역, 3줄 요약, 문화 용어, 카드뉴스 생성 작업을 비동기 큐에 등록한다.
6. AI 결과가 완료되면 해당 기사를 Elasticsearch/OpenSearch에 색인한다.
7. RSS 요청 실패 시 기존 검색 서비스는 계속 제공하며 다음 스케줄에서 재시도한다.

```text
MBN 연예 RSS
→ GUID·URL 중복 검사
→ 기사 및 본문 저장
→ AI 결과 사전 생성
→ 검색엔진 색인
→ 사용자 검색 제공
```

### `GET /api/v1/news/search`

1. `keyword`, `category`, `language`를 정규화한다.
2. 유효한 `search_snapshot` 캐시가 있으면 `search_result`와 `timeline_event`를 조회한다.
3. 캐시가 없으면 검색 엔진 결과를 받아 `search_snapshot`과 `search_result`를 생성한다.
4. 타임라인이 준비되지 않았으면 `generation_status=PENDING`으로 생성하고 비동기 작업을 요청한다.
5. 화면 목록은 `article`, `article_localization`, 대표 `article_media`를 조합한다.

### `GET /api/v1/news/{articleId}`

1. `article`과 요청 언어의 `article_localization`을 조회한다.
2. 현지화 데이터가 없으면 원문 또는 기본 언어로 폴백하고 비동기 번역을 요청한다.
3. `ai_summary`, `card_news`, `article_term_occurrence`를 언어별로 조회한다.
4. `article_promotion`을 통해 유효 기간 내 `promotion`만 반환한다.
5. `LONG_FORM`이면 `article_media.variant`가 `ORIGINAL`, `CLEAN`인 영상 URL을 함께 반환한다.

### 댓글 좋아요

1. 최초 접속 시 클라이언트가 충분히 긴 랜덤 익명 토큰을 생성해 쿠키 또는 LocalStorage에 보관하고, 서버는 토큰 해시를 `anonymous_visitor`에 저장한다.
2. 댓글 작성 및 좋아요 요청은 익명 토큰으로 방문자를 확인하며, `comment_like`의 `(comment_id, visitor_id)` 복합 PK로 중복 좋아요를 차단한다.
3. 익명 토큰이 일치하는 경우에만 본인이 작성한 댓글의 수정·삭제를 허용한다. 토큰을 잃어버리면 소유권을 복구할 수 없다.
4. 트랜잭션 안에서 좋아요 행 삽입/삭제와 `comment.like_count` 증감을 수행한다.
5. 동시성 제어는 `comment.version`을 이용한 낙관적 락 또는 Redis 카운터 후 DB 반영 방식을 사용한다.

## 7. 설계 결정이 필요한 항목에 대한 권장안

1. **AI 타임라인:** 배치 사전 생성 + 캐시 미스 시 비동기 실시간 생성의 하이브리드 방식을 권장한다. `generation_mode`에 `BATCH`, `REALTIME`을 기록한다.
2. **댓글 번역:** 번역 결과 자체는 `comment_translation`에 공용 캐시하고, 이용 횟수 제한이 필요하면 익명 방문자 단위로 Redis/API Gateway에서 처리한다.
3. **자막 제거 영상:** 별도 기사 컬럼 대신 `article_media.variant='CLEAN'`으로 모델링하여 향후 더빙·세로형·저화질 버전을 쉽게 추가한다.
4. **검색어 기록:** 계정 기능이 없으므로 최근 검색어는 LocalStorage에만 저장하고 핵심 DB에는 기록하지 않는다.

## 8. MVP 축소안

해커톤 MVP에서는 아래 18개 테이블부터 구현해도 기능 흐름을 충족한다.

`language`, `anonymous_visitor`, `article`, `article_localization`, `article_media`, `ai_summary`, `ai_summary_line`, `card_news`, `card_news_slide`, `cultural_term`, `cultural_term_localization`, `article_term_occurrence`, `comment`, `comment_translation`, `comment_like`, `promotion`, `promotion_localization`, `article_promotion`

연예 기사는 MBN RSS로 미리 수집하고 사용자 검색에는 Elasticsearch/OpenSearch 응답을 사용한다. 타임라인만 JSON 캐시로 저장하면 `search_snapshot`, `search_result`, `timeline_event` 계열은 2차 개발로 미룰 수 있다.

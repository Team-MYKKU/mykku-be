# MyKKU Backend Project Guide for Claude

## Project Overview

MyKKU는 덕질 커뮤니티 플랫폼의 백엔드 서비스입니다.

## Architecture: Clean Architecture (Hexagonal Architecture)

이 프로젝트는 **Clean Architecture (Hexagonal Architecture)**를 따릅니다. 의존성은 항상 외부에서 내부(Domain)를 향합니다.

### 아키텍처 구조

```
┌─────────────────────────────────────────────────────────────┐
│                    Adapter (Input)                          │
│  ┌─────────────────────────────────────────────────────┐   │
│  │  Web (Controller, Request/Response DTO)              │   │
│  └─────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                    Application Layer                         │
│  ┌─────────────────────────────────────────────────────┐   │
│  │  Port (Input)  │  UseCase  │  Port (Output)         │   │
│  │  (UseCase IF)  │  (Impl)   │  (Repository IF)       │   │
│  └─────────────────────────────────────────────────────┘   │
│  │  DTO (Command, Query, Result)                        │   │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                    Domain Layer                              │
│  ┌─────────────────────────────────────────────────────┐   │
│  │  Domain Entity  │  Value Object                      │   │
│  └─────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
                              ▲
                              │
┌─────────────────────────────────────────────────────────────┐
│                    Adapter (Output)                          │
│  ┌─────────────────────────────────────────────────────┐   │
│  │  Persistence (JPA Entity, Repository Adapter)        │   │
│  └─────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
```

### 의존성 방향

```
Controller → UseCase (Port/Input) → Repository (Port/Output) ← RepositoryAdapter
                                           ↓
                                        Domain
```

**핵심 원칙**: 모든 의존성은 Domain을 향합니다. Domain은 외부에 의존하지 않습니다.

---

## Layer 설명

### 1. Adapter Layer (Input/Web)

- **위치**: `[domain]/adapter/input/web/`
- **역할**: HTTP 요청/응답 처리, API 엔드포인트 정의
- **의존성 규칙**: UseCase 인터페이스(Port/Input)에만 의존
- **금지사항**:
  - Repository나 JPA Entity에 직접 의존 불가
  - Domain 객체를 직접 반환하거나 받으면 안 됨
- **DTO 사용 규칙**:
  - Controller는 Request DTO를 받아 Command/Query로 변환
  - UseCase의 Result를 Response DTO로 변환하여 반환
- **예외**:
  - **@CurrentMember 어노테이션을 통한 Member 파라미터 수신은 허용**
  - 예: `fun createFolder(@CurrentMember member: Member, ...)`
- **예시**:
  ```kotlin
  @RestController
  @RequestMapping("/api/v1/feeds")
  class FeedController(
      private val createFeedUseCase: CreateFeedUseCase,  // UseCase 인터페이스만 의존
      private val getFeedDetailUseCase: GetFeedDetailUseCase
  ) {
      @PostMapping
      fun createFeed(
          @CurrentMember member: Member,
          @RequestBody request: CreateFeedRequest
      ): ResponseEntity<ApiResponse<CreateFeedResponse>> {
          val command = request.toCommand(member)  // Request → Command 변환
          val result = createFeedUseCase.execute(command)
          return ResponseEntity.ok(ApiResponse(data = CreateFeedResponse.from(result)))
      }
  }
  ```

### 2. Application Layer (Port/UseCase)

- **위치**: `[domain]/application/`
- **역할**: 비즈니스 로직 오케스트레이션, 트랜잭션 관리

#### Port (Input) - UseCase 인터페이스
- **위치**: `[domain]/application/port/input/`
- **역할**: UseCase 인터페이스 정의
- **예시**:
  ```kotlin
  interface CreateFeedUseCase {
      fun execute(command: CreateFeedCommand): CreateFeedResult
  }
  ```

#### Port (Output) - Repository 인터페이스
- **위치**: `[domain]/application/port/output/`
- **역할**: 데이터 접근 인터페이스 정의 (도메인 엔티티 기준)
- **예시**:
  ```kotlin
  interface FeedRepository {
      fun save(feed: Feed): Feed
      fun findById(id: FeedId): Feed?
      fun findAll(pageable: Pageable): Page<Feed>
  }
  ```

#### UseCase 구현체
- **위치**: `[domain]/application/usecase/`
- **역할**: 비즈니스 로직 구현
- **의존성 규칙**: Repository Port(Output)에만 의존
- **예시**:
  ```kotlin
  @Service
  @Transactional
  class CreateFeedUseCaseImpl(
      private val feedRepository: FeedRepository,  // Port/Output 인터페이스만 의존
      private val memberRepository: MemberRepository
  ) : CreateFeedUseCase {
      override fun execute(command: CreateFeedCommand): CreateFeedResult {
          val member = memberRepository.findById(command.memberId)
              ?: throw FeedException.memberNotFound()
          val feed = Feed.create(command, member)
          val savedFeed = feedRepository.save(feed)
          return CreateFeedResult.from(savedFeed)
      }
  }
  ```

#### Application DTO
- **위치**: `[domain]/application/dto/`
- **종류**:
  - **Command**: 쓰기 작업 입력 (CreateFeedCommand, UpdateFeedCommand)
  - **Query**: 읽기 작업 입력 (GetFeedDetailQuery, ListFeedsQuery)
  - **Result**: UseCase 출력 (CreateFeedResult, FeedDetailResult)

### 3. Domain Layer

- **위치**: `[domain]/domain/`
- **역할**: 순수 비즈니스 로직, 핵심 도메인 모델
- **의존성 규칙**: 외부 의존성 없음 (순수 Kotlin 코드)

#### Domain Entity
- **위치**: `[domain]/domain/` 또는 `[domain]/domain/entity/`
- **역할**: 비즈니스 규칙을 포함한 도메인 모델
- **특징**: JPA 어노테이션 없음, 순수 도메인 로직만 포함
- **예시**:
  ```kotlin
  data class Feed(
      val id: FeedId,
      val title: String,
      val content: String,
      val authorId: MemberId,
      val createdAt: LocalDateTime
  ) {
      companion object {
          fun create(command: CreateFeedCommand, author: Member): Feed {
              return Feed(
                  id = FeedId(0),
                  title = command.title,
                  content = command.content,
                  authorId = author.id,
                  createdAt = LocalDateTime.now()
              )
          }
      }
  }
  ```

#### Value Object
- **위치**: `[domain]/domain/vo/`
- **역할**: 불변 값 객체, 도메인 개념의 명시적 표현
- **예시**:
  ```kotlin
  @JvmInline
  value class FeedId(val value: Long)

  @JvmInline
  value class MemberId(val value: String)
  ```

### 4. Adapter Layer (Output/Persistence)

- **위치**: `[domain]/adapter/output/persistence/`
- **역할**: Repository Port 구현, JPA 영속성 처리

#### JPA Entity
- **위치**: `[domain]/adapter/output/persistence/entity/`
- **역할**: JPA 영속성 전용 엔티티
- **특징**: JPA 어노테이션 포함, Domain Entity와 분리
- **예시**:
  ```kotlin
  @Entity
  @Table(name = "feed")
  class FeedJpaEntity(
      @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
      val id: Long = 0,
      val title: String,
      val content: String,
      val authorId: String,
      val createdAt: LocalDateTime
  ) : BaseEntity() {
      fun toDomain(): Feed = Feed(
          id = FeedId(id),
          title = title,
          content = content,
          authorId = MemberId(authorId),
          createdAt = createdAt
      )

      companion object {
          fun from(feed: Feed): FeedJpaEntity = FeedJpaEntity(
              id = feed.id.value,
              title = feed.title,
              content = feed.content,
              authorId = feed.authorId.value,
              createdAt = feed.createdAt
          )
      }
  }
  ```

#### Repository Adapter
- **위치**: `[domain]/adapter/output/persistence/`
- **역할**: Repository Port 구현, JPA Entity ↔ Domain Entity 변환
- **예시**:
  ```kotlin
  @Repository
  class FeedRepositoryAdapter(
      private val feedJpaRepository: FeedJpaRepository
  ) : FeedRepository {
      override fun save(feed: Feed): Feed {
          val entity = FeedJpaEntity.from(feed)
          return feedJpaRepository.save(entity).toDomain()
      }

      override fun findById(id: FeedId): Feed? {
          return feedJpaRepository.findById(id.value).orElse(null)?.toDomain()
      }
  }
  ```

#### JPA Repository
- **위치**: `[domain]/adapter/output/persistence/`
- **역할**: Spring Data JPA 인터페이스
- **예시**:
  ```kotlin
  interface FeedJpaRepository : JpaRepository<FeedJpaEntity, Long> {
      fun findByAuthorId(authorId: String): List<FeedJpaEntity>
  }
  ```

---

## JPA Entity Relationship Rules

### OneToMany 관계 사용 금지
- **절대 OneToMany 관계를 사용하지 마세요**
- 부모 엔티티에서 자식 컬렉션을 관리하는 것은 성능 및 메모리 문제를 야기할 수 있습니다
- 대신 다음 패턴을 사용하세요:
  - 자식 엔티티에서 ManyToOne 관계만 사용
  - 부모 엔티티의 자식들이 필요한 경우, 별도의 Repository 메서드로 조회
  - UseCase 계층에서 부모와 자식을 각각 조회하여 조합

```kotlin
// ❌ 잘못된 예시 - OneToMany 사용
@Entity
class FanNoteJpaEntity {
    @OneToMany(mappedBy = "fanNote")
    val pages: List<FanNotePageJpaEntity> = mutableListOf()
}

// ✅ 올바른 예시 - ManyToOne만 사용
@Entity
class FanNotePageJpaEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fan_note_id")
    var fanNote: FanNoteJpaEntity? = null
}

// UseCase에서 별도 조회
class GetFanNoteDetailUseCaseImpl(
    private val fanNoteRepository: FanNoteRepository,
    private val fanNotePageRepository: FanNotePageRepository
) : GetFanNoteDetailUseCase {
    override fun execute(query: GetFanNoteDetailQuery): FanNoteDetailResult {
        val fanNote = fanNoteRepository.findById(query.fanNoteId)
        val pages = fanNotePageRepository.findByFanNoteId(query.fanNoteId)
        return FanNoteDetailResult.from(fanNote, pages)
    }
}
```

---

## Directory Structure

```
src/main/kotlin/com/example/mykku/
├── [domain]/
│   ├── adapter/
│   │   ├── input/
│   │   │   └── web/
│   │   │       ├── dto/                    # Request/Response DTO
│   │   │       └── *Controller.kt
│   │   └── output/
│   │       └── persistence/
│   │           ├── entity/                 # JPA Entity
│   │           ├── *JpaRepository.kt       # Spring Data JPA
│   │           └── *RepositoryAdapter.kt   # Port 구현체
│   ├── application/
│   │   ├── dto/                            # Command/Query/Result DTO
│   │   ├── port/
│   │   │   ├── input/                      # UseCase 인터페이스
│   │   │   └── output/                     # Repository Port 인터페이스
│   │   └── usecase/                        # UseCase 구현체
│   ├── domain/
│   │   ├── entity/                         # 도메인 엔티티 (일부 도메인)
│   │   ├── vo/                             # Value Object
│   │   └── *.kt                            # 도메인 모델
│   └── exception/                          # 도메인별 예외
├── admin/                                  # Admin 모듈 (기존 구조 유지)
├── common/
│   ├── domain/                             # 공통 Entity 기반 클래스
│   ├── exception/                          # 공통 예외 처리 구조
│   └── util/                               # 공통 유틸리티
├── config/                                 # 설정 클래스
├── email/                                  # 이메일 모듈 (기존 구조 유지)
└── image/                                  # 이미지 업로드
```

---

## Testing Guidelines

### 테스트 위치 및 작성 규칙

#### 1. ControllerTest (통합 테스트)
- **위치**: `src/test/kotlin/com/example/mykku/[domain]/adapter/input/web/`
- **상속**: `BaseControllerTest`를 상속
- **목적**: API 엔드포인트 통합 테스트
- **필수 요소**:
  - RestAssured 기반 테스트
  - 실제 HTTP 요청/응답 검증
  - 인증/인가 시나리오 테스트

#### 2. DocumentTest (API 문서화)
- **위치**: `src/test/kotlin/com/example/mykku/[domain]/adapter/input/web/`
- **상속**: `BaseDocumentTest`를 상속
- **목적**: RestDocs 스니펫 생성 (Asciidoctor로 `index.html` 문서 빌드)
- **필수 요소**:
  - `@MockitoBean`으로 UseCase Mock
  - `document("<prefix>", <상태코드 | "ERROR_CODE">)` 메서드로 스니펫 생성 → `build/generated-snippets/<prefix>/<code>/`
  - `request()`, `response()` DSL(`ApiRequestConfig`)로 파라미터·필드 설명
- **문서(adoc) 반영 필수**:
  - 진입점: `src/docs/asciidoc/index.adoc` (`==` 그룹 헤딩과 문서 속성 담당), API 본문은 `src/docs/asciidoc/api/<도메인>.adoc` partial
  - API 제목·설명은 테스트가 아니라 partial의 `===` 섹션에 작성
  - 성공 케이스: `operation::<prefix>/<status>[snippets='...']` (실제 생성된 스니펫만 나열)
  - 에러 케이스: `==== 에러 응답` 아래 `include::{snippets}/<prefix>/<ERROR_CODE>/http-response.adoc[]`
  - 없는 스니펫/파일을 참조하면 `asciidoctor` 태스크가 실패함 (`./gradlew asciidoctor`로 확인, 결과: `build/docs/asciidoc/index.html`, 배포 경로: `/docs/index.html`)
- **참고**: Admin API (`/admin/**`)는 Document 테스트 작성하지 않음

#### 3. UseCaseTest (단위 테스트)
- **위치**: 필요시 작성
- **어노테이션**: `@ExtendWith(MockitoExtension::class)`
- **목적**: 비즈니스 로직 테스트
- **필수 요소**:
  - `@Mock` Repository Port 주입
  - `@InjectMocks` UseCase 주입
  - 정상 케이스와 예외 케이스 테스트

### 테스트 작성 체크리스트
- [ ] ControllerTest로 엔드포인트 검증
- [ ] DocumentTest로 API 문서 생성 (Admin 제외)
- [ ] 필요시 UseCaseTest로 비즈니스 로직 검증

---

## Exception Handling (도메인별 예외 처리 구조)

### 개요
- 각 도메인이 독립적인 예외 처리 체계를 보유
- 도메인별 예외 클래스, 에러 코드, 핸들러 분리
- 공통 기반 구조를 상속받아 일관성 유지

### 예외 처리 구조
```
common/exception/
├── DomainErrorCode.kt         # 모든 도메인 에러 코드가 구현할 인터페이스
├── BaseException.kt            # 모든 커스텀 예외의 추상 클래스
├── BaseDomainException.kt      # 도메인 예외의 추상 클래스
├── BaseExceptionHandler.kt     # 공통 예외 처리 핸들러
├── CommonErrorCode.kt          # 공통 에러 코드 (Validation 등)
└── CommonException.kt          # 공통 예외 클래스

[domain]/exception/
├── [Domain]ErrorCode.kt        # 도메인별 에러 코드 enum
├── [Domain]Exception.kt        # 도메인별 예외 클래스
└── [Domain]ExceptionHandler.kt # 도메인별 예외 핸들러
```

### 예외 처리 예시
```kotlin
enum class FeedErrorCode(
    override val status: HttpStatus,
    override val message: String
) : DomainErrorCode {
    FEED_NOT_FOUND(HttpStatus.NOT_FOUND, "피드를 찾을 수 없습니다")
}

class FeedException(
    errorCode: FeedErrorCode
) : BaseDomainException(errorCode) {
    companion object {
        fun feedNotFound() = FeedException(FeedErrorCode.FEED_NOT_FOUND)
    }
}

// 사용 예시
throw FeedException.feedNotFound()
```

### Exception Testing Rules

#### 예외 검증 시 ErrorCode Enum 사용 필수
- **테스트에서 예외 검증 시 반드시 ErrorCode enum 사용**
- **예외 메시지 문자열 직접 비교 금지**

```kotlin
// ❌ 잘못된 예시 - 메시지 문자열 직접 비교
val exception = assertThrows<FeedException> {
    getFeedDetailUseCase.execute(query)
}
assertEquals("피드를 찾을 수 없습니다", exception.message)

// ✅ 올바른 예시 - ErrorCode enum 사용
val exception = assertThrows<FeedException> {
    getFeedDetailUseCase.execute(query)
}
assertEquals(FeedErrorCode.FEED_NOT_FOUND, exception.errorCode)
```

---

## Code Style Guidelines

### Code Comments
- **절대 주석을 작성하지 마세요**
- 코드는 자체적으로 명확해야 하며, 주석이 필요하다면 코드를 리팩토링하세요
- 예외: 복잡한 알고리즘의 수학적 설명이 필요한 경우

### Kotlin Conventions
- 함수명과 변수명은 camelCase 사용
- 클래스명은 PascalCase 사용
- 상수는 UPPER_SNAKE_CASE 사용
- 최대 라인 길이: 120자

### Documentation
- REST API는 RestDocs로 문서화
- 테스트 메서드명은 한글로 작성 가능

---

## Common Patterns

### Pagination
- Spring Data의 `Pageable`과 `Page` 사용
- 기본 페이지 크기: 20
- Sort 기본값: createdAt DESC

### Transaction Management
- UseCase 계층에서 `@Transactional` 사용
- 읽기 전용 메서드는 `@Transactional(readOnly = true)` 사용

---

## Clean Code Rules

### 메서드 복잡도 규칙

#### Indent Depth (최대 2단계)
- 한 메서드 내 중첩 깊이는 최대 2까지만 허용
- Early return 패턴 활용

```kotlin
// ❌ 잘못된 예시 - 3중첩
fun processData(data: List<Item>) {
    for (item in data) {
        if (item.isValid()) {
            if (item.needsProcessing()) {
                // 처리 로직
            }
        }
    }
}

// ✅ 올바른 예시 - 최대 2중첩
fun processData(data: List<Item>) {
    for (item in data) {
        if (!item.isValid() || !item.needsProcessing()) continue
        // 처리 로직
    }
}
```

#### Method Length (최대 15줄)
- 모든 메서드는 15줄 이내로 작성
- 복잡한 로직을 여러 작은 메서드로 분할

---

## Development Workflow

1. **새 기능 개발 시**:
   - Domain Layer 설계 (Entity, Value Object)
   - Application Layer 구현 (Port 인터페이스, UseCase)
   - Adapter Layer 구현 (Controller, Repository Adapter)
   - 테스트 작성 (ControllerTest, DocumentTest)

2. **코드 리뷰 체크리스트**:
   - [ ] 의존성 방향이 내부(Domain)를 향하는가
   - [ ] Controller가 UseCase 인터페이스만 의존하는가
   - [ ] JPA Entity와 Domain Entity가 분리되어 있는가
   - [ ] 테스트가 작성되어 있는가
   - [ ] RestDocs 문서화가 되어 있는가

---

## Member ID 사용 규칙

Member는 두 가지 식별자를 가집니다:
- `id: MemberPk` (Long) - 내부 DB PK
- `memberId: String?` - 클라이언트 대면 사용자 ID

### 규칙
- **클라이언트 응답(Response DTO)**: 반드시 `memberId`(String) 사용
- **DB FK, 내부 Command/Query**: `id`(Long PK) 사용
- **`1L.toString()` 등 Long → String 변환 패턴 금지**: 클라이언트에는 실제 String `memberId` 필드를 전달

```kotlin
// ❌ 잘못된 예시 - Long PK를 클라이언트에 노출
data class SomeResponse(
    val memberId: Long  // DB PK 노출
)

// ❌ 잘못된 예시 - Long PK를 String으로 변환
val memberId = member.id.value.toString()

// ✅ 올바른 예시 - String memberId 사용
data class SomeResponse(
    val memberId: String?  // 클라이언트 대면 ID
)
val memberId = member.memberId  // String? 필드 직접 사용
```

### 테스트에서의 적용
```kotlin
// ❌ 잘못된 예시
.body("data.memberId", equalTo(member.id.toInt()))
fieldWithPath("data.memberId").type(JsonFieldType.NUMBER)

// ✅ 올바른 예시
.body("data.memberId", equalTo(member.memberId))
fieldWithPath("data.memberId").type(JsonFieldType.STRING)
```

---

## Important Notes

- **Controller는 UseCase 인터페이스(Port/Input)에만 의존하세요**
- **UseCase는 Repository 인터페이스(Port/Output)에만 의존하세요**
- **JPA Entity와 Domain Entity를 분리하세요**
- **Domain 계층은 외부 의존성이 없어야 합니다**
- **Request/Response DTO는 Adapter Layer에, Command/Query/Result DTO는 Application Layer에 위치합니다**
- **@CurrentMember 어노테이션을 통한 Member 파라미터 수신은 Controller에서 허용됩니다**
- **Admin API는 Document 테스트를 작성하지 않습니다 (Controller 테스트만 작성)**
- **각 도메인은 독립적인 예외 처리 체계를 유지하세요**

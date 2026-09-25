# MyKKU 에러 코드 목록

## 에러 응답 형식

모든 에러는 다음과 같은 구조를 가집니다. 클라이언트 분기는 `message`가 아니라 `code`로 하세요.

```json
{
  "code": "AU201",
  "message": "인증이 필요합니다"
}
```

예외로, 관리자 이벤트 당첨자 저장 API(`PUT /admin/api/v1/events/{eventId}/winners`)의 EV106 응답에는 저장할 수 없는 입력값 목록 `invalidMemberIds`(문자열 배열, 입력 순서, 중복 없음)가 더 붙습니다.

## 에러 코드 체계

| Prefix | 도메인 | 설명 |
|--------|--------|------|
| C | Common | 공통 에러 |
| AD | Admin | 관리자 에러 |
| AU | Auth | 인증 에러 |
| BL | Block | 차단 에러 |
| BO | Board | 게시판 에러 |
| CN | Contest | 콘테스트 에러 |
| DM | DailyMessage | 일상 메시지 에러 |
| EM | Email | 이메일 에러 |
| EV | Event | 이벤트 에러 |
| FN | FanNote | 덕질노트 에러 |
| FD | Feed | 피드 에러 |
| IM | Image | 이미지 에러 |
| LK | Like | 좋아요 에러 |
| MB | Member | 회원 에러 |
| NF | Notification | 알림 에러 |
| PR | Preference | 취향 에러 |
| RL | Role | 칭호 에러 |
| RP | Report | 신고 에러 |

### 번호 체계

- 001-099: NOT_FOUND (리소스 없음)
- 100-199: BAD_REQUEST (잘못된 요청)
- 200-299: UNAUTHORIZED/FORBIDDEN (인증/권한 오류)
- 300-399: CONFLICT (충돌)
- 400-499: INTERNAL_SERVER_ERROR/SERVICE_UNAVAILABLE (서버 오류)
- 500-599: TOO_MANY_REQUESTS (요청 제한)

번호 대역과 HTTP 상태가 다른 코드도 있습니다. 공통(C) 코드는 이 체계를 따르지 않고(예: C201은 404, C202는 405), 도메인 코드 중에서는 EV302·EV404가 404, EV303·EV401~EV403이 400입니다. HTTP 상태는 각 표의 값을 기준으로 하세요.

정의되어 있지만 현재 어떤 API도 반환하지 않는 코드도 있습니다(예: C102, AU401, EM501).

---

## Common (C)

| 코드 | 이름 | HTTP 상태 | 메시지 |
|------|------|-----------|--------|
| C101 | INVALID_INPUT | 400 | 입력값이 올바르지 않습니다 (입력값 검증 실패 시에는 필드별 검증 메시지를 ", "로 이어 붙인 값으로 바뀝니다) |
| C102 | INVALID_SORT_DIRECTION | 400 | 잘못된 정렬 방향입니다. 'asc' 또는 'desc'를 사용해주세요 |
| C103 | INVALID_PAGE_NUMBER | 400 | 페이지 번호는 0 이상이어야 합니다 |
| C104 | INVALID_PAGE_SIZE | 400 | 페이지 크기는 1 이상 1000 이하여야 합니다 |
| C105 | MISSING_REQUEST_PARAMETER | 400 | 필수 요청 값이 누락되었습니다 |
| C106 | INVALID_PARAMETER_TYPE | 400 | 요청 값의 형식이 올바르지 않습니다 |
| C107 | INVALID_MULTIPART_REQUEST | 400 | multipart 요청 형식이 올바르지 않습니다 |
| C201 | ENDPOINT_NOT_FOUND | 404 | 요청한 API를 찾을 수 없습니다 |
| C202 | METHOD_NOT_ALLOWED | 405 | 지원하지 않는 HTTP 메서드입니다 |
| C203 | UNSUPPORTED_MEDIA_TYPE | 415 | 지원하지 않는 Content-Type입니다 |
| C204 | NOT_ACCEPTABLE | 406 | 지원하지 않는 Accept 형식입니다 |
| C205 | PAYLOAD_TOO_LARGE | 413 | 업로드 가능한 파일 크기를 초과했습니다 |
| C301 | RESOURCE_LOCK_CONFLICT | 409 | 다른 요청이 처리 중입니다. 잠시 후 다시 시도해주세요 |
| C302 | DATA_INTEGRITY_VIOLATION | 409 | 이미 존재하는 값이거나 참조할 수 없는 값입니다 |
| C401 | REDIS_CONNECTION_FAILURE | 500 | Redis 연결에 실패했습니다. 잠시 후 다시 시도해주세요 |
| C402 | INTERNAL_SERVER_ERROR | 500 | 서버 오류가 발생했습니다. 관리자에게 문의해주세요 |

## Admin (AD)

| 코드 | 이름 | HTTP 상태 | 메시지 |
|------|------|-----------|--------|
| AD001 | RESOURCE_NOT_FOUND | 404 | 요청한 리소스를 찾을 수 없습니다 |
| AD101 | INVALID_REQUEST | 400 | 잘못된 요청입니다 |
| AD102 | MISSING_REQUIRED_FIELD | 400 | 필수 필드가 누락되었습니다 |
| AD201 | INVALID_TOKEN | 401 | 유효하지 않은 관리자 토큰입니다 |
| AD202 | UNAUTHORIZED | 401 | 관리자 인증이 필요합니다 |
| AD401 | INTERNAL_ERROR | 500 | 서버 내부 오류가 발생했습니다 |

## Auth (AU)

| 코드 | 이름 | HTTP 상태 | 메시지 |
|------|------|-----------|--------|
| AU101 | OAUTH_USER_INFO_FAILED | 400 | 사용자 정보를 가져오는데 실패했습니다 |
| AU102 | MOBILE_LOGIN_NOT_SUPPORTED | 400 | EMAIL 제공자는 모바일 로그인을 지원하지 않습니다 |
| AU201 | UNAUTHORIZED | 401 | 인증이 필요합니다 |
| AU202 | INVALID_TOKEN | 401 | 유효하지 않은 토큰입니다 |
| AU203 | OAUTH_INVALID_TOKEN | 401 | 유효하지 않은 토큰입니다 |
| AU204 | OAUTH_ACCESS_DENIED | 403 | OAuth 접근이 거부되었습니다 |
| AU401 | OAUTH_EXTERNAL_SERVICE_ERROR | 503 | 외부 서비스 오류가 발생했습니다 |
| AU402 | OAUTH_SERVER_ERROR | 503 | OAuth 서버에서 오류가 발생했습니다 |

## Block (BL)

| 코드 | 이름 | HTTP 상태 | 메시지 |
|------|------|-----------|--------|
| BL001 | MEMBER_BLOCK_NOT_FOUND | 404 | 차단 정보를 찾을 수 없습니다 |
| BL002 | MEMBER_TO_BLOCK_NOT_FOUND | 404 | 차단할 사용자를 찾을 수 없습니다 |
| BL003 | KEYWORD_BLOCK_NOT_FOUND | 404 | 키워드 차단 정보를 찾을 수 없습니다 |
| BL101 | CANNOT_BLOCK_SELF | 400 | 자기 자신을 차단할 수 없습니다 |
| BL102 | KEYWORD_TOO_LONG | 400 | 키워드는 50자 이하여야 합니다 |
| BL103 | KEYWORD_EMPTY | 400 | 키워드를 입력해주세요 |
| BL104 | KEYWORD_LIMIT_EXCEEDED | 400 | 키워드는 최대 100개까지 등록할 수 있습니다 |
| BL301 | MEMBER_ALREADY_BLOCKED | 409 | 이미 차단된 사용자입니다 |
| BL302 | KEYWORD_ALREADY_BLOCKED | 409 | 이미 차단된 키워드입니다 |

## Board (BO)

| 코드 | 이름 | HTTP 상태 | 메시지 |
|------|------|-----------|--------|
| BO001 | BOARD_NOT_FOUND | 404 | 게시판을 찾을 수 없습니다 |

## Contest (CN)

| 코드 | 이름 | HTTP 상태 | 메시지 |
|------|------|-----------|--------|
| CN001 | CONTEST_NOT_FOUND | 404 | 콘테스트를 찾을 수 없습니다 |
| CN002 | CONTEST_WINNER_NOT_FOUND | 404 | 콘테스트 수상자를 찾을 수 없습니다 |
| CN003 | PARTICIPATION_NOT_FOUND | 404 | 콘테스트 참여 정보를 찾을 수 없습니다 |
| CN004 | WINNER_ANNOUNCEMENT_NOT_FOUND | 404 | 콘테스트 수상자 발표 공지를 찾을 수 없습니다 |
| CN101 | CONTEST_IMAGE_LIMIT_EXCEEDED | 400 | 콘테스트 이미지는 10개 이하여야 합니다 |
| CN102 | CONTEST_TAG_LIMIT_EXCEEDED | 400 | 콘테스트 태그는 7개 이하여야 합니다 |
| CN104 | TAG_TITLE_TOO_LONG | 400 | 태그는 20자 이하여야 합니다 |
| CN105 | TAG_INVALID_FORMAT | 400 | 태그는 한글, 영문, 숫자만 사용할 수 있습니다 |
| CN106 | INVALID_WINNER_RANK | 400 | 수상 순위는 1, 2, 3 중 하나여야 합니다 |
| CN107 | INVALID_WINNER_COUNT | 400 | 수상자는 1~3명이어야 합니다 |
| CN108 | DUPLICATE_WINNER_RANK | 400 | 중복된 수상 순위가 있습니다 |
| CN109 | PARTICIPATION_NOT_BELONG_TO_CONTEST | 400 | 해당 참여작은 이 콘테스트에 속하지 않습니다 |
| CN110 | CONTEST_NOT_EXPIRED | 400 | 종료되지 않은 콘테스트입니다 |
| CN111 | WINNER_NOT_ANNOUNCED | 400 | 아직 수상자가 발표되지 않았습니다 |
| CN113 | CONTEST_TAG_REQUIRED | 400 | 콘테스트 태그는 1개 이상이어야 합니다 |
| CN114 | DUPLICATE_WINNER_PARTICIPATION | 400 | 같은 참여작을 중복해서 선정할 수 없습니다 |
| CN115 | DUPLICATE_WINNER_MEMBER | 400 | 한 회원은 한 콘테스트에서 한 번만 수상할 수 있습니다 |
| CN201 | NOT_WINNER_OWNER | 403 | 수상 소감을 수정할 권한이 없습니다 |
| CN301 | ALREADY_PARTICIPATED_WITH_FEED | 409 | 이 피드로 이미 참여한 콘테스트입니다 |

## DailyMessage (DM)

| 코드 | 이름 | HTTP 상태 | 메시지 |
|------|------|-----------|--------|
| DM001 | DAILY_MESSAGE_NOT_FOUND | 404 | 일상 메시지를 찾을 수 없습니다 |
| DM002 | DAILY_MESSAGE_COMMENT_NOT_FOUND | 404 | 일상 메시지 댓글을 찾을 수 없습니다 |
| DM101 | DAILY_MESSAGE_CONTENT_TOO_LONG | 400 | 일상 메시지 내용은 42자 이하여야 합니다 |
| DM102 | DAILY_MESSAGE_COMMENT_CONTENT_TOO_LONG | 400 | 일상 메시지 댓글은 1000자 이하여야 합니다 |
| DM103 | REPLY_DEPTH_EXCEEDED | 400 | 답글에는 답글을 작성할 수 없습니다 |
| DM201 | COMMENT_FORBIDDEN_ACCESS | 403 | 댓글에 접근할 권한이 없습니다 |
| DM301 | DAILY_MESSAGE_DATE_ALREADY_EXISTS | 409 | 해당 날짜의 일상 메시지가 이미 존재합니다 |

## Email (EM)

| 코드 | 이름 | HTTP 상태 | 메시지 |
|------|------|-----------|--------|
| EM101 | VERIFICATION_CODE_EXPIRED | 400 | 인증 코드가 만료되었습니다 |
| EM102 | INVALID_VERIFICATION_CODE | 400 | 유효하지 않은 인증 코드입니다 |
| EM201 | INVALID_EMAIL_OR_PASSWORD | 401 | 이메일 또는 비밀번호가 올바르지 않습니다 |
| EM301 | EMAIL_ALREADY_EXISTS | 409 | 이미 사용 중인 이메일입니다 |
| EM401 | EMAIL_SEND_FAILED | 500 | 이메일 발송에 실패했습니다 |
| EM501 | TOO_MANY_REQUESTS | 429 | 너무 많은 요청이 발생했습니다. 잠시 후 다시 시도해주세요 |

## Event (EV)

| 코드 | 이름 | HTTP 상태 | 메시지 |
|------|------|-----------|--------|
| EV001 | EVENT_NOT_FOUND | 404 | 이벤트를 찾을 수 없습니다 |
| EV101 | EVENT_IMAGE_LIMIT_EXCEEDED | 400 | 이벤트 이미지는 최대 10개까지 등록할 수 있습니다 |
| EV102 | EVENT_NOT_ACTIVE | 400 | 진행 중인 이벤트가 아닙니다 |
| EV103 | EVENT_NOT_EXPIRED | 400 | 아직 종료되지 않은 이벤트입니다 |
| EV106 | INVALID_WINNER_MEMBER_IDS | 400 | 당첨자로 지정할 수 없는 회원 아이디가 있습니다 |
| EV301 | ALREADY_PARTICIPATED | 409 | 이미 참여한 이벤트입니다 |
| EV302 | EVENT_PARTICIPATION_NOT_FOUND | 404 | 이벤트 참여 정보를 찾을 수 없습니다 |
| EV303 | PARTICIPATION_NOT_BELONG_TO_EVENT | 400 | 해당 이벤트의 참여 정보가 아닙니다 |
| EV401 | EMPTY_WINNERS | 400 | 당첨자 목록은 비어있을 수 없습니다 |
| EV402 | DUPLICATE_WINNER | 400 | 중복된 당첨자가 있습니다 |
| EV403 | WINNER_NOT_ANNOUNCED | 400 | 아직 당첨자가 발표되지 않았습니다 |
| EV404 | WINNER_ANNOUNCEMENT_NOT_FOUND | 404 | 이벤트 당첨자 발표 공지를 찾을 수 없습니다 |

## FanNote (FN)

| 코드 | 이름 | HTTP 상태 | 메시지 |
|------|------|-----------|--------|
| FN001 | FAN_NOTE_NOT_FOUND | 404 | 덕질노트를 찾을 수 없습니다 |
| FN002 | FAN_NOTE_PAGE_NOT_FOUND | 404 | 덕질노트 페이지를 찾을 수 없습니다 |

## Feed (FD)

| 코드 | 이름 | HTTP 상태 | 메시지 |
|------|------|-----------|--------|
| FD001 | FEED_NOT_FOUND | 404 | 피드를 찾을 수 없습니다 |
| FD002 | FEED_COMMENT_NOT_FOUND | 404 | 피드 댓글을 찾을 수 없습니다 |
| FD101 | FEED_CONTENT_TOO_LONG | 400 | 피드 내용은 1000자 이하여야 합니다 |
| FD102 | FEED_IMAGE_LIMIT_EXCEEDED | 400 | 피드 이미지는 10개 이하여야 합니다 |
| FD103 | FEED_IMAGE_NOT_FOUND | 400 | 삭제할 이미지를 찾을 수 없습니다 |
| FD104 | FEED_TAG_LIMIT_EXCEEDED | 400 | 피드 태그는 7개 이하여야 합니다 |
| FD105 | FEED_COMMENT_CONTENT_TOO_LONG | 400 | 피드 댓글은 1000자 이하여야 합니다 |
| FD106 | TAG_TITLE_TOO_LONG | 400 | 태그는 20자 이하여야 합니다 |
| FD107 | TAG_INVALID_FORMAT | 400 | 태그는 한글, 영문, 숫자만 사용할 수 있습니다 |
| FD108 | IMAGE_INVALID_DIMENSIONS | 400 | 이미지 크기가 유효하지 않습니다 |
| FD109 | SEARCH_KEYWORD_EMPTY | 400 | 검색어를 입력해주세요 |
| FD110 | SEARCH_KEYWORD_TOO_LONG | 400 | 검색어는 50자 이하여야 합니다 |
| FD201 | FEED_FORBIDDEN_ACCESS | 403 | 피드에 접근할 권한이 없습니다 |
| FD202 | FEED_COMMENT_FORBIDDEN_ACCESS | 403 | 피드 댓글에 접근할 권한이 없습니다 |

## Image (IM)

| 코드 | 이름 | HTTP 상태 | 메시지 |
|------|------|-----------|--------|
| IM101 | IMAGE_FILE_EMPTY | 400 | 이미지 파일이 비어있습니다 |
| IM102 | IMAGE_FILE_TOO_LARGE | 400 | 이미지 파일 크기가 너무 큽니다 |
| IM103 | IMAGE_INVALID_FORMAT | 400 | 지원하지 않는 이미지 형식입니다 |
| IM104 | IMAGE_UNREADABLE | 400 | 이미지 파일을 읽을 수 없습니다 |
| IM105 | IMAGE_INVALID_DIMENSIONS | 400 | 이미지 크기가 유효하지 않습니다 |
| IM401 | IMAGE_SIZE_EXTRACTION_FAILED | 500 | 이미지 크기를 추출할 수 없습니다 |
| IM402 | IMAGE_UPLOAD_SERVICE_UNAVAILABLE | 503 | 이미지 업로드 서비스를 사용할 수 없습니다 |

## Like (LK)

| 코드 | 이름 | HTTP 상태 | 메시지 |
|------|------|-----------|--------|
| LK001 | LIKE_BOARD_NOT_FOUND | 404 | 즐겨찾기한 게시판을 찾을 수 없습니다 |
| LK002 | LIKE_FEED_NOT_FOUND | 404 | 좋아요한 피드를 찾을 수 없습니다 |
| LK003 | LIKE_FEED_COMMENT_NOT_FOUND | 404 | 좋아요한 피드 댓글을 찾을 수 없습니다 |
| LK004 | LIKE_DAILY_MESSAGE_COMMENT_NOT_FOUND | 404 | 좋아요한 하루 덕담 댓글을 찾을 수 없습니다 |
| LK101 | LIKE_BOARD_ALREADY_LIKED | 400 | 이미 즐겨찾기한 게시판입니다 |
| LK102 | LIKE_FEED_ALREADY_LIKED | 400 | 이미 좋아요한 피드입니다 |
| LK103 | LIKE_FEED_COMMENT_ALREADY_LIKED | 400 | 이미 좋아요한 피드 댓글입니다 |
| LK104 | LIKE_DAILY_MESSAGE_COMMENT_ALREADY_LIKED | 400 | 이미 좋아요한 하루 덕담 댓글입니다 |

## Member (MB)

| 코드 | 이름 | HTTP 상태 | 메시지 |
|------|------|-----------|--------|
| MB001 | MEMBER_NOT_FOUND | 404 | 회원을 찾을 수 없습니다 |
| MB101 | MEMBER_NICKNAME_TOO_LONG | 400 | 닉네임은 10자 이하여야 합니다 |
| MB102 | MEMBER_NICKNAME_INVALID_FORMAT | 400 | 닉네임은 한글, 영문, 숫자만 사용할 수 있습니다 |
| MB103 | INVALID_CURRENT_PASSWORD | 400 | 현재 비밀번호가 일치하지 않습니다 |
| MB104 | MEMBER_ID_TOO_LONG | 400 | 아이디는 16자 이하여야 합니다 |
| MB105 | MEMBER_ID_INVALID_FORMAT | 400 | 아이디는 영문과 숫자만 사용할 수 있습니다 |
| MB106 | MEMBER_ID_EMPTY | 400 | 아이디를 입력해주세요 |
| MB201 | PROFILE_NOT_COMPLETED | 403 | 아이디와 닉네임을 설정한 후에 이용할 수 있습니다 |
| MB301 | NICKNAME_ALREADY_EXISTS | 409 | 이미 사용 중인 닉네임입니다 |
| MB302 | MEMBER_ID_ALREADY_EXISTS | 409 | 이미 사용 중인 아이디입니다 |

## Notification (NF)

| 코드 | 이름 | HTTP 상태 | 메시지 |
|------|------|-----------|--------|
| NF001 | NOTIFICATION_NOT_FOUND | 404 | 알림을 찾을 수 없습니다 |
| NF002 | FCM_TOKEN_NOT_FOUND | 404 | FCM 토큰을 찾을 수 없습니다 |
| NF003 | NOTIFICATION_SETTING_NOT_FOUND | 404 | 알림 설정을 찾을 수 없습니다 |
| NF101 | FCM_TOKEN_INVALID | 400 | 유효하지 않은 FCM 토큰입니다 |
| NF102 | INVALID_NOTIFICATION_TYPE | 400 | 유효하지 않은 알림 타입입니다 |
| NF201 | NOTIFICATION_NOT_AUTHORIZED | 403 | 알림에 대한 권한이 없습니다 |
| NF401 | FCM_SEND_FAILED | 500 | FCM 알림 발송에 실패했습니다 |

## Preference (PR)

| 코드 | 이름 | HTTP 상태 | 메시지 |
|------|------|-----------|--------|
| PR101 | INVALID_GENRE_TYPE | 400 | 유효하지 않은 장르입니다 |
| PR102 | INVALID_GOODS_TYPE | 400 | 유효하지 않은 굿즈입니다 |
| PR103 | INVALID_MOOD_TYPE | 400 | 유효하지 않은 분위기입니다 |
| PR104 | EMPTY_PREFERENCE_LIST | 400 | 취향 목록이 비어있습니다 |

## Role (RL)

| 코드 | 이름 | HTTP 상태 | 메시지 |
|------|------|-----------|--------|
| RL001 | ROLE_NOT_FOUND | 404 | 칭호를 찾을 수 없습니다 |
| RL002 | MEMBER_ROLE_NOT_FOUND | 404 | 보유하지 않은 칭호입니다 |
| RL101 | ROLE_NAME_DUPLICATE | 400 | 이미 존재하는 칭호 이름입니다 |
| RL102 | ROLE_IN_USE | 400 | 사용 중인 칭호는 삭제할 수 없습니다 |
| RL103 | MEMBER_ROLE_ALREADY_EXISTS | 400 | 이미 보유한 칭호입니다 |
| RL104 | REPRESENTATIVE_ROLE_REQUIRED | 400 | 대표 칭호는 필수입니다 |
| RL105 | ROLE_NAME_IS_AWARD_CONDITION_KEY | 400 | 자동 획득 조건이 걸린 칭호는 이름을 변경하거나 삭제할 수 없습니다 |
| RL201 | MEMBER_ROLE_UNAUTHORIZED | 403 | 해당 칭호에 접근할 권한이 없습니다 |

## Report (RP)

| 코드 | 이름 | HTTP 상태 | 메시지 |
|------|------|-----------|--------|
| RP001 | REPORT_NOT_FOUND | 404 | 신고 내역을 찾을 수 없습니다 |
| RP002 | REPORT_TARGET_NOT_FOUND | 404 | 신고 대상을 찾을 수 없습니다 |
| RP101 | CANNOT_REPORT_OWN_CONTENT | 400 | 자신의 콘텐츠는 신고할 수 없습니다 |
| RP102 | REPORT_DETAIL_REQUIRED | 400 | 기타 사유는 상세 내용을 입력해주세요 |
| RP103 | REPORT_DETAIL_TOO_LONG | 400 | 상세 내용은 500자 이하여야 합니다 |
| RP104 | REPORT_STATUS_NOT_PROCESSABLE | 400 | 처리 상태는 RESOLVED 또는 REJECTED만 가능합니다 |
| RP301 | ALREADY_REPORTED | 409 | 이미 신고한 콘텐츠입니다 |
| RP302 | REPORT_ALREADY_PROCESSED | 409 | 이미 처리된 신고입니다 |

---

## API 엔드포인트

이 문서의 기준은 각 도메인의 `*ErrorCode` enum입니다. 서버에 배포된 에러 코드 목록은 다음 API로 조회할 수 있으며, 사용법은 API 문서의 "에러 코드 API" 절에 있습니다. 공통 에러의 발생 조건은 API 문서 개요의 "공통 에러" 절을 참고하세요.

- `GET /api/error-codes` - 전체 에러 코드 조회 (code 오름차순)
- `GET /api/error-codes/domains` - 도메인 키 목록 조회 (예: `dailyMessage`, `fanNote`)
- `GET /api/error-codes/{domain}` - 특정 도메인의 에러 코드 조회 (없는 도메인 키는 빈 목록)

신고(RP) 코드는 현재 위 API 응답에 포함되어 있지 않습니다.

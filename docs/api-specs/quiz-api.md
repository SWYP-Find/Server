# 퀴즈(Quiz) API 명세

기준 코드:  
`src/main/java/com/swyp/picke/domain/quiz/controller/QuizController.java`  
`src/main/java/com/swyp/picke/domain/admin/controller/AdminQuizController.java`

## 1. 사용자 API

### 1.1 퀴즈 목록
- `GET /api/v1/quizzes`
- 쿼리 파라미터:
  - `page` (기본값: `1`)
  - `size` (기본값: `10`)

### 1.2 퀴즈 상세
- `GET /api/v1/quizzes/{quizId}`

---

## 2. 관리자 API

### 2.1 퀴즈 생성
- `POST /api/v1/admin/quizzes`
- 요청 본문(`AdminQuizCreateRequest`) 주요 필드:
  - `title`
  - `status`
  - `options[]`
    - `label` (`A`, `B`, ...)
    - `text`
    - `detailText`
    - `isCorrect`

### 2.2 퀴즈 목록
- `GET /api/v1/admin/quizzes`
- 쿼리 파라미터:
  - `page`
  - `size`
  - `status` (선택)

### 2.3 퀴즈 상세
- `GET /api/v1/admin/quizzes/{quizId}`

### 2.4 퀴즈 수정
- `PATCH /api/v1/admin/quizzes/{quizId}`
- 요청 본문(`AdminQuizUpdateRequest`) 구조는 생성과 동일

### 2.5 퀴즈 삭제
- `DELETE /api/v1/admin/quizzes/{quizId}`

---

## 3. 필드 정책 메모

- 퀴즈는 태그를 사용하지 않음
- 퀴즈 투표/정답 판정은 Vote API의 `quiz-vote` 경로 사용

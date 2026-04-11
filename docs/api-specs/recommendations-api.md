# 추천(Recommendation) API 명세

기준 코드: `src/main/java/com/swyp/picke/domain/recommendation/controller/RecommendationController.java`

## 1. 흥미 기반 추천

- `GET /api/v1/battles/{battleId}/recommendations/interesting`
- 설명: 특정 배틀 기준으로 흥미 유사 배틀 목록 조회
- 인증 사용자면 개인화 가중치가 적용될 수 있음

### 응답 (`RecommendationListResponse`) 요약
- `items[]`
  - `battleId`
  - `title`
  - `summary`
  - `thumbnailUrl`
  - `tags`
  - `options`

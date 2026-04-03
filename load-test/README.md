# 성능테스트 보고서

> MatchMyDuo의 핵심 병목 개선과 최신 Clean Run 결과만 빠르게 읽을 수 있도록 정리한 문서입니다.
> 초기 `mixed_*`, `party_write_*` 측정은 legacy baseline으로 보존하고, 현재 공식 결과는 `realistic_peak` 반복 측정 기준으로 해석합니다.

## 1. 요약

### 핵심 결과
- `auth_login`의 refresh token 저장 경로를 `MySQL -> Redis`로 전환해 DB write 병목을 제거했다.
- `party_add_members`는 배치 조회 + `saveAll()` 구조로 1차 개선을 진행했고, legacy baseline 기준 `p95 54.50ms -> 24.44ms`로 개선됐다.
- 최신 Clean Run의 `realistic_peak peak1~3`에서는 `party_add_members success count 104`, `success p95 35.76ms / 36.64ms / 29.95ms`, `http_req_failed = 0`를 기록했다.

### 이번 문서의 초점
- `auth_login`, `party_add_members` 병목이 어떻게 줄었는지 정리
- 현재 코드가 `realistic_peak` 혼합 트래픽을 반복 실행에서도 안정적으로 처리하는지 재검증

## 2. 측정 대상과 환경

### 수행 방식
| 구분 | 내용 |
| --- | --- |
| Backend | Host 직접 실행 (`Spring Boot`) |
| Load Generator | Host 직접 실행 (`k6`) |
| Monitoring | Docker 기반 `Prometheus`, `Grafana` |
| Database | Docker 기반 `MySQL` |
| Cache | Docker 기반 `Redis` |
| 대상 URL | `http://localhost:8080` |

### 장비 및 런타임
| 항목 | 내용 |
| --- | --- |
| 장비 | MacBook Pro |
| 칩셋 | Apple M3 Pro |
| CPU 코어 | 11 Core (5 Performance / 6 Efficiency) |
| 메모리 | 18GB |
| OS | macOS 26.4 |
| Java | 21.0.7 |
| Spring Boot | 3.5.8 |
| MySQL | 8.0 |
| Redis | `redis:latest` |
| k6 | 1.4.2 |
| Prometheus | `prom/prometheus:latest` |
| Grafana | `grafana/grafana:latest` |
| Docker | 28.3.2 |

### 현재 공식 시나리오
- 공식 결과는 `realistic_peak` 기준으로 정리한다.
- `realistic_soak`, `party_write_contention`은 현재 runner에 포함돼 있지만 본문에서는 다루지 않는다.
- 핵심 검증 기준은 아래 3개만 사용한다.
  - `party_add_members success p95 < 40ms`
  - `party_add_members success count >= 100`
  - `http_req_failed < 1%`

## 3. 개선 내용

### 3.1 `auth_login`
- 문제: refresh token 저장을 MySQL upsert로 처리하면서 불필요한 DB write 비용이 발생했다.
- 해결: refresh token 저장소를 Redis로 전환했다.
- 결과: 기존 측정 기준 해당 구간은 `2~5ms` 수준이었고, Redis 전환 이후 `0.003~0.016ms` 수준으로 감소했다.

### 3.2 `party_add_members`
- 문제: 초대 대상별 가입 여부 조회, 유저 조회, 저장이 반복돼 요청당 쿼리 수와 라운드 트립이 늘어나는 구조였다.
- 해결:
  - `findAllByPartyIdAndUserIdIn(...)`, `findAllById(...)`, `saveAll(...)` 기반 배치 처리 구조로 변경
  - preset 기반 realistic runner 도입
  - write-bank seed/reset 자동화
  - `Party.capacity`, `Party.joinedMemberCount` 도입
  - `countByPartyIdAndState()` 제거
  - `Post` 상태 변경 bulk update 전환
  - target user 조회 1회 통합
  - 응답 DTO 생성 시 lazy association 접근 제거
- 결과:
  - 1차 개선: legacy baseline 기준 `p95 54.50ms -> 24.44ms`
  - 2차 검증: 최신 Clean Run에서 `success p95 35.76ms / 36.64ms / 29.95ms`

### 3.3 Legacy Baseline 메모
- `auth_login` legacy 재측정: `auth_high_rerun p95 112.42ms`
- `party_add_members` legacy mixed 기준: `mixed_high_rerun p95 24.44ms`
- 그 외 `posts/chat/party_members`, `mixed_stress`, `party_write_stress` 상세 기록은 기존 run 결과로 보존하고, 현재 문서에서는 핵심 병목 2개만 남긴다.

## 4. 최신 Clean Run 결과

- `baseline` run은 워밍업 실행으로 제외했고, 공식 결과는 `peak1~3`만 사용했다.

| Run ID | `party_add_members` success count | `party_add_members` success p95 | `auth_login` p95 | `http_req_failed` |
| --- | ---: | ---: | ---: | ---: |
| `realistic_peak_peak1_20260402_181427` | 104 | 35.76ms | 139.81ms | 0 |
| `realistic_peak_peak2_20260402_182125` | 104 | 36.64ms | 111.56ms | 0 |
| `realistic_peak_peak3_20260402_210300` | 104 | 29.95ms | 109.28ms | 0 |

### 결과 해석
- 세 번 모두 `party_add_members success count >= 100`을 만족했다.
- 세 번 모두 `party_add_members success p95 < 40ms`를 만족했다.
- 세 번 모두 `http_req_failed = 0`, `dropped_iterations = 0`이었다.
- `party_add_members success p95` 평균은 `34.12ms`였다.
- `posts_list`, `chat_rooms`, `chat_messages`, `party_members`도 모두 threshold 안쪽에서 안정적으로 유지됐다.

### 의미
- 이 결과는 단일 실행에서 우연히 빠르게 나온 수치가 아니라, `realistic_peak` 혼합 트래픽 기준에서 반복 측정해도 재현되는 결과라는 데 의미가 있다.
- 즉 현재 구조는 `write target 부족`, `seed 불일치`, `표본 부족` 문제를 제거한 뒤에도 `party_add_members`를 안정적으로 처리하는 수준까지 올라왔다고 해석할 수 있다.

## 5. 한계와 해석

### 시각 자료
#### `party_add_members` p95 개선
![party_add_members p95 비교](https://github.com/HongRae-Kim/WEB7_9_FinalScreening_BE/raw/main/load-test/images/party-add-members-p95.svg)

> `party_add_members`는 배치 조회 + `saveAll()` 구조로 1차 개선을 적용한 뒤 legacy mixed 기준 p95를 `54.50ms -> 20.01ms`까지 줄였다.

#### `auth_login` 단계별 비용 변화
![auth_login 단계별 비용 요약](https://github.com/HongRae-Kim/WEB7_9_FinalScreening_BE/raw/main/load-test/images/auth-login-stage-summary.svg)

> refresh token 저장 구조를 Redis로 전환해 DB write 병목을 제거했고, 이후 남은 주요 비용이 `BCrypt` 검증 구간임을 확인했다.

### 해석 시 주의점
- 본 결과는 로컬 환경 기반 측정이므로 운영 환경의 절대 성능 수치로 직접 일반화하기는 어렵다.
- legacy baseline의 `mixed_*`, `party_write_*` 결과와 현재 `realistic_*` preset은 실행 체계가 다르므로 절대값을 1:1로 단순 비교하면 안 된다.
- Grafana 패널은 추세 확인용이고, 최종 수치는 동일 run의 `k6 summary-export JSON`을 기준으로 해석했다.

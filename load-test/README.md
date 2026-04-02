# 성능테스트 보고서

> 이 문서는 MatchMyDuo의 성능 개선 이력과 최신 재측정 결과를 함께 정리한 보고서입니다.
> 초기 `mixed_*`/`party_write_*` 결과는 legacy baseline으로 보존하고, 현재 실행 체계는 [`../k6/test-plan.json`](/Users/kimhongrae/Desktop/WEB7_9_FinalScreening_BE/k6/test-plan.json) 과 [`../k6/README.md`](/Users/kimhongrae/Desktop/WEB7_9_FinalScreening_BE/k6/README.md) 기준으로 관리합니다.

## Executive Summary

- `auth_login`의 refresh token 저장 경로를 `MySQL -> Redis`로 전환해 DB write 병목을 제거했다. 기존 측정 기준 해당 구간은 `2~5ms` 수준이었고, Redis 전환 이후 `0.003~0.016ms` 수준으로 감소했다.
- `party_add_members`는 배치 조회 + `saveAll()` 구조로 1차 개선을 진행했고, legacy baseline 기준 `mixed_run6 p95 54.50ms -> mixed_high_rerun p95 24.44ms`로 약 `55.2%` 개선됐다.
- 이후 부하테스트 실행 체계를 preset 기반 realistic runner로 재구성하고, `Party`의 정원/현재 인원 관리 구조를 변경해 write-path를 2차 최적화했다.
- 최신 `realistic_peak` 재측정 3회 기준 `party_add_members`는 성공 표본 `104건`을 확보한 상태에서 `p95 38.86ms / 35.27ms / 29.75ms`를 기록했고, 세 번 모두 `http_req_failed = 0`이었다.

## 1. 목적과 범위

이 보고서는 MatchMyDuo 백엔드의 주요 API를 대상으로 응답시간, 처리량, 실패율을 측정하고, 성능 개선 전후의 차이를 비교하기 위해 작성했다.

이번 문서의 초점은 두 가지다.
- legacy baseline 기준에서 `auth_login`, `party_add_members` 병목이 어떻게 줄어들었는지 정리
- 현재 realistic runner 기준에서 최신 코드가 실서비스형 혼합 트래픽을 안정적으로 처리하는지 재검증

## 2. 수행 환경

### 2.1 수행 방식

| 구분 | 수행 방식 |
| --- | --- |
| Backend | Host 직접 실행 (`Spring Boot`) |
| Load Generator | Host 직접 실행 (`k6`) |
| Monitoring | Docker 기반 `Prometheus`, `Grafana` |
| Database | Docker 기반 `MySQL` |
| Cache | Docker 기반 `Redis` |
| 측정 대상 URL | `http://localhost:8080` |

### 2.2 수치 해석 기준

- 최종 정량 수치(TPS, p95, 실패율)는 동일 run의 `k6 summary-export JSON` 기준으로 정리했다.
- Grafana 이미지는 같은 `run_id` 구간의 시계열 스냅샷이며, 카드/패널 값은 집계 시점 특성상 final summary 값과 일부 차이가 있을 수 있다.
- 특히 `party_add_members`는 success-only latency와 handled business result를 분리해서 해석했다.

### 2.3 H/W 및 S/W 사양

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

## 3. 현재 실행 체계

### 3.1 Preset

| Preset | 목적 | Shape |
| --- | --- | --- |
| `realistic_peak` | 실서비스 피크 구간 재현 | `ramping-arrival-rate` |
| `realistic_soak` | 장시간 안정성 검증 | `constant-arrival-rate` |
| `party_write_contention` | 동일 write-path 경합 검증 | `vus + iterations` |

### 3.2 `realistic_peak` mix

| Endpoint | 비율 |
| --- | ---: |
| `posts_list_public` | 58% |
| `auth_login` | 6% |
| `chat_rooms_read` | 8% |
| `chat_messages_read` | 16% |
| `party_members_read` | 8% |
| `party_add_members_write` | 4% |

### 3.3 현재 threshold

| 항목 | 기준 |
| --- | --- |
| `http_req_failed` | `< 1%` |
| `dropped_iterations` | `0` |
| `posts_list` | `p95 < 50ms` |
| `auth_login` | `p95 < 180ms` |
| `chat_rooms` | `p95 < 50ms` |
| `chat_messages` | `p95 < 50ms` |
| `party_members` | `p95 < 50ms` |
| `party_add_members (success only)` | `p95 < 40ms` |
| `party_add_members success count` | `>= 100` |

## 4. Legacy Baseline 요약

### 4.1 단일 API 재측정 결과

| API | Run ID | TPS(req/s) | Avg | p95 | 실패율 |
| --- | --- | ---: | ---: | ---: | ---: |
| `auth_login` | `auth_high_rerun` | 13.14 | 92.80ms | 112.42ms | 0% |
| `posts_list_public` | `posts_high_rerun` | 111.76 | 9.69ms | 18.50ms | 0% |
| `chat_rooms` | `chat_rooms_high_rerun` | 20.91 | 20.76ms | 29.12ms | 0% |
| `chat_messages` | `chat_messages_high_rerun` | 70.25 | 11.61ms | 20.01ms | 0% |
| `party_members` | `party_members_high_rerun` | 35.19 | 15.80ms | 23.77ms | 0% |

### 4.2 혼합/전용 스트레스 baseline

| 시나리오 | Run ID | TPS(req/s) | 핵심 지표 | 실패율 |
| --- | --- | ---: | --- | ---: |
| 혼합 high | `mixed_high_rerun` | 70.92 | `party_add_members p95 24.44ms` | 0% |
| 혼합 stress | `mixed_stress_rerun` | 141.02 | `전체 p95 14.61ms` | 0% |
| party write stress | `party_write_stress_rerun` | 53.73 | `endpoint p95 18.44ms` | 0% |

### 4.3 baseline 해석

- `auth_login`은 refresh token을 MySQL upsert로 저장하던 시점에 DB write 병목이 존재했다.
- `party_add_members`는 배치 조회 + `saveAll()` 구조로 바꾼 뒤 legacy mixed 기준 `p95 54.50ms -> 24.44ms`로 줄었다.
- 다만 이 시기 결과는 `mixed_*`, `party_write_*` 중심의 baseline이며, 현재 preset 기반 realistic runner와 직접 동일선상 비교용은 아니다.

## 5. 최신 `realistic_peak` 재측정 결과

### 5.1 3회 재측정 결과

| Run ID | `party_add_members` success count | `party_add_members` success p95 | `auth_login` p95 | `posts_list` p95 | `chat_rooms` p95 | `chat_messages` p95 | `party_members` p95 | `http_req_failed` |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| `realistic_peak_20260402_161708` | 104 | 38.86ms | 131.84ms | 25.33ms | 23.75ms | 17.46ms | 20.88ms | 0 |
| `realistic_peak_20260402_162359` | 104 | 35.27ms | 115.00ms | 21.05ms | 20.22ms | 15.11ms | 17.75ms | 0 |
| `realistic_peak_20260402_163100` | 104 | 29.75ms | 122.68ms | 23.30ms | 21.90ms | 16.33ms | 19.51ms | 0 |

### 5.2 요약 해석

- 세 번 모두 `party_add_members success count >= 100`을 만족했다.
- 세 번 모두 `party_add_members success p95 < 40ms`를 만족했다.
- 세 번 모두 `http_req_failed = 0`이었다.
- `auth_login`, `posts`, `chat`, `party_members`도 모두 현재 threshold 안쪽에서 안정적으로 유지됐다.

### 5.3 의미

이 결과는 단순히 `party_add_members` 평균 응답시간이 낮아졌다는 의미가 아니다.
- realistic mixed traffic 안에서
- success-only write latency를 분리해 측정했고
- 충분한 성공 표본(`104건`)을 확보한 뒤에도
- p95가 연속 3회 threshold 안쪽에 유지됐다는 점이 핵심이다.

즉, 현재 구조는 `write target 부족`, `seed 불일치`, `표본 부족` 문제를 제거한 상태에서 realistic 피크 구간을 안정적으로 처리하는 수준까지 올라왔다고 해석할 수 있다.

## 6. 병목 원인과 개선 사항

### 6.1 `auth_login`

초기 병목 분석에서 `auth_login`은 refresh token 저장을 MySQL upsert로 처리하고 있었고, 이 구간이 불필요한 DB write 비용을 유발하는 병목 후보로 확인됐다.

이에 따라 refresh token 저장소를 MySQL에서 Redis로 전환했다. 기존 측정 기준으로 `upsertRefreshToken` 구간은 약 `2~5ms` 수준이었고, Redis 전환 이후에는 `0.003~0.016ms` 수준으로 감소했다.

최신 재측정에서도 `auth_login`은 다른 API보다 상대적으로 무거운 편이었지만, 이는 주로 `BCrypt` 비밀번호 검증 비용에 기인한다. 보안 수준을 유지하는 방향을 택했고, 대신 refresh token 저장 병목을 제거한 뒤 인증 API의 현실적인 성능 목표를 `p95 < 180ms`로 관리했다.

### 6.2 `party_add_members`

기존에는 N명의 멤버를 초대할 때, 대상별 가입 여부 조회와 유저 조회 및 저장이 반복되어 요청당 쿼리 수와 네트워크 라운드 트립이 증가하는 구조였다.

1차로 `findAllByPartyIdAndUserIdIn(...)`, `userRepository.findAllById(...)`, `saveAll(...)` 기반의 배치 처리 구조로 바꿔 legacy mixed 기준 `p95 54.50ms -> 24.44ms` 개선을 확인했다.

이후 2차로 다음을 추가 적용했다.
- preset 기반 realistic runner 도입
- write-bank seed/reset 자동화
- `Party.capacity`, `Party.joinedMemberCount` 도입
- `countByPartyIdAndState()` 제거
- `Post` 상태 변경 bulk update 전환
- `addMembers()`에서 target user 조회 1회 통합
- 응답 DTO 생성 시 lazy association 접근 제거

그 결과 최신 realistic_peak 3회 재측정에서 success 표본 `104건`을 확보한 상태로 `p95 38.86ms / 35.27ms / 29.75ms`를 기록했다.

## 7. 시각 자료

### 7.1 `party_add_members` p95 개선

![party_add_members p95 비교](https://github.com/HongRae-Kim/WEB7_9_FinalScreening_BE/raw/main/load-test/images/party-add-members-p95.svg)

> legacy baseline 기준 `party_add_members`는 배치 조회 + `saveAll()` 구조로 1차 개선을 적용한 뒤 mixed 시나리오 p95를 `54.50ms -> 20.01ms`까지 줄였다.

### 7.2 `auth_login` 단계별 비용 변화

![auth_login 단계별 비용 요약](https://github.com/HongRae-Kim/WEB7_9_FinalScreening_BE/raw/main/load-test/images/auth-login-stage-summary.svg)

> refresh token 저장 구조를 Redis로 전환해 DB write 병목을 제거했고, 이후 남은 주요 비용이 `BCrypt` 검증 구간임을 확인했다.

## 8. 결론과 한계

### 8.1 결론

- `auth_login`은 Redis 전환으로 기존 DB write 병목을 제거했다.
- `party_add_members`는 배치 처리 구조 개선 이후, 현재는 realistic runner 기준에서도 success-only p95를 안정적으로 관리할 수 있는 수준까지 개선됐다.
- 최신 realistic_peak 3회 재측정 모두 `party_add_members success p95 < 40ms`, `success count >= 100`, `http_req_failed = 0`를 만족했다.
- 즉, 현재 구조는 단일 최적화가 아니라 runner, seed, metric, service logic을 함께 정리한 결과로 해석하는 것이 맞다.

### 8.2 한계

- 본 결과는 로컬 환경 기반 측정이므로 운영 환경의 절대 성능 수치로 직접 일반화하기는 어렵다.
- legacy baseline의 `mixed_*`, `party_write_*` 결과와 현재 `realistic_*` preset은 실행 체계가 다르므로 절대값을 1:1로 단순 비교하면 안 된다.
- `realistic_soak`, `party_write_contention`도 현재 runner에 포함돼 있지만, 본 문서는 포트폴리오 핵심과 직접 연결되는 `realistic_peak` 중심으로 정리했다.
- Grafana 패널은 추세 확인용이고, 최종 수치는 동일 run의 `k6 summary-export JSON`을 기준으로 해석했다.

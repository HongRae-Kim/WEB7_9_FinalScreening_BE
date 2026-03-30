# 성능테스트 보고서

## Executive Summary

- 초기 병목 후보였던 `auth_login`의 refresh token 저장 경로를 `MySQL -> Redis`로 전환해 DB write 비용을 제거했다. 기존 측정 기준 해당 구간은 `2~5ms` 수준이었고, Redis 전환 이후 `0.003~0.016ms` 수준으로 감소해 약 `99% 이상` 개선됐다.
- `party_add_members`는 배치 조회 및 `saveAll()` 기반 구조로 개선한 뒤, 기존 `mixed_run6` 기준 `p95 54.50ms`에서 최신 `mixed_high_rerun` 기준 `p95 24.44ms`로 약 `55.2%` 개선됐다.
- 최신 재측정 기준 혼합 시나리오 스트레스 테스트(`mixed_stress_rerun`)에서는 `141.02 req/s`, 전체 `p95 14.61ms`, 실패율 `0%`를 기록했다.
- `party_add_members` 전용 스트레스 테스트(`party_write_stress_rerun`)에서도 endpoint `p95 18.44ms`, 실패율 `0%`를 유지해 write-path 안정성을 확인했다.

## 1. 성능테스트 개요

### 1.1 목적
본 테스트는 MatchMyDuo 백엔드의 주요 API를 대상으로 응답시간, 처리량, 실패율을 측정하고, 기존 병목 개선 이후 최신 코드 기준 성능을 재검증하기 위해 수행했다.  
특히 초기 병목 후보였던 `auth_login`의 refresh token 저장 경로 개선과, `party` 도메인 2차 수정 이후의 안정성을 함께 확인하는 데 초점을 두었다.

### 1.2 대상 API
- `auth_login`
- `posts_list_public`
- `chat_rooms`
- `chat_messages`
- `party_members`
- `party_add_members`

### 1.3 검증 기준
| 항목 | 기준 |
| --- | --- |
| `auth_login` | `p95 < 700ms` |
| `posts_list_public` | `p95 < 800ms`, `p99 < 1500ms` |
| `chat_rooms` | `p95 < 900ms` |
| `chat_messages` | `p95 < 1000ms` |
| `party_members` | `p95 < 800ms` |
| `party_add_members` | `p95 < 1200ms` |
| 전체 실패율 | `http_req_failed < 5%` |

## 2. 수행 환경

### 2.1 수행 방식
본 테스트는 로컬 환경에서 수행했다.

| 구분 | 수행 방식 |
| --- | --- |
| Backend | Host 직접 실행 (`Spring Boot`) |
| Load Generator | Host 직접 실행 (`k6`) |
| Monitoring | Docker 기반 `Prometheus`, `Grafana` |
| Database | Docker 기반 `MySQL` |
| Cache | Docker 기반 `Redis` |
| 측정 대상 URL | `http://localhost:8080` |

### 2.2 측정 기준
초기 테스트 과정에서 Docker bridge 경로(`host.docker.internal`)에서 간헐적인 timeout이 관찰되었다.  
따라서 최종 재측정 결과는 `localhost:8080` 기준 host 직접 실행 결과를 기준으로 정리했다.

### 2.3 수치 해석 기준
본 보고서의 정량 수치(TPS, p95, 실패율)는 동일 run의 `k6 summary-export JSON` 기준으로 정리했다.  
Grafana 이미지는 같은 `run_id`의 스냅샷으로 첨부했으며, 상단 카드와 endpoint 표는 시계열 집계 특성상 final summary 값과 일부 차이가 있을 수 있다.

## 3. H/W 및 S/W 사양

### 3.1 H/W 사양
| 항목 | 내용 |
| --- | --- |
| 장비 | MacBook Pro |
| 칩셋 | Apple M3 Pro |
| CPU 코어 | 11 Core (5 Performance / 6 Efficiency) |
| 메모리 | 18GB |
| OS | macOS 26.4 |

### 3.2 S/W 사양
| 항목 | 버전 |
| --- | --- |
| Java | 21.0.7 |
| Spring Boot | 3.5.8 |
| Gradle Dependency Management | 1.1.7 |
| MySQL | 8.0 |
| Redis | `redis:latest` |
| k6 | 1.4.2 |
| Prometheus | `prom/prometheus:latest` |
| Grafana | `grafana/grafana:latest` |
| Docker | 28.3.2 |

## 4. 테스트 시나리오

### 4.1 단일 API 테스트
| 시나리오 | 목적 | 부하 조건 |
| --- | --- | --- |
| `auth_high_rerun` | 로그인 API 단독 성능 측정 | `TEST_MODE=auth`, `LOAD_PROFILE=high` |
| `posts_high_rerun` | 게시글 목록 조회 성능 측정 | `TEST_MODE=posts`, `LOAD_PROFILE=high` |
| `chat_rooms_high_rerun` | 채팅방 목록 조회 성능 측정 | `TEST_MODE=chat_rooms`, `LOAD_PROFILE=high` |
| `chat_messages_high_rerun` | 채팅 메시지 조회 성능 측정 | `TEST_MODE=chat_messages`, `LOAD_PROFILE=high` |
| `party_members_high_rerun` | 파티 멤버 조회 성능 측정 | `TEST_MODE=party_members`, `LOAD_PROFILE=high` |

### 4.2 혼합 시나리오 테스트
| 시나리오 | 목적 | 부하 조건 |
| --- | --- | --- |
| `mixed_high_rerun` | 일반 혼합 부하 기준 성능 검증 | `./run.sh mixed high mixed_high_rerun` |
| `mixed_stress_rerun` | 고부하 혼합 시나리오 안정성 검증 | `./run.sh mixed stress mixed_stress_rerun` |

### 4.3 전용 스트레스 테스트
| 시나리오 | 목적 | 부하 조건 |
| --- | --- | --- |
| `party_write_stress_rerun` | `party_add_members` 전용 스트레스 검증 | `./run.sh party stress party_write_stress_rerun` |

## 5. 테스트 결과 요약

### 5.1 단일 API 재측정 결과
| API | Run ID | TPS(req/s) | Avg | p95 | 실패율 |
| --- | --- | ---: | ---: | ---: | ---: |
| `auth_login` | `auth_high_rerun` | 13.14 | 92.80ms | 112.42ms | 0% |
| `posts_list_public` | `posts_high_rerun` | 111.76 | 9.69ms | 18.50ms | 0% |
| `chat_rooms` | `chat_rooms_high_rerun` | 20.91 | 20.76ms | 29.12ms | 0% |
| `chat_messages` | `chat_messages_high_rerun` | 70.25 | 11.61ms | 20.01ms | 0% |
| `party_members` | `party_members_high_rerun` | 35.19 | 15.80ms | 23.77ms | 0% |

### 5.2 혼합 시나리오 재측정 결과
| 조건 | Run ID | TPS(req/s) | 전체 avg | 전체 p95 | auth p95 | party_add_members p95 | 실패율 | dropped_iterations |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| 혼합 시나리오 부하 테스트 | `mixed_high_rerun` | 70.92 | 11.80ms | 21.38ms | 138.46ms | 24.44ms | 0% | 55 |
| 혼합 시나리오 스트레스 테스트 | `mixed_stress_rerun` | 141.02 | 7.14ms | 14.61ms | 108.80ms | 12.46ms | 0% | 100 |

### 5.3 `party_add_members` 전용 스트레스 결과
| 조건 | Run ID | TPS(req/s) | endpoint avg | endpoint p95 | scenario p95 | 실패율 | iterations |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| `60 VUs / 4800 iterations` | `party_write_stress_rerun` | 53.73 | 13.30ms | 18.44ms | 21.08ms | 0% | 4800 |

### 5.4 요약 해석
- 모든 재측정 결과는 설정한 threshold를 충족했다.
- 초기 병목 후보였던 `auth_login`은 refresh token 저장 경로를 `MySQL -> Redis`로 전환한 이후 DB write 비용이 제거됐고, 최신 재측정에서도 임계치 내에서 안정적으로 동작했다.
- 읽기 API(`posts`, `chat`, `party_members`)는 전반적으로 낮은 지연시간을 유지했다.
- `party_add_members`는 혼합 시나리오와 전용 스트레스 시나리오 모두에서 낮은 p95와 0% 실패율을 유지했다.
- 일부 혼합 시나리오에서는 `dropped_iterations`가 관찰되었으나, 이는 실패 요청이 아니라 목표 부하를 순간적으로 모두 소화하지 못한 현상으로 해석할 수 있으며, 실패율 증가나 임계치 초과로 이어지지는 않았다.

## 6. 성능테스트 상세

### 6.1 단일 API 상세

#### 6.1.1 `auth_login`
- Avg: `92.80ms`
- p95: `112.42ms`
- TPS: `13.14 req/s`
- 실패율: `0%`

해석:
- 로그인 API는 다른 API 대비 CPU 연산 비용이 큰 편이지만, 임계치 내에서 안정적으로 동작했다.
- 현재 남은 주요 비용은 `BCrypt` 비밀번호 검증 비용으로 해석하는 것이 적절하다.

#### 6.1.2 `posts_list_public`
- Avg: `9.69ms`
- p95: `18.50ms`
- TPS: `111.76 req/s`
- 실패율: `0%`

#### 6.1.3 `chat_rooms`
- Avg: `20.76ms`
- p95: `29.12ms`
- TPS: `20.91 req/s`
- 실패율: `0%`

#### 6.1.4 `chat_messages`
- Avg: `11.61ms`
- p95: `20.01ms`
- TPS: `70.25 req/s`
- 실패율: `0%`

#### 6.1.5 `party_members`
- Avg: `15.80ms`
- p95: `23.77ms`
- TPS: `35.19 req/s`
- 실패율: `0%`

### 6.2 혼합 시나리오 부하 테스트 (`mixed_high_rerun`)
- 전체 TPS: `70.92 req/s`
- 전체 avg: `11.80ms`
- 전체 p95: `21.38ms`
- `auth_login p95`: `138.46ms`
- `party_add_members p95`: `24.44ms`
- 실패율: `0%`
- `dropped_iterations=55`

해석:
- 일반 혼합 부하 기준에서 모든 API가 threshold 안쪽에서 안정적으로 응답했다.
- `auth_login`이 가장 무거운 API였지만 병목으로 보기 어려운 수준이었다.
- `party_add_members`도 혼합 시나리오에서 낮은 지연을 유지했다.

![혼합 시나리오 부하 테스트 대시보드](docs/mixed_high_rerun.png)

캡션:
- `mixed_high_rerun` Grafana 대시보드 스냅샷
- 정량 수치는 동일 run의 k6 summary-export JSON 기준으로 정리했다.

### 6.3 혼합 시나리오 스트레스 테스트 (`mixed_stress_rerun`)
- 전체 TPS: `141.02 req/s`
- 전체 avg: `7.14ms`
- 전체 p95: `14.61ms`
- `auth_login p95`: `108.80ms`
- `party_add_members p95`: `12.46ms`
- 실패율: `0%`
- `dropped_iterations=100`

해석:
- 고부하 혼합 시나리오에서도 실패율 0%를 유지했다.
- `dropped_iterations`는 증가했지만, 응답시간과 실패율은 안정적으로 유지됐다.
- `party_add_members`는 혼합 스트레스 조건에서도 낮은 지연을 유지했다.

![혼합 시나리오 스트레스 테스트 대시보드](docs/mixed_stress_rerun.png)

캡션:
- `mixed_stress_rerun` Grafana 대시보드 스냅샷
- `party_add_members`의 `400` 응답은 시나리오상 기대된 handled business response로 해석했다.

### 6.4 `party_add_members` 전용 스트레스 테스트 (`party_write_stress_rerun`)
- TPS: `53.73 req/s`
- endpoint avg: `13.30ms`
- endpoint p95: `18.44ms`
- scenario p95: `21.08ms`
- 실패율: `0%`
- iterations: `4800`

해석:
- `party_add_members`는 전용 스트레스 환경에서도 낮은 지연과 0% 실패율을 유지했다.
- 현재 시나리오는 동일 파티와 동일 초대 대상을 반복 호출하는 구조이므로, 본 결과는 success-only insert latency보다 `write-path handled latency`와 안정성 검증에 가깝게 해석해야 한다.
- 대시보드에 보이는 `400` 응답은 시스템 실패가 아니라 기대된 handled business response다.

![party_add_members 전용 스트레스 테스트 대시보드](docs/party_write_stress_rerun.png)

캡션:
- `party_write_stress_rerun` Grafana 대시보드 스냅샷 (`scenario=party_add_members_write`, `endpoint=party_add_members`)
- 동일 파티/대상 유저 조합을 반복 호출하므로 `400` 응답은 시스템 오류가 아니라 handled business response로 해석했다.

### 6.5 Resource 지표 해석
본 보고서의 정량 수치는 `k6` summary-export 기준의 TPS, 응답시간, 실패율, `dropped_iterations` 중심으로 정리했다.  
CPU, Memory, JVM Heap, GC 등 리소스 지표는 Grafana 패널을 통해 추세 확인용으로 병행 검토했으며, 로컬 환경 특성상 절대값보다 실행 간 비교와 이상 징후 유무 확인에 의미를 두었다.

## 7. 병목 원인 및 개선 사항

### 7.1 기존 병목 개선

#### `auth_login`
초기 병목 분석에서 `auth_login`은 refresh token 저장을 MySQL upsert로 처리하고 있었고, 이 구간이 불필요한 DB write 비용을 유발하는 병목 후보로 확인됐다.

이에 따라 refresh token 저장소를 MySQL에서 Redis로 전환했다. 기존 측정 기준으로 `upsertRefreshToken` 구간은 약 `2~5ms` 수준이었고, Redis 전환 이후에는 `0.003~0.016ms` 수준으로 감소했다.

최신 재측정에서도 `auth_login`은 다른 API보다 상대적으로 무거운 편이었지만, 이는 주로 `BCrypt` 비밀번호 검증 비용에 기인한다.  
여기서 Cost Factor를 낮추면 응답시간은 더 줄일 수 있지만, 비밀번호 대입 공격에 대한 저항성도 함께 낮아질 수 있다. 따라서 본 프로젝트에서는 보안 수준을 유지하는 방향을 택했고, 대신 refresh token 저장 병목을 제거한 뒤 인증 API의 현실적인 성능 목표를 `p95 < 700ms`로 관리했다.

#### `party_add_members`
기존에는 N명의 멤버를 초대할 때, 대상별 가입 여부 조회와 유저 조회 및 저장이 반복되어 요청당 쿼리 수와 네트워크 라운드 트립이 증가하는 구조였다.

이를 `findAllByPartyIdAndUserIdIn(...)`, `userRepository.findAllById(...)`, `saveAll(...)` 기반의 배치 처리 구조로 변경해 요청당 왕복 횟수를 줄였다. 그 결과 기존 `mixed_run6` 기준 `p95 54.50ms`에서 최신 `mixed_high_rerun` 기준 `p95 24.44ms`로 약 `55.2%` 개선됐다.

또한 최신 `party_write_stress_rerun` 기준 전용 스트레스 환경에서도 endpoint `p95 18.44ms`, 실패율 `0%`를 유지해 write-path 안정성을 확인했다.

### 7.2 추가 발견 이슈 및 2차 수정
기존 병목 개선 이후 `party` 도메인의 write 경로를 다시 점검하면서, 파티 상태 변경과 멤버 변경이 동시에 발생할 때 정합성이 흔들릴 수 있는 지점을 추가로 확인했다.

이에 따라 `addMembers`, `removeMember`, `closeParty`, `leaveParty`에서 파티 조회를 `findByIdForUpdate()` 기반으로 통일해 write 경로의 동시성 제어를 보강했다. 이 수정은 응답시간 자체를 낮추기 위한 최적화라기보다, 고부하 환경에서 상태 전이와 멤버 변경의 일관성을 보장하기 위한 안정성 강화 작업에 가깝다.

이번 재측정에서는 `party_add_members`를 k6로 다시 검증했고, 나머지 write 경로는 서비스 테스트를 통해 정상 동작을 확인했다.

## 8. 결론 및 한계

### 8.1 결론
- 최신 코드 기준 재측정 결과, 모든 주요 API가 목표 threshold를 충족했다.
- `auth_login`은 Redis 전환으로 기존 DB write 병목을 제거했고, 남은 비용은 보안상 필요한 `BCrypt` 검증 비용으로 해석된다.
- 읽기 API는 전반적으로 낮은 지연시간을 유지했다.
- `party_add_members`는 혼합 시나리오와 전용 스트레스 시나리오 모두에서 안정적으로 처리됐다.
- 기존 병목 개선 이후 추가로 적용한 `party` write 경로 동시성 보강도 현재 측정 범위 안에서는 성능 저하 없이 유지됐다.

### 8.2 한계
- 본 결과는 로컬 환경 기반 측정이므로 운영 환경의 절대 성능 수치로 직접 일반화하기는 어렵다.
- `mixed` 시나리오에서는 일부 `dropped_iterations`가 발생했으므로, 목표 도착률을 100% 완전히 소화한 것으로 해석하면 안 된다.
- `dropped_iterations`는 서비스 오류보다는 로컬 자원 한계, k6 generator 측 자원 부족, 애플리케이션 스레드 풀, DB connection pool 경합 가능성을 함께 시사한다.
- `party_add_members` 전용 스트레스 결과는 success-only 신규 추가 latency보다 handled write-path 안정성에 더 가깝다.
- `removeMember`, `closeParty`, `leaveParty`는 서비스 테스트 기반 검증이 중심이며, 별도 부하 시나리오는 추후 보완 여지가 있다.

# k6 Runner

현재 기본 실행기는 `k6/test-plan.json` 기반 preset runner입니다.

## 실행

```bash
cd k6
./run.sh up
./run.sh run
```

선택 라벨을 붙여 실행할 수도 있습니다.

```bash
cd k6
./run.sh run smoke
```

기본 Run ID 규칙:

- `./run.sh run` -> `realistic_peak_20260401_182500`
- `./run.sh run smoke` -> `realistic_peak_smoke_20260401_182500`
- `RUN_ID=my_custom_run ./run.sh run` -> `my_custom_run`

필수 env:

```env
LEADER_CREDENTIALS=email:password[,email:password...]
MEMBER_CREDENTIALS=email:password[,email:password...]
```

선택 env:

```env
BASE_URL=http://localhost:8080
AUTO_PREPARE_WRITE_BANK=true
```

`realistic_*` preset은 성공 write 표본을 위해 전용 write bank를 전제로 합니다.

```bash
docker exec -i matchduo_db-mysql mysql -uroot -pmysql123! matchduo_db < k6/sql/load-test-seed.sql
docker exec -i matchduo_db-mysql mysql -uroot -pmysql123! matchduo_db < k6/sql/reset-party-write.sql
```

기본값으로 `./run.sh run`은 `realistic_*`, `party_write_contention` preset 실행 전에 위 두 단계를 자동으로 수행합니다.
수동으로 제어하고 싶으면 `AUTO_PREPARE_WRITE_BANK=false ./run.sh run`으로 끌 수 있습니다.

## preset 변경

`k6/test-plan.json`의 `preset` 값을 `realistic_peak`, `realistic_soak`, `party_write_contention` 중 하나로 바꾸면 됩니다.

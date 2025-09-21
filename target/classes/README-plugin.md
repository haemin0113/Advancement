# Advancement — 목표/도전과제 허브

이 플러그인은 퀘스트 플러그인 없이도 **일/주/시즌 목표**를 간단한 YAML로 선언하고,
채광/사냥/제작/리전 체류 등 다양한 이벤트를 집계하여 보상을 지급합니다.

---

## 설치 & 요구사항
- Paper 1.20.1~1.21.x
- Java 17+ (1.21은 21 권장)
- 선택: PlaceholderAPI, Vault(머니), WorldGuard 7.x, MythicMobs 5.x, ItemsAdder, MMOItems

플러그인을 `plugins/`에 넣고 서버를 실행하면,
`plugins/Advancement/config/` 아래에 설정 파일들이 생성됩니다.

- `config.yml` : 전역 옵션
- `messages.yml` : 출력 문구
- `sounds.yml` : 이벤트별 사운드
- `gui.yml` : GUI 레이아웃/스킨
- `goals.yml` : 목표 정의
- `reward.yml` : 보상 아이템 저장소(`/도전과제 admin item import`)

---

## 목표 타입 (goals.type)
| 타입 | 의미 | 대표 예시 |
|---|---|---|
| `counter` | 이벤트 발생 수 누적 | 블록 300개 채굴, 물고기 10마리 |
| `unique` | 고유 대상 n종 달성 | 서로 다른 보스 10종 처치 |
| `checklist` | 하위 항목 m개 달성 | 5개 지역 중 3곳 방문 |
| `streak` | 연속 성공/콤보 | 180초 내 연속 제작/제련 50회 |
| `timetrial` | 제한시간 내 목표량 | 10분 안에 밀 300개 수확 |
| `collection` | 아이템 수집/보유 | 특정 재료 64개 모으기 (준비중) |
| `explore` | 지역 방문/체류 | 리전에 10초 체류 (브리지 필요) |
| `economy` | 경제 누계 | 판매/구매/송금 합계 (준비중) |
| `custom` | 외부 API 증분 | 다른 플러그인이 직접 increment |

> 현재 v0.1.x에서는 `counter / unique / checklist / streak / timetrial` 실동작.

---

## 이벤트 소스 (track.source)
- block_break:<MATS…> 예) block_break:wheat,carrots
- mob_kill:any 바닐라 엔티티 타입 이름 사용
- mob_kill:mythic:* MythicMobs 개체(옵션: mythic_ids, tier_min)
- craft:any | craft:<MATS>
- smelt:any | smelt:<MATS>
- pickup:any | pickup:<MATS>
- fish:any
- region_stay:<regionId> (WorldGuard) 1초마다 +1

### IA/MMOItems 필터 (선택)
- `ia_ids: [ "pack:bread", ... ]`  → ItemsAdder 네임스페이스 ID 매칭
- `mmo_type: "MATERIAL"`, `mmo_ids: ["BOSS_TOKEN"]` → MMOItems 타입/ID 매칭  
  이 키들은 `craft/pickup` 등에 함께 사용합니다.

---

## 필터 DSL (간단판)
`filter: "world in ['world'] && region!='spawn' && tool in HOE"`

- 키: `world`, `region`, `x/y/z`, `tool`, `time`, `weather`, `spawn_reason` …
- 헬퍼: `time.in('06:00-23:00')`, `y.between(20,60)`, `tool in HOE|PICKAXE`
- 브리지는 자동 감지되며 미탑재 시 해당 조건은 무시/폴백됩니다.

---

## 보상 (rewards)
`rewards` 항목은 목록(List)이며, 다음 키를 지원합니다.

- `money: <금액>` — Vault 필요
- `cmd: "<콘솔 명령>"` — `%player%` 치환
- `give: "<아이템 명세>"`
    - 바닐라: `"minecraft:bread 16"` / `"DIAMOND 2"`
    - 저장된 아이템: `"@이름 1"`  ← `/도전과제 admin item import <이름>` 으로 손에 든 아이템을 저장

예)
```yml
rewards:
  - give: "@농부팩 1"
  - money: 1500
  - cmd: "broadcast &6%player%&f가 달성!"
```

## 체크리스트 작성 예시
```yaml
season_explorer:
  title: "&b시즌 탐험가"
  type: checklist
  require: 3
  items:
    - { key: "ancient_ruins", title: "고대 유적 방문", when: "region_enter:ancient_ruins" }
    - { key: "frozen_peak",   title: "얼어붙은 봉우리 방문", when: "region_enter:frozen_peak" }
    - { key: "kill_golem",    title: "보스 골렘 처치", when: "mob_kill:mythic:BOSS_GOLEM" }
  lore:
    - "&7- 3개 항목을 달성하면 완료"
  reset: "season:2025S3"
  rewards: [ { give: "@시즌보상상자 1" } ]
```

## 스트릭 작성 예시
```yaml
smith_combo:
  title: "&6대장장이 콤보"
  type: streak
  track:
    - { source: "craft:any" }
    - { source: "smelt:any" }
  streak:
    window_sec: 180
    breaks_on: ["death","world_change","logout","time_gap"]
  target: 50
  lore:
    - "&7- 180초 안에 연속 제작/제련 유지"
  reset: weekly
  rewards: [ { money: 2000 } ]
```

## 타임트라이얼 작성 예시
```yaml
wheat_speedrun:
  title: "&e밀 속사수"
  type: timetrial
  track: [ { source: "block_break:wheat" } ]
  timetrial:
    duration_sec: 600
    start: "on_first_event"
    cooldown_sec: 60
    metric: "count"
  target: 300
  lore:
    - "&7- 10분 내 밀 300개 수확"
  reset: weekly
  rewards:
    - { money: 1500 }
    - { give: "@농부팩 1" }
```
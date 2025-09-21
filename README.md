# Advancement (Paper Plugin) — 풀버전 README

> YAML로 **일/주/시즌/반복** 목표를 선언하고, 진행/보상/GUI/리더보드를 제공하는 초경량 도전과제 플러그인.  
> 목표 설계는 `preset/when/where` **축약 DSL**로 간단하게, 필요하면 세부 옵션으로 정교하게 설정합니다.

---

## 0) 요구사항 & 의존성
- **서버:** Paper 1.20.1 ~ 1.21.x
- **Java:** 17+ (1.21은 21 권장)
- **선택 연동(softdepend):** PlaceholderAPI, Vault, WorldGuard 7.x, MythicMobs 5.x, ItemsAdder, MMOItems
- **파일 경로 기준:** `plugins/Advancement/`

---

## 1) 설치/업데이트
1. `Advancement-<version>.jar`를 `plugins/`에 넣고 서버 실행
2. 최초 실행 시 `config/`, `data/` 폴더와 예시 `*.yml` 자동 생성
3. 업데이트 후에는 `/도전과제 admin reload` 또는 재부팅

> 기존 버전에서 구조가 바뀐 경우, `config/*.yml` 백업을 권장합니다.

---

## 2) 폴더 구조
```
plugins/Advancement/
  config/
    config.yml          # 일반 설정(메시지/세이브/보드)
    gui.yml             # GUI 템플릿
    sounds.yml          # GUI/진행/완료/수령 등 사운드
    reward.yml          # 저장된 보상 아이템(@키)
    goals/
      daily.yml         # 일일 목표
      weekly.yml        # 주간 목표
      season.yml        # 시즌 목표
      repeat.yml        # 반복 목표(완료 즉시 초기화)
  data/
    players/<UUID>.yml  # 플레이어별 진행 저장
  readme.md             # 안내(자동 생성본)
```

---

## 3) 빌드(개발자용)
```bash
mvn -DskipTests package
```
산출물: `target/Advancement-<version>.jar`

선택(깃허브 액션 템플릿):
```yaml
name: build
on: { push: { branches: [ main ] }, pull_request: { branches: [ main ] } }
jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with: { distribution: temurin, java-version: "21", cache: maven }
      - run: mvn -B -DskipTests package
      - uses: actions/upload-artifact@v4
        with: { name: Advancement, path: target/*.jar }
```

---

## 4) 가장 쉬운 작성 — 축약 DSL
필수 키: `title`, `reset`, `preset`, `when`, `target`, `rewards`

```yml
goals:
  daily_wheat:
    title: "&e일일 채집가"
    reset: daily
    preset: break          # preset 목록은 아래 표 참고
    when: wheat,carrots,potatoes
    target: 300
    rewards:
      - money: 800
      - give: "@빵세트 8"   # /도전과제 admin item import 빵세트
```

### 4.1 preset 목록
| preset        | 내부 소스               | 설명                          |
|---------------|--------------------------|-------------------------------|
| `break`       | `block_break`            | 블록 캐기                     |
| `harvest`     | `harvest`                | 성숙 작물 수확(최대 단계만)   |
| `place`       | `block_place`            | 블록 놓기                     |
| `kill`        | `mob_kill`               | 몹 처치                        |
| `mythic_kill` | `mob_kill:mythic`        | MythicMobs ID/티어 필터       |
| `shear`       | `shear`                  | 동물 털깎기 성공              |
| `breed`       | `breed`                  | 플레이어가 번식시킨 동물      |
| `tame`        | `tame`                   | 생명체 길들이기               |
| `craft`       | `craft`                  | 제작 결과물                   |
| `smelt`       | `smelt`                  | 제련 결과물                   |
| `pickup`      | `pickup`                 | 아이템 주움(획득)             |
| `fish`        | `fish`                   | 낚시 성공                     |
| `trade`       | `trade`                  | 주민 거래 결과 수령           |
| `enchant`     | `enchant`                | 아이템 마법 부여              |
| `anvil`       | `anvil`                  | 모루 수리/합성 결과 수령      |
| `smithing`    | `smithing`               | 대장장이 작업대 결과 수령    |
| `brew`        | `brew`                   | 포션 양조(완성 병 수만큼)     |
| `consume`     | `consume`                | 음식·포션 섭취               |
| `distance`    | `distance`               | 이동 거리 누적 (on_foot/boat/elytra) |
| `advancement` | `advancement`            | 어드밴스먼트 달성             |
| `stay`        | `region_stay`            | WorldGuard 리전 **1초당 +1**  |

### 4.2 when(대상)
- 여러 개: `diamond_ore,ancient_debris`
- 전부 허용: `*` 또는 `any`
- MythicMobs: `mythic_kill` + `when: BOSS_A,BOSS_B` 또는 `*`
- 수확/털깎기/번식/길들이기: 블록·엔티티 키(`wheat`, `sheep` 등)
- 거래: 주민 직업(`farmer`), 레벨(`master`), 조합(`farmer:master`), 숫자(`5`), `any`
- 인챈트: `sharpness`, `mending` 등 인챈트 키 (레벨 조건은 `where.level_min/max`)
- 모루/대장장이/양조/섭취: 결과 아이템 키 또는 포션 키(`minecraft:strong_healing`)
- 이동: `on_foot`, `boat`, `elytra`
- 어드밴스먼트: `minecraft:story/mine_stone`, `minecraft:story/*` 처럼 와일드카드 허용

### 4.3 where(선택 — 필터)
```yml
where:
  world: world,world_nether
  region: dungeon_a
  region_not: spawn
  time: "06:00-23:00"
  tool: HOE              # HOE | PICKAXE | AXE ...
  y_between: "20..60"
  level_min: 3           # 인챈트 최소 레벨
  level_max: 5
  merchant_profession: FARMER
  distance_sample_ms: 250
  distance_min_m: 0.2
  mode: elytra           # distance 모드 강제(on_foot/boat/elytra)
```
> 위 형식은 내부 DSL로 자동 변환되어 적용됩니다.
- `level_min`, `level_max`: 인챈트 레벨 필터
- `merchant_profession`: 거래 시 주민 직업 고정
- `distance_sample_ms`: 이동 샘플 간 최소 시간(기본 250ms)
- `distance_min_m`: 이동 거리 최소 허용값(기본 0.2m)
- `mode`: distance 목표에서 강제할 이동 모드

### 4.5 신규 preset 샘플
```yml
goals:
  daily_harvest:
    title: "&e일일 수확"
    reset: daily
    preset: harvest
    when: wheat,carrots,potatoes
    target: 200
    rewards: [{ money: 600 }]

  weekly_trader:
    title: "&6주간 상인"
    reset: weekly
    preset: trade
    when: any
    target: 50
    rewards: [{ money: 4000 }]

  brewer_week:
    title: "&d양조 달인"
    reset: weekly
    preset: brew
    when: any
    target: 64
    rewards: [{ money: 3000 }]

  elytra_runner:
    title: "&a엘리트라 장거리"
    reset: weekly
    preset: distance
    when: elytra
    target: 20000
    rewards: [{ money: 5000 }]

  vanilla_master:
    title: "&c바닐라 성취가"
    type: unique
    reset: season:2025S4
    preset: advancement
    when: "minecraft:story/*"
    unique_by: advancement_key
    target: 10
    rewards:
      - { cmd: "lp user %player% perm settemp cosmetic.title.master true 30d" }
```

### 4.4 reset(초기화)
- `daily`, `weekly`, `monthly`, `season:<ID>`, `repeat`
- `repeat`: 완료 즉시 초기화 → 재도전 가능

---

## 5) 타입별 상세

기본값은 `counter`(누적)입니다. 필요한 경우 `type`을 명시합니다.

### 5.1 counter — 누적
```yml
goals:
  miner_week:
    title: "&b채광왕"
    reset: weekly
    preset: break
    when: diamond_ore,ancient_debris
    target: 200
    rewards: [{ money: 5000 }]
```

### 5.2 unique — 서로 다른 대상 N종
```yml
goals:
  weekly_boss_hunter:
    title: "&c주간 보스 헌터"
    type: unique
    reset: weekly
    preset: mythic_kill
    when: "*"
    unique_by: mythic_id
    target: 10
    rewards: [{ money: 5000 }]
```

### 5.3 checklist — 체크리스트(전체/Require M개)
```yml
goals:
  season_explorer:
    title: "&d시즌 탐험가"
    type: checklist
    reset: season:2025S3
    require: 3
    items:
      - { key: ruins,  title: "고대 유적 방문",   when: "region_enter:ancient_ruins" }
      - { key: peak,   title: "얼어붙은 봉우리",   when: "region_enter:frozen_peak" }
      - { key: golem,  title: "보스 골렘 처치",   when: "mob_kill:mythic:BOSS_GOLEM" }
      - { key: nether, title: "네더 입장",       when: "world_enter:world_nether" }
```

### 5.4 streak — 연속 콤보
```yml
goals:
  smith_combo:
    title: "&6대장장이 콤보"
    type: streak
    reset: weekly
    preset: craft
    when: iron_ingot
    target: 50
    streak:
      window_sec: 180
      breaks_on: [death, world_change]
    rewards: [{ money: 2000 }]
```

### 5.5 timetrial — 시간 내 달성
```yml
goals:
  wheat_speedrun:
    title: "&a밀 스피드런"
    type: timetrial
    reset: repeat
    preset: break
    when: wheat
    target: 300
    timetrial:
      duration_sec: 600
      cooldown_sec: 60
    rewards: [{ money: 1000 }]
```

---

## 6) 의존 목표 & 역보상/보정

### 6.1 선행 목표 필요(`requires`)
```yml
goals:
  weekly_bosses:
    title: "&c주간 보스 헌터"
    type: unique
    reset: weekly
    preset: mythic_kill
    when: "*"
    unique_by: mythic_id
    target: 10
    requires: ["daily_wheat"]   # daily_wheat 완료 후 활성
```

### 6.2 특정 아이템 보유 시 활성(`activate_if_has`)
```yml
goals:
  season_explorer:
    ...
    activate_if_has: ["ia:server:adventurer_badge"]
```
- 지정한 아이템을 보유하면 선행 목표 없이도 활성

### 6.3 진행량 보정(`boosts`)
```yml
goals:
  daily_wheat:
    ...
    boosts:
      - { if_has: "ia:server:harvest_gloves", multiplier: 1.25 }
      - { if_has: "mmoitems:PICKAXE:ELITE_PICK", multiplier: 1.5 }
      - { if_has: "mc:minecraft:bread", add: 1 }
```
- `multiplier`: 곱연산, 여러 항목은 누적 곱
- `add`: 매 카운트마다 가산
- 계산식: `gain = round(delta * Πmultiplier) + Σadd`

**식 예시:** `delta=1`, 장갑×1.25, 곡괭이×1.5, add=+1 → `gain=round(1*1.875)+1=3`

---

## 7) 보상(rewards)
지원 키:
- `money: <정수>` → Vault 경제 지급
- `give: "<아이템|@저장키> 수량"`  
  - 예: `minecraft:bread 16`, `@빵세트 8`
- `cmd: "<콘솔 명령어>"` → `%player%` 치환 가능

### 7.1 보상 아이템 템플릿 저장/사용
```
/도전과제 admin item import <이름>
```
- 손에 든 아이템을 `reward.yml`에 저장
- 목표에서는 `give: "@<이름> 수량"`으로 지급

---

## 8) GUI/Sounds

### 8.1 `gui.yml`
```yml
title: "&6도전과제 — %tab%"
nav:
  all:    { slot: 0, material: BOOKSHELF,   name: "&e전체" }
  daily:  { slot: 1, material: SUNFLOWER,   name: "&6일일" }
  weekly: { slot: 2, material: CLOCK,       name: "&b주간" }
  season: { slot: 3, material: NETHER_STAR, name: "&d시즌" }

list_slots: [9,10,11, ... ,53]

item:
  in_progress: { material: WHITE_STAINED_GLASS_PANE }
  completed:   { material: LIME_STAINED_GLASS_PANE }
  locked:      { material: RED_STAINED_GLASS_PANE, name: "&c잠금: 선행 필요" }

  name: "%title%"
  lore:
    - "&7진행: &a%value%&7/&f%target% &8(%percent%%)"
    - "&7보상:"
    - "%rewards%"      # 보상 목록(명령은 OP만 표시)
    - "%goal_lore%"    # 자동 생성되는 달성 조건
    - "{op}&8키: %key%" # {op} 접두 시 OP에게만 표시
```

### 8.2 `sounds.yml`
```yml
events:
  gui_open:     { sound: UI_BUTTON_CLICK, volume: 1.0, pitch: 1.2 }
  gui_close:    { sound: UI_BUTTON_CLICK, volume: 1.0, pitch: 0.8 }
  progress_add: { sound: ENTITY_EXPERIENCE_ORB_PICKUP, volume: 1.0, pitch: 1.0 }
  completed:    { sound: UI_TOAST_CHALLENGE_COMPLETE, volume: 1.0, pitch: 1.0 }
  claimed:      { sound: ENTITY_PLAYER_LEVELUP, volume: 1.0, pitch: 1.0 }
```

---

## 9) 명령어/권한
### 9.1 명령어
```
/도전과제                 # GUI
/도전과제 list            # 내 진행 요약
/도전과제 claim <key>     # 보상 수령

/도전과제 admin reload                        # 설정 리로드
/도전과제 admin set <goal> <player> <value>   # 진행 강제
/도전과제 admin reset <goal> [player]         # 초기화
/도전과제 admin reset --season <ID>           # 시즌 초기화
/도전과제 admin item import <이름>            # 보상템 템플릿 저장
/도전과제 admin save                           # 즉시 저장
```

### 9.2 권한
- `advancement.user` (기본 true)
- `advancement.admin`
- `advancement.bypass.filters` (필터 무시 테스트)
- `advancement.bypass.cooldown`

---

## 10) PlaceholderAPI
- `%adv_name::<goal>%`
- `%adv_value::<goal>%` `%adv_target::<goal>%` `%adv_percent::<goal>%`
- `%adv_state::<goal>%` → `ACTIVE/COMPLETED`

---

## 11) 저장/성능
- 저장 경로: `data/players/<UUID>.yml`
- 자동 저장: 60초 주기
- 동기/비동기 선택: `config.yml → general.save_async`
- 리더보드/캐시: 진행/상위 N, 집중 목표 캐시
- 대형 서버 권장: MySQL 백엔드(향후 옵션), 인덱스 `(goal_key, season_id, player)`

---

## 12) 안티-치즈(치트 억제)
- `placed vs natural`: 설치 블록 제외 옵션
- `spawn_reason` 필터: 자연/스킬/스포너 구분
- 좌표 반복 감쇠: 동일/근접 좌표 반복 파밍 인정량 하향
- 속도 캡: 단위 시간 최대 인정량
- 최소 샘플 보호: 신규 목표 초기 과보상 방지

---

## 13) 문제 해결
- **채팅 색상에 HEX가 그대로 보임** → `<#RRGGBB>`/`&` 코드가 `Text.legacy()`로 변환되는지 확인(메시지 prefix도 같은 규칙)
- **GUI 탭이/로어가 안 보임** → `gui.yml` 존재 확인, `%goal_lore%` 포함 여부 체크, `/도전과제 admin reload`
- **완료 후 즉시 재도전됨** → `reset: repeat`만 즉시 재도전. `daily/weekly/season`은 같은 기간 재도전 불가
- **저장 안 됨** → `data/players` 권한, `general.save_async: false`로 테스트, `/도전과제 admin save`, 콘솔 에러 확인
- **WorldGuard/IA/MMOItems 인식 안 됨** → 플러그인 버전/의존성 설치 확인(softdepend), 서버 로그의 “hooked/disabled” 메시지 확인

---

## 14) 예제 모음 (복붙용)

### 14.1 일일 채집 + 보정
```yml
goals:
  daily_wheat:
    title: "&e일일 채집가"
    reset: daily
    preset: break
    when: wheat
    target: 200
    boosts:
      - { if_has: "ia:server:harvest_gloves", multiplier: 1.25 }
      - { if_has: "mmoitems:PICKAXE:ELITE_PICK", multiplier: 1.5 }
    rewards: [{ money: 500 }]
```

### 14.2 주간 보스(선행 필요)
```yml
goals:
  weekly_bosses:
    title: "&c주간 보스 헌터"
    type: unique
    reset: weekly
    preset: mythic_kill
    when: "*"
    unique_by: mythic_id
    target: 10
    requires: ["daily_wheat"]
    rewards: [{ money: 5000 }]
```

### 14.3 시즌 탐험(아이템 보유 시 바로 활성)
```yml
goals:
  season_explorer:
    title: "&b시즌 탐험가"
    type: checklist
    reset: season:2025S3
    require: 3
    items:
      - { key: ruins,  title: "고대 유적 방문", when: "region_enter:ancient_ruins" }
      - { key: peak,   title: "얼어붙은 봉우리", when: "region_enter:frozen_peak" }
      - { key: golem,  title: "보스 골렘 처치", when: "mob_kill:mythic:BOSS_GOLEM" }
    activate_if_has: ["ia:server:adventurer_badge"]
    rewards:
      - cmd: "lp user %player% permission settemp cosmetic.trail.wings true 30d"
```

---

## 15) 라이선스(예시)
오픈소스로 공개한다면 `LICENSE` 파일을 추가하세요. (MIT 예시)
```text
MIT License
Copyright (c) 2025 ...
Permission is hereby granted, free of charge, to any person obtaining a copy ...
```
사내용이면 생략 가능합니다.

---

즐거운 운영 되세요! 필요하면 템플릿/예제 더 만들어 드릴게요.

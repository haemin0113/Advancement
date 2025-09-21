# Advancement — 목표/도전과제 설정 가이드 (최신판)

> 초보 운영자도 10분 안에 목표 하나 만들 수 있도록 구성했습니다.  
> 모든 설정은 `plugins/Advancement/config/` 기준입니다.

---

## 0) 개요
- 플러그인: **Advancement**
- 서버: Paper 1.20.1 ~ 1.21.x / Java 17+
- 선택 연동: PlaceholderAPI, Vault(경제), WorldGuard(리전·체류), MythicMobs(몹 ID/티어), ItemsAdder/MMOItems(커스텀 아이템)
- 컨셉: YAML로 **일/주/시즌/반복** 목표 선언 → 진행·보상·리더보드 관리
- 신규 기능(요약)
  - **의존 목표(Prerequisite)**: A 완료 후에만 B 활성 (`requires`)
  - **역보상/보정**: 특정 **커스텀 아이템 보유** 시 목표 자동 활성(`activate_if_has`), 진행량 보정(`boosts`)
  - **체류형 목표**: WorldGuard 리전 **1초당 +1** (`preset: stay`)
  - **GUI/Sounds 자동 생성**: `gui.yml`, `sounds.yml`가 자동 생성/로드
  - **데이터 저장 강화**: 자동 저장, 수동 저장 명령, 동기/비동기 선택

---

## 1) 폴더 구조
```
plugins/Advancement/
  config/
    config.yml          # 일반 설정(메시지·저장옵션 등)
    gui.yml             # GUI 템플릿(타이틀/슬롯/아이템/로어)
    sounds.yml          # 소리 설정(열기/닫기/진행/완료/수령)
    reward.yml          # 저장된 보상 아이템(@키)
    goals/
      daily.yml         # 일일 목표
      weekly.yml        # 주간 목표
      season.yml        # 시즌 목표
      repeat.yml        # 반복 목표(완료 즉시 재도전)
  data/
    players/<UUID>.yml  # 플레이어별 진행 저장소
```
> `goals/` 이하 **모든 `.yml`**이 합쳐져 로드됩니다. `daily_mining.yml`처럼 자유롭게 추가 가능.

---

## 2) 가장 쉬운 작성법 — 축약 DSL
필수 키 6개: **`title`, `reset`, `preset`, `when`, `target`, `rewards`**

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

### 2.1 preset 목록
- `break`       : 블록 캐기 (`block_break`)
- `place`       : 블록 놓기 (`block_place`)
- `kill`        : 몹 처치 (`mob_kill`)
- `mythic_kill` : MythicMobs 처치 (`mob_kill:mythic`)
- `craft`       : 제작 결과물 (`craft`)
- `smelt`       : 제련 결과물 (`smelt`)
- `pickup`      : 아이템 주움 (`pickup`)
- `fish`        : 낚시 성공 (`fish`)
- `stay`        : 리전 체류 1초당 +1 (`region_stay`)
- `harvest`     : 성숙 작물 수확 (`harvest`)
- `shear`       : 동물 털깎기 (`shear`)
- `breed`       : 동물 번식 (`breed`)
- `tame`        : 몹 길들이기 (`tame`)
- `trade`       : 주민 거래 결과 수령 (`trade`)
- `enchant`     : 아이템 마법 부여 (`enchant`)
- `anvil`       : 모루 결과 수령 (`anvil`)
- `smithing`    : 대장간 결과 수령 (`smithing`)
- `brew`        : 물약 양조 완료 (`brew`)
- `consume`     : 음식/물약 소비 (`consume`)
- `distance`    : 이동 거리 누적 (`distance`)
- `advancement` : 마인크래프트 업적 달성 (`advancement`)

### 2.2 when(대상)
- 기본은 소문자, 여러 개는 `diamond_ore,ancient_debris`
- 전부 허용: `*` 또는 `any`
- `harvest`/`shear`/`breed`/`tame`/`trade`/`consume`: 블록·엔티티·아이템 키
- `trade`: `farmer:2`처럼 직업+레벨, 직업만 지정 시 `farmer`
- `enchant`: 인챈트 키 (`sharpness` 등) + `where.level_min`, `where.level_max`
- `smithing`/`anvil`/`brew`: 결과 아이템 또는 포션 키 (`long_swiftness` 등)
- `distance`: `on_foot`, `boat`, `elytra`
- `advancement`: `minecraft:story/mine_stone` 형태, `minecraft:story/*` 와일드카드 허용
- MythicMobs: `mythic_kill` + `when: BOSS_A,BOSS_B` 또는 `*`

### 2.3 reset(초기화)
- `daily`, `weekly`, `monthly`, `season:<ID>`, `repeat`
- `repeat`는 **완료 즉시 초기화**되어 재도전 가능

### 2.4 where(선택—필터)
아래처럼 쓰면 자동으로 내부 필터가 생성됩니다.
```yml
where:
  world: world,world_nether
  region: dungeon_a
  region_not: spawn
  time: "06:00-23:00"
  tool: HOE              # HOE | PICKAXE | AXE ...
  y_between: "20..60"
  level_min: 3           # enchant 전용
  level_max: 5           # enchant 전용
  merchant_profession: farmer   # trade 전용
  mode: elytra           # distance 전용
  distance_sample_ms: 400
  distance_min_m: 1.5
```

---

## 3) 타입별 작성(조금 더)

`type`을 생략하면 기본은 **counter(누적)** 입니다.

### 3.1 counter — 누적
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

### 3.2 unique — 서로 다른 대상 N종
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

### 3.3 checklist — 체크리스트 M개 달성
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
> `require` 생략 시 `items` 전부가 필요합니다.

### 3.4 streak — 연속 콤보
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

### 3.5 timetrial — 시간 내 달성
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

## 4) 의존/역보상(신규)

### 4.1 선행 목표 필요(`requires`)
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

### 4.2 커스텀 아이템 보유 시 활성(`activate_if_has`)
```yml
goals:
  season_explorer:
    ...
    activate_if_has: ["ia:server:adventurer_badge"]
```
- 지정한 아이템을 보유하면 선행 목표 없이도 활성됩니다.

### 4.3 진행량 보정(`boosts`)
```yml
goals:
  daily_wheat:
    ...
    boosts:
      - { if_has: "ia:server:harvest_gloves", multiplier: 1.25 }
      - { if_has: "mmoitems:PICKAXE:ELITE_PICK", multiplier: 1.5 }
      - { if_has: "mc:minecraft:bread", add: 1 }
```
- `multiplier`: 해당 아이템 보유 시 **곱연산**
- `add`: 해당 아이템 보유 시 매 카운트마다 **가산**

식: `gain = round(delta * 모든 multiplier 곱) + 모든 add 합`

식 예: `delta=1`, 장갑×1.25, 곡괭이×1.5, add=+1 → `gain = round(1*1.875)+1 = 3`

---

## 5) 보상(rewards)
- `money: <액수>` : Vault 경제
- `give: "<아이템|@저장키> 수량"`  
  - 예: `minecraft:bread 16`, `@빵세트 8`
- `cmd: "<콘솔 명령어>"` : `%player%` 치환 가능

### 5.1 보상 아이템 저장/사용
```
/도전과제 admin item import <이름>
```
- 손에 든 아이템을 `reward.yml`에 `<이름>`으로 저장
- 목표에서는 `give: "@<이름> 수량"` 으로 지급

---

## 6) GUI/Sounds 커스터마이징

### 6.1 GUI (`gui.yml`)
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
    - "%rewards%"      # 보상 목록(명령은 OP만 노출)
    - "%goal_lore%"    # 자동 생성되는 달성 조건
    - "{op}&8키: %key%" # {op} 접두 시 OP에게만 노출
```

### 6.2 소리 (`sounds.yml`)
```yml
events:
  gui_open:     { sound: UI_BUTTON_CLICK, volume: 1.0, pitch: 1.2 }
  gui_close:    { sound: UI_BUTTON_CLICK, volume: 1.0, pitch: 0.8 }
  progress_add: { sound: ENTITY_EXPERIENCE_ORB_PICKUP, volume: 1.0, pitch: 1.0 }
  completed:    { sound: UI_TOAST_CHALLENGE_COMPLETE, volume: 1.0, pitch: 1.0 }
  claimed:      { sound: ENTITY_PLAYER_LEVELUP, volume: 1.0, pitch: 1.0 }
```

---

## 7) 데이터 저장
- 경로: `plugins/Advancement/data/players/<UUID>.yml`
- `config.yml`에서 저장 방식 선택
  ```yml
  general:
    save_async: true   # true: 비동기(기본), false: 동기(즉시 저장)
  ```
- 자동 저장: 60초 주기
- 수동 저장:
  ```
  /도전과제 admin save
  ```

---

## 8) 명령어
```
/도전과제                 # GUI 열기
/도전과제 list            # 내 진행 텍스트 요약
/도전과제 claim <key>     # 보상 수령(완료 상태)

/도전과제 admin reload                        # 설정 리로드
/도전과제 admin set <goal> <player> <value>   # 진행 강제 설정
/도전과제 admin reset <goal> [player]         # 진행 초기화
/도전과제 admin item import <이름>            # 보상 아이템 템플릿 저장
/도전과제 admin save                           # 즉시 저장
```

---

## 9) PlaceholderAPI
- `%adv_name::<goal>%`
- `%adv_value::<goal>%` `%adv_target::<goal>%` `%adv_percent::<goal>%`
- `%adv_state::<goal>%` → `ACTIVE/COMPLETED`

---

## 10) 연동/브리지
- **WorldGuard**: `preset: stay` 사용 시 1초 체류마다 +1
- **MythicMobs**: `preset: mythic_kill`, `unique_by: mythic_id`
- **ItemsAdder/MMOItems**: `activate_if_has`, `boosts.if_has` 등에 사용  
  - 포맷:  
    - IA: `ia:<namespace>:<id>`  
    - MMOItems: `mmoitems:<TYPE>:<ID>`  
    - 바닐라: `mc:minecraft:<item>`

---

## 11) 색상 코드
- 레거시 `&a, &6`과 `§` 지원
- **HEX 색상**: `<#RRGGBB>` 권장  
  예: `"<#F6D365>[Adv]&r Hello"`

---

## 12) 문제 해결
- 색이 채팅에 그대로 보임 → 메시지를 `Text.legacy()`로 변환하는 최신 코어를 사용하고 있는지 확인. `config.yml`의 prefix는 `<#HEX>` 또는 `&` 코드 사용.
- GUI에 탭이 안 보임 → `gui.yml`가 존재하는지 확인 후 `/도전과제 admin reload`.
- GUI에 달성 조건이 안 보임 → `lore`에 `%goal_lore%` 포함하거나, 포함하지 않으면 자동으로 하단에 삽입.
- 진행 저장이 안 됨 → `data/players` 권한 확인, `general.save_async: false`로 테스트, `/도전과제 admin save` 실행, 콘솔 로그의 저장 에러 확인.
- 시즌/일일 즉시 재도전됨 → `reset: repeat`만 즉시 재도전. `daily/weekly/season`은 같은 기간 재도전 불가.

---

## 13) 예제 모음 (복붙용)

### 일일 채집 + 보정
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
    rewards:
      - money: 500
```

### 주간 보스(선행 필요)
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

### 시즌 탐험(아이템 보유 시 바로 활성)
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

## 14) 변경 기록(요약)
- `requires`, `activate_if_has`, `boosts(multiplier/add)` 추가
- `gui.yml`, `sounds.yml` 자동 생성·로드
- 저장소: 자동 저장 + `/도전과제 admin save` + 동기/비동기
- HEX 색상 `<#RRGGBB>` 지원 강화를 문서화

즐거운 운영 되세요! :)

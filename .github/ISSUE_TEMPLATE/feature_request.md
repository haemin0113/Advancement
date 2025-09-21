name: "Feature request (Codex)"
labels: ["enhancement","codex-task"]
body:
  - type: input
    id: title
    attributes: { label: "한 줄 요약" }
    validations: { required: true }
  - type: textarea
    id: spec
    attributes:
      label: "기능 상세(수용기준)"
      description: |
        - 대상 명령어/이벤트:
        - 설정 항목 추가/변경:
        - 메시지 키/플레이스홀더:
        - 성능/스레딩 제약:
        - 테스트 시나리오(성공/실패 케이스 포함):
    validations: { required: true }
  - type: checkboxes
    id: done
    attributes:
      label: "완료 조건"
      options:
        - label: "CI 통과"
        - label: "config.yml/README 갱신"
        - label: "하위 호환 유지"

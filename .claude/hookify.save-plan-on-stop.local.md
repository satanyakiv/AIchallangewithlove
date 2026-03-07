---
name: save-plan-on-stop
enabled: true
event: stop
action: block
conditions:
  - field: transcript
    operator: contains
    pattern: Exited Plan Mode
  - field: transcript
    operator: not_contains
    pattern: .claude/plans/
---

**Plan Mode був використаний, але план не збережено!**

Ти вийшов з Plan Mode під час цієї сесії, але файл плану не був створений у `.claude/plans/`.

**Обов'язкова дія перед завершенням:**
1. Збережи план у `.claude/plans/YYYY-MM-DD-<topic>.md`
2. Використай формат дати сьогоднішнього дня
3. Після збереження можеш завершити сесію

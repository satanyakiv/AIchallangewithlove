Read .claude/rules/architecture.md, .claude/rules/testing.md

## Code Review

Виконай повний code review поточних змін у проєкті.

## Process

### Step 1 — CODEX REVIEW (автоматичний)

Запусти автоматичний ревʼю через OpenAI Codex.

**Для uncommitted змін:**
```
/codex:review --background
```

**Для ревʼю гілки відносно main:**
```
/codex:review --base main --background
```

Перевір статус і результат:
```
/codex:status
/codex:result
```

> Якщо Codex недоступний (помилка авторизації або мережі) — пропусти цей крок.

### Step 2 — DIFF

Переглянь всі зміни:
```bash
git diff --staged
git diff
```

### Step 3 — ARCHITECTURE CHECK

Для кожного зміненого .kt файлу перевір:
- [ ] Файл < 150 рядків?
- [ ] Функції < 20 рядків?
- [ ] Agent клас < 80 рядків (тільки оркестрація)?
- [ ] Немає inline prompt strings? (мають бути в resources/)
- [ ] DI через конструктор? (не створення всередині класу)
- [ ] Sealed types замість strings для типів/станів?
- [ ] Конверсії через Mapper, не inline?
- [ ] Один клас = один файл?
- [ ] UseCase має єдиний execute()?

### Step 4 — TEST CHECK

Для кожної мутації даних:
- [ ] Є тест happy path?
- [ ] Є тест no-op?
- [ ] Є тест persistence?
- [ ] Тести іменовані: `testWhat_condition_expected()`?

### Step 5 — REPORT

Покажи результат у форматі:

**Codex ревʼю:** [результат або "пропущено"]

**Файли перевірені:** X
**Проблеми знайдені:** Y
| # | Файл | Проблема | Severity | Джерело |
|---|------|----------|----------|---------|

Колонка "Джерело" = `Codex` або `Manual`.

**Рекомендації:** список конкретних дій для виправлення.
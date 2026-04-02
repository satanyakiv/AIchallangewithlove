Read .claude/rules/*

## Full Development Pipeline

Повний цикл розробки фічі: spec → interview → code → review → test → mobile test → PR.

**Input:** $ARGUMENTS (шлях до SPEC.md)

## Stage 1 — SPEC

Прочитай `$ARGUMENTS`. Зрозумій скоуп, вимоги, контекст.

## Stage 2 — INTERVIEW

Проаналізуй спек і виявив прогалини, неоднозначності, технічні ризики.

Використовуй AskUserQuestion для глибокого інтерв'ю. По 2-3 питання за раз.

**Категорії (чергуй):**
- **Technical:** архітектура, data flow, edge cases, error handling, API contracts
- **UI/UX:** стани компонентів, transitions, empty/error/loading states
- **Tradeoffs:** performance vs readability, complexity vs flexibility
- **Integration:** з існуючим кодом, breaking changes, міграції
- **Concerns:** що може піти не так в проді, масштабування

**Правила:**
- Питання НЕ очевидні — розкривають те, що автор спеку не врахував
- Продовжуй доки всі прогалини не закриті
- Групуй пов'язані питання разом

Після завершення — створи `SPEC-refined.md` поруч з оригіналом.

## --- CHECKPOINT ---

AskUserQuestion: "Інтерв'ю завершено, SPEC-refined.md записаний. Починаю кодинг?"
Опції: "Go" / "Ще питання" / "Стоп"

Якщо "Ще питання" — продовжити інтерв'ю. Якщо "Стоп" — зупинитись.

## Stage 3 — PLAN

Увійди в EnterPlanMode. Створи план імплементації:
- Файли для створення/зміни (з package paths)
- Нові тести (назва + що верифікує)
- UI елементи (якщо є)

Перед показом плану — перевір архітектурний чеклист:
- [ ] UseCase з єдиним execute()?
- [ ] Agent — тільки оркестрація?
- [ ] Промпти в resources/, не inline?
- [ ] Один клас = один файл?
- [ ] Файли < 150 рядків, функції < 20?
- [ ] DI через конструктор?
- [ ] Sealed types замість strings?
- [ ] Конверсії через Mapper?
- [ ] Кожна мутація — 3 тести (happy, no-op, persistence)?

## Stage 4 — CODE

Імплементація за планом. Test-first де можливо:
1. Написати тести що фейляться
2. Написати продакшн код
3. Тести мають пройти

## Stage 5 — REVIEW

Архітектурний ревʼю всіх змінених файлів:
- [ ] Файл < 150 рядків?
- [ ] Функції < 20 рядків?
- [ ] Agent клас < 80 рядків?
- [ ] Немає inline prompt strings?
- [ ] DI через конструктор?
- [ ] Sealed types замість strings?
- [ ] Конверсії через Mapper?

Якщо є порушення — виправити перед наступним етапом.

## Stage 6 — TEST

Запустити всі нові та пов'язані тести:
```bash
./gradlew :server:test --tests "*RelevantTest"
```
**НІКОЛИ** без `--tests` фільтра.

Якщо є фейли — виправити і перезапустити.

## Stage 7 — MOBILE TEST (success flow only)

Перевірити success flow через claude-in-mobile:

1. **SETUP** — `list_devices` для знаходження підключеного пристрою, `set_device`
2. **LAUNCH** — `launch_app` для запуску додатку
3. **NAVIGATE** — `screenshot` + `tap`/`find_and_tap` для навігації до нового екрану
4. **TEST SUCCESS FLOW:**
   - Пройти основний happy path фічі від початку до кінця
   - На кожному кроці: `screenshot` → перевірити стан → наступна дія
   - Верифікувати що UI відображає очікувані дані
5. **REPORT** — показати результат:
   - Кроки виконані: X
   - Скріншоти зроблені: Y
   - Success flow: passed/failed
   - Якщо failed — що саме пішло не так

Якщо mobile test failed — виправити і повторити тест.

## Stage 8 — REVIEW 2

Повторний архітектурний чеклист після всіх фіксів (той самий що Stage 5).
Якщо все чисто — далі.

## Stage 9 — PR

Створити Pull Request:
```bash
gh pr create --title "..." --body "..."
```

Summary має включати:
- Що зроблено (з SPEC-refined.md)
- Файли змінені
- Тести додані
- Mobile test result

## Rules

- Mock LlmClient в тестах. Ніколи не викликати реальне API.
- Ніколи не запускати `./gradlew test` без `--tests` фільтра.
- НЕ ламати існуючі тести.
- Mobile test — тільки success flow, не тестувати edge cases.
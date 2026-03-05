---
name: block-integration-tests
enabled: true
event: bash
action: block
pattern: gradlew(?!.*--tests).*\btest\b
---

**Заборонено: запуск тестів без фільтра `--tests`**

Ця команда запустить **всі** тести, включаючи інтеграційні (`*IntegrationTest`), які викликають реальний DeepSeek API і **коштують грошей**.

**Завжди використовуй фільтр:**
```bash
./gradlew :server:test --tests "*.Day13StateMachineTest"
./gradlew :server:test --tests "*.Day11IntegrationTest"
./gradlew :server:test --tests "*PersistenceTest"
```

**Правило з Testing.md:** Integration tests (`*IntegrationTest.kt`) — тільки ручний запуск розробником, ніколи автоматично.

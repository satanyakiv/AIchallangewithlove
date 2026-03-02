# Day 10: Three Context Management Strategies
**Date:** 2026-03-02

## Context

Day 10 реалізує 3 окремих підходи до управління контекстом LLM — без summary/компресії (на відміну від Day 9). Навігація: MainScreen → Day10Screen (хаб з 4 картками) → окремі екрани стратегій або екран порівняння.

---

## Реалізовані файли

### Database
- `Day10MessageEntity.kt` — спільна таблиця для всіх 3 стратегій (strategyId + branchId)
- `Day10BranchEntity.kt` — метадані гілок (лише для branching)
- `Day10FactEntity.kt` — KV-факти (лише для facts)
- `Day10Dao.kt` — всі Room-запити, включаючи складний branch query
- `Day10Repository.kt` — зручний API поверх DAO
- `ChatDatabase.kt` — оновлено до version=2, AutoMigration 1→2

### Server
- `Day10SlidingAgent.kt` — надсилає останні N повідомлень
- `Day10FactsAgent.kt` — 2 виклики DeepSeek: витягти факти + відповідь
- `Day10BranchingAgent.kt` — надсилає повну гілкову історію
- `AgentRoutes.kt` — 3 нові маршрути `/chat-v10/sliding|facts|branching`
- `Application.kt` — реєстрація 3 нових агентів

### Client
- `AgentApi.kt` — 6 нових DTO + 3 нові методи
- `Day10SlidingViewModel.kt`, `Day10FactsViewModel.kt`, `Day10BranchingViewModel.kt`
- `Day10HubScreen.kt` — 4-карткова навігація
- `Day10SlidingScreen.kt`, `Day10FactsScreen.kt`, `Day10BranchingScreen.kt`
- `Day10SharedComponents.kt` — ChatInputBar, SuggestionChipsRow, Day10ChatMessageList
- `Day10ComparisonData.kt` — hardcoded метрики (оновити після integration тестів)
- `Day10ComparisonScreen.kt` — LinearProgressIndicator бар-чарти
- `App.kt` — 5 нових Screen sealed class entries + навігація
- `MainScreen.kt` — картка Day10 (id=10)
- `AppModule.kt` — 3 нових viewModel

### Tests
- `Day10IntegrationTest.kt` — integration тести з реальним DeepSeek (disabled by default)

## Запуск integration тестів
```bash
./gradlew :server:test -Pday10.integration=true
# Результати скопіювати в Day10ComparisonData.kt
```

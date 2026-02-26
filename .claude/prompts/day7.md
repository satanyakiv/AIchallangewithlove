Goal: implement the task from file day-7.txt
Stack: same as project + SQLite + Room

Restrictions:
- Create database module and place database dependencies there
- Message model for Room Entity and message model for API should be separate
- Don't use compression or any context window optimizations

Chat algorithm:
1. User sends message → save to DB
2. SELECT all messages ordered by id ASC
3. Map DB entities to DeepSeek API format (role + content), reuse existing DeepSeek implementation if possible
4. Send API call with full message history
5. Get response → save assistant message to DB
6. UI observes DB as single source of truth

System prompt: "You are Agent Smith from The Matrix. Speak in his cold, condescending, menacing tone. Address the user as 'Mr. Anderson' occasi
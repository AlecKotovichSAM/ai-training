# Homework 1

## Reading reflection

	-	а. Building effective agents: не ставить агентам "размытые" цели
	-	б. Kiro + пост на LinkedIn: не работал, интересно попробовать (как понял, это централизация контекста дл всей команды). Мысль про большой контекст понятна.
	-	в. Effective context engineering - более прикладная и интересная статья. Больше подтвердились мои интуитивные предположения о том, как организовать контекст более эффективно (compaction, NOTES.md, subagents, tools, оптимизация system prompt)


## Workplace rubric

### Проект
Представляет из себя AI ассистент по сайту с использованием MCP tools, RAG и локально развернутой модели. Архитектурно - набор микросервисов на Java и .NET

### Feedback signals: 

*L2*

Есть CI на каждый merge request. Есть branch protection.
Pre-commit hooks, арх. тесты, линтеры - отсутствуют

### Context substrate: 

*L0*

Ничего нет, проект не использует AI для разработки.

### Blast radius control: 

*L0*

Только чат, код руками, проект не использует AI для разработки.

### Самое слабое место: 

На проекте не применяется AI.

### Первый шаг к решению проблемы:

Начать использовать агентов в IDE для дальнейшей разработки проекта


### Карта доступов

| Область | Состояние |
| --- | --- |
| GitLab | есть |
| CI/CD | есть (сборка, тесты, Docker образы, деплой при помощи Argo CD  |
| Issue tracker | есть (Jira) |
| Staging / Dev env | есть (DEV стейдж)  |
| Pre-commit hooks/ review bots / secrets | Частично (ревью нет, есть semantic release bot в GitLab, секреты в Vault для прода |
# Homework 1.5

## Добить учебный репозиторий Audit Log Service

✅

## Прочитать материалы

✅

## Посмотреть записи предыдущих митапов

TODO

## Подготовить инструменты

| Инструмент | Состояние |
| --- | --- |
| Codex App | ✅ |
| RTK / Rust Token Killer | ✅  |
| Claude Max / Opus / Max thinking настроены | ✅  |

## Рабочая задача:

-	Problem:

При встраивании чата с AI ассистентом происходит подавление запроса из-за CORS

-	Current behavior:

Cross-Origin Request Blocked: The Same Origin Policy disallows reading the remote resource at https://chat-ui.ai-assistant.*************.com/inject-mcp-chat.js. (Reason: CORS header ‘Access-Control-Allow-Origin’ does not match ‘https://ai-assistant.*************.com’).

-	Desired behavior:

Запрос проходит с 200 OK

-	Constraints:

nginx.conf подкладывается на сервере руками

-	How we verify success:

Открываем страницу, в которую встроен скрипт и получаем 200 ОК.


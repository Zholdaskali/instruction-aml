# Instructions Service

Учебный REST-сервис указаний по делу и уведомлений. Java 17, Spring Boot 3.5,
PostgreSQL.

## Запуск

Требуются JDK 17+ и Docker.

```bash
docker compose up -d
mvn spring-boot:run
```

Если Maven Wrapper отсутствует, используйте `mvn spring-boot:run`.

По умолчанию PostgreSQL доступен на `localhost:5433`, база `instructions_db`,
пользователь `instructions_user`, пароль `instructions_password`.

## Учебные пользователи

| Логин            | Пароль        | Роль         | Регион |
| ---------------- | ------------- | ------------ | ------ |
| supervisor-north | supervisor123 | SUPERVISOR   | NORTH  |
| anton            | anton123      | INVESTIGATOR | NORTH  |
| vera             | vera123       | INVESTIGATOR | NORTH  |
| supervisor-south | south123      | SUPERVISOR   | SOUTH  |

Тестовые данные создаются при первом запуске. Антон участвует в DEMO-001, Вера
находится в том же регионе, но не участвует в деле. DEMO-002 относится к SOUTH.

## API

### 1. Список дел руководителя

```bash
curl -u supervisor-north:supervisor123 http://localhost:8080/cases
```

### 2. Получатели по делу

Скопируйте `id` DEMO-001 из предыдущего ответа:

```bash
curl -u supervisor-north:supervisor123 \
  http://localhost:8080/cases/CASE_UUID/instruction-recipients
```

### 3. Отправить указание

Подставьте `caseId` DEMO-001 и `recipientId` Антона:

```bash
curl -i -u supervisor-north:supervisor123 \
  -H 'Content-Type: application/json' \
  -d '{"caseId":"CASE_UUID","recipientId":"ANTON_UUID","text":"Проверьте наличие акта № 7"}' \
  http://localhost:8080/instructions
```

Ожидаемый HTTP-статус: `201 Created`.

### 4. Уведомления Антона

```bash
curl -u anton:anton123 'http://localhost:8080/notifications?page=0&size=20'
curl -u anton:anton123 http://localhost:8080/notifications/unread-count
```

### 5. Прочитать уведомление

```bash
curl -i -X PATCH -u anton:anton123 \
  http://localhost:8080/notifications/NOTIFICATION_UUID/read
```

Повторный PATCH не меняет первоначальный `readAt`.

## Проверки ограничений

- Вера не появляется среди получателей DEMO-001.
- Попытка отправить указание Вере или по DEMO-002 от северного руководителя
  отклоняется.
- Уведомления выбираются только по username текущего пользователя.
- PATCH чужого уведомления возвращает `404`, не раскрывая его существование.
- Создание указания и уведомления выполняется в одном `@Transactional` методе.

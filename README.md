# Бот, который знает твоё расписание

### О функционале

##### Main.kt

Запускает бота и ежедневно обновляет расписание в базе данных

## Что нужно для запуска

### Java SE Development Kit 15.0.2

Чтобы проверить свою версию в консоли пропишите `java --version`

### Для транзакций с дб используется [Kotlin Exposed](https://github.com/JetBrains/Exposed)

Почитайте их [Wiki DataBase](https://github.com/JetBrains/Exposed/wiki/DataBase-and-DataSource), чтобы понять, какую
зависимость нужно добавить в build.gradle для работы с вашей бд и что прописать в `.env` (я по умолчанию добавил
зависимость для взаимодействия с sqlite в build.gradle).

### Так же здесь используется [dotenv](https://github.com/cdimascio/dotenv-kotlin)

Поэтому не забудьте добавить в корень файлик `.env` и пропишите внутри:

```
TOKEN = <ваш токен>
DATABASE_URL = <url дб>
DATABASE_DRIVER = <драйвер дб>
DATABASE_USER = <логин бд>
DATABASE_PASSWORD = <пароль бд>
```

## Как запустить бота

```
gradlew build 
java -jar build/libs/schedule_mtuci_bot-1.2-all.jar
```

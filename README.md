# Бот, который знает твоё расписание
##Обновил бота до версии 1.1 🔥

### О функционале

##### FilesToDb.kt

Теперь запускает только одну функцию putDataInDb(), которая скачивает временно файл, достаёт оттуда данные и удаляет его.

##### Main.kt

Запускает бота. Чтобы Main.kt нормально работал, нужно предварительно загрузить данные в базу данных.

## Что нужно для запуска

### Java SE Development Kit 15.0.2

Чтобы проверить свою версию в консоли пропишите `java --version`

### Для транзакций с дб используется [Kotlin Exposed](https://github.com/JetBrains/Exposed)

Почитайте их [Wiki DataBase](https://github.com/JetBrains/Exposed/wiki/DataBase-and-DataSource), чтобы понять, какую
зависимость нужно добавить в build.gradle для работы с вашей бд и что прописать в `.env` (я по умолчанию добавил
зависимость для взаимодействия с postgresql в build.gradle).

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
java -jar build/libs/schedule_mtuci_bot-1.0-all.jar
```

P.s. В build.gradle в mainСlassName указан MainKt, получается в jar файле у нас запускается только бот. FilesToDb.kt я
запускал отдельно (обычно через InteLLiJ IDEA). Можно, конечно, добавить функцию из FilesToDb.kt в Main.kt, чтобы при
запуске бота у нас сразу парсилось расписание, но я решил этого не делать т.к. планировал FilesToDb запускать отдельно
раз в день автоматически(бот то постоянно должен работать). Как запускать FilesToDb раз в день придумайте сами😁
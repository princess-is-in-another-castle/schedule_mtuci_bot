import dev.inmo.tgbotapi.bot.Ktor.telegramBot
import dev.inmo.tgbotapi.requests.send.SendTextMessage
import kotlinx.coroutines.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import Data.*
import dev.inmo.tgbotapi.extensions.api.send.sendMessage
import dev.inmo.tgbotapi.extensions.behaviour_builder.buildBehaviour
import dev.inmo.tgbotapi.extensions.behaviour_builder.expectations.waitText
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.*
import dev.inmo.tgbotapi.types.ParseMode.HTMLParseMode
import dev.inmo.tgbotapi.types.buttons.ReplyKeyboardMarkup
import dev.inmo.tgbotapi.types.buttons.SimpleKeyboardButton
import dev.inmo.tgbotapi.utils.matrix
import dev.inmo.tgbotapi.utils.row
import io.github.cdimascio.dotenv.dotenv
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import java.time.Duration
import java.util.concurrent.TimeUnit
import java.util.concurrent.Executors
import java.time.ZonedDateTime
import java.time.ZoneId





suspend fun main() {
    val dotenv = dotenv()
    val botToken = dotenv["TOKEN"].toString()
    val dataBaseUrl = dotenv["DATABASE_URL"].toString()
    val dataBaseDriver = dotenv["DATABASE_DRIVER"].toString()
    val dataBaseUser = dotenv["DATABASE_USER"].toString()
    val dataBasePassword = dotenv["DATABASE_PASSWORD"].toString()

    val pathFolder = dotenv["PATH_FILE"].toString()

    val url = "https://mtuci.ru/time-table/"

    val bot = telegramBot(botToken)

    val scope = CoroutineScope(Dispatchers.Default)





    Database.connect(dataBaseUrl,
        driver = dataBaseDriver,
        user = dataBaseUser,
        password = dataBasePassword)


    /** Я не буду описывать, как идёт взаимодействие с ботом, здесь полный мрак, но оно работает и почти без ошибок. */


    bot.buildBehaviour(scope = scope) {

        // Выдаёт клавиатуру с выбором
        val todayAndTomorrowMarkUp = ReplyKeyboardMarkup(
            matrix {
                row {
                    +SimpleKeyboardButton("На сегодня")
                    +SimpleKeyboardButton("На завтра")
                    +SimpleKeyboardButton("Другой день")
                }
                row {
                    +SimpleKeyboardButton("Изменить группу")
                }
            },
            resizeKeyboard = true,
        )

        val changeWeekMarkUp = ReplyKeyboardMarkup(
            matrix {
                row {
                    +SimpleKeyboardButton("Чётная")
                    +SimpleKeyboardButton("Нечётная")

                }
            },
            resizeKeyboard = true,
        )

        val changeDayMarkUp = ReplyKeyboardMarkup(
            matrix {
                row {
                    +SimpleKeyboardButton("Понедельник")
                    +SimpleKeyboardButton("Вторник")
                    +SimpleKeyboardButton("Среда")

                }
                row {
                    +SimpleKeyboardButton("Четверг")
                    +SimpleKeyboardButton("Пятница")
                    +SimpleKeyboardButton("Суббота")

                }
            },
            resizeKeyboard = true,
        )

        onCommand("start") {

            val chatId = it.chat.id

            // Получает название группы
            var academicGroup = waitText(
                SendTextMessage(
                    chatId,
                    "Привет, я знаю твоё расписание\n" + "Какая у тебя академическая группа?"
                )
            ).first().text.toUpperCase()

            val groups = getListOfGroups()

            while (!groups.contains(academicGroup)) {

                val possibleGroups = mutableListOf<String>()
                for (i in groups) {
                    if (i.contains(academicGroup)) {
                        possibleGroups += i
                    }
                }

                if (possibleGroups.isEmpty()) {
                    sendMessage(
                        it.chat.id,
                        "Таких групп нет, введите ещё раз"
                    )

                } else {
                    sendMessage(
                        it.chat.id,
                        "Возможно вы учитесь в одной из этих групп\n" +
                                possibleGroups.joinToString("\n")
                    )
                }
                academicGroup = waitText().first().text.toUpperCase()
            }


            transaction {
                addLogger(StdOutSqlLogger)
                SchemaUtils.create(Users)

                Users.insertIgnore {
                    it[id] = chatId.chatId.toString()
                    it[group] = academicGroup
                }
                Users.update({ Users.id eq chatId.chatId.toString() }) {
                    it[group] = academicGroup
                }

            }

            sendMessage(
                it.chat.id,
                "Ваша академическая группа $academicGroup",
                replyMarkup = todayAndTomorrowMarkUp
            )
        }

        onText(additionalFilter = { it.content.text == "Изменить группу" }) {
            val chatId = it.chat.id

            var academicGroup = waitText(
                SendTextMessage(chatId, "Введите новую академическую группу")
            ).first().text.toUpperCase()

            val groups = getListOfGroups()

            while (!groups.contains(academicGroup)) {

                val possibleGroups = mutableListOf<String>()
                for (i in groups) {
                    if (i.contains(academicGroup)) {
                        possibleGroups += i
                    }
                }

                if (possibleGroups.isEmpty()) {
                    sendMessage(
                        it.chat.id,
                        "Таких групп нет, введите ещё раз"
                    )
                } else {
                    sendMessage(
                        it.chat.id,
                        "Возможно вы учитесь в одной из этих групп\n" +
                                possibleGroups.joinToString("\n")
                    )
                }
                academicGroup = waitText().first().text.toUpperCase()
            }


            transaction {
                addLogger(StdOutSqlLogger)
                SchemaUtils.create(Users)

                Users.insertIgnore {
                    it[id] = chatId.chatId.toString()
                    it[group] = academicGroup
                }

                Users.update({ Users.id eq chatId.chatId.toString() }) {
                    it[group] = academicGroup
                }

            }

            sendMessage(
                chatId,
                "Группа $academicGroup успешно добавлена!",
                parseMode = HTMLParseMode,
                replyMarkup = todayAndTomorrowMarkUp
            )
        }

        onText(additionalFilter = { it.content.text == "На сегодня" }) {
            val chatId = it.chat.id

            sendMessage(
                it.chat.id,
                getSchedule(chatId),
                parseMode = HTMLParseMode,
                replyMarkup = todayAndTomorrowMarkUp
            )

        }

        onText(additionalFilter = { it.content.text == "На завтра" }) {
            val chatId = it.chat.id

            sendMessage(
                it.chat.id,
                getSchedule(chatId, 1),
                parseMode = HTMLParseMode,
                replyMarkup = todayAndTomorrowMarkUp
            )

        }

        onText(additionalFilter = { it.content.text == "Другой день" }) {


            sendMessage(
                it.chat.id,
                "Выберите неделю",
                replyMarkup = changeWeekMarkUp
            )

            val weekType = when (waitText().first().text.toUpperCase()) {
                "ЧЁТНАЯ", "ЧЕТНАЯ" -> "even"
                "НЕЧЁТНАЯ", "НЕЧЕТНАЯ" -> "odd"
                else -> "ошибка"
            }
            println(weekType)

            sendMessage(
                it.chat.id,
                "Выберите день",
                replyMarkup = changeDayMarkUp
            )

            val day = changeRussianToEnglishDay(
                waitText().first().text.toUpperCase()
            )
            println(day)

            val group = transaction {
                addLogger(StdOutSqlLogger)
                SchemaUtils.create(Users)
                Users.select {
                    (Users.id.eq(it.chat.id.chatId.toString()))
                }.first()[Users.group]
            }

            val checkWeekDay = transaction {
                addLogger(StdOutSqlLogger)
                SchemaUtils.create(Lessons)
                println(Lessons.id.eq(weekType + group + day))
                Lessons.select {
                    (Lessons.id.eq(weekType + group + day))
                }.firstOrNull()
            }

            if (checkWeekDay == null) {
                sendMessage(
                    it.chat.id,
                    "У вас выходной",
                    replyMarkup = todayAndTomorrowMarkUp
                )

            } else {
                val output = makeSchedule(group, weekType, day)
                sendMessage(
                    it.chat.id,
                    makeSchedule(group, weekType, day),
                    parseMode = HTMLParseMode,
                    replyMarkup = todayAndTomorrowMarkUp
                )
            }
        }

        /** Последние триггера на картинку и стикер были написаны по фану.
         * У вас же бывало, когда вы боту скидываете случайно что-то. Он на такое отреагирует. */
        onPhoto {
            sendMessage(it.chat.id, "is this for me? " + "\uD83E\uDD7A" + "\uD83D\uDC49" + "\uD83D\uDC48")
        }

        onSticker {
            sendMessage(it.chat.id, "\uD83E\uDD21")
        }
    }


    /** Код ниже запускает добавление данных в дб раз в день в какое-то установленное время.
     * Возможно позже будет переписано.*/

    val now = ZonedDateTime.now(ZoneId.of("Europe/Moscow"))
    var nextRun = now.withHour(3).withMinute(0).withSecond(0)
    if (now.compareTo(nextRun) > 0) nextRun = nextRun.plusDays(1)

    val duration: Duration = Duration.between(now, nextRun)
    val initalDelay: Long = duration.getSeconds()

    val scheduler = Executors.newScheduledThreadPool(1)
    scheduler.scheduleAtFixedRate(
        { println("Curent Time is ${ZonedDateTime.now(ZoneId.of("Europe/Moscow"))}")
            println("Начинаю обновление базы данных")
            putDataInDb(url, pathFolder)
            println("Обновление базы данных закончено")
        },
        initalDelay,
        TimeUnit.DAYS.toSeconds(1),
        TimeUnit.SECONDS
    )

    scope.coroutineContext.job.join()

}

/** копирует список групп из базы данных и возвращает его.
 * Вообще костыль, потому что лучше искать по базе данных группу, а не копировать список групп. */
fun getListOfGroups(): List<String> {

    val groups = mutableListOf<String>()
    transaction {
        Groups.selectAll().forEach {
            groups.add(it[Groups.groups])
        }
    }

    return groups.toList()
}
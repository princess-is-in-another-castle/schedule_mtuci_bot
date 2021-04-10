
import org.jetbrains.exposed.sql.transactions.transaction
import  Data.*
import dev.inmo.tgbotapi.types.ChatId
import org.jetbrains.exposed.sql.*

/** Получает id чата и парамент plusDays, в котором добавляет количество дней вперёд, чтобы узнать занятие.
 * plusDays используется, когда выводится расписание "На завтра".
 * Функция возращает готовое расписание*/
fun getSchedule(chatId: ChatId, plusDays: Int = 0): String {
    // из-за того, что код повторялся этот (а меня это напрягало), я вынес его в отдельную функцию
    // тип недели
    val weekType = getWeekType()
    // день недели
    val weekday = getWeekday(plusDays)
    // группа
    var group = transaction {
        addLogger(StdOutSqlLogger)
        SchemaUtils.create(Users)
        Users.select {
            (Users.id eq (chatId.chatId.toString()))
        }.first()[Users.group]
    }

    transaction {
        SchemaUtils.create(Lessons)
        Lessons.select {
            (Lessons.id.eq(weekType + group + weekday))
        }.firstOrNull()
    } ?: return "У вас выходной"

    return makeSchedule(group, weekType, weekday)
}

/** создаёт расписание дня и возращает его.
 * На вход получает группу, тип дня(чёт, нечёт) и какой день */
fun makeSchedule(group: String, weekType: String, day: String): String {

    var output = "<b>Ваше расписание</b>\n" +
                 "<b><u>1 пара (9.30-11.05):</u></b>\n"

    output += transaction {
        addLogger(StdOutSqlLogger)
        SchemaUtils.create(Lessons)
        Lessons.select {
            (Lessons.id.eq(weekType + group + day))
        }.single()[Lessons.first]
    }

    output += "\n\n<u><b>2 пара (11.20-12.55)</b></u>:\n"
    output += transaction {
        addLogger(StdOutSqlLogger)
        SchemaUtils.create(Lessons)
        Lessons.select {
            (Lessons.id.eq(weekType + group + day) and Lessons.weekDay.eq(day))
        }.single()[Lessons.second]
    }

    output += "\n\n<u><b>3 пара (13.10-14.45):</b></u>\n"
    output += transaction {
        addLogger(StdOutSqlLogger)
        SchemaUtils.create(Lessons)
        Lessons.select {
            (Lessons.id.eq(weekType + group + day) and Lessons.weekDay.eq(day))
        }.single()[Lessons.third]
    }

    output += "\n\n<u><b>4 пара (15.25-17.00):</b></u>\n"
    output += transaction {
        addLogger(StdOutSqlLogger)
        SchemaUtils.create(Lessons)
        Lessons.select {
            (Lessons.id.eq(weekType + group + day) and Lessons.weekDay.eq(day))
        }.single()[Lessons.fourth]
    }

    output += "\n\n<u><b>5 пара (17.15-18.50):</b></u>\n"
    output += transaction {
        addLogger(StdOutSqlLogger)
        SchemaUtils.create(Lessons)
        Lessons.select {
            (Lessons.id.eq(weekType + group + day) and Lessons.weekDay.eq(day))
        }.single()[Lessons.fifth]
    }
    return output
}




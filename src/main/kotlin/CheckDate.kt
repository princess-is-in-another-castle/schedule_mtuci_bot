
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import org.joda.time.Weeks
import java.time.DayOfWeek

/** Возвзвращает номер текущей недели. Высчитывает, начиная с первого дня учебного дня семестра
 * Сейчас здесь прописано 1 февраля 2021г. */
fun numberOfWeek(): Int {
    // Возвзвращает номер текущей недели
    val myZone: DateTimeZone = DateTimeZone.forID("Europe/Moscow")
    DateTimeZone.setDefault(myZone)
    val dateTime1 = DateTime(2021, 2, 1, 0, 0)
    val dateTime2 = DateTime()
    return Weeks.weeksBetween(dateTime1, dateTime2).weeks + 1
}

/** Возвращает тип недели, чётная или нечётная */
fun getWeekType(): String {
    return when (numberOfWeek() % 2) {
        1 -> "odd"
        else -> "even"
    }
}

/** возвращает английское название дня недели заглавными буквами */
fun getWeekday(i: Int = 0): String {
    val weekDay = DateTime().plusDays(i)
    return DayOfWeek.of(weekDay.dayOfWeek).toString()
}

/** костыль, который меняет название дня недели с русского на английский */
fun changeRussianToEnglishDay(day: String): String {
    val days = listOf(
        "MONDAY" to "ПОНЕДЕЛЬНИК", "TUESDAY" to "ВТОРНИК",
        "WEDNESDAY" to "СРЕДА", "THURSDAY" to "ЧЕТВЕРГ",
        "FRIDAY" to "ПЯТНИЦА", "SATURDAY" to "СУББОТА", "SUNDAY" to "ВОСКРЕСЕНЬЕ"
    )
    for (dayCheck in days) {
        if (dayCheck.second == day) {
            return dayCheck.first
        }
    }
    return "ошибка"
}
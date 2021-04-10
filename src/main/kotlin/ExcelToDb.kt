import org.apache.poi.ss.usermodel.Cell
import org.apache.poi.ss.usermodel.Sheet
import org.apache.poi.ss.usermodel.WorkbookFactory
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.FileInputStream
import java.io.FileOutputStream
import org.apache.poi.ss.util.SheetUtil.getCell
import org.apache.poi.ss.util.SheetUtil.*
import Data.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

/**
 * У нас каждый день идёт размером в 20 строк
 * то есть всего 5 занятий, каждое по 4 строки.
 * В одном занятии у на два разных представления данных:
 *      -либо нечетное занятие 2 ячейки сверху и четное 2 ячейки снизу
 *      -либо этот день объединён в одно постоянное занятие. Здесь способ представления данных может варьироваться.
 *       У нас может быть как объединёная большая ячейка (merged cell), так и расписанное занятие отдельно по 4 ячейкам.
 */


/** на вход получает таблицу с расписанием и название группы. Проверяет, есть ли суббота и дальше передаёт
 *  значения в getDay() */
fun getTimeTable(sheet: Sheet, group: String) {
    // изначально в findCell() мы получаем координаты ячейки с названием группы. Но чтобы спустится на расписание дней,
    // нам нужно на какое количество ячеек спустися вниз по строкам. Поэтому создан лист из пар, где, соответственно,
    // указан день недели и сколько нужно добавить клеточек, чтобы переместиться на его первую строку.
    val weekDays = listOf(
        "MONDAY" to 1, "TUESDAY" to 21,
        "WEDNESDAY" to 41, "THURSDAY" to 61,
        "FRIDAY" to 81, "SATURDAY" to 101
    )

    val (row, col) = findCell(sheet, group)
    // бредово написанная проверка на то, есть ли суббота в расписании
    if (hasSaturdayOrNot(sheet, row, col)) {
        for (i in 0..5) {
            getDay(sheet, row + weekDays[i].second, col, group, weekDays[i].first)
        }
    } else {
        for (i in 0..4) {
            getDay(sheet, row + weekDays[i].second, col, group, weekDays[i].first)
        }
    }

}

/** Получает на вход расписание, первую строку начала ячеек дня и колонку группы, название группы, день.
 * День записывает в базу */
fun getDay(sheet: Sheet, row: Int, col: Int, group: String, day: String) {
    val weeks = listOf("odd", "even")
    for (type in weeks) {
        transaction {
            SchemaUtils.create(Lessons)

            // insertIgnore и update делают в теорию простую вещь. Если нету элемента, добавить, если есть, обновить.
            // чтобы insertIgnore игнорировал уже созданную строку таблицы, был написан лютейший костыль. В базе данных
            // добавлен ещё один параметр id с типом UNIQUE, который отвечает за то, что строка может быть только одна.
            // id состоит из "тип_недели"+"название_группы"+"день_недели". Кстати, если элемента нету, то он
            // сначала его вставляет, а потом обновляет, такие дела
            Lessons.insertIgnore() {
                it[id] = type + group + day
                it[groupName] = group
                it[weekDay] = day
                it[typeOfWeek] = type
                it[first] = getLesson(sheet, row, col, type)
                // У нас одно занятие занимает 4 ячейки, поэтому к row прибавляются числа, делящиеся на 4, чтобы
                // перемещаться по занятиям.
                it[second] = getLesson(sheet, row + 4, col, type)
                it[third] = getLesson(sheet, row + 8, col, type)
                it[fourth] = getLesson(sheet, row + 12, col, type)
                it[fifth] = getLesson(sheet, row + 16, col, type)
            } get (Lessons.groupName)

            Lessons.update({ Lessons.id eq type + group + day }) {
                it[groupName] = group
                it[weekDay] = day
                it[typeOfWeek] = type
                it[first] = getLesson(sheet, row, col, type)
                it[second] = getLesson(sheet, row + 4, col, type)
                it[third] = getLesson(sheet, row + 8, col, type)
                it[fourth] = getLesson(sheet, row + 12, col, type)
                it[fifth] = getLesson(sheet, row + 16, col, type)
            }
        }
    }

}

/** На вход функция получает расписание, номер строки, с которой начинается это занятие и номер столбца.
 * Возращает занятие. */
fun getLesson(sheet: Sheet, row: Int, col: Int, typeOfWeek: String): String {
    var lesson = ""
    // Если есть разделение недели, то
    if (isBottomBorderThin(sheet, row + 1, col)) {
        when (typeOfWeek) {
            "odd" -> { // парсит первые две ячейки для нечетной недели
                for (i in row..row + 1) {
                    lesson += "${getMergedCell(sheet, i, col)}"

                    if (i != row + 1) lesson += "\n"
                }
            }
            "even" -> { // парсит последние две ячейки для четной недели
                for (i in row + 2..row + 3) {
                    lesson += "${getMergedCell(sheet, i, col)}"

                    if (i != row + 3) lesson += "\n"
                }
            }
        }
    } else { // Если нету разделения на недели, то
        for (i in row..row + 3) { // парсит все 4 ячейки
            lesson += "${getMergedCell(sheet, i, col)}"
            // if для того, чтобы в конце не ставился лишний перенос строки
            // это можно написать какой-нибудь нормальной функцией, но мне лень
            if (i != row + 3) lesson += "\n"
        }
    }
    return lesson
}

/** В excel есть такая штука, как объединённые ячейки (брр). В этих объединённых ячейках значение хранится только в
 * самой первой (сверху слева), в остальных null. Эта функция позволяет в каждой ячейке получать значение */
fun getMergedCell(sheet: Sheet, row: Int, col: Int): Cell? {
    // если ячейки объединены, то возращает значение первой ячейки, иначе возращает значение самой ячейки
    val c = getCell(sheet, row, col)
    // перебирает все регионы с объединённый ячейками
    for (mergedRegion in sheet.mergedRegions) {
        if (mergedRegion.isInRange(row, col)) {
            // The cell wanted is in this merged range
            // Return the primary (top-left) cell for the range
            val r = sheet.getRow(mergedRegion.firstRow)
            return r.getCell(mergedRegion.firstColumn)
        }
    }
    return c
    // Вообще это изменённый код функции getCellWithMerges() (она у меня не работала нормально). Костыль.
}

/** находит ячейку по содержанию, например "биб2001", и выдаёт строку и столбец этой ячейки.
 * Если ячейки нет, то возращает пару "0 to 0". Это типо нулевой ошибки.
 * Мне было лень разбираться с настоящим null...*/
fun findCell(sheet: Sheet, cellContent: String): Pair<Int, Int> {

    for (row in sheet) {
        for (column in row) {
            // просто перебирает все ячейки, пока не наткнётся на нужную
            if (column.toString().contains(cellContent)) {

                return column.rowIndex.toInt() to column.columnIndex.toInt()
            }
        }
    }
    // если не нашлось, возращает координаты первой ячейки в таблице. Это типо null значения, которое потом проверяется
    // перед парсингом расписания. Да, это костыль
    return 0 to 0 // row to column
}

/** Получает на вход расписание и строку, столбец ячейки с названием группы!
 *  Можно переписать, чтобы на вход получала название группы, искала её и тд, но мне впадлу, и так работает */
fun hasSaturdayOrNot(sheet: Sheet, row: Int, col: Int): Boolean {
    // переменная row спускается с ячейки группы на одну вниз, чтобы при переходе по ячейкам влево обязательно
    // наткнулась на понедельник
    val row = row + 1
    var col = col

    do { // выполняется, пока не наткнётся на ячейку с содержанием "Понедельник"
        col -= 1
    } while (getMergedCell(sheet, row, col).toString() != "Понедельник")
    // Если спустится на 100 ячеек вниз от первой ячейки понедельника, можно наткнутся на ячейку с содержанием "Суббота"
    // Один день занимает 20 ячеек, поэтому спускаемся на 100 вниз.
    if (getMergedCell(sheet, row + 100, col).toString() == "Суббота") {
        return true
    }
    return false
}

/** Настоящая killer фича при составлении расписания. На вход получает расписание и вторую ячейку в расписании пары,
 * считая сверху вниз. Сейчас объясню: у нас в расписание пары 4 ячейки(проверенное опытным путём). Пара может быть
 * разделена на четную и нечетную, а может быть общей. И чтобы у нас втупую не записывались 2 ячейки сверху в нечётную
 * и 2 снизу в чётную, добавлена эта проверка на то, есть ли разделение в паре.
 * Разделение, как правило, находится в середине, то есть у второй ячейки снизу или у третей сверху,  считая сверху вниз.
 * До сих пор непонятно? посмотри на расписание*/
fun isBottomBorderThin(sheet: Sheet, row: Int, col: Int): Boolean {
    // используется для проверки нижней border у ячейки на толщину. Важный элемент проверки на разделение расписания.
    if (getMergedCell(sheet, row, col)?.cellStyle?.borderBottomEnum.toString() == "THIN") {
        return true
    }
    return false
}

/** Эта функция автоматом парсит все группы из расписания. Для в функцию передаётся расписание.
 *  Дальше она находит первую! ячейку с содержанием "Понедельник" (опытным путём доказано, что если подняться на одну
 *  ячейку вверх от понедельника, то на этой строке будут находиться все названия групп.*/
fun getGroups(sheet: Sheet) {
    var (row, col) = findCell(sheet, "Понедельник")
    row--
    // начинает перебирать все ячейки по строке до конца
    for (i in col..sheet.getRow(0).lastCellNum) {
        var cell = getMergedCell(sheet, row, i).toString()
        // проверяет, есть ли в названии где-либо подряд идущие заглавные буквы, а за ними цифры
        if (cell.matches("""(.*)([А-Я]+\d+)(.*)""".toRegex())) {
            // удаляет всё, после слэшей. То есть из "  БУП2077  //322" получится "  БУП2077  "
            cell = cell.replace(regex = """\\.*""".toRegex(), "")
            cell = cell.replace(regex = """\/.*""".toRegex(), "")
            // удаляет все пробелы в начале. Получается "БУП2077  "
            cell = cell.replace(regex = """^\ +""".toRegex(), "")
            // удаляет все пробелы в конце. Получается "БУП2077"
            cell = cell.replace(regex = """\ +$""".toRegex(), "")

            transaction {
                SchemaUtils.create(Groups)
                Groups.insertIgnore {
                    it[groups] = cell
                    println("Группа $cell успешно добавлена в базу")
                }
            }

        }
    }
}

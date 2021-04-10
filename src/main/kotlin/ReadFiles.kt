
import java.io.File
import java.io.FileInputStream
import org.apache.poi.ss.usermodel.Workbook
import org.apache.poi.ss.usermodel.WorkbookFactory
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import Data.*

/** Получает путь до папки с расписаниями.
 * По всем файлам в папке(а там должны быть только доки с расписанием)
 * ищет и добавляет группы в базу, ищет и добавляет расписание групп в базу.
 * Можно же сразу находить группу и добавлять её расписание? Можно, но это надо переписывать код.*/
fun readFiles(pathFolder: String) {
    // код для того, чтобы прочитать все имеющиеся файлы в папке
    val folder = File(pathFolder)
    val listOfFiles: Array<File> = folder.listFiles()

    for (file in listOfFiles) {
        if (file.isFile) {
            putUsersInDb(file.path)
        }
    }

    for (file in listOfFiles) {
        if (file.isFile) {
            putTimeTableInDb(file.path)
        }
    }
}

/** Получает путь до файла расписания.
 * Добавляет группы в базу. */
fun putUsersInDb(filepath: String) {
    val input = FileInputStream(filepath)
    val workbook: Workbook = WorkbookFactory.create(input)
    val sheet = workbook.getSheetAt(0)

    getGroups(sheet)

}

/** Получает путь до файла расписания.
 * Ищет группы в документе и добавляет их расписание в базу.*/
fun putTimeTableInDb(filepath: String) {
    val input = FileInputStream(filepath)
    val workbook: Workbook = WorkbookFactory.create(input)
    val sheet = workbook.getSheetAt(0)

    val groups = getListOfGroups()

    for (group in groups) {
        if (findCell(sheet, group) != 0 to 0) {
            getTimeTable(sheet, group)
            println("Группа $group успешно добавлена в базу")
        }
    }

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

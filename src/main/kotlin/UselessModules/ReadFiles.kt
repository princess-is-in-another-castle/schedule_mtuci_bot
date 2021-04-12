import java.io.File
import java.io.FileInputStream
import org.apache.poi.ss.usermodel.Workbook
import org.apache.poi.ss.usermodel.WorkbookFactory


/** Получает путь до папки с расписаниями.
 * По всем файлам в папке(а там должны быть только доки с расписанием)
 * ищет и добавляет группы в базу, ищет и добавляет расписание групп в базу.
 * Можно же сразу находить группу и добавлять её расписание? Можно, но это надо переписывать код.
 * Кстати, код переписан и теперь эта функция подлежит удалению*/
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
 * Добавляет группы в базу.
 *  Уже не нужно*/
fun putUsersInDb(filepath: String) {
    val input = FileInputStream(filepath)
    val workbook: Workbook = WorkbookFactory.create(input)
    val sheet = workbook.getSheetAt(0)

    getGroups(sheet)

}

/** Получает путь до файла расписания.
 * Ищет группы в документе и добавляет их расписание в базу.
 *  Уже не нужно*/
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

/** Скачивает все файлы по url в папку
 *  Функцию уже нет смысла использовать */
fun getFiles(url: String, pathFolder: String) {

    deleteAllFilesInFolder(pathFolder)

    val listNews = getHtmlAttributes(url)
    // перебирает все элементы
    for (element in listNews) {
        // если расписание не для ЦЗОПБ и ЦЗОПМ
        if (!(element.text().contains("ЦЗОПБ")) and
            !(element.text().contains("ЦЗОПМ")) and
            !(element.text().contains("СиСС - 11.04.02 (71,75)"))
        ) { // просто бредовое расписание

            // название файла. Ахтунг!, в названии файла указана сначала папка files/
            val fileName = element.attr("href").removePrefix("files/")
            //вывести ссылку название аттрибута в консоль
            println(fileName)

            getFile(url, fileName, pathFolder)
        }
    }
}
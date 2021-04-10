import org.apache.commons.io.FileUtils
import org.jsoup.Jsoup
import org.jsoup.select.Elements
import java.io.File
import java.net.URL

/** Скачивает все файлы по url в папку */
fun getFiles(url: String, pathFolder: String) {

    deleteAllFilesFolder(pathFolder)

    val listNews = getHtmlAttributes(url)
    // перебирает все элементы
    for (element in listNews) {
            // если расписание не для ЦЗОПБ и ЦЗОПМ
            if (!(element.text().contains("ЦЗОПБ")) and
                !(element.text().contains("ЦЗОПМ")) and
                !(element.text().contains("СиСС - 11.04.02 (71,75)"))  ) { // просто бредовое расписание

                // название файла. Ахтунг!, в названии файла указана сначала папка files/
                val fileName = element.attr("href").removePrefix("files/")
                //вывести ссылку название аттрибута в консоль
                println(fileName)

                getFile(url, fileName, pathFolder)
            }
        }
}

/** функция возращает элементы, имеющие в названии подстроку "Расписание занятий" */
fun getHtmlAttributes(url: String): Elements {
    // функция возращает элементы, имеющие в названии подстроку "Расписание занятий"
    // парсит html страницу
    val doc = Jsoup.connect(url)
        .userAgent("Mozilla")
        .get()

    // достаёт все элементы, у которых аттрибут в названии имеет подстроку "Расписание занятий"
    val listNews = doc.getElementsByAttributeValueContaining("href", "Расписание занятий")
    return listNews
}

/** на вход получает url сайта, имя файла и путь к папке.
 *  Закидывает файл в папку */
fun getFile(url: String, fileName:String, pathFolder: String) {
    // создаёт ссылку, где будет храниться файл (путь + название файла)
    val pathFile = pathFolder + fileName
    // создаёт ссылку на файл на сайте
    val fileUrl = url + "files/" +fileName
    // скачивает файл по url в папку
    FileUtils.copyURLToFile(
        URL(fileUrl),
        File(pathFile)
    )
    println("файл успешно спизжен")
}

/** очищает папку от файлов.
 * Будет полезно, когда зальют новое расписание, а оно будет отличаться по названию от старого.*/
fun deleteAllFilesFolder(path: String) {

    val folder = File(path)
    val listOfFiles = folder.listFiles()
    for (myFile in listOfFiles)
        if (myFile.isFile) myFile.delete()
}


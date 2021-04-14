import io.ktor.http.*
import org.apache.commons.io.FileUtils
import org.apache.poi.ss.usermodel.Workbook
import org.apache.poi.ss.usermodel.WorkbookFactory
import org.jsoup.Jsoup
import org.jsoup.select.Elements
import java.io.File
import java.io.FileInputStream
import java.net.URL
import java.net.URLEncoder
import javax.net.ssl.HttpsURLConnection

/** Получает на вход ссылку на сайт и папку для временных файлов
 *  Каждый файл скачивает, потом отправляет на обработку для добавления данных в дб, после обработки файл удаляется. */
fun putDataInDb(url: String, pathFolder: String) {
    // Список с именами файлов
    val docNames = getDocNames(getHtmlAttributes(url))
    docNames.forEach { docName ->
        downloadFile(
            url = url,
            fileName = docName,
            pathFolder = pathFolder
        )
        val folder = File(pathFolder)
        folder.listFiles().forEach { doc ->
            val input = FileInputStream(doc)
            val workbook: Workbook = WorkbookFactory.create(input)
            val sheet = workbook.getSheetAt(0)
            getGroups(sheet)
        }
        deleteAllFilesInFolder(pathFolder)

    }

}


/** Получает ссылку на сайт и название файла.
 *  Возвращает ссылку на файл на сайте для дальнейшего скачивания. */
fun getFileUrl(url: String, fileName: String): String {
    // нужно кодировать название файла в UTF-8, иначе он не сможет его скачать и выдаст ошибку
    return url + "files/" + URLEncoder.encode(fileName,  "UTF-8").replace("+", "%20")
}


/** Получает на вход путь до папки и название файла.
 *  Возвращает путь до файла в этой папке. */
fun getPathToFile(pathFolder: String, fileName: String): String {
    return pathFolder + fileName
}


/** Возвращает список названий документов */
fun getDocNames(listOfElements: Elements): List<String> {
    val namesOfDocs = mutableListOf<String>()
    listOfElements.forEach { element ->
        element.takeUnless{
            // если расписание не для ЦЗОПБ, ЦЗОПМ и магистров 11.04.02 (71,75), расписание которых спарсить нормально не получится
                    (it.text().contains("ЦЗОПБ")) or
                    (it.text().contains("ЦЗОПМ")) or
                    (it.text().contains("СиСС - 11.04.02 (71,75)"))
        }?.apply {
            // название файла. Ахтунг!, в названии файла указана вначале папка files/, поэтому удаляем её
            val fileName = this.attr("href").removePrefix("files/")
            //вывести название файла в консоль
            println(fileName)
            namesOfDocs.add(fileName)
        }
    }
    return namesOfDocs.toList()
}


/** Качает файл в папку*/
fun downloadFile(url: String, fileName: String, pathFolder: String) {
    val fileUrl = getFileUrl(url, fileName)
    val filePath = getPathToFile(pathFolder, fileName)
    FileUtils.copyURLToFile(
        URL(fileUrl),
        File(filePath),)
    println("Файл $fileName успешно скачан")
}


/** На входе получает ссылку на сайт.
 * Возвращает элементы "href" c html страницы, имеющие в названии подстроку "Расписание занятий". */
fun getHtmlAttributes(url: String): Elements {
    val doc = Jsoup.connect(url)
        .userAgent("Mozilla")
        .get()
    // достаёт все элементы, у которых аттрибут в названии имеет подстроку "Расписание занятий"
    return doc.getElementsByAttributeValueContaining("href", "Расписание занятий")
}


/** очищает папку от файлов.*/
fun deleteAllFilesInFolder(folderPath: String) {
    File(folderPath).listFiles().forEach { file ->
        file.delete()
    }
}


/** на вход получает url сайта, имя файла и путь к папке.
 *  Закидывает файл в папку */
fun getFile(url: String, fileName: String, pathFolder: String) {
    // создаёт ссылку, где будет храниться файл (путь + название файла)
    val pathToFile = getPathToFile(pathFolder, fileName)
    // создаёт ссылку на файл на сайте
    val fileUrl = getFileUrl(url, fileName)
    // скачивает файл по url в папку
    FileUtils.copyURLToFile(
        URL(fileUrl),
        File(pathToFile)
    )
    println("файл успешно скачан")
}



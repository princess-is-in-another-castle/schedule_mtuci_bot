import io.github.cdimascio.dotenv.dotenv
import org.jetbrains.exposed.sql.Database

/** В этом файле я обычно скачивал файлы и загружал данные в базу данных */

fun main() {
    val dotenv = dotenv()
    val dataBaseUrl = dotenv["DATABASE_URL"].toString()
    val dataBaseDriver = dotenv["DATABASE_DRIVER"].toString()
    val dataBaseUser = dotenv["DATABASE_USER"].toString()
    val dataBasePassword = dotenv["DATABASE_PASSWORD"].toString()

    Database.connect(dataBaseUrl,
        driver = dataBaseDriver,
        user = dataBaseUser,
        password = dataBasePassword)

    val url = "https://mtuci.ru/time-table/"
    val pathFolder = "./src/main/resources/data/ExcelFiles/"

    // скачивает все файлы папку
    getFiles(url, pathFolder)
    // читает файлы и записывает нужную информацию в базу данных
    readFiles(pathFolder)
}
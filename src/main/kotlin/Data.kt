import org.jetbrains.exposed.sql.Table


/** У нас в базе данных долнжо быть 3 таблицы: users, lessons, groups
 * все столбцы групп перечислены ниже. Все данные в таблице представлены в виде текста */
class Data() {
    object Users : Table() {
        val id = varchar("id", 50)
        val group = varchar("group", length = 20)
    }

    object Lessons : Table() {
        val id = varchar("id", length = 40)
        val groupName = varchar("group", length = 20)
        val typeOfWeek = varchar("typeofweek", length = 10)
        val weekDay = varchar("weekday", length = 10)
        val first = varchar("first", 1000)
        val second = varchar("second", 1000)
        val third = varchar("third", 1000)
        val fourth = varchar("fourth", 1000)
        val fifth = varchar("fifth", 1000)
    }

    object Groups : Table() {
        val groups = varchar("groups", length = 20)
    }

}



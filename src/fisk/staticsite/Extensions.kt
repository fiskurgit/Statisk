package fisk.staticsite

import java.io.File


fun File.dir(): File {
    val dirPath = this.absolutePath.substring(0, this.absolutePath.lastIndexOf("/"))
    return File(dirPath)
}

//Expect path in form: /Users/670m/pi/fisk_solar_website/2019/07/25/
fun File.dateLabel(): String {
    if(name.endsWith(".html")){
        var path = this.path.substring(0, this.path.lastIndexOf("/"))

        val day = path.takeLast(2)

        path = path.substring(0, path.lastIndexOf("/"))

        val month = path.takeLast(2)

        path = path.substring(0, path.lastIndexOf("/"))

        val year = path.takeLast(4)

        val dateLabel = "$day/$month/$year"

        Out.d("date label: $dateLabel")

        return dateLabel
    }else{
        return "ERROR"
    }
}

fun String.isYear(): Boolean {
    return when {
        this.length == 4 -> {
            val i = this.toIntOrNull()
            when(i) {
                null -> false
                else -> true
            }
        }
        else -> false
    }
}

fun String.isMonthOrDay(): Boolean {
    return when {
        this.length == 2 -> {
            val i = this.toIntOrNull()
            when (i) {
                null -> false
                else -> true
            }
        }
        else -> false
    }
}
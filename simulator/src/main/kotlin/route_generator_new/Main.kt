package route_generator_new

import java.nio.file.Files
import java.nio.file.Paths

fun main() {
    //1 получаю конфигурацию домов из xodr файла TODO передать рустаму какие данные мне от него нужны
    //2 инициализация файла Model.


    var resourceReader = ResourceReader()
    val content = resourceReader.readTextResource("travel_desire_function_const.json")
    println(content)


}



class ResourceReader {
    fun readTextResource(filename: String): String {
        val uri = this.javaClass.getResource("/$filename").toURI()
        return Files.readString(Paths.get(uri))
    }
}

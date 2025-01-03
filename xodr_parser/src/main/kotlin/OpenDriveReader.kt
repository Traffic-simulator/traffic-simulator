import jakarta.xml.bind.JAXBContext
import jakarta.xml.bind.JAXBException
import opendrive.OpenDRIVE

import java.io.FileReader
import java.io.IOException
import java.nio.file.Paths

class OpenDriveReader {
    @Throws(JAXBException::class, IOException::class)
    fun read(filename: String): OpenDRIVE {
        val context: JAXBContext = JAXBContext.newInstance(OpenDRIVE::class.java)
//        println(Paths.get("src/main/resources/$filename").toAbsolutePath())
        return context.createUnmarshaller()
            .unmarshal(FileReader("src/main/resources/$filename")) as OpenDRIVE
    }
}

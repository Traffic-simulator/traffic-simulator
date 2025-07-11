import jakarta.xml.bind.JAXBContext
import jakarta.xml.bind.JAXBException
import opendrive.OpenDRIVE
import java.io.File
import java.io.FileNotFoundException

import java.io.FileReader
import java.io.IOException
import java.io.InputStream
import java.nio.file.Paths

class OpenDriveReader {
    @Throws(JAXBException::class, IOException::class)
    fun read(filename: String): OpenDRIVE {
        val context: JAXBContext = JAXBContext.newInstance(OpenDRIVE::class.java)
        val reader = try {
            FileReader("src/main/resources/$filename")
        } catch (e: FileNotFoundException) {
            FileReader("../src/main/resources/$filename")
        }
        return context.createUnmarshaller().unmarshal(reader) as OpenDRIVE
    }

    @Throws(JAXBException::class, IOException::class)
    fun readUsingFileReader(inputStream: InputStream): OpenDRIVE {
        val context: JAXBContext = JAXBContext.newInstance(OpenDRIVE::class.java)
        val unmarshallData = context
                                .createUnmarshaller()
                                .unmarshal(inputStream)

        return unmarshallData as OpenDRIVE
    }
}

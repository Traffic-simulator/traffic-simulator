import jakarta.xml.bind.JAXBContext
import jakarta.xml.bind.JAXBException
import jakarta.xml.bind.Marshaller
import opendrive.OpenDRIVE
import java.io.*


class OpenDriveWriter {
    @Throws(JAXBException::class, IOException::class)
    fun write(drive: OpenDRIVE, filename: String) {
        val context: JAXBContext = JAXBContext.newInstance(opendrive.OpenDRIVE::class.java)
        val marshaller: Marshaller = context.createMarshaller()
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, java.lang.Boolean.TRUE)

        val prefix = "src/main/resources/export/"

        File(prefix).mkdirs()
        val file = File("$prefix$filename")
        if (!file.exists()) {
            file.createNewFile()
        }

        marshaller.marshal(drive, FileWriter(file))
    }
}

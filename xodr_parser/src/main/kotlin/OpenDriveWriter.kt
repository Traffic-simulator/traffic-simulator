import jakarta.xml.bind.JAXBContext
import jakarta.xml.bind.JAXBException
import jakarta.xml.bind.Marshaller
import opendrive.OpenDRIVE
import java.io.*
import java.io.IOException
import java.io.StringWriter


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

    @Throws(JAXBException::class, IOException::class)
    fun toString(drive: OpenDRIVE): String {
        val context: JAXBContext = JAXBContext.newInstance(OpenDRIVE::class.java)
        val marshaller: Marshaller = context.createMarshaller()
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true)

        val stringWriter = StringWriter()
        marshaller.marshal(drive, stringWriter)

        return stringWriter.toString()
    }
}

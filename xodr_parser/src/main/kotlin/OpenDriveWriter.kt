import jakarta.xml.bind.JAXBContext
import jakarta.xml.bind.JAXBException
import jakarta.xml.bind.Marshaller
import opendrive.OpenDRIVE
import java.io.FileNotFoundException
import java.io.FileWriter
import java.io.IOException
import java.io.StringWriter


class OpenDriveWriter {
    @Throws(JAXBException::class, IOException::class)
    fun write(drive: OpenDRIVE, filename: String) {
        val context: JAXBContext = JAXBContext.newInstance(opendrive.OpenDRIVE::class.java)
        val marshaller: Marshaller = context.createMarshaller()
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, java.lang.Boolean.TRUE)

        val writer = try {
            FileWriter("src/main/resources/$filename")
        } catch (e: FileNotFoundException) {
            FileWriter("../src/main/resources/$filename")
        }

        marshaller.marshal(drive, writer)
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

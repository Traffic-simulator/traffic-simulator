import jakarta.xml.bind.JAXBContext
import jakarta.xml.bind.JAXBException
import jakarta.xml.bind.Marshaller
import opendrive.OpenDRIVE

import java.io.FileWriter
import java.io.IOException


class OpenDriveWriter {
    @Throws(JAXBException::class, IOException::class)
    fun write(drive: OpenDRIVE, filename: String) {
        val context: JAXBContext = JAXBContext.newInstance(opendrive.OpenDRIVE::class.java)
        val marshaller: Marshaller = context.createMarshaller()
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, java.lang.Boolean.TRUE)
        marshaller.marshal(drive, FileWriter("../src/main/resources/$filename"))
    }
}

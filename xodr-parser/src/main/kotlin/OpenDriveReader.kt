package org.example


import jakarta.xml.bind.JAXBContext
import jakarta.xml.bind.JAXBException
import opendrive.OpenDRIVE

import java.io.FileReader
import java.io.IOException


class OpenDriveReader {
    @Throws(JAXBException::class, IOException::class)
    fun read(filename: String): OpenDRIVE {
        val context: JAXBContext = JAXBContext.newInstance(OpenDRIVE::class.java)
        return context.createUnmarshaller()
            .unmarshal(FileReader("src/main/resources/$filename")) as OpenDRIVE
    }
}

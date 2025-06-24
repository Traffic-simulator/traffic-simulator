package ru.nsu.trafficsimulator.graphics

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.VertexAttributes
import com.badlogic.gdx.graphics.glutils.ShaderProgram
import com.badlogic.gdx.graphics.glutils.VertexData
import com.badlogic.gdx.utils.BufferUtils
import java.nio.Buffer
import java.nio.FloatBuffer
import java.nio.IntBuffer

class VBOWithVAOBatched(isStatic: Boolean, private val numVertices: Int, private val attributes: VertexAttributes) : VertexData {
    private val tmpHandle: IntBuffer = BufferUtils.newIntBuffer(1)
    private val byteBuffer = BufferUtils.newUnsafeByteBuffer(this.attributes.vertexSize * numVertices)
    private val buffer = byteBuffer.asFloatBuffer()
    private val ownsBuffer: Boolean = true
    private var bufferHandles: List<Int>
    private var bufferForRender = 0
    private var usage: Int = 0
    private var isDirty: Boolean = false
    private var isBound: Boolean = false
    private var vaoHandle: Int = -1
    private var cachedLocations = com.badlogic.gdx.utils.IntArray()

    init {
        (buffer as Buffer).flip()
        (byteBuffer as Buffer).flip()
        bufferHandles = listOf(Gdx.gl20.glGenBuffer(), Gdx.gl20.glGenBuffer(), Gdx.gl20.glGenBuffer())
        usage = if (isStatic) GL20.GL_STATIC_DRAW else GL20.GL_DYNAMIC_DRAW
        createVAO()
    }

    override fun getAttributes(): VertexAttributes {
        return attributes
    }

    override fun getNumVertices(): Int {
        return buffer.limit() * 4 / attributes.vertexSize
    }

    override fun getNumMaxVertices(): Int {
        return byteBuffer!!.capacity() / attributes.vertexSize
    }


    @Deprecated("use {@link #getBuffer(boolean)} instead ")
    override fun getBuffer(): FloatBuffer? {
        isDirty = true
        return buffer
    }

    override fun getBuffer(forWriting: Boolean): FloatBuffer? {
        isDirty = isDirty or forWriting
        return buffer
    }

    private fun bufferChanged() {
        if (isBound) {
            Gdx.gl20.glBindBuffer(GL20.GL_ARRAY_BUFFER, bufferHandles[bufferForRender])
            Gdx.gl20.glBufferData(GL20.GL_ARRAY_BUFFER, byteBuffer!!.limit(), byteBuffer, usage)
            isDirty = false
        }
    }

    override fun setVertices(vertices: FloatArray, offset: Int, count: Int) {
        isDirty = true
        if (offset != 0) {
            throw Exception("Not expected, need to think through")
        }

        // Reset attributes offsets to interleaved ones
        var cnt = 0
        for (i in 0..<attributes.size()) {
            val attribute = attributes[i]
            attribute.offset = cnt
            cnt += attribute.numComponents
        }

        (buffer as Buffer).position(0)
        (buffer as Buffer).limit(count)
        for (attribute in attributes) {
            for (vertexId in 0..<numVertices) {
                for (componentId in 0..<attribute.numComponents) {
                    val pos = vertexId * (attributes.vertexSize / 4) + attribute.offset + componentId
                    buffer.put(vertices[pos])
                }
            }
        }

        // Reset attributes offsets to batched ones
        var nextAttributeStart = 0
        for (attribute in attributes) {
            attribute.offset = nextAttributeStart
            nextAttributeStart += attribute.sizeInBytes * numVertices
        }

        val gl = Gdx.gl30
        for (bufferHandle in bufferHandles) {
            gl.glBindBuffer(GL20.GL_ARRAY_BUFFER, bufferHandle)
            (byteBuffer as Buffer).limit(buffer.limit() * 4)
            gl.glBufferData(GL20.GL_ARRAY_BUFFER, byteBuffer.limit(), byteBuffer, usage)
        }
        isDirty = false
    }

    override fun updateVertices(targetOffset: Int, vertices: FloatArray?, sourceOffset: Int, count: Int) {
        isDirty = true
        val pos = byteBuffer!!.position()
        (byteBuffer as Buffer).position(targetOffset * 4)
        BufferUtils.copy(vertices, sourceOffset, count, byteBuffer)
        (byteBuffer as Buffer).position(pos)
        (buffer as Buffer).position(0)

        val gl = Gdx.gl30
        for (bufferHandle in bufferHandles) {
            gl.glBindBuffer(GL20.GL_ARRAY_BUFFER, bufferHandle)
            (byteBuffer as Buffer).limit(buffer.limit() * 4)
            gl.glBufferData(GL20.GL_ARRAY_BUFFER, byteBuffer.limit(), byteBuffer, usage)
        }
        isDirty = false
    }

    fun updateVerticesImmediately(offset: Int, vertices: FloatArray, count: Int) {
        val pos = byteBuffer!!.position()
        (byteBuffer as Buffer).position(offset * 4)
        BufferUtils.copy(vertices, offset, count, byteBuffer)

        (buffer as Buffer).position(0)

        byteBuffer.position(offset * 4)

        val lastBuffer = Math.floorMod((bufferForRender - 1), bufferHandles.size)
        val gl = Gdx.gl30
        gl.glBindVertexArray(0)
        gl.glBindBuffer(GL20.GL_ARRAY_BUFFER, bufferHandles[lastBuffer])
        gl.glBufferSubData(GL20.GL_ARRAY_BUFFER, offset * 4, count * 4, byteBuffer)

        (byteBuffer as Buffer).position(pos)
    }

    /** Binds this VertexBufferObject for rendering via glDrawArrays or glDrawElements
     *
     * @param shader the shader
     */
    override fun bind(shader: ShaderProgram) {
        bind(shader, null)
    }

    override fun bind(shader: ShaderProgram, locations: IntArray?) {
        val gl = Gdx.gl30

        gl.glBindVertexArray(vaoHandle)
        Gdx.gl.glBindBuffer(GL20.GL_ARRAY_BUFFER, bufferHandles[bufferForRender])
        bufferForRender = (bufferForRender + 1) % bufferHandles.size

        bindAttributes(shader, locations)

        // if our data has changed upload it:
//        bindData(gl)

        isBound = true
    }

    private fun bindAttributes(shader: ShaderProgram, locations: IntArray?) {
        var stillValid = false // cachedLocations.size != 0
        val numAttributes = attributes.size()

        if (stillValid) {
            if (locations == null) {
                var i = 0
                while (stillValid && i < numAttributes) {
                    val attribute = attributes[i]
                    val location = shader.getAttributeLocation(attribute.alias)
                    stillValid = location == cachedLocations[i]
                    i++
                }
            } else {
                stillValid = locations.size == cachedLocations.size
                var i = 0
                while (stillValid && i < numAttributes) {
                    stillValid = locations[i] == cachedLocations[i]
                    i++
                }
            }
        }

        if (!stillValid) {
            unbindAttributes(shader)
            cachedLocations.clear()

            for (i in 0..<numAttributes) {
                val attribute = attributes[i]

                if (locations == null) {
                    cachedLocations.add(shader.getAttributeLocation(attribute.alias))
                } else {
                    cachedLocations.add(locations[i])
                }

                val location = cachedLocations[i]
                if (location < 0) {
                    continue
                }

                shader.enableVertexAttribute(location)
                shader.setVertexAttribute(
                    location, attribute.numComponents, attribute.type, attribute.normalized,
                    0, attribute.offset
                )
            }
        }
    }

    private fun unbindAttributes(shaderProgram: ShaderProgram) {
        if (cachedLocations.size == 0) {
            return
        }
        val numAttributes = attributes.size()
        for (i in 0..<numAttributes) {
            val location = cachedLocations[i]
            if (location < 0) {
                continue
            }
            shaderProgram.disableVertexAttribute(location)
        }
    }

    private fun bindData(gl: GL20) {
//        if (isDirty) {
//            gl.glBindBuffer(GL20.GL_ARRAY_BUFFER, bufferHandles[bufferForRender])
//            (byteBuffer as Buffer).limit(buffer.limit() * 4)
//            gl.glBufferData(GL20.GL_ARRAY_BUFFER, byteBuffer.limit(), byteBuffer, usage)
//            isDirty = false
//        }
    }

    /** Unbinds this VertexBufferObject.
     *
     * @param shader the shader
     */
    override fun unbind(shader: ShaderProgram?) {
        unbind(shader, null)
    }

    override fun unbind(shader: ShaderProgram?, locations: IntArray?) {
        val gl = Gdx.gl30
        gl.glBindVertexArray(0)
        isBound = false
    }

    /** Invalidates the VertexBufferObject so a new OpenGL buffer handle is created. Use this in case of a context loss.  */
    override fun invalidate() {
        bufferHandles = listOf(Gdx.gl30.glGenBuffer(), Gdx.gl30.glGenBuffer(), Gdx.gl30.glGenBuffer())
        bufferForRender = 0
        createVAO()
        isDirty = true
    }

    /** Disposes of all resources this VertexBufferObject uses.  */
    override fun dispose() {
        val gl = Gdx.gl30

        gl.glBindBuffer(GL20.GL_ARRAY_BUFFER, 0)
        for (bufferHandle in bufferHandles) {
            gl.glDeleteBuffer(bufferHandle)
        }
        if (ownsBuffer) {
            BufferUtils.disposeUnsafeByteBuffer(byteBuffer)
        }
        deleteVAO()
    }

    private fun createVAO() {
        (tmpHandle as Buffer).clear()
        Gdx.gl30.glGenVertexArrays(1, tmpHandle)
        vaoHandle = tmpHandle.get()
    }

    private fun deleteVAO() {
        if (vaoHandle != -1) {
            (tmpHandle as Buffer).clear()
            tmpHandle.put(vaoHandle)
            (tmpHandle as Buffer).flip()
            Gdx.gl30.glDeleteVertexArrays(1, tmpHandle)
            vaoHandle = -1
        }
    }
}

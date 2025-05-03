package ru.nsu.trafficsimulator.graphics

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.g3d.Environment
import com.badlogic.gdx.graphics.g3d.Model
import com.badlogic.gdx.graphics.g3d.ModelInstance
import com.badlogic.gdx.graphics.g3d.Renderable
import com.badlogic.gdx.graphics.g3d.Shader
import com.badlogic.gdx.graphics.g3d.model.Node
import com.badlogic.gdx.graphics.g3d.model.NodePart
import net.mgsx.gltf.scene3d.shaders.PBRShaderConfig
import net.mgsx.gltf.scene3d.shaders.PBRShaderProvider

class ShaderModelInstance(val model: Model, val environment: Environment, vsPath: String, fsPath: String) : ModelInstance(model) {
    private val vs = Gdx.files.internal(vsPath).readString()
    private val fs = Gdx.files.internal(fsPath).readString()
    private val provider: PBRShaderProvider = PBRShaderProvider(PBRShaderConfig().apply {
        fragmentShader = fs
        vertexShader = vs
    })
    private var shader: Shader? = null

    override fun getRenderable(out: Renderable, node: Node, nodePart: NodePart): Renderable? {
        super.getRenderable(out, node, nodePart)
        if (shader == null) {
            out.environment = environment
            shader = provider.getShader(out)
        }
        out.shader = shader
        return out
    }
}

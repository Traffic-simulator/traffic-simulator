package ru.nsu.trafficsimulator.graphics

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.g3d.Renderable
import com.badlogic.gdx.graphics.g3d.Shader
import net.mgsx.gltf.scene3d.shaders.PBRShaderConfig
import net.mgsx.gltf.scene3d.shaders.PBRShaderProvider

class CustomShaderProvider(vsPath: String, fsPath: String) : PBRShaderProvider(PBRShaderConfig()) {
    private val customProvider: PBRShaderProvider = PBRShaderProvider(PBRShaderConfig().apply {
        fragmentShader = Gdx.files.internal(fsPath).readString()
        vertexShader = Gdx.files.internal(vsPath).readString()
    })

    override fun getShader(renderable: Renderable?): Shader {
        if (renderable?.material?.get(RoadMaterialAttribute.getType()) != null) {
            return customProvider.getShader(renderable)
        }
        return super.getShader(renderable)
    }
}

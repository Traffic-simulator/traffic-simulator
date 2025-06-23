package ru.nsu.trafficsimulator.graphics

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.g3d.Renderable
import com.badlogic.gdx.graphics.g3d.Shader
import net.mgsx.gltf.scene3d.shaders.PBRShaderConfig
import net.mgsx.gltf.scene3d.shaders.PBRShaderProvider

class CustomShaderProvider(roadVsPath: String, roadFsPath: String, vsPath: String, fsPath: String) : PBRShaderProvider(PBRShaderConfig()) {
    private val roadProvider: PBRShaderProvider = PBRShaderProvider(PBRShaderConfig().apply {
        fragmentShader = Gdx.files.internal(roadFsPath).readString()
        vertexShader = Gdx.files.internal(roadVsPath).readString()
    })
    private val defaultProvider: PBRShaderProvider = PBRShaderProvider(PBRShaderConfig().apply {
        fragmentShader = Gdx.files.internal(fsPath).readString()
        vertexShader = Gdx.files.internal(vsPath).readString()
    })

    override fun getShader(renderable: Renderable?): Shader {
        if (renderable?.material?.get(RoadMaterialAttribute.getType()) != null) {
            return roadProvider.getShader(renderable)
        } else {
            return defaultProvider.getShader(renderable)
        }
    }
}

plugins {
    kotlin("jvm") version "2.0.10"
    application
}

group = "ru.nsu.trafficsimulator"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
    val lwjglVersion = "3.3.4"
    val lwjglNatives = "natives-linux"

    implementation(platform("org.lwjgl:lwjgl-bom:$lwjglVersion"))

    implementation("org.lwjgl", "lwjgl")
    implementation("org.lwjgl", "lwjgl-assimp")
    implementation("org.lwjgl", "lwjgl-glfw")
    implementation("org.lwjgl", "lwjgl-openal")
    implementation("org.lwjgl", "lwjgl-opengl")
    implementation("org.lwjgl", "lwjgl-stb")
    implementation("io.github.spair:imgui-java-app:1.87.4")
    runtimeOnly("org.lwjgl", "lwjgl", classifier = lwjglNatives)
    runtimeOnly("org.lwjgl", "lwjgl-assimp", classifier = lwjglNatives)
    runtimeOnly("org.lwjgl", "lwjgl-glfw", classifier = lwjglNatives)
    runtimeOnly("org.lwjgl", "lwjgl-openal", classifier = lwjglNatives)
    runtimeOnly("org.lwjgl", "lwjgl-opengl", classifier = lwjglNatives)
    runtimeOnly("org.lwjgl", "lwjgl-stb", classifier = lwjglNatives)

}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(17)
}
application {
    mainClass = "trafficsim.frontend.Main"
}

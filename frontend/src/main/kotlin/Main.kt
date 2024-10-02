package trafficsim.frontend
import imgui.ImGui
import imgui.gl3.ImGuiImplGl3
import org.lwjgl.Version
import org.lwjgl.glfw.*
import org.lwjgl.glfw.Callbacks.*
import org.lwjgl.glfw.GLFW.*
import org.lwjgl.opengl.*
import org.lwjgl.opengl.GL11.*
import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryUtil
import imgui.glfw.ImGuiImplGlfw

class Main {
    // The window handle
    private var window: Long = 0
    private var imGuiGlfw: ImGuiImplGlfw = ImGuiImplGlfw()
    private var imGuiGl3: ImGuiImplGl3 = ImGuiImplGl3()

    fun run() {
        init()
        loop()

        // Free the window callbacks and destroy the window
        glfwFreeCallbacks(window)
        glfwDestroyWindow(window)

        // Terminate GLFW and free the error callback
        glfwTerminate()
        glfwSetErrorCallback(null)?.free()
    }

    private fun init() {
        // Setup an error callback. The default implementation
        // will print the error message in System.err.
        GLFWErrorCallback.createPrint(System.err).set()

        // Initialize GLFW. Most GLFW functions will not work before doing this.
        check(glfwInit()) { "Unable to initialize GLFW" }

        // Configure GLFW
        glfwDefaultWindowHints() // optional, the current window hints are already the default
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE) // the window will stay hidden after creation
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE) // the window will be resizable

        // Create the window
        window = glfwCreateWindow(1920, 1080, "Hello World!", MemoryUtil.NULL, MemoryUtil.NULL)
        if (window == MemoryUtil.NULL) throw RuntimeException("Failed to create the GLFW window")

        // Setup a key callback. It will be called every time a key is pressed, repeated or released.
        glfwSetKeyCallback(window) { window, key, scancode, action, mods ->
            if (key == GLFW_KEY_ESCAPE && action == GLFW_RELEASE) glfwSetWindowShouldClose(
                window,
                true
            ) // We will detect this in the rendering loop
        }

        // Make the OpenGL context current
        glfwMakeContextCurrent(window)
        // Enable v-sync
        glfwSwapInterval(1)

        // Make the window visible
        glfwShowWindow(window)

        // This line is critical for LWJGL's interoperation with GLFW's
        // OpenGL context, or any context that is managed externally.
        // LWJGL detects the context that is current in the current thread,
        // creates the GLCapabilities instance and makes the OpenGL
        // bindings available for use.
        GL.createCapabilities()

        ImGui.createContext()
        ImGui.styleColorsDark()
        imGuiGlfw.init(window, true)
        imGuiGl3.init()
    }

    private fun loop() {
        val color = floatArrayOf(1.0f, 0.0f, 0.0f);

        // Run the rendering loop until the user has attempted to close
        // the window or has pressed the ESCAPE key.
        while (!glfwWindowShouldClose(window)) {

            imGuiGl3.newFrame()
            imGuiGlfw.newFrame()
            ImGui.newFrame()
            run {
                ImGui.begin("Hello world!")
                ImGui.text("Choose color for background:")
                ImGui.colorEdit3("background", color)
                ImGui.end()
            }
            ImGui.render()
            glClearColor(color[0], color[1], color[2], 0.0f)
            glClear(GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT)

            imGuiGl3.renderDrawData(ImGui.getDrawData())

            glfwSwapBuffers(window) // swap the color buffers

            // Poll for window events. The key callback above will only be
            // invoked during this call.
            glfwPollEvents()
        }
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            Main().run()
        }
    }
}

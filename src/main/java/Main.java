import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.lwjgl.glfw.*;
import org.lwjgl.opengl.*;
import org.lwjgl.system.MemoryStack;

import java.nio.FloatBuffer;

import static org.lwjgl.glfw.Callbacks.*;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.glBindVertexArray;
import static org.lwjgl.opengl.GL30.glGenVertexArrays;
import static org.lwjgl.system.MemoryUtil.*;

public class Main {

    private long window;

    private int simpleShader;
    private int simpleMatrix;
    private int simpleColor;

    private int squareVao;
    private int squareVbo;

    private Matrix4f proj = new Matrix4f(), view = new Matrix4f();

    private static final float[] squareCoords = {
        -0.5f, -0.5f,
        -0.5f, 0.5f,
        0.5f, 0.5f,
        0.5f, 0.5f,
        0.5f, -0.5f,
        -0.5f, -0.5f,
    };

    public void run() {
        GLFWErrorCallback.createPrint(System.err).set();
        if ( !glfwInit() )
            throw new IllegalStateException("Unable to initialize GLFW");
        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE);
        window = glfwCreateWindow(800, 600, "Momentum", NULL, NULL);
        if ( window == NULL )
            throw new RuntimeException("Failed to create the GLFW window");
        glfwSetKeyCallback(window, (window, key, scancode, action, mods) -> {
            if ( key == GLFW_KEY_ESCAPE && action == GLFW_RELEASE )
                glfwSetWindowShouldClose(window, true);
        });

        glfwMakeContextCurrent(window);
        glfwSwapInterval(1);
        glfwShowWindow(window);
        GL.createCapabilities();

        glClearColor(0, 0, 0, 1);
        glEnable(GL_BLEND);
        glBlendFunc(GL_ONE, GL_ONE_MINUS_SRC_ALPHA);

        {
            int vertex = Shaders.createShader("simplev.glsl", GL_VERTEX_SHADER);
            int fragment = Shaders.createShader("simplef.glsl", GL_FRAGMENT_SHADER);
            simpleShader = glCreateProgram();
            glAttachShader(simpleShader, vertex);
            glAttachShader(simpleShader, fragment);
            glBindAttribLocation(simpleShader, Shaders.Attribute.POSITION.position, "position");
            glLinkProgram(simpleShader);
            Shaders.checkLinking(simpleShader);
            glUseProgram(simpleShader);
            simpleMatrix = glGetUniformLocation(simpleShader, "matrix");
            simpleColor = glGetUniformLocation(simpleShader, "color");
            glDeleteShader(vertex);
            glDeleteShader(fragment);
            checkGLError("Shader link simple " + simpleShader);
        }

        try(MemoryStack stack = MemoryStack.stackPush()) {
            squareVao = glGenVertexArrays();
            glBindVertexArray(squareVao);
            squareVbo = glGenBuffers();
            FloatBuffer squareVerts = stack.mallocFloat(squareCoords.length);
            squareVerts.put(squareCoords);
            squareVerts.flip();
            glBindBuffer(GL_ARRAY_BUFFER, squareVbo);
            glBufferData(GL_ARRAY_BUFFER, squareVerts, GL_STATIC_DRAW);
            glEnableVertexAttribArray(Shaders.Attribute.POSITION.position);
            glVertexAttribPointer(Shaders.Attribute.POSITION.position,
                2, GL_FLOAT, false, 4 * 2, 0);
        }

        Font arial = new Font("arial.ttf", 32, 512, 512);

        glfwSetWindowSizeCallback(window, (long window, int width, int height) -> {
            this.resize(width, height);
        });
        this.resize(800, 600);

        TileGridRenderer gridRenderer = new TileGridRenderer();
        gridRenderer.loadGrid(new byte[][]{
            {1, 1, 1, 1},
            {1, 0, 1, 0},
        });

        double currentTime = glfwGetTime(), lastTime = currentTime;
        double delta;
        double fpsTime = 0;
        int frameCounter = 0;
        int fps = 0;
        while ( !glfwWindowShouldClose(window) ) {
            currentTime = glfwGetTime();
            delta = currentTime - lastTime;
            lastTime = currentTime;

            fpsTime += delta;
            ++frameCounter;
            if(fpsTime > 0.5) {
                fpsTime -= 0.5;
                fps = frameCounter * 2;
                frameCounter = 0;
            }

            glClear(GL_COLOR_BUFFER_BIT);

            try(MemoryStack stack = MemoryStack.stackPush()) {
                Matrix4f test = new Matrix4f();
                test.translate(800, 50, 0);
                test.scale(100);
                FloatBuffer buffer = stack.mallocFloat(16);
                glUseProgram(simpleShader);
                glBindVertexArray(squareVao);
                glUniform4f(simpleColor, 1, 1, 0, 0.5f);
                glUniformMatrix4fv(simpleMatrix, false, new Matrix4f(proj).mul(view).mul(test).get(buffer));
                glDrawArrays(GL_TRIANGLES, 0, 6);
            }

            Matrix4f test = new Matrix4f().translate(50, 50, 0).scale(100);
            gridRenderer.draw(new Matrix4f(proj).mul(view).mul(test));

            arial.draw("FPS: " + fps, 0, 24, proj);
            arial.draw("Test test\ntest test TEST \n\nTest", 800, 1000, proj);

            glfwSwapBuffers(window);
            glfwPollEvents();
        }

        arial.cleanUp();

        glDeleteProgram(simpleShader);

        glfwFreeCallbacks(window);
        glfwDestroyWindow(window);

        glfwTerminate();
        glfwSetErrorCallback(null).free();
    }

    public void resize(int width, int height) {
        glViewport(0, 0, width, height);
        proj.setOrtho(0.0f, width, height, 0.0f, 0.0f, 1.0f);
    }

    public static void main(String... args) {
        new Main().run();
    }

    public static void checkGLError(String message) {
        int err = glGetError();
        if(err != 0)
        {
            throw new RuntimeException("OpenGL error: " + err + "\n" + message);
        }
    }
}

import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector4f;
import org.lwjgl.glfw.*;
import org.lwjgl.opengl.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.lwjgl.glfw.Callbacks.*;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL30.glGenVertexArrays;
import static org.lwjgl.system.MemoryUtil.*;

public class Main {

    private long window;

    private Matrix4f proj = new Matrix4f(), view = new Matrix4f();

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

        glfwSetWindowSizeCallback(window, (long window, int width, int height) -> {
            this.resize(width, height);
        });
        this.resize(800, 600);

        BoxRenderer boxRenderer = new BoxRenderer();
        Font arial = new Font("arial.ttf", 32, 512, 512);
        GridRenderer gridRenderer = new GridRenderer();

        view = new Matrix4f().scale(40);

        LevelInfo level = LevelInfo.loadLevel("level1.txt");
        gridRenderer.loadGrid(level.data);
        Grid grid = new Grid(level.data);
        Box player = new Box(level.player.x, level.player.y, 1, 1);
        Vector2f playerMotion = new Vector2f(0);
        Box collider = new Box(6, 4, 2, 1);

        double currentTime = glfwGetTime(), lastTime = currentTime;
        float delta;
        double fpsTime = 0;
        int frameCounter = 0;
        int fps = 0;
        while ( !glfwWindowShouldClose(window) ) {
            currentTime = glfwGetTime();
            delta = (float) (currentTime - lastTime);
            lastTime = currentTime;

            fpsTime += delta;
            ++frameCounter;
            if(fpsTime > 0.5) {
                fpsTime -= 0.5;
                fps = frameCounter * 2;
                frameCounter = 0;
            }

            Vector2f playerMove = new Vector2f(0);
//            Vector2f keyMove = new Vector2f(0);
//            if(glfwGetKey(window, GLFW_KEY_S) == GLFW_PRESS) keyMove.y++;
//            if(glfwGetKey(window, GLFW_KEY_W) == GLFW_PRESS) keyMove.y--;
//            if(glfwGetKey(window, GLFW_KEY_D) == GLFW_PRESS) keyMove.x++;
//            if(glfwGetKey(window, GLFW_KEY_A) == GLFW_PRESS) keyMove.x--;
//            playerMove.add(keyMove.mul(delta * 3));

            playerMotion.add(0, delta * 0.5f);
            playerMove.add(playerMotion);

            player.add(playerMove);
            while(playerMove.lengthSquared() > 0.0f) {
                Vector2f partialMove = new Vector2f(playerMove);
                if(partialMove.length() > 0.25f) {
                    partialMove.normalize(0.25f);
                }
                playerMove.sub(partialMove);
                player.add(partialMove);
                List<Vector2f> resolveOptions = new ArrayList<>();
                for (Box box : grid.getCollidingBoxes(player)) {
                    resolveOptions.addAll(Arrays.asList(Box.resolveOptions(box, player)));
                }
                Vector2f min = null;
                for (int i = 0;
                     i < resolveOptions.size();
                     ++i) {
                    Vector2f option = new Vector2f(resolveOptions.get(i));
                    if (min == null || option.lengthSquared() < min.lengthSquared()) {
                        Box playerWithOption = new Box(player);
                        playerWithOption.add(option);
                        boolean works = true;
                        for (Box box : grid.getCollidingBoxes(playerWithOption)) {
                            if (Box.intersect(box, playerWithOption)) {
                                works = false;
                                break;
                            }
                        }
                        if (works) {
                            min = option;
                        }
                    }
                    Vector2f optionI = option;
                    for (int j = i + 1;
                         j < resolveOptions.size();
                         ++j) {
                        option = new Vector2f(optionI).add(resolveOptions.get(j));
                        if (min == null || option.lengthSquared() < min.lengthSquared()) {
                            Box playerWithOption = new Box(player);
                            playerWithOption.add(option);
                            boolean works = true;
                            for (Box box : grid.getCollidingBoxes(playerWithOption)) {
                                if (Box.intersect(box, playerWithOption)) {
                                    works = false;
                                    break;
                                }
                            }
                            if (works) {
                                min = option;
                            }
                        }
                    }
                }
                if (min != null) {
                    player.add(min);
                    if (min.y < 0) {
                        playerMotion.y = 0;
                    }
                }
            }

            glClear(GL_COLOR_BUFFER_BIT);

            Matrix4f gridMatrix = new Matrix4f().translate(0, 0, 0).scale(1);
            gridRenderer.draw(new Matrix4f(proj).mul(view).mul(gridMatrix));

            Matrix4f playerMatrix = new Matrix4f(proj).mul(view).mul(player.getMatrix());
            boxRenderer.draw(playerMatrix, new Vector4f(1, 1, 0, 1));

//            for(int i = 0; i < resolveOptions.size(); ++i) {
//                Vector2f option = resolveOptions.get(i);
//                Matrix4f mat = new Matrix4f(proj).mul(view);
//                mat.translate(player.x, player.y, 0);
//                mat.translate(option.x, option.y, 0);
//                boxRenderer.draw(mat, new Vector4f(1, 0, 0, 0.1f));
//            }
//            if(min != null) {
//                Vector2f option = min;
//                Matrix4f mat = new Matrix4f(proj).mul(view);
//                mat.translate(player.x, player.y, 0);
//                mat.translate(option.x, option.y, 0);
//                boxRenderer.draw(mat, new Vector4f(0, 1, 1, 0.4f));
//            }

            arial.draw("FPS: " + fps, 0, 24, proj);

            glfwSwapBuffers(window);
            glfwPollEvents();
        }

        arial.cleanUp();
        boxRenderer.cleanUp();
        gridRenderer.cleanUp();

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

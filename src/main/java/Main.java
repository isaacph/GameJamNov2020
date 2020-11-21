import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector4f;
import org.lwjgl.glfw.*;
import org.lwjgl.opengl.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Vector;

import static org.lwjgl.glfw.Callbacks.*;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL30.glGenVertexArrays;
import static org.lwjgl.system.MemoryUtil.*;

public class Main {

    private long window;
    private float windowWidthTile, windowHeightTile;
    private float windowWidth, windowHeight;

    private Matrix4f proj = new Matrix4f(), view = new Matrix4f();
    private LevelInfo level;
    private Grid grid;
    private List<Magnet> magnets;
    private Box goal;
    private Box player;
    private Vector2f playerMotion;
    private boolean win = false;
    private int bounceFrameCD = 0;

    private GridRenderer gridRenderer;

    private void loadLevel(String path) {
        level = LevelInfo.loadLevel(path);
        gridRenderer.loadGrid(level.data);
        grid = new Grid(level.data);
        magnets = level.magnets;
        player = new Box(level.player.x, level.player.y, 1, 1);
        playerMotion = new Vector2f(0);
        goal = new Box(level.goal.x, level.goal.y, 3, 3);
        win = false;
        this.resize((int) windowWidth, (int) windowHeight);
    }

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

        BoxRenderer boxRenderer = new BoxRenderer();
        CircleRenderer circleRenderer = new CircleRenderer();
        Font arial = new Font("arial.ttf", 32, 512, 512);
        Font bigArial = new Font("arial.ttf", 64, 512, 512);
        gridRenderer = new GridRenderer();

        loadLevel("level1.txt");

        this.resize(800, 600);

        glfwSetKeyCallback(window, (long window, int key, int scancode, int action, int mods) -> {
            if(action == GLFW_PRESS && key == GLFW_KEY_R) {
                player.x = level.player.x;
                player.y = level.player.y;
                playerMotion.zero();
                win = false;
            }
            if(action == GLFW_PRESS) {
                if(key >= GLFW_KEY_1 && key <= GLFW_KEY_9) {
                    loadLevel("level" + (key - GLFW_KEY_0) + ".txt");
                }
            }
        });

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

            boolean hold = false, fall = true;
            if(glfwGetKey(window, GLFW_KEY_SPACE) > 0) {
                hold = true;
            }

            Vector2f playerMove = new Vector2f(0);
//            Vector2f keyMove = new Vector2f(0);
//            if(glfwGetKey(window, GLFW_KEY_S) == GLFW_PRESS) keyMove.y++;
//            if(glfwGetKey(window, GLFW_KEY_W) == GLFW_PRESS) keyMove.y--;
//            if(glfwGetKey(window, GLFW_KEY_D) == GLFW_PRESS) keyMove.x++;
//            if(glfwGetKey(window, GLFW_KEY_A) == GLFW_PRESS) keyMove.x--;
//            playerMove.add(keyMove.mul(delta * 3));

            for(Magnet magnet : magnets) {
                magnet.inRange = false;
                magnet.using = false;
                Vector2f diff = new Vector2f(magnet.getCenter()).sub(player.getCenter());
                if(diff.lengthSquared() > 2.0f * 2.0f && diff.length() < magnet.strength) {
                    magnet.inRange = true;
                    if(hold) {
                        fall = false;
                        magnet.using = true;
                        Vector2f parallel = new Vector2f(diff).normalize().mul(new Vector2f(diff).normalize().dot(playerMotion));
                        Vector2f perp = new Vector2f(playerMotion).sub(parallel);
                        playerMotion.add(perp.normalize(delta * 10));
                        parallel = new Vector2f(diff).normalize().mul(new Vector2f(diff).normalize().dot(playerMotion));
                        perp = new Vector2f(playerMotion).sub(parallel);
                        playerMotion.add(new Vector2f(diff).normalize().mul((perp.lengthSquared() / diff.length()) * delta));
//                        playerMotion.add(new Vector2f(diff).normalize((magnet.strength / 100.0f) / diff.lengthSquared()));
//                        Vector2f parallel = new Vector2f(diff).normalize(new Vector2f(diff).normalize().dot(playerMotion));
//                        Vector2f perp = new Vector2f(playerMotion).sub(parallel);
//                        playerMotion.add(new Vector2f(diff).normalize(perp.lengthSquared() / diff.length() * delta * 50));
//                        playerMotion.add(new Vector2f(diff).normalize(0.5f * delta));
                    }
                }
            }
            if(fall) {
                playerMotion.add(0, delta * 8.0f);
            }
            playerMove.add(new Vector2f(playerMotion).mul(delta));
            --bounceFrameCD;
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
                    boolean decX = false, decY = false;
                    decX = min.y != 0;
                    decY = min.x != 0;
                    if (min.y != 0) {
                        playerMotion.y = -playerMotion.y * 0.7f;
                        if(Math.abs(playerMotion.y) < 0.01f) {
                            playerMotion.y = 0;
//                            System.out.println("y reset");
                        }
//                            System.out.println("y bounce");

                    }
                    if (min.x != 0) {
                        playerMotion.x = -playerMotion.x * 0.7f;
                        if(Math.abs(playerMotion.x) < 0.01f) {
                            playerMotion.x = 0;
                        }
                    }
                    if(decX) {
                        float dec = -Math.signum(playerMotion.x) * delta * 20.0f;
                        if(Math.abs(playerMotion.x) >= Math.abs(dec)) {
                            playerMotion.x += dec;
                        } else {
                            playerMotion.x = 0;
                        }
                    }
                    if(decY) {
                        float dec = -Math.signum(playerMotion.y) * delta * 20.0f;
                        if(Math.abs(playerMotion.y) >= Math.abs(dec)) {
                            playerMotion.y += dec;
                        } else {
                            playerMotion.y = 0;
                        }
                    }
                }
            }

            if(Box.intersect(player, goal)) {
                win = true;
            }

            glClear(GL_COLOR_BUFFER_BIT);

            Matrix4f gridMatrix = new Matrix4f().translate(0, 0, 0).scale(1);
            gridRenderer.draw(new Matrix4f(proj).mul(view).mul(gridMatrix));

            Matrix4f playerMatrix = new Matrix4f(proj).mul(view).mul(player.getMatrix());
            boxRenderer.draw(playerMatrix, new Vector4f(1, 1, 0, 1));
            boxRenderer.draw(new Matrix4f(proj).mul(view).mul(goal.getMatrix()), new Vector4f(0, 1, 1, 0.5f));

            for(Magnet magnet : magnets) {
                Vector4f color = !magnet.inRange ? new Vector4f(0, 1, 0, 1)
                    : magnet.using ? new Vector4f(1, 0, 0, 1)
                    : new Vector4f(0, 0, 1, 1);
                Matrix4f mat = new Matrix4f(proj).mul(view).translate(magnet.x, magnet.y, 0);
                mat.rotate((float) Math.PI / 4.0f, 0, 0, 1);
                boxRenderer.draw(mat, color);
                circleRenderer.draw(new Matrix4f(proj).mul(view), magnet.getCenter(), magnet.strength, 0.1f,
                    color);
                circleRenderer.draw(new Matrix4f(proj).mul(view), magnet.getCenter(), 2.0f, 0.1f,
                    color);
            }

            if(win) {
                boxRenderer.draw(new Matrix4f(proj).translate(windowWidth / 2.0f, windowHeight / 2.0f, 0).scale(300), new Vector4f(1, 1, 1, 1));
                bigArial.draw("Victory!", windowWidth / 2.0f - bigArial.textWidth("Victory") / 2.0f, windowHeight / 2.0f + 16.0f, new Matrix4f(proj), new Vector4f(0, 0, 0, 1));
            }

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

            boxRenderer.draw(new Matrix4f(proj).translate(50, 13, 0).scale(100, 26, 0), new Vector4f(0, 0, 0, 0.4f));
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
        float levelScale;
        if(height / grid.height < width / grid.width) {
            levelScale = (float) height / grid.height;
        } else {
            levelScale = (float) width / grid.width;
        }
        view = new Matrix4f();
        view.translate(width / 2.0f - grid.width * levelScale / 2.0f, height / 2.0f - grid.height * levelScale / 2.0f, 0);
        view.scale(levelScale);
        windowWidthTile = width / levelScale;
        windowHeightTile = height / levelScale;
        windowWidth = width;
        windowHeight = height;
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

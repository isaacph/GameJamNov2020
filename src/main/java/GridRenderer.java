
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.lwjgl.system.MemoryStack;

import java.nio.FloatBuffer;
import java.util.Random;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;


public class GridRenderer {

    private int shader;
    private int shaderMatrix;
    private int shaderSampler;
    private int texture;
    private int vbo;
    private int vao;
    private int vertexCount;
    private boolean loaded = false;
    private TextureRenderer textureRenderer;
    private int width, height;
    private Texture asteroidTexture;

    public GridRenderer() {
        int vertex = Shaders.createShader("texturev.glsl", GL_VERTEX_SHADER);
        int fragment = Shaders.createShader("gridf.glsl", GL_FRAGMENT_SHADER);
        shader = glCreateProgram();
        glAttachShader(shader, vertex);
        glAttachShader(shader, fragment);
        glBindAttribLocation(shader, Shaders.Attribute.POSITION.position, "position");
        glBindAttribLocation(shader, Shaders.Attribute.TEXTURE.position, "tex");
        glLinkProgram(shader);
        Shaders.checkLinking(shader);
        glUseProgram(shader);
        shaderMatrix = glGetUniformLocation(shader, "matrix");
        shaderSampler = glGetUniformLocation(shader, "sampler");
        Main.checkGLError("Grid Renderer shader init");

        texture = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, texture);
//        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, 2, 2, 0, GL_RGBA, GL_FLOAT, new float[] {
//            1.0f, 1.0f, 1.0f, 1.0f,
//            0.0f, 0.0f, 0.0f, 1.0f,
//            0.0f, 0.0f, 0.0f, 1.0f,
//            1.0f, 1.0f, 1.0f, 1.0f,
//        });
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, 1, 1, 0, GL_RGBA, GL_FLOAT, new float[] {
            0.0f, 0.0f, 0.0f, 0.0f,
        });
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glGenerateMipmap(GL_TEXTURE_2D);

        textureRenderer = new TextureRenderer();

        Main.checkGLError("Grid Renderer texture init");
    }

    public void loadGrid(byte[][] grid) {
        cleanUpGrid();
        if(grid.length == 0) return;
        vertexCount = 0;
        height = grid.length;
        width = grid[0].length;

        try(MemoryStack stack = MemoryStack.stackPush()) {
            float[] test = {
                0.0f, 0.0f, 0.0f, 0.0f,
                0.0f, 1.0f, 0.0f, 1.0f,
                1.0f, 1.0f, 1.0f, 1.0f,
                1.0f, 1.0f, 1.0f, 1.0f,
                1.0f, 0.0f, 1.0f, 0.0f,
                0.0f, 0.0f, 0.0f, 0.0f
            };

            for(byte[] sub : grid)
                for(byte b : sub)
                    if(b != 0) ++vertexCount;
            vertexCount *= 6;
            FloatBuffer buffer = stack.mallocFloat(vertexCount * 4);
//            buffer.put(test);
            for(int i = 0; i < grid.length; ++i) {
                for(int j = 0; j < grid[i].length; ++j) {
                    if(grid[i][j] != 0) {
                        buffer.put(j).put(i).put(0).put(0);
                        buffer.put(j).put(i + 1).put(1).put(0);
                        buffer.put(j + 1).put(i + 1).put(1).put(1);
                        buffer.put(j + 1).put(i + 1).put(1).put(1);
                        buffer.put(j + 1).put(i).put(0).put(1);
                        buffer.put(j).put(i).put(0).put(0);
                    }
                }
            }
            buffer.flip();

            vao = glGenVertexArrays();
            glBindVertexArray(vao);
            vbo = glGenBuffers();
            glBindBuffer(GL_ARRAY_BUFFER, vbo);
            glBufferData(GL_ARRAY_BUFFER, buffer, GL_STATIC_DRAW);
            setAttribPointers();


            asteroidTexture = new Texture(32 * width, 32 * height, generatePixels(32 * width, 32 * height));

            loaded = true;
        }
        Main.checkGLError("test");
    }

    private float[] generatePixels(int width, int height) {
        float[] pixels = new float[width * height * 4];
        Random random = new Random();
        for(int i = 0; i < width * height / 20.0f; ++i) {
            float x = random.nextFloat() * width, y = random.nextFloat() * height;
            float radius = random.nextFloat() * 8.0f;
            generateCircle(pixels, width, height, x, y, radius);
        }
//        generateCircle(pixels, width, height, 100, 100, 100);
        return pixels;
    }

    private void generateCircle(float[] pixels, int width, int height, float x, float y, float radius) {
        int minX = (int) Math.floor(x - radius);
        int maxX = (int) Math.ceil(x + radius);
        int minY = (int) Math.floor(y - radius);
        int maxY = (int) Math.ceil(y + radius);
        for(int i = minY; i <= maxY; ++i) {
            if(i >= 0 && i < height) {
                for(int j = minX; j <= maxX; ++j) {
                    if(j >= 0 && j < width) {
                        float diff = (i - y) * (i - y) + (j - x) * (j - x);
                        if(diff <= radius * radius) {
                            int k = (i * width + j) * 4;
                            float a = (0.5f - diff / (radius * radius) / 10.0f);
                            pixels[k + 0] = a * 160 / 256.0f;
                            pixels[k + 1] = a * 139 / 256.0f;
                            pixels[k + 2] = a * 120 / 256.0f;
                            pixels[k + 3] = 1.0f * 1.0f;
                        }
                    }
                }
            }
        }
    }

    private void cleanUpGrid() {
        if(!loaded) return;
        loaded = false;
        glDeleteBuffers(vbo);
        glDeleteVertexArrays(vao);
        asteroidTexture.cleanUp();
    }

    private void setAttribPointers() {
        glEnableVertexAttribArray(Shaders.Attribute.POSITION.position);
        glVertexAttribPointer(Shaders.Attribute.POSITION.position,
            2, GL_FLOAT, false, 4 * 4, 0);
        glEnableVertexAttribArray(Shaders.Attribute.TEXTURE.position);
        glVertexAttribPointer(Shaders.Attribute.TEXTURE.position,
            2, GL_FLOAT, false, 4 * 4, 4 * 2);
    }

    public void draw(Matrix4f matrix) {
        if(!loaded) {
            throw new RuntimeException("Attempted to draw unloaded grid!");
        }

        glEnable(GL_STENCIL_TEST);

        // Draw floor
        glStencilFunc(GL_ALWAYS, 1, 0xFF); // Set any stencil to 1
        glStencilOp(GL_KEEP, GL_KEEP, GL_REPLACE);
        glStencilMask(0xFF); // Write to stencil buffer
//        glDepthMask(false); // Don't write to depth buffer
        glClear(GL_STENCIL_BUFFER_BIT); // Clear stencil buffer (0 by default)

        try(MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer buffer = stack.mallocFloat(16);
            glBindVertexArray(vao);
            glUseProgram(shader);
            glActiveTexture(GL_TEXTURE0);
            glBindTexture(GL_TEXTURE_2D, texture);
            glUniform1i(shaderSampler, 0);
            glUniformMatrix4fv(shaderMatrix, false, matrix.get(buffer));
            glDrawArrays(GL_TRIANGLES, 0, vertexCount);
        }

        glStencilFunc(GL_EQUAL, 1, 0xFF); // Pass test if stencil value is 1
        glStencilMask(0x00); // Don't write anything to stencil buffer
//        glDepthMask(true); // Write to depth buffer

        matrix.translate(width / 2.0f, height / 2.0f, 0);
        matrix.scale(width, height, 0);
        asteroidTexture.bind();
        textureRenderer.draw(matrix, new Vector4f(1));

        glDisable(GL_STENCIL_TEST);
    }

    public void cleanUp() {
        cleanUpGrid();
        glDeleteProgram(shader);
        glDeleteTextures(texture);
        textureRenderer.cleanUp();
    }
}

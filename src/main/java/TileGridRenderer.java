
import org.joml.Matrix4f;
import org.lwjgl.system.MemoryStack;

import java.nio.FloatBuffer;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;


public class TileGridRenderer {

    private int shader;
    private int shaderMatrix;
    private int shaderSampler;
    private int texture;
    private int vbo;
    private int vao;
    private int vertexCount;
    private boolean loaded = false;

    public TileGridRenderer() {
        int vertex = Shaders.createShader("texturev.glsl", GL_VERTEX_SHADER);
        int fragment = Shaders.createShader("texturef.glsl", GL_FRAGMENT_SHADER);
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
        Main.checkGLError("Tile Grid Renderer shader init");

        texture = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, texture);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, 2, 2, 0, GL_RGBA, GL_FLOAT, new float[] {
            1.0f, 1.0f, 1.0f, 1.0f,
            0.0f, 0.0f, 0.0f, 1.0f,
            0.0f, 0.0f, 0.0f, 1.0f,
            1.0f, 1.0f, 1.0f, 1.0f,
        });
//        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, 1, 1, 0, GL_RGBA, GL_FLOAT, new float[] {
//            1.0f, 1.0f, 1.0f, 1.0f,
//        });
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glGenerateMipmap(GL_TEXTURE_2D);
        Main.checkGLError("test");
    }

    public void loadGrid(byte[][] grid) {
        cleanUpGrid();
        if(grid.length == 0) return;
        vertexCount = 0;

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
            loaded = true;
        }
        Main.checkGLError("test");
    }

    private void cleanUpGrid() {
        if(!loaded) return;
        loaded = false;
        glDeleteBuffers(vbo);
        glDeleteVertexArrays(vao);
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
    }

    public void cleanUp() {
        cleanUpGrid();
        glDeleteProgram(shader);
        glDeleteTextures(texture);
    }
}

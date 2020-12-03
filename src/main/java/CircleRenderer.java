import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector4f;
import org.lwjgl.system.MemoryStack;

import java.nio.FloatBuffer;

import static org.lwjgl.opengl.GL11.GL_TRIANGLES;
import static org.lwjgl.opengl.GL11.glDrawArrays;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

public class CircleRenderer {

    private int shader;
    private int shaderMatrix;
    private int shaderColor;
    private int shaderLineWidth;

//    private int squareVao;
    private int squareVbo;

    private static final float[] squareCoords = {
        -0.5f, -0.5f, 0.0f, 0.0f,
        -0.5f, 0.5f, 0.0f, 1.0f,
        0.5f, 0.5f, 1.0f, 1.0f,
        0.5f, 0.5f, 1.0f, 1.0f,
        0.5f, -0.5f, 1.0f, 0.0f,
        -0.5f, -0.5f, 0.0f, 0.0f
    };

    public CircleRenderer() {
        int vertex = Shaders.createShader("circlev.glsl", GL_VERTEX_SHADER);
        int fragment = Shaders.createShader("circlef.glsl", GL_FRAGMENT_SHADER);
        shader = glCreateProgram();
        glAttachShader(shader, vertex);
        glAttachShader(shader, fragment);
        glBindAttribLocation(shader, Shaders.Attribute.POSITION.position, "position");
        glBindAttribLocation(shader, Shaders.Attribute.TEXTURE.position, "tex");
        glLinkProgram(shader);
        Shaders.checkLinking(shader);
        glUseProgram(shader);
        shaderMatrix = glGetUniformLocation(shader, "matrix");
        shaderColor = glGetUniformLocation(shader, "color");
        shaderLineWidth = glGetUniformLocation(shader, "lineWidth");
        glDeleteShader(vertex);
        glDeleteShader(fragment);
        Main.checkGLError("Shader link simple " + shader);

        try(MemoryStack stack = MemoryStack.stackPush()) {
//            squareVao = glGenVertexArrays();
//            glBindVertexArray(squareVao);
            squareVbo = glGenBuffers();
            FloatBuffer squareVerts = stack.mallocFloat(squareCoords.length);
            squareVerts.put(squareCoords);
            squareVerts.flip();
            glBindBuffer(GL_ARRAY_BUFFER, squareVbo);
            glBufferData(GL_ARRAY_BUFFER, squareVerts, GL_STATIC_DRAW);
            glEnableVertexAttribArray(Shaders.Attribute.POSITION.position);
            glVertexAttribPointer(Shaders.Attribute.POSITION.position,
                2, GL_FLOAT, false, 4 * 4, 0);
            glEnableVertexAttribArray(Shaders.Attribute.TEXTURE.position);
            glVertexAttribPointer(Shaders.Attribute.TEXTURE.position,
                2, GL_FLOAT, false, 4 * 4, 4 * 2);
            Main.checkGLError("VBO simple " + shader);
        }
    }

    public void draw(Matrix4f projView, Vector2f position, float radius, float thickness, Vector4f color) {
        Matrix4f matrix = new Matrix4f(projView);
        matrix.translate(position.x, position.y, 0);
        matrix.scale(radius / 0.9f * 2);
        draw(matrix, color, thickness / (radius / 0.9f * 2));
    }

    public void draw(Matrix4f matrix, Vector4f color, float lineWidth) {
        try(MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer buffer = stack.mallocFloat(16);
            glUseProgram(shader);
//            glBindVertexArray(0);
            glBindBuffer(GL_ARRAY_BUFFER, squareVbo);
            glEnableVertexAttribArray(Shaders.Attribute.POSITION.position);
            glVertexAttribPointer(Shaders.Attribute.POSITION.position,
                2, GL_FLOAT, false, 4 * 4, 0);
            glEnableVertexAttribArray(Shaders.Attribute.TEXTURE.position);
            glVertexAttribPointer(Shaders.Attribute.TEXTURE.position,
                2, GL_FLOAT, false, 4 * 4, 4 * 2);
            glUniform4f(shaderColor, color.x, color.y, color.z, color.w);
            glUniformMatrix4fv(shaderMatrix, false, matrix.get(buffer));
            glUniform1f(shaderLineWidth, lineWidth);
            glDrawArrays(GL_TRIANGLES, 0, 6);
            glDisableVertexAttribArray(Shaders.Attribute.POSITION.position);
            glDisableVertexAttribArray(Shaders.Attribute.TEXTURE.position);
        }
    }

    public void cleanUp() {
        glDeleteProgram(shader);
//        glDeleteVertexArrays(squareVao);
        glDeleteBuffers(squareVbo);
    }
}

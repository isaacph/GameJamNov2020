#version 330 core
uniform mat4 matrix;
in vec2 position;
void main()
{
    gl_Position = matrix * vec4(position, 0.0, 1.0);
}
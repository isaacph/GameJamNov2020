#version 330 core
in vec2 texCoord;
out vec4 outColor;
uniform sampler2D sampler;
uniform vec4 color;
void main() {
    outColor = color * texture2D(sampler, texCoord);
}
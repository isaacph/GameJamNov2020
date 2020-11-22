#version 330 core
in vec2 texCoord;
out vec4 outColor;
uniform sampler2D sampler;

void main() {
    outColor = texture2D(sampler, texCoord);
}
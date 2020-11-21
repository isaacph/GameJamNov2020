#version 330 core
in vec2 texCoord;
uniform float lineWidth;
uniform vec4 color;
out vec4 outColor;
void main() {
    float diff = length(texCoord - vec2(0.5));
    if(abs(diff - 0.45) > lineWidth / 2) {
        outColor = vec4(0);
    } else {
        outColor = color;
        outColor *= 1 - abs(diff - 0.45) / lineWidth * 2;
    }
//    outColor = vec4(abs(diff - 0.45));
}
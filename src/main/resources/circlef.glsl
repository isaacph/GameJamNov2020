#version 110
varying vec2 texCoord;
uniform float lineWidth;
uniform vec4 color;
void main() {
    float diff = length(texCoord - vec2(0.5));
    if(abs(diff - 0.45) > lineWidth / 2.0) {
        gl_FragColor = vec4(0.0);
    } else {
        gl_FragColor = color;
        gl_FragColor *= 1.0 - abs(diff - 0.45) / lineWidth * 2.0;
    }
//    outColor = vec4(abs(diff - 0.45));
}
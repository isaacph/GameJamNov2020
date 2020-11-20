uniform vec4 color;
void main() {
    gl_FragColor = color.w * vec4(color.x, color.y, color.z, 1.0);
}
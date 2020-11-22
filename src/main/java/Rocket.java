import org.joml.Matrix4f;
import org.joml.Vector4f;

public class Rocket {
    private TextureRenderer textureRenderer;
    private Texture[] textures = new Texture[7];
    public Box visBox, physBox;
    public int currentTexture;
    public float timer = 0;

    public Rocket() {
        textureRenderer = new TextureRenderer();
        for(int i = 0; i < 7; ++i) {
            textures[i] = new Texture("rocket" + (i + 1) + ".png");
        }
    }
    public void reset(float x, float y) {
        this.visBox = new Box(x, y, 3, 3);
        this.physBox = new Box(x, y - 2 / 32.0f * 3.0f, 1.5f, 3.0f - 4 / 32.0f * 3.0f);
        this.currentTexture = 0;
//        start();
    }

    public void start() {
        this.currentTexture = 1;
        this.timer = 0.1f;
    }

    public void draw(Matrix4f projView, float delta) {
        if(this.currentTexture > 0) {
            timer += delta;
            visBox.y -= delta * 5.0f;
            this.currentTexture = (int) (timer / 0.1f) + 1;
            if(this.currentTexture >= 5) {
                this.currentTexture = (this.currentTexture - 5) % 2 + 5;
            }
        }
        this.textures[this.currentTexture].bind();
        textureRenderer.draw(new Matrix4f(projView).mul(visBox.getMatrix()), new Vector4f(1));
    }

    public void cleanUp() {
        this.textureRenderer.cleanUp();
        for(Texture t : textures) {
            t.cleanUp();
        }
    }
}

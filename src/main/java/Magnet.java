import org.joml.Vector2f;

public class Magnet {

    public float x, y, strength;

    public Magnet() {
        this(0, 0, 1);
    }
    public Magnet(float x, float y) {
        this(x, y, 1);
    }
    public Magnet(float x, float y, float strength) {
        this.x = x;
        this.y = y;
        this.strength = strength;
    }

    public Vector2f getCenter() {
        return new Vector2f(x, y);
    }
}

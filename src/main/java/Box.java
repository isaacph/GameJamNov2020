import org.joml.Matrix4f;
import org.joml.Vector2f;

public class Box {

    public float x, y, width, height;

    public Box() {
        this(0, 0, 1, 1);
    }

    public Box(float size) {
        this(size, size);
    }

    public Box(float width, float height) {
        this(0, 0, width, height);
    }

    public Box(float x, float y, float width, float height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public void add(Vector2f offset) {
        x += offset.x;
        y += offset.y;
    }

    public Vector2f getCenter() {
        return new Vector2f(x, y);
    }

    public Vector2f getScale() {
        return new Vector2f(width, height);
    }

    public Matrix4f getMatrix() {
        return new Matrix4f().translate(x, y, 0).scale(width, height, 0);
    }

    public Vector2f[] points() {
        return new Vector2f[] {
            new Vector2f(x - width / 2, y - height / 2),
            new Vector2f(x - width / 2, y + height / 2),
            new Vector2f(x + width / 2, y + height / 2),
            new Vector2f(x + width / 2, y - height / 2)
        };
    }

    public Shadow shadow(Vector2f span) {
        if(span.x == 0 && span.y == 0) return new Shadow(0, 0);
        return new Shadow(Shadow.project(span, points()));
    }

    public static boolean intersect(Box a, Box b) {
        return Shadow.intersect(a.shadow(new Vector2f(1, 0)),
                b.shadow(new Vector2f(1, 0))) &&
               Shadow.intersect(a.shadow(new Vector2f(0, 1)),
                b.shadow(new Vector2f(0, 1)));
    }

    public static Vector2f resolveX(Box pusher, Box mover) {
        if(!intersect(pusher, mover)) {
            return new Vector2f(0);
        }
        return new Vector2f(Shadow.resolveIntersect(
            pusher.shadow(new Vector2f(1, 0)), mover.shadow(new Vector2f(1, 0))
        ), 0);
    }

    public static Vector2f resolveY(Box pusher, Box mover) {
        if(!intersect(pusher, mover)) {
            return new Vector2f(0);
        }
        return new Vector2f(0, Shadow.resolveIntersect(
            pusher.shadow(new Vector2f(0, 1)), mover.shadow(new Vector2f(0, 1))
        ));
    }

    public static Vector2f resolve(Box pusher, Box mover) {
        return Shadow.absMin(resolveX(pusher, mover), resolveY(pusher, mover));
    }
}

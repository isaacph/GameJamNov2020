import org.joml.Vector2f;

public class Shadow {

    public float min, max;

    public Shadow(float min, float max) {
        this.min = min;
        this.max = max;
    }

    public Shadow(float... points) {
        if(points.length > 0) {
            this.min = min(points);
            this.max = max(points);
        }
    }

    public static boolean intersect(Shadow a, Shadow b) {
        return a.min <= b.min && b.min <= a.max ||
            a.min <= b.max && b.max <= a.max ||
            b.min <= a.min && a.min <= b.max ||
            b.min <= a.max && a.max <= b.max;
    }

    public static float resolveIntersect(Shadow pusher, Shadow mover) {
        if(!intersect(pusher, mover)) return 0;
        return absMin(pusher.max - mover.min, -(mover.max - pusher.min));
    }

    public static float min(float... points) {
        float min = points[0];
        for(int i = 1; i < points.length; ++i) {
            if(points[i] < min) {
                min = points[i];
            }
        }
        return min;
    }

    public static float max(float... points) {
        float max = points[0];
        for(int i = 1; i < points.length; ++i) {
            if(points[i] > max) {
                max = points[i];
            }
        }
        return max;
    }

    public static float[] project(Vector2f v, Vector2f... points) {
        Vector2f u = v.normalize();
        float[] f = new float[points.length];
        for(int i = 0; i < points.length; ++i) {
            f[i] = u.dot(points[i]);
        }
        return f;
    }

    public static float absMin(float... args) {
        float absMin = args[0];
        for(int i = 1; i < args.length; ++i) {
            if(Math.abs(args[i]) < Math.abs(absMin)) {
                absMin = args[i];
            }
        }
        return absMin;
    }

    public static Vector2f absMin(Vector2f... args) {
        Vector2f absMin = new Vector2f(args[0]);
        for(int i = 1; i < args.length; ++i) {
            if(args[i].lengthSquared() < absMin.lengthSquared()) {
                absMin = args[i];
            }
        }
        return absMin;
    }
}

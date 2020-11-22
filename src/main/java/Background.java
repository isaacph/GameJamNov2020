import org.joml.Matrix4f;
import org.joml.Vector4f;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Random;

public class Background {


    private TextureRenderer textureRenderer;
    private Texture[] asteroids;
    private float[] size;

    int width, height;
    float offset = 0;
    float genLimit = 0;
    Deque<Point> asteroidQueue = new ArrayDeque<>();

    private BoxRenderer boxRenderer;
    ArrayList<Deque<Point>> stars = new ArrayList<>();
    ArrayList<Float> genLimitStars = new ArrayList<>();
    ArrayList<Float> offsetStars = new ArrayList<>();

    public Background() {
        textureRenderer = new TextureRenderer();
        asteroids = new Texture[7];
        size = new float[7];
        for(int i = 0; i < 7; ++i) {
            int size = i + 4;
            this.size[i] = size;
            float[] pixels = new float[size * size * 4];
            generateCircle(pixels, size, size, size / 2.0f, size / 2.0f, size / 2.0f);
            asteroids[i] = new Texture(size, size, pixels);
        }

        boxRenderer = new BoxRenderer();
    }

    public void draw(Matrix4f proj, float delta) {
        offset += delta * 40;
        for(int i = 0; i < stars.size(); ++i) {
            offsetStars.set(i, offsetStars.get(i) + i * delta / 3.0f);
        }

        while(!asteroidQueue.isEmpty() && asteroidQueue.getFirst().x - offset < 0) {
            asteroidQueue.removeFirst();
        }
        while(offset >= genLimit - width) {
            generateScreen();
        }
        for(int i = 0; i < stars.size(); ++i) {
            while(!stars.get(i).isEmpty() && stars.get(i).getFirst().x - offsetStars.get(i) < 0) {
                stars.get(i).removeFirst();
            }
            while(offsetStars.get(i) >= genLimitStars.get(i) - width) {
                generateScreenStars(i);
            }
        }

        for(Point a : asteroidQueue) {
            drawAsteroid(proj, a.x - offset, a.y, a.size);
        }
        for(int i = 0; i < stars.size(); ++i) {
            for(Point s : stars.get(i)) {
                drawStar(proj, s.x - offsetStars.get(i), s.y, s.size);
            }
        }
    }

    public void drawAsteroid(Matrix4f proj, float x, float y, int version) {
        Matrix4f matrix = new Matrix4f(proj);
        matrix.translate(x - size[version] / 2.0f, y - size[version] / 2.0f, 0);
        matrix.scale(size[version] * 2);
        asteroids[version].bind();
        textureRenderer.draw(matrix, new Vector4f(1));
    }

    public void drawStar(Matrix4f proj, float x, float y, int version) {
        Matrix4f matrix = new Matrix4f(proj);
        Box box = new Box(x, y, version + 1, version + 1);
        matrix.mul(box.getMatrix());
        boxRenderer.draw(matrix, new Vector4f(1));
    }

    public void reset(int width, int height) {
        this.width = width;
        this.height = height;
        asteroidQueue.clear();
        offset = 0;
        genLimit = 0;
        stars = new ArrayList<>();
        genLimitStars = new ArrayList<>();
        offsetStars = new ArrayList<>();
        for(int i = 0; i < 3; ++i) {
            genLimitStars.add(0.0f);
            stars.add(new ArrayDeque<>());
            offsetStars.add(0.0f);
        }
    }

    public void generateScreen() {
        float lowerBoundX = genLimit;
        float upperBoundX = genLimit + width;
        float lowerBoundY = -height / 4.0f;
        float upperBoundY = height * 5.0f / 4;

        int count = (int) (upperBoundX - lowerBoundX) * (int) (upperBoundY - lowerBoundY) / 10000;

        Random random = new Random();
        for(int i = 0; i < count; ++i) {
            float x = random.nextFloat() * (upperBoundX - lowerBoundX) + lowerBoundX;
            float y = random.nextFloat() * (upperBoundY - lowerBoundY) + lowerBoundY;
            int size = random.nextInt(7);
            asteroidQueue.addLast(new Point(size, x, y));
        }

        genLimit += width;
    }

    public void generateScreenStars(int level) {
        float lowerBoundX = genLimitStars.get(level);
        float upperBoundX = genLimitStars.get(level) + width;
        float lowerBoundY = -height / 4.0f;
        float upperBoundY = height * 5.0f / 4;

        int count = (int) (upperBoundX - lowerBoundX) * (int) (upperBoundY - lowerBoundY) / 50000;

        Random random = new Random();
        for(int i = 0; i < count; ++i) {
            float x = random.nextFloat() * (upperBoundX - lowerBoundX) + lowerBoundX;
            float y = random.nextFloat() * (upperBoundY - lowerBoundY) + lowerBoundY;
            int size = random.nextInt(stars.size());
            stars.get(size).addLast(new Point(size, x, y));
        }

        genLimitStars.set(level, genLimitStars.get(level) + width);
    }

    public void cleanUp() {
        textureRenderer.cleanUp();
        for(Texture t : asteroids) {
            t.cleanUp();
        }
    }

    private void generateCircle(float[] pixels, int width, int height, float x, float y, float radius) {
        int minX = (int) Math.floor(x - radius);
        int maxX = (int) Math.ceil(x + radius);
        int minY = (int) Math.floor(y - radius);
        int maxY = (int) Math.ceil(y + radius);
        for(int i = minY; i <= maxY; ++i) {
            int ii = i % height;
            for(int j = minX; j <= maxX; ++j) {
                int jj = j % width;
                float diff = (i - y) * (i - y) + (j - x) * (j - x);
                if(diff <= radius * radius) {
                    int k = ((ii % height) * width + jj) * 4;
                    float a = (0.4f - diff / (radius * radius) / 10.0f);
                    pixels[k + 0] = a * 160 / 256.0f;
                    pixels[k + 1] = a * 139 / 256.0f;
                    pixels[k + 2] = a * 120 / 256.0f;
                    pixels[k + 3] = 1.0f * 1.0f;
                }
            }
        }
    }

    private void generateCircle2(float[] pixels, int width, int height, float x, float y, float radius) {
        int minX = (int) Math.floor(x - radius);
        int maxX = (int) Math.ceil(x + radius);
        int minY = (int) Math.floor(y - radius);
        int maxY = (int) Math.ceil(y + radius);
        for(int i = minY; i <= maxY; ++i) {
            int ii = i % height;
            for(int j = minX; j <= maxX; ++j) {
                int jj = j % width;
                float diff = (i - y) * (i - y) + (j - x) * (j - x);
                if(diff <= radius * radius) {
                    int k = ((ii % height) * width + jj) * 4;
                    float a = 1;
                    pixels[k + 0] = a;
                    pixels[k + 1] = a;
                    pixels[k + 2] = a;
                    pixels[k + 3] = 1;
                }
            }
        }
    }

    private static class Point {
        int size;
        float x, y;
        public Point(int size, float x, float y) {
            this.size = size;
            this.x = x;
            this.y = y;
        }
    }
}

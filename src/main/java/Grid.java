import java.util.ArrayList;
import java.util.List;

public class Grid {

    public byte[][] data;
    public int width, height;

    public Grid() {
        this(new byte[][]{});
    }
    public Grid(byte[][] data) {
        this.data = data;
        this.height = data.length;
        this.width = 0;
        if(this.height > 0) {
            this.width = data[0].length;
        }
    }

    public List<Box> getCollidingBoxes(Box mover) {
        int minX = (int) (mover.x - mover.width / 2);
        int maxX = (int) Math.ceil(mover.x + mover.width / 2);
        int minY = (int) (mover.y - mover.height / 2);
        int maxY = (int) Math.ceil(mover.y + mover.height / 2);

        ArrayList<Box> boxes = new ArrayList<>();
        for(int i = minY; i <= maxY; ++i) {
            for(int j = minX; j <= maxX; ++j) {
                if(i >= 0 && i < height && j >= 0 && j < width && hasCollider(data[i][j])) {
                    boxes.add(new Box(j + 0.5f, i + 0.5f, 1.0f, 1.0f));
                }
            }
        }
        return boxes;
    }

    public static boolean hasCollider(byte blockType) {
        return blockType != 0;
    }
}

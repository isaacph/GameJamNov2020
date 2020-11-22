import org.joml.Vector2f;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class LevelInfo {

    public byte[][] data;
    public List<Magnet> magnets;
    public Vector2f player;
    public Vector2f goal;

    public LevelInfo() {
        this(new byte[][]{});
    }

    public LevelInfo(byte[][] data) {
        this(data, Collections.emptyList(), new Vector2f(0, 0), new Vector2f(-5, -5));
    }

    public LevelInfo(byte[][] data, List<Magnet> magnets, Vector2f player, Vector2f goal) {
        this.data = data;
        this.magnets = magnets;
        this.player = player;
        this.goal = goal;
    }

    public static LevelInfo loadLevel(String path) {
        List<String> file = new ArrayList<>();
        try {
            InputStream stream = Shaders.class.getResourceAsStream(path);
            BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
            while(reader.ready()) {
                file.add(reader.readLine());
            }
        } catch(Exception e) {
            System.err.println("Error loading grid " + path);
            e.printStackTrace();
            return new LevelInfo();
        }

        int width = 0, height = file.size() - 1;
        if(height == 0) {
            return new LevelInfo();
        }
        width = file.get(0).split("\\s").length;
        byte[][] data = new byte[height][width];
        List<Magnet> magnets = new ArrayList<>();
        Vector2f goal = new Vector2f(-5, -5);

        for(int row = 0; row < height; ++row) {
            String[] split = file.get(row).split("\\s");
            if(split.length != width) {
                throw new RuntimeException(path + ": invalid line length: " + row
                    + ". All lines in input file must be the same length");
            }
            byte[] fill = data[row];
            for(int col = 0; col < width; ++col) {
                fill[col] = getTileID(split[col]);
                Magnet m = getMagnet(split[col], col, row);
                if(m != null) magnets.add(m);
                if(split[col].equals("G")) {
                    System.out.println(col + ", " + row);
                    goal = new Vector2f(col + 0.5f, row + 0.5f);
                }
            }
        }

        String[] posStr = file.get(file.size() - 1).split("\\s");
        if(posStr.length != 2) {
            throw new RuntimeException(path + ": invalid last line. Length must be 2 (was " + posStr.length + ")");
        }
        Vector2f player = new Vector2f(Float.parseFloat(posStr[0]), Float.parseFloat(posStr[1]));

        return new LevelInfo(data, magnets, player, goal);
    }

    public static byte getTileID(String input) {
        switch(input) {
            case "G":
            case "0":
                return 0;
            case "W":
                return 1;
        }
        if(input.length() == 1) {
            char c = input.charAt(0);
            if(c > '0' && c <= '9') {
                return 0;
            }
        }
        throw new RuntimeException("Unidentified tile input: " + input);
    }

    public static Magnet getMagnet(String input, float x, float y) {

        if(input.length() == 1) {
            char c = input.charAt(0);
            if(c > '0' && c <= '9') {
                return new Magnet(x + 0.5f, y + 0.5f, c - '0');
            }
        }
        return null;
    }
}

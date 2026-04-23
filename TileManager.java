import java.awt.*;
import java.awt.image.*;
import javax.imageio.ImageIO;
import java.io.File;

public class TileManager {
    // This class knows how to draw tiles and read special map spots like portal or spawn.
    private TileMap tileMap;
    private final int SIZE = 32;
    private BufferedImage[] portalFrames = new BufferedImage[8];
    private int animFrame = 0;
    private long lastAnimTime = 0;
    private BufferedImage dirt, grass, fullGreen;
    private boolean EditOn = false;

    public TileManager(TileMap map) {
        // Store the current map and load the tile images once.
        this.tileMap = map;
        loadImages();
    }

    private void loadImages() {
        // Load the terrain images and portal animation frames.
        try {
            String path = "Assets/tileSet/";
            dirt = ImageIO.read(new File(path + "Dirt.png"));
            grass = ImageIO.read(new File(path + "GrassDirt.png"));
            fullGreen = ImageIO.read(new File(path + "FullGreen.png"));

            for (int i = 0; i < 8; i++) {
                portalFrames[i] = ImageIO.read(new File("Assets/portal/spr_portal_strip8-" + (i + 1) + ".png"));
            }
        } catch (Exception e) { System.out.println("Image loading failed."); }
    }

    public void draw(Graphics g, boolean allEnemiesDead) {
        // Animate the portal and then draw the map tile by tile.
        if (System.currentTimeMillis() - lastAnimTime > 150) {
            animFrame = (animFrame + 1) % 8;
            lastAnimTime = System.currentTimeMillis();
        }

        int[][] grid = tileMap.getMap();
        for (int r = 0; r < grid.length; r++) {
            for (int c = 0; c < grid[r].length; c++) {
                int id = grid[r][c];
                if (id == 0){
                    continue;
                } else if (id == 1) {
                    g.drawImage(dirt, c * SIZE, r * SIZE, null);
                } else if (id == 2) {
                    g.drawImage(grass, c * SIZE, r * SIZE, null);
                } else if (id == 3) {
                    g.drawImage(fullGreen, c * SIZE, r * SIZE, null);
                } else if (id == 7) { 
                    // FIX: Only draw portal if enemies are gone
                    if (allEnemiesDead) {
                        g.drawImage(portalFrames[animFrame], c * SIZE - 32, r * SIZE - 64, 128, 128, null);
                    }
                } else if (id == 8 && EditOn){
                    g.setColor(Color.RED);
                    g.drawRect(c * SIZE, r * SIZE, 32, 64);
                } else if (id == 9 && EditOn) {
                    g.setColor(Color.CYAN);
                    g.drawRect(c * SIZE, r * SIZE, 32, 64);
                }
            }
        }
    }

    // Overload for TileEditor which doesn't care about enemies
    public void draw(Graphics g) {
        // The editor always shows the portal, so it passes true here.
        draw(g, true); 
    }

    public Point getPlayerSpawnLocation() {
        // Find tile 9 and use it as the player spawn point.
        int[][] grid = tileMap.getMap();
        for (int r = 0; r < grid.length; r++) {
            for (int c = 0; c < grid[r].length; c++) {
                if (grid[r][c] == 9) { 
                    return new Point(c * SIZE, r * SIZE);
                }
            }
        }
        return new Point(100, 100);
    }

    public boolean isTileSolid(int col, int row) {
        // Solid tiles block movement and hold characters up.
        int[][] grid = tileMap.getMap();
        if (row < 0 || row >= grid.length || col < 0 || col >= grid[0].length) {
            return false;
        }
        int tileID = grid[row][col];
        return tileID == 1 || tileID == 2 || tileID == 3;
    }

    public Rectangle getBound(int r, int c) { return new Rectangle(c * SIZE, r * SIZE, SIZE, SIZE); }
    public Rectangle getPortalBounds(int r, int c) { return new Rectangle(c * SIZE - 16, r * SIZE - 32, 64, 64); }
    public boolean isPortal(int r, int c) { return tileMap.getTile(r, c) == 7; }
    public void setTileMap(TileMap m) { this.tileMap = m; }
    public TileMap getTileMap() { return tileMap; }
}

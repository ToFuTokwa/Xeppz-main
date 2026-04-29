import java.awt.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.IOException;
import java.io.File;

public class Drop {
    public enum Type { HEAL, DMG_BOOST }
    
    public int x, y;
    public Type type;
    private boolean collected = false;
    private static final int SIZE = 64; // Size of the drop's hitbox and image

    private static BufferedImage healImage;
    private static BufferedImage dmgBoostImage;

    public Drop(int x, int y, Type type) {
        this.x = x;
        this.y = y;
        this.type = type;
    }

    public void update() {
        // Drops are stationary, so no movement logic is needed here.
    }

    // Hitbox is 32x32
    public Rectangle getHitbox() { 
        return new Rectangle(x, y + 20, SIZE, SIZE); 
    }

    public static void init() {
        try {
            // We use new File() to look directly into the Assets folder on your hard drive
            File healFile = new File("Assets/Health_Drop.png");
            File dmgFile = new File("Assets/Attack_Drop.png");

            if (healFile.exists()) {
                healImage = ImageIO.read(healFile);
            } else {
                System.out.println("ERROR: Health_Drop.png not found at: " + healFile.getAbsolutePath());
            }

            if (dmgFile.exists()) {
                dmgBoostImage = ImageIO.read(dmgFile);
            } else {
                System.out.println("ERROR: Attack_Drop.png not found at: " + dmgFile.getAbsolutePath());
            }

        } catch (IOException e) {
            System.out.println("An error occurred while loading images.");
            e.printStackTrace();
        }
    }

    private static BufferedImage getImage(Type type) {
        return (type == Type.HEAL) ? healImage : dmgBoostImage;
    }

    public void draw(Graphics2D g2) {
        BufferedImage img = getImage(type);
        
        if (img != null) {
            // I scaled this to 32x32 so it matches your hitbox exactly!
            g2.drawImage(img, x, y + 20, SIZE, SIZE, null);
        } else {
            // Fallback to simple colored rectangle if image fails to load
            g2.setColor(type == Type.HEAL ? Color.GREEN : Color.RED);
            g2.fillRect(x, y + 20, SIZE, SIZE);
        }
    }

    public boolean isCollected() { return collected; }
    public void collect() { this.collected = true; }
}

import java.awt.*;

public class Drop {
    public enum Type { HEAL, DMG_BOOST }
    
    public int x, y;
    public Type type;
    // Removed the 'hitbox' variable here because we will generate it on the fly
    private boolean collected = false;
    

    public Drop(int x, int y, Type type) {
        this.x = x;
        this.y = y;
        this.type = type;
    }

    // THIS WAS MISSING: The getter method for the hitbox
    // Hitbox is 32x32
    public Rectangle getHitbox() { return new Rectangle(x, y + 20, 32, 32); }

    // Drawing is 20x20
    public void draw(Graphics2D g2) {
        if (type == Type.HEAL) {
            g2.setColor(Color.GREEN);
        } else {
            g2.setColor(Color.RED);
        }
        g2.fillRect(x, y + 20, 20, 20);
    }

    public boolean isCollected() { return collected; }
    public void collect() { this.collected = true; }
}

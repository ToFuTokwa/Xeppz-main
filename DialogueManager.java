import java.awt.*;

public class DialogueManager {
    private boolean active = false;
    private String[] lines;
    private int currentIndex = 0;

    // Positioning constants for the 1280x736 screen
    private final int boxX = 100;
    private final int boxY = 520;
    private final int boxWidth = 1080;
    private final int boxHeight = 160;

    public void startDialogue(String[] text) {
        this.lines = text;
        this.currentIndex = 0;
        this.active = true;
    }

    public void update(boolean interactPressed) {
        if (!active) return;

        // If the player presses the interact key, move to the next line
        if (interactPressed) {
            currentIndex++;
            if (currentIndex >= lines.length) {
                active = false; // Close dialogue when lines run out
            }
        }
    }

    public void draw(Graphics2D g2) {
        if (!active) return;

        // 1. Optional: Dim the game world slightly
        g2.setColor(new Color(0, 0, 0, 100));
        g2.fillRect(0, 0, 1280, 736);

        // 2. Draw the main dialogue box
        g2.setColor(new Color(15, 15, 15, 240)); // Darker, almost opaque
        g2.fillRoundRect(boxX, boxY, boxWidth, boxHeight, 25, 25);
        
        // 3. Draw the border
        g2.setColor(Color.WHITE);
        g2.setStroke(new BasicStroke(3));
        g2.drawRoundRect(boxX, boxY, boxWidth, boxHeight, 25, 25);

        // 4. Draw the Text
        g2.setFont(new Font("Monospaced", Font.BOLD, 24));
        g2.drawString(lines[currentIndex], boxX + 40, boxY + 70);

        // 5. Draw the "Continue" prompt
        g2.setFont(new Font("SansSerif", Font.ITALIC, 16));
        g2.drawString("Press E to continue...", boxX + boxWidth - 200, boxY + boxHeight - 25);
    }

    public boolean isActive() {
        return active;
    }
}
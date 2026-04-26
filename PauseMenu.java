import java.awt.*;
import java.awt.event.KeyEvent;

public class PauseMenu { 
    private GamePanel gp;
    private boolean active = false;
    private boolean canToggle = true; 
    private int volume = 50; 

    // Constructor to get access to GamePanel methods
    public String pauseTitle = "SETTINGS";

    public PauseMenu(GamePanel gp) {
        this.gp = gp;
    }

    public void draw(Graphics2D g2) {
        if (!active) return;

        // 1. Draw a semi-transparent overlay
        g2.setColor(new Color(0, 0, 0, 180));
        g2.fillRect(0, 0, 1280, 736);

        // 2. Text Styling
        g2.setColor(Color.WHITE);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // Header
        g2.setFont(new Font("Arial", Font.BOLD, 45));
        drawCenteredString(pauseTitle, 200, g2);
        
        // Options
        g2.setFont(new Font("Arial", Font.PLAIN, 22));
        drawCenteredString("Volume: " + volume + "% (Use UP/DOWN Arrows)", 320, g2);
        
        g2.setFont(new Font("Arial", Font.ITALIC, 18));
        drawCenteredString("Press 'ESC' to Resume", 420, g2);
        drawCenteredString("Press 'R' to Reset Level", 470, g2);
        drawCenteredString("Press 'M' to Quit to Main Menu", 520, g2);
    }

    // Helper method to keep text centered regardless of string length
    private void drawCenteredString(String text, int y, Graphics2D g2) {
        FontMetrics metrics = g2.getFontMetrics(g2.getFont());
        int x = (1280 - metrics.stringWidth(text)) / 2;
        g2.drawString(text, x, y);
    }

    public void update(boolean pPressed) {
        // Debounce logic for the ESC key
        if (pPressed && canToggle) {
            active = !active; 
            canToggle = false; 
        }
        
        if (!pPressed) {
            canToggle = true; 
        }
    }

    public void handleKeyPress(KeyEvent e) {
        if (!active) return;

        switch (e.getKeyCode()) {
            case KeyEvent.VK_R:
                gp.resetGame(); 
                active = false;
                System.out.println("Game Reset!");
                break;
            case KeyEvent.VK_M:
                // This calls the method we just updated in GamePanel
                gp.returnToMainMenu();
                active = false; // Close the pause menu state
                break;
            case KeyEvent.VK_UP:
                if (volume < 100) volume += 5;
                // gp.soundPlayer.setVolume(volume); // Uncomment if your SoundPlayer supports this
                break;
            case KeyEvent.VK_DOWN:
                if (volume > 0) volume -= 5;
                // gp.soundPlayer.setVolume(volume);
                break;
        }
    }

    public boolean isActive() { return active; }
    public int getVolume() { return volume; }
}

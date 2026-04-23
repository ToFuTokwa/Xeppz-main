import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

public class Level3EndPanel extends JPanel {
    SoundPLayer soundPlayer = new SoundPLayer();
    // This panel appears after the last level is cleared.
    private final CardLayout cardLayout;
    private final JPanel mainPanel;
    private final GamePanel gamePanel; // Reference to the actual game panel

    // Add GamePanel to the constructor parameters
    public Level3EndPanel(CardLayout cardLayout, JPanel mainPanel, GamePanel gamePanel) {
        // Keep a reference to the real game panel so it can be reset.
        this.cardLayout = cardLayout;
        this.mainPanel = mainPanel;
        this.gamePanel = gamePanel; // Store the reference
        
        this.setBackground(Color.BLACK);
        this.setPreferredSize(new Dimension(1280, 736));
        this.setFocusable(true);

        // Allow player to return to home screen by pressing any key
        this.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                gamePanel.resetGame(); // Reset the REAL game panel here
                cardLayout.show(mainPanel, "Home");
            }
        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        // Draw the ending message in the middle of the screen.
        Graphics2D g2 = (Graphics2D) g;

        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Monospaced", Font.BOLD, 30));
        
        String text = "THE END OF BETA TEST";
        String subText = "You have conquered the Mine Shaft.";
        String prompt = "Press any key to return to Main Menu";

        // Center the text
        int textWidth = g2.getFontMetrics().stringWidth(text);
        g2.drawString(text, (1280 - textWidth) / 2, 300);

        g2.setFont(new Font("Monospaced", Font.PLAIN, 18));
        int subWidth = g2.getFontMetrics().stringWidth(subText);
        g2.drawString(subText, (1280 - subWidth) / 2, 350);
        
        g2.setColor(Color.GRAY);
        int promptWidth = g2.getFontMetrics().stringWidth(prompt);
        g2.drawString(prompt, (1280 - promptWidth) / 2, 600);
    }
}

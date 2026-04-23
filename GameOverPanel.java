import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

public class GameOverPanel extends JPanel {
    SoundPLayer soundPlayer = new SoundPLayer();
    // These references are used to restart the same game panel.
    private CardLayout cardLayout;
    private JPanel mainPanel;
    private GamePanel gamePanel;

    public GameOverPanel(CardLayout cardLayout, JPanel mainPanel, GamePanel gamePanel) {
        // Set up the game over screen and its keyboard controls.
        soundPlayer.stop("BgSound");
        soundPlayer.loop("UISound");
        this.cardLayout = cardLayout;
        this.mainPanel = mainPanel;
        this.gamePanel = gamePanel;

        this.setPreferredSize(new Dimension(1280, 736));
        this.setBackground(Color.BLACK);
        this.setFocusable(true);

        // Handle Restart Logic
        this.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_R) {
                    restartGame();
                }
                if (e.getKeyCode() == KeyEvent.VK_Q) {
                    System.exit(0);
                }
            }
        });
    }

    private void restartGame() {
        // Reset the game and return the player to gameplay.
        gamePanel.resetGame(); // Reset player/enemies
        cardLayout.show(mainPanel, "Game");
        gamePanel.requestFocusInWindow();
        soundPlayer.stop("UISound");  
        soundPlayer.loop("BgSound");
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        // Draw the game over text and instructions.
        g.setColor(Color.RED);
        g.setFont(new Font("Arial", Font.BOLD, 72));
        g.drawString("GAME OVER", 420, 300);

        g.setFont(new Font("Arial", Font.PLAIN, 30));
        g.setColor(Color.WHITE);
        g.drawString("Press 'R' to Restart", 500, 400);
        
        g.setFont(new Font("Arial", Font.PLAIN, 30));
        g.setColor(Color.WHITE);
        g.drawString("Press 'Q' to Quit", 500, 450);
    }
}

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

public class HomeUI extends JPanel {
    SoundPLayer soundPlayer = new SoundPLayer();
    // These references let the menu switch screens and start the game.
    private final CardLayout cardLayout;
    private final JPanel mainPanel;
    private final GamePanel gamePanel;
    private final StartSequence introPanel;

    // Variable
    private int screenWidth = 1280;
    private int screenHeight = 736;

    // Button
    private JButton startButton;
    private JButton quitButton;

    // Images
    private Image bgImage;
    private final Color COLOR_BG = Color.BLACK;
    private final Color COLOR_ACCENT = (Color.LIGHT_GRAY);
    private final Font FONT_BUTTON = new Font("Arial", Font.BOLD, 22);

    public HomeUI(CardLayout cardLayout, JPanel mainPanel, GamePanel gamePanel, StartSequence introPanel) {
        // Build the home screen step by step.
        this.cardLayout = cardLayout;
        this.mainPanel = mainPanel;
        this.gamePanel = gamePanel;
        this.introPanel = introPanel;
        
        imgLoader();
        initializePanel();
        createComponents();
        setupActions();
        layoutComponents();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        // Paint the menu background first.
        drawBackgroundImage(g);
    }

    private void imgLoader(){
        // Load the image used behind the menu buttons.
        try {
            // Updated to .png based on your last snippet
            File imgFile = new File("Assets/HomeBackground.png");
            if (imgFile.exists()) {
                this.bgImage = ImageIO.read(imgFile);
            } else {
                System.out.println("Error: Background image not found at " + imgFile.getAbsolutePath());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void drawBackgroundImage(Graphics g) {
        // If the image is missing, use a plain color instead.
        if (bgImage != null) {
            g.drawImage(bgImage, 0, 0, getWidth(), getHeight(), this);
        } else {
            g.setColor(COLOR_BG);
            g.fillRect(0, 0, getWidth(), getHeight());
        }
    }

    private void initializePanel() {
        // Set the menu size and layout before adding buttons.
        this.setPreferredSize(new Dimension(screenWidth, screenHeight));
        this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        this.setOpaque(true); 
    }

    private void createComponents() {
        // Create the main buttons shown on the menu.
        startButton = createStyledButton("START GAME");
        quitButton = createStyledButton("QUIT");
    }

    private JButton createStyledButton(String text) {
        // Build one button with the menu style already applied.
        JButton button = new JButton(text);
        button.setFont(FONT_BUTTON);
        button.setForeground(COLOR_ACCENT);
        
        button.setContentAreaFilled(false); 
        button.setBorderPainted(false);      
        button.setFocusPainted(false);
        button.setOpaque(false);
        
        button.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                button.setForeground(new Color(255, 255, 255));
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                button.setForeground(COLOR_ACCENT);
            }
        });
        
        return button;
    }

    private void setupActions() {
        // Start opens the intro, quit closes the game.
        startButton.addActionListener(e -> {
            introPanel.playIntro();
        });

        quitButton.addActionListener(e -> System.exit(0));
    }

    private void layoutComponents() {
        // Stack the buttons in the middle of the screen.
        // This centers the buttons vertically in the screen
        add(Box.createVerticalGlue());
        add(startButton);
        add(Box.createRigidArea(new Dimension(0, 20))); // Space between buttons
        add(quitButton);
        add(Box.createVerticalGlue());
    }
}

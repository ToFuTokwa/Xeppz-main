import java.awt.*;
import javax.swing.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class GamePanel extends JPanel implements Runnable {
    SoundPLayer soundPlayer = new SoundPLayer();
    private DialogueManager dialogueManager = new DialogueManager();
    // These objects are shared across the whole gameplay screen.
    private CardLayout cardLayout; 
    private JPanel mainPanel; 
    private Player player = new Player();
    private LevelManager levelManager = new LevelManager();
    private TileManager tileManager;
    private CheckCollision collisionChecker = new CheckCollision();
    private List<Enemy> enemies = new ArrayList<>();
    private Image currentBackground;
    private Thread gameThread;
    private int HPMax = player.MAX_HP;

    public GamePanel(CardLayout cardLayout, JPanel mainPanel) {
        // Prepare the first level and hook player input into this panel.
        soundPlayer.stop("GameMusic");
        soundPlayer.loop("MenuMusic");
        this.cardLayout = cardLayout; 
        this.mainPanel = mainPanel; 
        this.setPreferredSize(new Dimension(1280, 736));
        this.setFocusable(true);
        this.addKeyListener(player);
        this.addMouseListener(player);
        
        tileManager = new TileManager(levelManager.getCurrentLevel());
        spawnEnemies(); 
        updateLevelVisuals();
        spawnPlayer(); 
    }

    private void spawnPlayer() {
        Point spawnPoint = tileManager.getPlayerSpawnLocation();
        player.setPosition(spawnPoint.x, spawnPoint.y - 48);
    }

    private void spawnEnemies() {
        // Read the map and create an enemy wherever tile 8 appears.
        enemies.clear();
        int[][] grid = tileManager.getTileMap().getMap();
        for (int r = 0; r < grid.length; r++) {
            for (int c = 0; c < grid[r].length; c++) {
                if (grid[r][c] == 8) { 
                    enemies.add(new Enemy(c * 32, r * 32));
                }
            }
        }
    }

    private void updateLevelVisuals() {
        // Load the background image for the current level.
        String bgPath = levelManager.getCurrentBackgroundPath();
        if (new File(bgPath).exists()) {
            this.currentBackground = new ImageIcon(bgPath).getImage();
        }
    }

    public void startGameThread() {
        if (gameThread == null || !gameThread.isAlive()) {
            gameThread = new Thread(this);
            gameThread.start();

            // Trigger the dialogue here so it starts when the level is actually shown
            dialogueManager.startDialogue(new String[]{
                "Welcome to Dungeon Venture...",
                "Click A or D to move, and click SPACE to Jump.",
                "When interacting with Object like Portals, click E to interact.",
                "Your goal is to find the exit in each level.",
                "Clear them all to reveal the exit."
            });
        }
    }

    public void stopGameThread(){
        gameThread = null;
    }

    @Override
    public void run() {
        while (gameThread != null) {
            // Keep updating and redrawing while the game is running.
            update();
            repaint();
            try { Thread.sleep(16); } catch (InterruptedException e) { e.printStackTrace(); }
        }
    }

    private void update() {
    // Check if dialogue is running
    if (dialogueManager.isActive()) {
        dialogueManager.update(player.isInteractPressed());
        player.resetInputs(); // Clear key state so it doesn't trigger other things
        return; // STOP physics and movement here!
    }

    // --- NORMAL GAME LOGIC BELOW ---
    player.update(collisionChecker, tileManager, enemies); 
    playerDead();
    enemies.removeIf(e -> e.isDead());

    for (Enemy e : enemies) {
        e.update(1.0f/60.0f, player, collisionChecker, tileManager);
    }

    if (player.isInteractPressed()) {
        checkPortalContact();
    }
}

    public void playerDead(){ 
        // Switch to the game over screen when HP reaches zero.
        if (player.isDead()) {
            stopGameThread();
            cardLayout.show(mainPanel, "GameOver");
            mainPanel.getComponent(3).requestFocusInWindow();
            soundPlayer.stop("BgSound");
            soundPlayer.loop("UISound");
        }
    }

    public void resetGame(){ 
        // Reset everything so the next run starts from level 1 again.
        stopGameThread();
        player.resetStatus(); 
        levelManager.setLevel(0); 
        tileManager.setTileMap(levelManager.getCurrentLevel());
        spawnEnemies();
        updateLevelVisuals();
        spawnPlayer(); 
    }

    private void checkPortalContact() {
        // The portal only works after all enemies are gone.
        if (!enemies.isEmpty()) {
            return;
        }

        Rectangle hitbox = player.getHitbox();
        int row = hitbox.y / 32;
        int col = hitbox.x / 32;

        for (int r = row - 1; r <= row + 1; r++) {
            for (int c = col - 1; c <= col + 1; c++) {
                if (tileManager.isPortal(r, c)) {
                    Rectangle portalArea = tileManager.getPortalBounds(r, c);
                    if (hitbox.intersects(portalArea)) {
                        advanceToNextLevel();
                        return;
                    }
                }
            }
        }
    }

    private void advanceToNextLevel() {
        // Clear any held input before moving to the next stage.
        player.resetInputs();

        int healAmount = (int) (HPMax * 0.10); 
        player.heal(healAmount);

        int currentLevel = levelManager.getCurrentLevelIndex();

        if (currentLevel == 2) {
            soundPlayer.stop("BgSound");
            soundPlayer.loop("UISound");
            cardLayout.show(mainPanel, "Ending");
            mainPanel.getComponent(4).requestFocusInWindow();
        } else {
            int nextLevel = currentLevel + 1;
            levelManager.setLevel(nextLevel);
            tileManager.setTileMap(levelManager.getCurrentLevel());
            spawnEnemies(); 
            updateLevelVisuals();
            spawnPlayer();
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g; // Cast for better control

        if (currentBackground != null) g2.drawImage(currentBackground, 0, 0, 1280, 736, null);
        
        tileManager.draw(g2, enemies.isEmpty());
        
        for (Enemy e : enemies) { e.draw(g2); }
        player.draw(g2);

        // Render dialogue on top of everything
        dialogueManager.draw(g2);
    }
}
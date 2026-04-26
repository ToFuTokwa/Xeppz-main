import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import javax.swing.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class GamePanel extends JPanel implements Runnable {
    SoundPLayer soundPlayer = new SoundPLayer();
    private DialogueManager dialogueManager = new DialogueManager();
    
    // Pass 'this' into the PauseMenu
    private PauseMenu pauseMenu;
    
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
        this.cardLayout = cardLayout; 
        this.mainPanel = mainPanel; 
        this.pauseMenu = new PauseMenu(this); // Initialize with reference to this panel

        this.setPreferredSize(new Dimension(1280, 736));
        this.setFocusable(true);
        
        // ADDED: Special KeyListener for the Pause Menu logic
        this.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (pauseMenu.isActive()) {
                    pauseMenu.handleKeyPress(e);
                }
            }
        });

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
        String bgPath = levelManager.getCurrentBackgroundPath();
        if (new File(bgPath).exists()) {
            this.currentBackground = new ImageIcon(bgPath).getImage();
        }
    }

    public void startGameThread() {
        if (gameThread == null || !gameThread.isAlive()) {
            gameThread = new Thread(this);
            gameThread.start();
            soundPlayer.stop("MenuMusic");
            soundPlayer.loop("GameMusic");

            dialogueManager.startDialogue(new String[]{
                "Welcome to Dungeon Venture...",
                "Click A or D to move, and click SPACE to Jump.",
                "When interacting with Object like Portals, click E to interact.",
                "If you want to pause the game, click ESC to open the Pause Menu.",
                "Now let's get started!",
                "Your goal is to find the exit in each level.",
                "Clear them all to reveal the exit."
            });
        }
    }

    public void stopGameThread(){
        gameThread = null;
    }

    public void returnToMainMenu() {
    // 1. Stop the game loop thread
    stopGameThread();
    
    // 2. Switch back to the "Home" card defined in MainFile
    cardLayout.show(mainPanel, "Home");
    
    // 3. Reset Audio: Stop game music and loop the UI/Menu music
    soundPlayer.stop("GameMusic");
    soundPlayer.loop("UISound"); 

    // 4. CRITICAL: Request focus for the Home screen so it accepts input immediately
    // Since HomeUI was the first component added to mainPanel, it's at index 0
    mainPanel.getComponent(0).requestFocusInWindow();
}

    @Override
    public void run() {
        while (gameThread != null) {
            update();
            repaint();
            try { Thread.sleep(16); } catch (InterruptedException e) { e.printStackTrace(); }
        }
    }

    private void update() {
        // Handle the Toggle logic (Fixed the null parameter crash)
        pauseMenu.update(player.isPausePressed());

        if (pauseMenu.isActive()) {
            player.resetInputs(); 
            return; // Freezes game world while paused
        }

        if (dialogueManager.isActive()) {
            dialogueManager.update(player.isInteractPressed());
            player.resetInputs(); 
            return; 
        }

        // --- NORMAL GAME LOGIC ---
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
        if (player.isDead()) {
            stopGameThread();
            cardLayout.show(mainPanel, "GameOver");
            mainPanel.getComponent(3).requestFocusInWindow();
            soundPlayer.stop("GameMusic");
            soundPlayer.loop("UISound");
        }
    }

    public void resetGame(){ 
        player.resetStatus(); 
        levelManager.setLevel(0); 
        tileManager.setTileMap(levelManager.getCurrentLevel());
        spawnEnemies();
        updateLevelVisuals();
        spawnPlayer(); 
        // Restart thread if it was stopped
        if(gameThread == null) startGameThread();
    }

    private void checkPortalContact() {
        if (!enemies.isEmpty()) return;

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
        player.resetInputs();
        int healAmount = (int) (HPMax * 0.10); 
        player.heal(healAmount);

        int currentLevel = levelManager.getCurrentLevelIndex();

        if (currentLevel == 2) {
            soundPlayer.stop("GameMusic");
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
        Graphics2D g2 = (Graphics2D) g;

        if (currentBackground != null) g2.drawImage(currentBackground, 0, 0, 1280, 736, null);
        tileManager.draw(g2, enemies.isEmpty());
        for (Enemy e : enemies) { e.draw(g2); }
        player.draw(g2);

        dialogueManager.draw(g2);

        // ALWAYS draw the pause menu last so it stays on top
        pauseMenu.draw(g2);
    }
}

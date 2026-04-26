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
    boolean dropDebug = false;
    
    // Drop system variables
    private List<Drop> activeDrops = new ArrayList<>();
    
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
    double drawInterval = 1000000000.0 / 60.0; // Use .0 to ensure double precision
    double delta = 0;
    long lastTime = System.nanoTime();
    long currentTime;
    
    // For debugging speed
    long timer = 0;
    int drawCount = 0;

    while (gameThread != null) {
        currentTime = System.nanoTime();
        delta += (currentTime - lastTime) / drawInterval;
        timer += (currentTime - lastTime);
        lastTime = currentTime;

        if (delta >= 1) {
            update();   
            repaint();
            delta--;
            drawCount++;
        }

        // Console check: This should print "FPS: 60" every second
        if (timer >= 1000000000) {
            System.out.println("FPS: " + drawCount);
            drawCount = 0;
            timer = 0;
        }
    }
}

    private void update() {
    // 1. Handle Pause first
    pauseMenu.update(player.isPausePressed());
    if (pauseMenu.isActive()) {
        player.resetInputs();
        return;
    }

    // 2. Handle Dialogue
    if (dialogueManager.isActive()) {
        dialogueManager.update(player.isInteractPressed());
        player.resetInputs();
        return;
    }

    // 3. Update Entities ONCE
    player.update(collisionChecker, tileManager, enemies);
    for (Enemy e : enemies) {
        e.update(1.0f/60.0f, player, collisionChecker, tileManager);
    }

    // 4. Check for Deaths and Spawn Drops
    for (int i = 0; i < enemies.size(); i++) {
        Enemy e = enemies.get(i);
        if (e.isDead()) {
            if (dropDebug) {
                activeDrops.add(new Drop((int)e.getX(), (int)e.getY(), Drop.Type.DMG_BOOST));
            } else {
                double roll = Math.random();
                if (roll < 0.70) {
                    double dropType = Math.random();
                    if (dropType < 0.50) activeDrops.add(new Drop((int)e.getX(), (int)e.getY(), Drop.Type.HEAL));
                    else activeDrops.add(new Drop((int)e.getX(), (int)e.getY(), Drop.Type.DMG_BOOST));
                }
            }
            enemies.remove(i);
            i--;
        }
    }

    // 5. Handle Drop Pickups
    Rectangle pBox = player.getHitbox();
    for (int i = 0; i < activeDrops.size(); i++) {
        Drop d = activeDrops.get(i);
        if (pBox.intersects(d.getHitbox())) {
            if (d.type == Drop.Type.HEAL) player.heal(100);
            else player.activateDamageBoost();
            activeDrops.remove(i);
            i--;
        }
    }

    player.updateBoostTimer();
    playerDead();

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
        activeDrops.clear();

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

        // 1. DRAW BACKGROUND FIRST (So it's at the back)
        if (currentBackground != null) {
            g2.drawImage(currentBackground, 0, 0, 1280, 736, null);
        }

        // 2. DRAW WORLD OBJECTS
        tileManager.draw(g2, enemies.isEmpty());
        
        for (Drop d : activeDrops) {
            d.draw(g2); // Now these appear ON TOP of the background
        }

        for (Enemy e : enemies) { e.draw(g2); }
        player.draw(g2);

        // 3. DRAW UI (Dialogue/Pause)
        dialogueManager.draw(g2);
        pauseMenu.draw(g2);
    }
}

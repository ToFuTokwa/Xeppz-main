import java.awt.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.File;

public class Enemy {
    
    // CONSTANTS
    // These values control enemy size, hitboxes, damage, and movement.
    private static final int ENEMY_WIDTH = 128;
    private static final int ENEMY_HEIGHT = 128;
    private static final int HITBOX_X_OFFSET = 43; 
    private static final int HITBOX_Y_OFFSET = 0;
    private static final int VISUAL_Y_OFFSET = -30; 
    private static final int ATTACK_HITBOX_WIDTH = 28;
    private static final int ATTACK_HITBOX_HEIGHT = 62;
    private static final int ATTACK_HITBOX_X_OFFSET = -5; // Adjust this for better alignment
    private static final int ATTACK_HITBOX_Y_OFFSET = 0; // Adjust this for better alignment
    private static final int DAMAGE_AMOUNT = 35; //35
    
    private static final float PATROL_SPEED = 0.5f;
    private static final float CHASE_SPEED = 3.0f;
    private static final float GRAVITY = 0.5f;
    private static final float MAX_FALL_SPEED = 12.0f;
    
    private static final float PATROL_RANGE = 50.0f;
    private static final int MAX_HEALTH = 500;
    
    private static final float TELEGRAPH_TIME = 0.5f;
    private static final float ATTACK_DAMAGE_FRAME = 0.1f;
    
    private static final int TILE_SIZE = 32;
    private static final float FPS = 60.0f;
    private static final float ANIMATION_SPEED = 0.08f;

    // STATE ENUM
    public enum State {
        PATROL, CHASE, TELEGRAPH, ATTACK, HURT, DEAD
    }

    // These flags help track physics and debug drawing.
    private boolean isOnGround = false; // Track ground state for better physics handling
    private boolean debugMode = false; // Set to true to enable hitbox debugging

    // Position & Physics
    private float x, y;
    private float vx, vy;
    private int direction = 1;
    
    // State Management
    private State currentState = State.PATROL;
    private float stateTimer = 0;
    private int health = MAX_HEALTH;
    
    // Animation
    private BufferedImage[] attackFrames;
    private BufferedImage[] patrolFrames;
    private BufferedImage[] chaseFrames;
    private BufferedImage[] telegraphFrames;
    private BufferedImage[] hurtFrames;

    private int currentFrame = 0;
    private float animationCounter = 0;
    private boolean isAttacking = false;
    
    // Timers
    private float attackTimer = 0;
    private float patrolTimer = 0;
    private float chaseTimer = 0;
    private float telegraphTimer = 0;
    private float hurtTimer = 0;

    // Sprite Paths - UPDATE THESE TO MATCH YOUR FILES
    private String attackPath = "Assets/skeletonPack/Attack/Sword";
    private String patrolPath = "Assets/skeletonPack/Walk/Sword";
    private String chasePath = "Assets/skeletonPack/Run/Sword";
    private String telegraphPath = "Assets/skeletonPack/Idle/Sword";
    private String hurtPath = "Assets/skeletonPack/Hurt";
    
    // CONSTRUCTOR
    public Enemy(float startX, float startY) {
        // Create the enemy at the spawn position from the map.
        this.x = startX;
        this.y = startY;
        loadSprites();
    }
    
    /**
     * Main update loop
     */
    public void update(float deltaTime, Player player, CheckCollision collisionChecker, TileManager tileManager) {
        if (currentState == State.DEAD) return;
        
        // Update movement, AI, combat, animation, and map limits every frame.
        stateTimer += deltaTime;
        updateGravity(tileManager); // Update gravity with tileManager for ground checks
        updateStateMachine(deltaTime, player);
        handleCollisions(collisionChecker, tileManager);
        handleAttack(player);
        updateAnimation(deltaTime);
        borderHandling(tileManager);
    }
    
    /**
     * Render enemy with state-based visuals
     */
    public void draw(Graphics g) {
        if (currentState == State.DEAD) return;

        // Draw the current enemy sprite or a fallback shape if sprites are missing.
        Graphics2D g2 = (Graphics2D) g;
        BufferedImage image = getCurrentFrame();
        int visualY = (int)y + VISUAL_Y_OFFSET;

        if (image != null) {
            int drawX = (int)x;
            int drawY = visualY;
            
            if (direction == 1) {
                // Facing right - Normal draw
                g2.drawImage(image, drawX + ENEMY_WIDTH, drawY, -ENEMY_WIDTH, ENEMY_HEIGHT, null);
            } else {
                // Facing left - Flip horizontally using negative width
                g2.drawImage(image, drawX, drawY, ENEMY_WIDTH, ENEMY_HEIGHT, null);
            }
        } else {
            // Fallback: Show colored rect + state info for debugging
            applyStateVisuals(g);
            g.setFont(new Font("Arial", Font.BOLD, 12));
            g.drawString(currentState.name(), (int)x, (int)(y + VISUAL_Y_OFFSET - 5));
            g.fillRect((int)x + 10, visualY, ENEMY_WIDTH - 20, ENEMY_HEIGHT - 20);
        }

        // Exclamation mark above enemy during telegraph state
        if (currentState == State.TELEGRAPH) {
            g.setColor(Color.RED);
            g.setFont(new Font("Arial", Font.BOLD, 24));
            FontMetrics fm = g.getFontMetrics();
            int exclamationWidth = fm.stringWidth("!");
            int exclamationX = (int)x + (ENEMY_WIDTH / 2) - (exclamationWidth / 2);
            int exclamationY = visualY + 25; // 50 pixels above the sprite
            g.drawString("!", exclamationX, exclamationY);
        }

        renderDebugHitboxes(g);
    }

    // SPRITE LOADING
    private void loadSprites() {
        // Load every animation set used by the enemy.
        loadAttackFrames();
        loadPatrolFrames();
        loadChaseFrames();
        loadTelegraphFrames();
        loadHurtFrames();
        debugSpriteStatus(); // Debug output
    }

    private void renderDebugHitboxes(Graphics g) {
        // This only shows extra hitbox lines when debug mode is on.
        if (debugMode == false) return;
        g.setColor(Color.BLUE);
        Rectangle body = getEnemyHitbox();
        g.drawRect(body.x, body.y, body.width, body.height);

        // Draw Attack Hitbox (Only when attacking)
        if (currentState == State.ATTACK && debugMode == true) {
            g.setColor(Color.RED);
            Rectangle attack = getAttackHitbox();
            g.drawRect(attack.x, attack.y, attack.width, attack.height);
        }
    }

    private void loadHurtFrames() {
        try {
            // Split the hurt sprite sheet into separate frames.
            BufferedImage sheet = ImageIO.read(new File(hurtPath + "/Skeleton_Default_Hurt.png"));
            int frameWidth = sheet.getWidth() / 2;
            int frameHeight = sheet.getHeight();
            hurtFrames = new BufferedImage[2];

            for (int i = 0; i < 2; i++) {
                hurtFrames[i] = sheet.getSubimage(i * frameWidth, 0, frameWidth, frameHeight);
            }
            System.out.println(" Hurt sprites loaded: " + hurtFrames.length + " frames");
        } catch (Exception e) {
            System.err.println(" Failed to load hurt sprites: " + e.getMessage());
            System.err.println("File path: " + hurtPath + "/Skeleton_Default_Hurt.png");
            hurtFrames = null;
        }
    }
    
    private void loadTelegraphFrames() {
        try {
            // Telegraph frames play just before the enemy attacks.
            BufferedImage sheet = ImageIO.read(new File(telegraphPath + "/Skeleton_Default_Idle_Sword.png"));
            int frameWidth = sheet.getWidth() / 6;
            int frameHeight = sheet.getHeight();
            telegraphFrames = new BufferedImage[6];

            for (int i = 0; i < 6; i++) {
                telegraphFrames[i] = sheet.getSubimage(i * frameWidth, 0, frameWidth, frameHeight);
            }
            System.out.println(" Telegraph sprites loaded: " + telegraphFrames.length + " frames");
        } catch (Exception e) {
            System.err.println(" Failed to load telegraph sprites: " + e.getMessage());
            System.err.println("File path: " + telegraphPath + "/Skeleton_Default_Idle_Sword.png");
            telegraphFrames = null;
        }
    }

    private void loadAttackFrames() {
        try {
            // Attack frames are used while the skeleton swings its weapon.
            BufferedImage sheet = ImageIO.read(new File(attackPath + "/Skeleton_Default_Attack_Sword.png"));
            int frameWidth = sheet.getWidth() / 6;
            int frameHeight = sheet.getHeight();
            attackFrames = new BufferedImage[6];

            for (int i = 0; i < 6; i++) {
                attackFrames[i] = sheet.getSubimage(i * frameWidth, 0, frameWidth, frameHeight);
            }
            System.out.println(" Attack sprites loaded: " + attackFrames.length + " frames");
        } catch (Exception e) {
            System.err.println(" Failed to load attack sprites: " + e.getMessage());
            System.err.println("File path: " + attackPath + "/Skeleton_Default_Attack_Sword.png");
            attackFrames = null;
        }
    }

    private void loadPatrolFrames() {
        try {
            // Patrol frames are used while the enemy walks around normally.
            BufferedImage sheet = ImageIO.read(new File(patrolPath + "/MP_Skeleton_Default_Walk_Sword.png"));
            int frameWidth = sheet.getWidth() / 6;
            int frameHeight = sheet.getHeight();
            patrolFrames = new BufferedImage[6];

            for (int i = 0; i < 6; i++) {
                patrolFrames[i] = sheet.getSubimage(i * frameWidth, 0, frameWidth, frameHeight);
            }
            System.out.println(" Patrol sprites loaded: " + patrolFrames.length + " frames");
        } catch (Exception e) {
            System.err.println(" Failed to load patrol sprites: " + e.getMessage());
            System.err.println("File path: " + patrolPath + "/MP_Skeleton_Default_Walk_Sword.png");
            patrolFrames = null;
        }
    }

    private void loadChaseFrames() {
        try {
            // Chase frames are used when the player is close enough to alert the enemy.
            BufferedImage sheet = ImageIO.read(new File(chasePath + "/Skeleton_Default_Run_Sword.png"));
            int frameWidth = sheet.getWidth() / 6;
            int frameHeight = sheet.getHeight();
            chaseFrames = new BufferedImage[6];

            for (int i = 0; i < 6; i++) {
                chaseFrames[i] = sheet.getSubimage(i * frameWidth, 0, frameWidth, frameHeight);
            }
            System.out.println(" Chase sprites loaded: " + chaseFrames.length + " frames");
        } catch (Exception e) {
            System.err.println(" Failed to load chase sprites: " + e.getMessage());
            System.err.println("File path: " + chasePath + "/Skeleton_Default_Run_Sword.png");
            chaseFrames = null;
        }
    }

    // ANIMATION SYSTEM
    private BufferedImage getCurrentFrame() {
        // Pick the current sprite based on the enemy's state.
        if (isAttacking && attackFrames != null && attackFrames.length > 0) {
            return attackFrames[Math.min(currentFrame, attackFrames.length - 1)];
        } else if (currentState == State.PATROL && patrolFrames != null && patrolFrames.length > 0) {
            return patrolFrames[Math.min(currentFrame, patrolFrames.length - 1)];
        } else if (currentState == State.CHASE && chaseFrames != null && chaseFrames.length > 0) {
            return chaseFrames[Math.min(currentFrame, chaseFrames.length - 1)];
        } else if (currentState == State.TELEGRAPH && telegraphFrames != null && telegraphFrames.length > 0) {
            return telegraphFrames[Math.min(currentFrame, telegraphFrames.length - 1)];
        } else if (currentState == State.HURT && hurtFrames != null && hurtFrames.length > 0) {
            return hurtFrames[Math.min(currentFrame, hurtFrames.length - 1)];
        }
        return null;
    }
    
    private void updateAnimation(float deltaTime) {
        // Send animation updates to the correct state animation.
        if (isAttacking) {
            updateAttackAnimation(deltaTime);
        } else if (currentState == State.PATROL && patrolFrames != null) {
            updatePatrolAnimation(deltaTime);
        } else if (currentState == State.CHASE && chaseFrames != null) {
            updateChaseAnimation(deltaTime);
        } else if (currentState == State.TELEGRAPH && telegraphFrames != null) {
            updateTelegraphAnimation(deltaTime);
        } else if (currentState == State.HURT && hurtFrames != null) {
            updateHurtAnimation(deltaTime);
        }
    }

    private void updateHurtAnimation(float deltaTime) {
        // Hurt animation loops while the enemy is in the hurt state.
        hurtTimer += deltaTime;
        if (hurtTimer >= ANIMATION_SPEED) {
            currentFrame = (currentFrame + 1) % hurtFrames.length;
            hurtTimer = 0;
        }
    }

    private void updateTelegraphAnimation(float deltaTime) { 
        // Telegraph animation warns the player before the attack starts.
        telegraphTimer += deltaTime;
        if (telegraphTimer >= ANIMATION_SPEED) { 
            currentFrame = (currentFrame + 1) % telegraphFrames.length;
            telegraphTimer = 0;
        }
    }
    
    private void updateAttackAnimation(float deltaTime) {
        // Attack animation ends automatically when its last frame is reached.
        animationCounter += deltaTime;
        if (animationCounter >= ANIMATION_SPEED) {
            currentFrame++;
            animationCounter = 0;
            
            if (currentFrame >= attackFrames.length) {
                endAttack();
            }
        }
    }
    
    private void updatePatrolAnimation(float deltaTime) {
        // Patrol animation loops continuously while walking.
        patrolTimer += deltaTime;
        if (patrolTimer >= ANIMATION_SPEED) {
            currentFrame = (currentFrame + 1) % patrolFrames.length;
            patrolTimer = 0;
        }
    }

    private void updateChaseAnimation(float deltaTime) {
        // Chase animation loops while the enemy runs after the player.
        chaseTimer += deltaTime;
        if (chaseTimer >= ANIMATION_SPEED) {
            currentFrame = (currentFrame + 1) % chaseFrames.length;
            chaseTimer = 0;
        }
    }

    // PHYSICS SYSTEM
    private void updateGravity(TileManager tileManager) {
        // Check for ground below the enemy, otherwise make it fall.
        Rectangle hitbox = getEnemyHitbox();
        int centerX = hitbox.x + (hitbox.width / 2);
        int bottomY = hitbox.y + hitbox.height;

        // Check if the pixel directly below the center of the hitbox is solid
        int tileX = centerX / TILE_SIZE;
        int tileYBelow = (bottomY + 1) / TILE_SIZE;

        this.isOnGround = tileManager.isTileSolid(tileX, tileYBelow);

        if (isOnGround) {
            if (vy > 0) { 
                vy = 0;
                // Snap Y to the top of the tile to prevent sinking/jittering
                y = (tileYBelow * TILE_SIZE) - (getEnemyHitbox().height + HITBOX_Y_OFFSET);
            }
        } else {
            vy += GRAVITY;
            if (vy > MAX_FALL_SPEED) vy = MAX_FALL_SPEED;
        }
    }

    // COLLISION SYSTEM
    private void handleCollisions(CheckCollision collisionChecker, TileManager tileManager) {
        // Handle horizontal and vertical collision separately for smoother movement.
        // --- 1. HANDLE X AXIS ---
        float oldX = x;
        x += vx;
        
        if (collisionChecker.isColliding(this, tileManager)) {
            x = oldX; // Wall hit, revert X
            if (currentState == State.PATROL) {
                direction *= -1;
            }
            vx = 0;
        }

        // --- 2. HANDLE Y AXIS ---
        float oldY = y;
        y += vy; // Apply vertical velocity here
        
        if (collisionChecker.isColliding(this, tileManager)) {
            y = oldY; // Floor/Ceiling hit, revert Y
            
            if (vy > 0) {
                isOnGround = true;
            }
            vy = 0;
        }
    }

    // STATE MACHINE
    private void updateStateMachine(float deltaTime, Player player) {
        // Decide what the enemy should do based on distance and current state.
        float enemyCenterX = getEnemyHitbox().x + (getEnemyHitbox().width / 2);
        float playerCenterX = player.getHitbox().x + (player.getHitbox().width / 2);
        float distToPlayer = Math.abs(playerCenterX - enemyCenterX);
        float playerY = player.getY();
        boolean sameLevel = Math.abs(playerY - y) < TILE_SIZE * 2; 

        // 3. Logic for switching states
        if (currentState == State.PATROL && distToPlayer < PATROL_RANGE && sameLevel) {
            transitionToChase();
        }
        
        // 4. Handle specific state behaviors
        switch (currentState) {
            case PATROL -> handlePatrolState(player, deltaTime);
            case CHASE -> handleChaseState(distToPlayer, player, deltaTime); 
            case TELEGRAPH -> handleTelegraphState(deltaTime, player);  
            case ATTACK -> handleAttackState(player);                  
            case HURT -> handleHurtState();
            case DEAD -> {}
        }
    }

    private void handlePatrolState(Player player, float deltaTime) {
        // Patrol means moving left or right until the player is spotted.
        vx = direction * PATROL_SPEED;
        
        // Simplified vision detection using same logic as state machine
        float enemyCenterX = getEnemyHitbox().x + (getEnemyHitbox().width / 2);
        float playerCenterX = player.getHitbox().x + (player.getHitbox().width / 2);
        float distToPlayer = Math.abs(playerCenterX - enemyCenterX);
        boolean sameLevel = Math.abs(player.getY() - y) < TILE_SIZE * 2;
        
        if (distToPlayer < PATROL_RANGE && sameLevel) {
            transitionToChase();
        }
    }

    // THIS IS THE METHOD THAT WAS FIXED
    // THIS IS THE FULLY FIXED METHOD
    private void handleChaseState(float distanceToPlayer, Player player, float deltaTime) {
        // Chase tries to line the enemy up for an attack instead of just running forever.
        chaseTimer += deltaTime;
        
        Rectangle enemyHb = getEnemyHitbox();
        Rectangle playerHb = player.getHitbox(); 
        
        float enemyCenterX = enemyHb.x + (enemyHb.width / 2.0f);
        float playerCenterX = playerHb.x + (playerHb.width / 2.0f);

        // Calculate the absolute horizontal distance between centers FIRST
        float horizontalDist = Math.abs(playerCenterX - enemyCenterX);
        
        // CHASE LOGIC with distance checks:
        // If the player runs more than 500 pixels away, go back to patrolling
        if (horizontalDist > 300) {
            currentState = State.PATROL; // Transition back to patrol
            stateTimer = 0;              // Reset state timer
            return;                      // Exit this method so we don't process chase movement!
        }
        
        // 1. Max distance: Enemy stops chasing and starts attacking
        float maxAttackDistance = enemyHb.width + (ATTACK_HITBOX_WIDTH / 3.0f); 
        
        // 2. Min distance: Enemy must back up. 
        float minAttackDistance = (enemyHb.width / 2.0f) + (playerHb.width / 2.0f); 

        if (horizontalDist < minAttackDistance) {
            // 1. PLAYER IS TOO CLOSE ("In the middle"): Move away to align the attack
            if (horizontalDist < 0.1f) {
                direction = 1; 
            } else {
                direction = (playerCenterX > enemyCenterX) ? -1 : 1; 
            }
            vx = direction * CHASE_SPEED;
            
        } else if (horizontalDist <= maxAttackDistance) {
            // 2. PLAYER IS IN THE SWEET SPOT: Stop and attack
            vx = 0; 
            direction = (playerCenterX > enemyCenterX) ? 1 : -1;
            transitionToTelegraph();  
            
        } else {
            // 3. PLAYER IS TOO FAR (but still under 500px): Chase normally
            direction = (playerCenterX > enemyCenterX) ? 1 : -1; 
            vx = direction * CHASE_SPEED;
        }
    }

    private void handleTelegraphState(float deltaTime, Player player) {  
        // Pause briefly before attacking so the player can react.
        vx = 0;
        stateTimer += deltaTime;
        if (stateTimer > TELEGRAPH_TIME) {
            System.out.println("TELEGRAPH END → ATTACK");
            startAttack(player);  
        }
    }

    private void handleAttackState(Player player) {  
        // Stay still while the attack animation is happening.
        vx = 0;
    }

    private void handleHurtState() {
        // After being hurt, either die or return to patrol.
        vx = 0;
        if (stateTimer > 0.3f) {
            if (health <= 0) {
                currentState = State.DEAD;
            } else {
                currentState = State.PATROL; // Back to patrol after hurt
            }
            stateTimer = 0;
        }
    }
    
    // State Transitions
    private void transitionToChase() {
        // Move from patrol into chase mode.
        currentState = State.CHASE;
        stateTimer = 0;
    }
    
    private void transitionToTelegraph() {
        // Telegraph is the warning state before the real attack starts.
        if (currentState != State.TELEGRAPH) {
            currentState = State.TELEGRAPH;
            stateTimer = 0;
            vx = 0;
        }
    }
    
    private void startAttack(Player player) {
        // Start the attack and face the player first.
        currentState = State.ATTACK;
        stateTimer = 0;
        isAttacking = true;
        currentFrame = 0;
        animationCounter = 0;
        attackTimer = 0;
        
        // FORCE the direction based on the player's position relative to the skeleton
        if (player != null) {
            if (player.getX() < this.x) {
                direction = -1; // Face Left
            } else {
                direction = 1;  // Face Right
            }
        }
    }
    
    private void endAttack() {
        // Return to chase after the attack animation finishes.
        isAttacking = false;
        currentState = State.CHASE;
        stateTimer = 0;
        currentFrame = 0;
        attackTimer = 0;
    }

    // COMBAT SYSTEM

    private Rectangle getAttackHitbox() {
        // Build the enemy's attack range in front of its body.
        int attackX;
        int bodyHitboxX = (int)(x + HITBOX_X_OFFSET);
        int bodyHitboxWidth = ENEMY_WIDTH / 3;
        int attackY = (int)(y + ATTACK_HITBOX_Y_OFFSET); 

        if (direction == 1) { // Right
            attackX = bodyHitboxX + bodyHitboxWidth + ATTACK_HITBOX_X_OFFSET;
        } else { // Left
            // Subtract the width and the offset to push it to the left side
            attackX = bodyHitboxX - ATTACK_HITBOX_WIDTH - ATTACK_HITBOX_X_OFFSET;
        }

        return new Rectangle(attackX, attackY, ATTACK_HITBOX_WIDTH, ATTACK_HITBOX_HEIGHT);
    }

    private void handleAttack(Player player) {
        // Damage is only applied during a short part of the attack.
        if (currentState != State.ATTACK || !isAttacking) return;
        
        attackTimer += 1.0f / FPS;
        if (attackTimer >= ATTACK_DAMAGE_FRAME && attackTimer < ATTACK_DAMAGE_FRAME + 0.1f) {
            performAttack(player);  
        } 
    }
    
    private void performAttack(Player player) {
        // Only hurt the player if the attack hitbox actually connects.
        // USE the attack hitbox here, not the enemy's body hitbox
        if (getAttackHitbox().intersects(player.getHitbox())) {
            player.takeDamage(DAMAGE_AMOUNT);
        }
    }
    
    public void takeDamage(int damage) {
        // Being hit moves the enemy into the hurt state with some knockback.
        if (currentState == State.DEAD || currentState == State.HURT) return;
        
        health -= damage;
        currentState = State.HURT;
        stateTimer = 0;
        vx = -direction * CHASE_SPEED * 1.5f;
        vy = -5.0f;
    }

    // RENDERING HELPERS
    private void applyStateVisuals(Graphics g) {
        // Fallback colors make different states easier to tell apart.
        switch (currentState) {
            case TELEGRAPH -> g.setColor(Color.YELLOW);
            case ATTACK -> g.setColor(Color.ORANGE);
            case HURT -> g.setColor(Color.MAGENTA);
            case DEAD -> g.setColor(Color.GRAY);
            default -> g.setColor(Color.RED);
        }
    }

    // UTILITY & DEBUG METHODS
    public void borderHandling(TileManager tileManager) {
        // Stop the enemy from walking outside the map width.
        int mapWidth = tileManager.getTileMap().getMap()[0].length * TILE_SIZE;
        
        if (x < 0) {
            x = 0;
            direction = 1;
        } else if (x + ENEMY_WIDTH > mapWidth) {
            x = mapWidth - ENEMY_WIDTH;
            direction = -1;
        }
    }
    
    public float distanceTo(Player player) {
        // Simple helper for checking horizontal distance to the player.
        return Math.abs(player.getX() - x);
    }

    // DEBUG: Check sprite loading status
    public void debugSpriteStatus() {
        System.out.println("=== ENEMY SPRITE STATUS ===");
        System.out.println("Attack frames: " + (attackFrames != null ? attackFrames.length : "NULL"));
        System.out.println("Patrol frames: " + (patrolFrames != null ? patrolFrames.length : "NULL"));
        System.out.println("Current state: " + currentState);
        System.out.println("isAttacking: " + isAttacking);
        System.out.println("Position: (" + x + ", " + y + ")");
        System.out.println("========================");
    }

    // ACCESSORS
    public Rectangle getEnemyHitbox() {
        // Return the body hitbox used for collisions and damage checks.
        return new Rectangle(
            (int)(x + HITBOX_X_OFFSET),
            (int)(y + HITBOX_Y_OFFSET),
            ENEMY_WIDTH / 3,
            ENEMY_HEIGHT / 2
        );
    }
    
    public float getX() { return x; }
    public float getY() { return y; }
    public State getCurrentState() { return currentState; }
    public int getHealth() { return health; }
    
    public void setPosition(float x, float y) {
        // Directly place the enemy at a given position.
        this.x = x;
        this.y = y;
    }
    
    public boolean isDead() {
        // Dead enemies are removed by the game panel later.
        return currentState == State.DEAD;
    }
    
    public void kill() {
        // Force this enemy into the dead state immediately.
        health = 0;
        currentState = State.DEAD;
    }
}

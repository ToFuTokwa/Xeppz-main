import java.awt.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.File;

public class Boss {
    // Independent Position and Physics
    public float x;
    public float y;
    private float vx;
    private float vy;
    private float gravity = 0.5f;
    private float maxFallSpeed = 12.0f;
    
    // Boss Dimensions
    private int width = 64;
    private int height = 128;

    // Stats
    public int health = 1000;
    public int current_health = 1000;
    public int phase = 1;
    public boolean isAlive = true;

    // Combat Logic
    public int attackDamage = 50;
    public int attackCooldown = 0;
    public int attackCooldownTime = 100;
    public int secondPhaseThreshold = 500;
    public int enhancedAttackDamage = 100;

    public boolean isAttacking = false;
    public boolean isInSecondPhase = false;

    public Boss(int startX, int startY) {
        this.x = (float) startX;
        this.y = (float) startY;
    }

    public void update(Player player, CheckCollision collisionChecker, TileManager tileManager) {
        if (!isAlive) return;

        // 1. Independent Gravity and Collision
        applyPhysics(collisionChecker, tileManager);

        // 2. State Management
        checkPhase();
        updateCooldown();

        // 3. AI Logic
        float distanceToPlayer = Math.abs(player.getX() - this.x);

        if (attackCooldown <= 0) {
            if (distanceToPlayer < 100) {
                ShortRangeAttack();
            } else if (distanceToPlayer < 400) {
                LongRangeAttack();
            }
        }

        // Phase 2 Behavior: Constant Movement
        if (isInSecondPhase) {
            float moveSpeed = 1.5f;
            if (player.getX() > this.x) {
                x += moveSpeed;
            } else {
                x -= moveSpeed;
            }
        }
    }

    private void applyPhysics(CheckCollision collisionChecker, TileManager tileManager) {
        vy += gravity;
        if (vy > maxFallSpeed) vy = maxFallSpeed;

        // Vertical collision check using your collisionChecker
        if (collisionChecker.isEntityOnFloor(x, y + vy, width, height, tileManager)) {
            vy = 0;
        } else {
            y += vy;
        }
        
        // Horizontal movement application
        x += vx;
        vx *= 0.9; // Friction to slow down after attacks
    }

    public void takeDamage(int damage) {
        current_health -= damage;
        if (current_health <= 0) {
            current_health = 0;
            isAlive = false;
        }
    }

    public void checkPhase() {
        if (current_health <= secondPhaseThreshold && !isInSecondPhase) {
            isInSecondPhase = true;
            phase = 2;
            secondPhase();
        }
    }

    public void updateCooldown() {
        if (attackCooldown > 0) {
            attackCooldown--;
        } else {
            isAttacking = false;
        }
    }

    public void ShortRangeAttack() {
        isAttacking = true;
        attackCooldown = attackCooldownTime;
        // Dash slightly toward player
        vx = (vx > 0) ? 5.0f : -5.0f; 
    }

    public void LongRangeAttack() {
        isAttacking = true;
        attackCooldown = attackCooldownTime + 50;
        // Projectile logic would be triggered here
    }

    public void secondPhase() {
        // Buff the boss for phase 2
        this.attackDamage = enhancedAttackDamage;
    }

    public Rectangle getHitbox() {
        // A standard hitbox for damage detection
        return new Rectangle((int)x + 20, (int)y + 20, width - 40, height - 20);
    }

    public void draw(Graphics g) {
        if (!isAlive) return;

        // Visual distinction for phases
        if (isInSecondPhase) {
            g.setColor(Color.RED);
        } else {
            g.setColor(Color.DARK_GRAY);
        }

        // Draw Boss Body
        g.fillRect((int)x, (int)y, width, height);

        // Draw Health Bar
        g.setColor(Color.BLACK);
        g.fillRect((int)x, (int)y - 25, width, 10);
        g.setColor(Color.RED);
        int healthBarWidth = (int)(((float)current_health / health) * width);
        g.fillRect((int)x, (int)y - 25, Math.max(0, healthBarWidth), 10);
    }
}

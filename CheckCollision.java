import java.awt.Rectangle;

public class CheckCollision {
    // Tile size is used to convert pixel positions into map rows and columns.
    private final int TILE_SIZE = 32;

    public boolean isColliding(Player player, TileManager tileManager) {
        // Check collision using the player's hitbox.
        return checkTiles(player.getHitbox(), tileManager);
    }

    public boolean isColliding(Enemy enemy, TileManager tileManager) {
        // Check collision using the enemy's hitbox.
        return checkTiles(enemy.getEnemyHitbox(), tileManager);
    }

    private boolean checkTiles(Rectangle hitbox, TileManager tileManager) {
        // Find which tiles the hitbox is currently touching.
        int[][] grid = tileManager.getTileMap().getMap();
        
        int topRow = hitbox.y / TILE_SIZE;
        int bottomRow = (hitbox.y + hitbox.height) / TILE_SIZE;
        int leftCol = hitbox.x / TILE_SIZE;
        int rightCol = (hitbox.x + hitbox.width) / TILE_SIZE;

        for (int r = topRow; r <= bottomRow; r++) {
            for (int c = leftCol; c <= rightCol; c++) {
                if (r >= 0 && r < grid.length && c >= 0 && c < grid[0].length) {
                    int id = grid[r][c];
                    // Tile ids 1 to 3 are solid ground or walls.
                    if (id >= 1 && id <= 3) {
                        if (hitbox.intersects(tileManager.getBound(r, c))) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }
}

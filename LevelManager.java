public class LevelManager {
    // These arrays keep the level map, background, and name lined up by index.
    private TileMap[] levels = new TileMap[3];
    private String[] backgrounds = {"Assets/Mocap1.png", "Assets/Mocap2.png", "Assets/Mocap3.png"};
    private String[] names = {"Home", "Mob Cave", "Mine Shaft"};
    private int currentIndex = 0;
    private boolean[] levelValid = new boolean[3];

    public LevelManager() {
        // Try to load all levels at startup so the game knows what is available.
        for (int i = 0; i < 3; i++) {
            String levelFile = "levels/level" + (i + 1) + ".txt";
            System.out.println("Loading: " + levelFile);
            
            try {
                levels[i] = new TileMap(levelFile);
                levelValid[i] = isValidTileMap(levels[i]);
                if (!levelValid[i]) {
                    System.out.println("Invalid map data, creating fallback");
                    createFallbackLevel(i);
                } else {
                    System.out.println("✓ " + names[i] + " loaded OK");
                }
            } catch (Exception e) {
                System.err.println("✗ Failed to load " + levelFile);
                createFallbackLevel(i);
            }
        }
        
        // Start on first valid level
        findFirstValidLevel();
    }
    
    private boolean isValidTileMap(TileMap tm) {
        // Basic safety check to make sure the map is big enough to use.
        if (tm == null || tm.getMap() == null) return false;
        int[][] map = tm.getMap();
        return map.length >= 23 && map[0] != null && map[0].length >= 40;
    }
    
    private void createFallbackLevel(int index) {
        // If a level file fails, use an empty level instead of crashing.
        levels[index] = new TileMap(23, 40); // Needs this constructor in TileMap!
        levelValid[index] = true;
    }
    
    private void findFirstValidLevel() {
        // Start the game on the first level that loaded correctly.
        for (int i = 0; i < 3; i++) {
            if (levelValid[i]) {
                currentIndex = i;
                return;
            }
        }
        currentIndex = 0;
    }

    public void setLevel(int i) { 
        // Change the current level only if the index is valid.
        if (i >= 0 && i < 3) currentIndex = i; 
    }
    
    public int getCurrentLevelIndex() { return currentIndex; }
    
    public String[] getLevelNames() { 
        // Add a small status mark so the editor can show valid and invalid levels.
        String[] display = new String[3];
        for (int i = 0; i < 3; i++) {
            display[i] = names[i] + (levelValid[i] ? " ✓" : " ⚠");
        }
        return display;
    }
    
    public TileMap getCurrentLevel() { return levels[currentIndex]; }
    public String getCurrentBackgroundPath() { return backgrounds[currentIndex]; }
    public boolean isCurrentLevelValid() { return levelValid[currentIndex]; }
    public int getFirstValidLevelIndex() { 
        // Return the first safe level index the editor can open.
        for (int i = 0; i < 3; i++) if (levelValid[i]) return i;
        return 0;
    }
}

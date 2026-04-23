import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.*;

public class TileEditorFrame extends JFrame {
    // Basic editor size matches the same grid size used by the game.
    private final int tileSize = 32;
    private final int rows = 23;
    private final int cols = 40;
    
    private LevelManager levelManager;
    private TileManager tileManager;
    private int selectedTileID = 1;
    private BufferedImage backgroundImage;
    private JComboBox<String> levelSelector;
    private JLabel statusLabel;

    public TileEditorFrame() {
        // Build the level editor window and its side tools.
        setTitle("New Shit Level Editor");
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(cols * tileSize + 220, rows * tileSize + 100);
        setLayout(null);

        // Initialize LevelManager
        levelManager = new LevelManager();
        
        // Start with first valid level
        updateEditorLevel(levelManager.getFirstValidLevelIndex());

        // Drawing Area (Canvas)
        JPanel canvas = new JPanel() {
            @Override 
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                
                // Paint background, level tiles, grid lines, and warnings in order.
                // 1. Draw Background
                if (backgroundImage != null) {
                    g.drawImage(backgroundImage, 0, 0, getWidth(), getHeight(), null);
                } else {
                    g.setColor(Color.BLACK);
                    g.fillRect(0, 0, getWidth(), getHeight());
                }

                // 2. Draw Tiles (only if valid)
                if (tileManager != null && levelManager.isCurrentLevelValid()) {
                    tileManager.draw(g);
                }

                // 3. Draw Grid
                g.setColor(new Color(255, 255, 255, 40));
                for (int i = 0; i <= cols; i++) {
                    g.drawLine(i * tileSize, 0, i * tileSize, rows * tileSize);
                }
                for (int i = 0; i <= rows; i++) {
                    g.drawLine(0, i * tileSize, cols * tileSize, i * tileSize);
                }
                
                // 4. Warning if invalid level
                if (!levelManager.isCurrentLevelValid()) {
                    g.setColor(new Color(255, 0, 0, 120));
                    g.fillRect(0, 0, getWidth(), getHeight());
                    g.setColor(Color.WHITE);
                    g.setFont(new Font("Arial", Font.BOLD, 24));
                    FontMetrics fm = g.getFontMetrics();
                    String warning = "INVALID LEVEL";
                    int x = (getWidth() - fm.stringWidth(warning)) / 2;
                    int y = (getHeight() + fm.getAscent()) / 2;
                    g.drawString(warning, x, y);
                    g.setFont(new Font("Arial", Font.PLAIN, 16));
                    g.drawString("Use dropdown to load valid level", 
                               (getWidth() - g.getFontMetrics().stringWidth("Use dropdown to load valid level")) / 2, 
                               y + 30);
                }
            }
        };
        
        canvas.setBounds(10, 10, cols * tileSize, rows * tileSize);
        canvas.setBackground(Color.BLACK);
        canvas.setOpaque(true);

        // Mouse Controls - SAFE
        MouseAdapter ma = new MouseAdapter() {
            private void paint(MouseEvent e) {
                // Left click paints the selected tile, right click erases.
                if (!levelManager.isCurrentLevelValid()) return;
                
                int c = e.getX() / tileSize;
                int r = e.getY() / tileSize;
                
                if (r < 0 || r >= rows || c < 0 || c >= cols) return;
                
                try {
                    int newTileID = SwingUtilities.isRightMouseButton(e) ? 0 : selectedTileID;
                    levelManager.getCurrentLevel().getMap()[r][c] = newTileID;
                    
                    if (tileManager != null) {
                        tileManager.setTileMap(levelManager.getCurrentLevel());
                    }
                    canvas.repaint();
                } catch (Exception ex) {
                    System.err.println("Paint error [" + r + "," + c + "]: " + ex.getMessage());
                }
            }
            
            @Override public void mousePressed(MouseEvent e) { paint(e); }
            @Override public void mouseDragged(MouseEvent e) { paint(e); }
        };
        
        canvas.addMouseListener(ma);
        canvas.addMouseMotionListener(ma);
        add(canvas);

        // Sidebar
        JPanel sidebar = new JPanel();
        sidebar.setBounds(cols * tileSize + 20, 10, 180, rows * tileSize);
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBackground(Color.DARK_GRAY);
        sidebar.setOpaque(true);

        // Level Selection
        JPanel levelPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        levelPanel.setOpaque(false);
        levelPanel.add(new JLabel("Level:"));
        levelSelector = new JComboBox<>(levelManager.getLevelNames());
        levelSelector.setPreferredSize(new Dimension(150, 25));
        levelSelector.addActionListener(e -> {
            // Switch the editor to another level from the dropdown.
            int index = levelSelector.getSelectedIndex();
            if (updateEditorLevel(index)) {
                statusLabel.setText("Loaded: " + levelManager.getLevelNames()[index]);
            } else {
                JOptionPane.showMessageDialog(this, "Cannot load invalid level!", "Error", JOptionPane.WARNING_MESSAGE);
                levelSelector.setSelectedIndex(levelManager.getCurrentLevelIndex());
            }
            canvas.repaint();
        });
        levelPanel.add(levelSelector);
        sidebar.add(levelPanel);

        sidebar.add(Box.createVerticalStrut(5));

        // Save Button
        JButton btnSave = new JButton("Save Level");
        btnSave.setPreferredSize(new Dimension(160, 30));
        btnSave.setMaximumSize(new Dimension(160, 30));
        btnSave.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnSave.addActionListener(e -> saveCurrentMap());
        sidebar.add(btnSave);

        sidebar.add(Box.createVerticalStrut(10));
        sidebar.add(new JSeparator(JSeparator.HORIZONTAL));

        // Status
        statusLabel = new JLabel("Ready", SwingConstants.CENTER);
        statusLabel.setForeground(Color.WHITE);
        statusLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        statusLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        sidebar.add(statusLabel);

        sidebar.add(Box.createVerticalStrut(5));
        sidebar.add(new JLabel("Tiles:", SwingConstants.CENTER));

        // FIXED TILE BUTTONS - NO MORE RED ERROR!
        String[] tileNames = {"Eraser", "Dirt", "Grass", "Green", "Portal", "Enemy", "Player"};
        int[] ids = {0, 1, 2, 3, 7, 8, 9};
        
        for (int i = 0; i < tileNames.length; i++) {
            final String name = tileNames[i];  // CAPTURED!
            final int id = ids[i];
            
            // Each button picks a tile id for painting on the map.
            JButton b = new JButton(name);
            b.setPreferredSize(new Dimension(160, 28));
            b.setMaximumSize(new Dimension(160, 28));
            b.setAlignmentX(Component.CENTER_ALIGNMENT);
            b.setMargin(new Insets(2, 5, 2, 5));
            b.addActionListener(e -> {
                selectedTileID = id;
                // Clear other buttons
                Component[] components = sidebar.getComponents();
                for (Component c : components) {
                    if (c instanceof JButton && !c.equals(b)) {
                        ((JButton) c).setBackground(null);
                    }
                }
                b.setBackground(selectedTileID == id ? Color.CYAN : null);
                statusLabel.setText("Tile: " + name + " (ID:" + id + ")");
            });
            sidebar.add(b);
            sidebar.add(Box.createVerticalStrut(2));
        }

        add(sidebar);
        
        // Animation
        // Repaint often so animated tiles like the portal stay moving in the editor.
        new Timer(150, e -> canvas.repaint()).start();

        // Final status
        statusLabel.setText("Ready - " + levelManager.getLevelNames()[levelManager.getCurrentLevelIndex()]);

        setLocationRelativeTo(null);
        setVisible(true);
    }

    private boolean updateEditorLevel(int index) {
        // Load the chosen level into the editor and refresh its background.
        try {
            levelManager.setLevel(index);
            
            if (!levelManager.isCurrentLevelValid()) {
                return false;
            }
            
            // Update TileManager
            if (tileManager == null) {
                tileManager = new TileManager(levelManager.getCurrentLevel());
            } else {
                tileManager.setTileMap(levelManager.getCurrentLevel());
            }

            // Update selector
            if (levelSelector != null) {
                levelSelector.setSelectedIndex(index);
            }

            // Load background
            try {
                String path = levelManager.getCurrentBackgroundPath();
                File f = new File(path);
                backgroundImage = f.exists() ? ImageIO.read(f) : null;
            } catch (IOException ignored) {
                backgroundImage = null;
            }
            
            return true;
        } catch (Exception e) {
            System.err.println("updateEditorLevel failed: " + e.getMessage());
            return false;
        }
    }

    private void saveCurrentMap() {
        // Save the current map as a text file in the levels folder.
        if (!levelManager.isCurrentLevelValid()) {
            JOptionPane.showMessageDialog(this, "Cannot save invalid level!", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String name = JOptionPane.showInputDialog(this, "Save as (e.g., level1):", "Save Level", JOptionPane.PLAIN_MESSAGE);
        if (name == null || name.trim().isEmpty()) return;

        File dir = new File("levels");
        if (!dir.exists() && !dir.mkdirs()) {
            JOptionPane.showMessageDialog(this, "Cannot create levels folder!", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        File file = new File(dir, name.trim() + ".txt");
        try (PrintWriter pw = new PrintWriter(file)) {
            int[][] map = levelManager.getCurrentLevel().getMap();
            for (int r = 0; r < rows; r++) {
                for (int c = 0; c < cols; c++) {
                    pw.print(map[r][c]);
                    if (c < cols - 1) pw.print(",");
                }
                pw.println();
            }
            JOptionPane.showMessageDialog(this, "Saved: " + file.getAbsolutePath(), "Success", JOptionPane.INFORMATION_MESSAGE);
            statusLabel.setText("Saved: " + name);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Save failed: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public static void main(String[] args) {
        // Start the editor using the system look and feel.
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}
        SwingUtilities.invokeLater(TileEditorFrame::new);
    }
}

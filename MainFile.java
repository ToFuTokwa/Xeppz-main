import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;

public class MainFile {
    // This is the main starting point of the whole game.
    private static final String JAVAFX_SDK_LIB = "Assets/javafx-sdk-20.0.2/lib";
    private static final String RELAUNCH_FLAG = "game.javafx.relaunch";

    public static void main(String[] args) {
        // If JavaFX is missing, the game relaunches itself with the needed setup.
        if (ensureJavaFxRuntime()) {
            return;
        }

        // Start the Swing window on the proper UI thread.
        SwingUtilities.invokeLater(MainFile::createAndShowGame);
    }

    private static void createAndShowGame() {
        // Set up the window and all screens the player can move between.
        SoundPLayer soundPlayer = new SoundPLayer();
        soundPlayer.init(); // Load sounds at the start of the game
        soundPlayer.loop("UISound"); // Start background music immediately
        JFrame window = new JFrame();
        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        window.setResizable(false);
        window.setTitle("Dungeon Venture"); // Updated title
        
        CardLayout cardLayout = new CardLayout();
        JPanel mainPanel = new JPanel(cardLayout);

        GamePanel gamePanel = new GamePanel(cardLayout, mainPanel);
        JPanel introPanel = createIntroPanel(cardLayout, mainPanel, gamePanel);
        HomeUI homeUI = new HomeUI(cardLayout, mainPanel, gamePanel, (StartSequence) introPanel);
        GameOverPanel gameOverPanel = new GameOverPanel(cardLayout, mainPanel, gamePanel);
        Level3EndPanel endingPanel = new Level3EndPanel(cardLayout, mainPanel, gamePanel);

        mainPanel.add(homeUI, "Home");
        mainPanel.add(introPanel, "Intro");
        mainPanel.add(gamePanel, "Game");
        mainPanel.add(gameOverPanel, "GameOver");
        mainPanel.add(endingPanel, "Ending"); 

        window.add(mainPanel);
        window.pack();
        window.setLocationRelativeTo(null);
        window.setVisible(true);

        // Start at the Home screen
        cardLayout.show(mainPanel, "Home");
    }

    private static JPanel createIntroPanel(CardLayout cardLayout, JPanel mainPanel, GamePanel gamePanel) {
        try {
            // Create the intro panel from its class name so startup stays flexible.
            Class<?> introClass = Class.forName("PreGameIntroPanel");
            Constructor<?> constructor = introClass.getConstructor(CardLayout.class, JPanel.class, GamePanel.class);
            Object introPanel = constructor.newInstance(cardLayout, mainPanel, gamePanel);
            return (JPanel) introPanel;
        } catch (Exception e) {
            throw new IllegalStateException("Unable to create intro video panel", e);
        }
    }

    private static boolean ensureJavaFxRuntime() {
        try {
            // These classes only exist when JavaFX is available.
            Class.forName("javafx.embed.swing.JFXPanel");
            Class.forName("javafx.scene.media.Media");
            return false;
        } catch (ClassNotFoundException ignored) {
            // Relaunch below if possible.
        }

        if (Boolean.getBoolean(RELAUNCH_FLAG)) {
            JOptionPane.showMessageDialog(
                null,
                "JavaFX could not be loaded even after relaunch.\nRun with the JavaFX SDK in the module path.",
                "Startup Error",
                JOptionPane.ERROR_MESSAGE
            );
            return false;
        }

        File javafxLib = new File(JAVAFX_SDK_LIB);
        if (!javafxLib.exists()) {
            JOptionPane.showMessageDialog(
                null,
                "JavaFX SDK was not found at:\n" + JAVAFX_SDK_LIB,
                "Startup Error",
                JOptionPane.ERROR_MESSAGE
            );
            return false;
        }

        try {
            // Build a new Java command with JavaFX modules included.
            List<String> command = new ArrayList<>();
            command.add(getJavaExecutable());
            command.add("--module-path");
            command.add(JAVAFX_SDK_LIB);
            command.add("--add-modules");
            command.add("javafx.controls,javafx.media,javafx.swing");
            command.add("--enable-native-access=javafx.graphics,javafx.media");
            command.add("-D" + RELAUNCH_FLAG + "=true");
            command.add("-cp");
            command.add(System.getProperty("java.class.path"));
            command.add(MainFile.class.getName());

            new ProcessBuilder(command)
                .directory(new File(System.getProperty("user.dir")))
                .inheritIO()
                .start();
            return true;
        } catch (IOException e) {
            JOptionPane.showMessageDialog(
                null,
                "Failed to relaunch with JavaFX:\n" + e.getMessage(),
                "Startup Error",
                JOptionPane.ERROR_MESSAGE
            );
            return false;
        }
    }

    private static String getJavaExecutable() {
        // Get the Java executable from the current Java installation.
        String javaHome = System.getProperty("java.home");
        return javaHome + File.separator + "bin" + File.separator + "java.exe";
    }
}

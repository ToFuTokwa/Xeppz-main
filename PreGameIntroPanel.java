import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.StackPane;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;

import javax.swing.*;
import java.awt.*;
import java.io.File;

public class PreGameIntroPanel extends JPanel implements StartSequence {
    private static final int SCREEN_WIDTH = 1280;
    private static final int SCREEN_HEIGHT = 736;
    private static final String VIDEO_PATH = "Assets/intro.mp4";

    private final CardLayout cardLayout;
    private final JPanel mainPanel;
    private final GamePanel gamePanel;
    private final SoundPLayer soundPlayer = new SoundPLayer();
    
    private JFXPanel fxPanel; 
    private MediaPlayer mediaPlayer;
    private volatile boolean introRunning;

    public PreGameIntroPanel(CardLayout cardLayout, JPanel mainPanel, GamePanel gamePanel) {
        this.cardLayout = cardLayout;
        this.mainPanel = mainPanel;
        this.gamePanel = gamePanel;

        // Set the size of the Swing Panel
        this.setPreferredSize(new Dimension(SCREEN_WIDTH, SCREEN_HEIGHT));
        this.setBackground(Color.BLACK);
        this.setLayout(new BorderLayout());

        // Initialize the JavaFX-to-Swing bridge
        fxPanel = new JFXPanel();
        // Crucial: Set the size of the fxPanel explicitly so it doesn't default to 0x0
        fxPanel.setPreferredSize(new Dimension(SCREEN_WIDTH, SCREEN_HEIGHT));
        
        this.add(fxPanel, BorderLayout.CENTER);

        ensureFxStarted();
    }

    private void ensureFxStarted() {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException ignored) {}
    }

    @Override
    public void playIntro() {
        gamePanel.resetGame();
        introRunning = true;
        soundPlayer.stop("UISound"); // Stop menu music

        // Show this panel in the CardLayout immediately
        cardLayout.show(mainPanel, "Intro");

        // Run the video setup on the JavaFX Thread
        Platform.runLater(this::setupVideoAndPlay);
    }

    private void setupVideoAndPlay() {
        File videoFile = new File(VIDEO_PATH);
        if (!videoFile.exists()) {
            System.out.println("Video not found at: " + videoFile.getAbsolutePath());
            SwingUtilities.invokeLater(this::finishIntro);
            return;
        }

        try {
            Media media = new Media(videoFile.toURI().toString());
            mediaPlayer = new MediaPlayer(media);
            MediaView mediaView = new MediaView(mediaPlayer);

            // Fit the video to your game dimensions
            mediaView.setFitWidth(SCREEN_WIDTH);
            mediaView.setFitHeight(SCREEN_HEIGHT);
            mediaView.setPreserveRatio(true);

            StackPane root = new StackPane(mediaView);
            root.setStyle("-fx-background-color: black;");
            Scene scene = new Scene(root, SCREEN_WIDTH, SCREEN_HEIGHT);

            // Re-attach skip controls to the embedded scene
            scene.setOnKeyPressed(event -> {
                if (event.getCode() == KeyCode.SPACE || event.getCode() == KeyCode.ENTER) {
                    SwingUtilities.invokeLater(this::finishIntro);
                }
            });

            // Set the scene to the bridge panel
            fxPanel.setScene(scene);

            mediaPlayer.setOnEndOfMedia(() -> SwingUtilities.invokeLater(this::finishIntro));
            
            // Start the video
            mediaPlayer.play();
            
        } catch (Exception e) {
            e.printStackTrace();
            SwingUtilities.invokeLater(this::finishIntro);
        }
    }

    private synchronized void finishIntro() {
        if (!introRunning) return;
        introRunning = false;

        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.dispose(); // Free up memory
        }

        soundPlayer.loop("BgSound"); // Start game music
        cardLayout.show(mainPanel, "Game");
        gamePanel.requestFocusInWindow();
        gamePanel.startGameThread(); // Start game loop
    }
}
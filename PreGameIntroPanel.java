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
    private volatile boolean introRunning = false;

    public PreGameIntroPanel(CardLayout cardLayout, JPanel mainPanel, GamePanel gamePanel) {
        this.cardLayout = cardLayout;
        this.mainPanel = mainPanel;
        this.gamePanel = gamePanel;

        this.setPreferredSize(new Dimension(SCREEN_WIDTH, SCREEN_HEIGHT));
        this.setBackground(Color.BLACK);
        this.setLayout(new BorderLayout());

        fxPanel = new JFXPanel();
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
        if (introRunning) return; // Prevent double-triggering
        
        introRunning = true;
        gamePanel.resetGame();
        soundPlayer.stop("UISound");

        // Force the card switch immediately
        cardLayout.show(mainPanel, "Intro");

        Platform.runLater(this::setupVideoAndPlay);
    }

    private void setupVideoAndPlay() {
        File videoFile = new File(VIDEO_PATH);
        if (!videoFile.exists()) {
            SwingUtilities.invokeLater(this::finishIntro);
            return;
        }

        try {
            Media media = new Media(videoFile.toURI().toString());
            mediaPlayer = new MediaPlayer(media);
            mediaPlayer.setCycleCount(1); 

            // 1. Existing trigger (Natural end)
            mediaPlayer.setOnEndOfMedia(() -> {
                System.out.println("EndOfMedia triggered");
                SwingUtilities.invokeLater(this::finishIntro);
            });

            // 2. NEW SAFETY TRIGGER: Watch for any status change
            mediaPlayer.statusProperty().addListener((obs, oldStatus, newStatus) -> {
                // If the video stops or finishes, move to the game
                if (newStatus == MediaPlayer.Status.STOPPED || newStatus == MediaPlayer.Status.HALTED) {
                    System.out.println("Status changed to: " + newStatus);
                    SwingUtilities.invokeLater(this::finishIntro);
                }
            });

            MediaView mediaView = new MediaView(mediaPlayer);
            mediaView.setFitWidth(SCREEN_WIDTH);
            mediaView.setFitHeight(SCREEN_HEIGHT);
            mediaView.setPreserveRatio(true);

            StackPane root = new StackPane(mediaView);
            root.setStyle("-fx-background-color: black;");
            Scene scene = new Scene(root, SCREEN_WIDTH, SCREEN_HEIGHT);

            // This part is working for you already
            scene.setOnKeyPressed(event -> {
                if (event.getCode() == KeyCode.SPACE || event.getCode() == KeyCode.ENTER) {
                    SwingUtilities.invokeLater(this::finishIntro);
                }
            });

            fxPanel.setScene(scene);
            mediaPlayer.play();

            fxPanel.setFocusable(true);
            fxPanel.requestFocusInWindow(); 
            Platform.runLater(() -> fxPanel.getScene().getRoot().requestFocus());
            
        } catch (Exception e) {
            e.printStackTrace();
            SwingUtilities.invokeLater(this::finishIntro);
        }
    }

    private synchronized void finishIntro() {
        // Prevent this from running multiple times if EndOfMedia and a key press happen at once
        if (!introRunning) return;
        introRunning = false;

        // 1. Cleanup JavaFX on the JavaFX Thread
        Platform.runLater(() -> {
            if (mediaPlayer != null) {
                mediaPlayer.stop();
                mediaPlayer.dispose(); 
                mediaPlayer = null;
            }
        });

        // 2. Update Swing UI on the Swing Thread
        SwingUtilities.invokeLater(() -> {
            soundPlayer.loop("BgSound"); 
            
            // Ensure "Game" matches the name used in MainFile.java
            cardLayout.show(mainPanel, "Game"); 
            
            gamePanel.requestFocusInWindow();
            gamePanel.startGameThread(); // Begins the game loop in GamePanel
            
            // Force UI refresh
            mainPanel.revalidate();
            mainPanel.repaint();
        });
    }
}
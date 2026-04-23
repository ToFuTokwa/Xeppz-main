import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.StackPane;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import javax.swing.*;
import java.awt.*;
import java.io.File;

public class PreGameIntroPanel extends JPanel implements StartSequence {
    private static final int SCREEN_WIDTH = 1280;
    private static final int SCREEN_HEIGHT = 736;
    private static final String VIDEO_PATH = "Assets/intro.mp4";
    private static final String LEGACY_VIDEO_PATH = "intro.mp4";

    private final CardLayout cardLayout;
    private final JPanel mainPanel;
    private final GamePanel gamePanel;
    private final SoundPLayer soundPlayer = new SoundPLayer();
    private final JLabel statusLabel = new JLabel("Preparing intro video...", SwingConstants.CENTER);

    private MediaPlayer mediaPlayer;
    private MediaView mediaView;
    private Stage videoStage;
    private volatile boolean introRunning;
    private volatile boolean pendingPlay;
    private volatile boolean videoReady;
    private volatile boolean videoFailed;
    private boolean fxStarted;

    public PreGameIntroPanel(CardLayout cardLayout, JPanel mainPanel, GamePanel gamePanel) {
        this.cardLayout = cardLayout;
        this.mainPanel = mainPanel;
        this.gamePanel = gamePanel;

        setPreferredSize(new Dimension(SCREEN_WIDTH, SCREEN_HEIGHT));
        setBackground(Color.BLACK);
        setLayout(new BorderLayout());
        setFocusable(true);

        statusLabel.setForeground(Color.WHITE);
        statusLabel.setFont(new Font("SansSerif", Font.BOLD, 22));
        add(statusLabel, BorderLayout.CENTER);

        ensureFxStarted();
        initializeVideoPlayer();
    }

    @Override
    public void playIntro() {
        gamePanel.resetGame();
        introRunning = true;
        pendingPlay = true;
        soundPlayer.stop("UISound");

        if (videoFailed) {
            finishIntro();
            return;
        }

        if (videoReady) {
            syncVideoStageToGameWindow();
            startPlayback();
        }
    }

    private void ensureFxStarted() {
        if (fxStarted) return;

        try {
            Platform.startup(() -> Platform.setImplicitExit(false));
        } catch (IllegalStateException ignored) {
            Platform.setImplicitExit(false);
        }
        fxStarted = true;
    }

    private void initializeVideoPlayer() {
        File videoFile = resolveVideoFile();
        if (!videoFile.exists()) {
            videoFailed = true;
            statusLabel.setText("Video file not found: " + videoFile.getPath());
            return;
        }

        Platform.runLater(() -> {
            try {
                Media media = new Media(videoFile.toURI().toString());
                media.setOnError(() -> handleVideoError(media.getError() == null ? "Unknown media error" : media.getError().getMessage()));

                mediaPlayer = new MediaPlayer(media);
                mediaPlayer.setAutoPlay(false);
                mediaPlayer.setCycleCount(1);

                mediaView = new MediaView(mediaPlayer);
                mediaView.setPreserveRatio(true);
                mediaView.setFitWidth(SCREEN_WIDTH);
                mediaView.setFitHeight(SCREEN_HEIGHT);

                StackPane root = new StackPane(mediaView);
                root.setStyle("-fx-background-color: black;");

                Scene scene = new Scene(root, SCREEN_WIDTH, SCREEN_HEIGHT);

                // Only Space and Enter skip the intro — no mouse, no Escape.
                scene.setOnKeyPressed(event -> {
                    if (event.getCode() == KeyCode.SPACE || event.getCode() == KeyCode.ENTER) {
                        SwingUtilities.invokeLater(this::finishIntro);
                    }
                });

                videoStage = new Stage(StageStyle.UNDECORATED);
                videoStage.setScene(scene);
                videoStage.setWidth(SCREEN_WIDTH);
                videoStage.setHeight(SCREEN_HEIGHT);
                videoStage.setResizable(false);
                videoStage.centerOnScreen();
                videoStage.setAlwaysOnTop(true);
                videoStage.setOnCloseRequest(event -> SwingUtilities.invokeLater(this::finishIntro));

                mediaPlayer.setOnReady(() -> SwingUtilities.invokeLater(() -> {
                    videoReady = true;
                    statusLabel.setText("");
                    if (introRunning && pendingPlay) {
                        syncVideoStageToGameWindow();
                        startPlayback();
                    }
                }));

                mediaPlayer.setOnEndOfMedia(() -> SwingUtilities.invokeLater(this::finishIntro));
                mediaPlayer.setOnError(() -> handleVideoError(mediaPlayer.getError() == null ? "Unknown player error" : mediaPlayer.getError().getMessage()));
            } catch (Exception e) {
                handleVideoError(e.getMessage());
            }
        });
    }

    private File resolveVideoFile() {
        File packagedVideo = new File(VIDEO_PATH);
        if (packagedVideo.exists()) return packagedVideo;
        return new File(LEGACY_VIDEO_PATH);
    }

    private void startPlayback() {
        pendingPlay = false;
        Platform.runLater(() -> {
            if (mediaPlayer == null || videoStage == null) {
                handleVideoError("Video player was not created.");
                return;
            }

            if (!videoStage.isShowing()) {
                videoStage.show();
                videoStage.toFront();
                videoStage.requestFocus();
            }

            mediaPlayer.stop();
            mediaPlayer.play();
        });
    }

    private void syncVideoStageToGameWindow() {
        SwingUtilities.invokeLater(() -> {
            Window window = SwingUtilities.getWindowAncestor(mainPanel);
            Point panelLocation = new Point(0, 0);
            SwingUtilities.convertPointToScreen(panelLocation, mainPanel);

            int targetWidth = Math.max(1, mainPanel.getWidth());
            int targetHeight = Math.max(1, mainPanel.getHeight());

            if (window != null && (targetWidth <= 1 || targetHeight <= 1)) {
                targetWidth = Math.max(1, window.getWidth());
                targetHeight = Math.max(1, window.getHeight());
                panelLocation = window.getLocationOnScreen();
            }

            final int stageX = panelLocation.x;
            final int stageY = panelLocation.y;
            final int stageWidth = targetWidth;
            final int stageHeight = targetHeight;

            Platform.runLater(() -> {
                if (videoStage != null) {
                    videoStage.setX(stageX);
                    videoStage.setY(stageY);
                    videoStage.setWidth(stageWidth);
                    videoStage.setHeight(stageHeight);
                }
                if (mediaView != null) {
                    mediaView.setFitWidth(stageWidth);
                    mediaView.setFitHeight(stageHeight);
                }
            });
        });
    }

    private void handleVideoError(String message) {
        videoFailed = true;
        videoReady = false;
        SwingUtilities.invokeLater(() -> {
            statusLabel.setText("Video error: " + (message == null ? "Unknown error" : message));
            finishIntro();
        });
    }

    private synchronized void finishIntro() {
        if (!introRunning) return;

        introRunning = false;
        pendingPlay = false;

        Platform.runLater(() -> {
            if (mediaPlayer != null) mediaPlayer.stop();
            if (videoStage != null && videoStage.isShowing()) videoStage.hide();
        });

        soundPlayer.loop("BgSound");
        cardLayout.show(mainPanel, "Game");
        gamePanel.requestFocusInWindow();
        gamePanel.startGameThread();
    }
}
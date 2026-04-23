import javax.sound.sampled.*;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class SoundPLayer {
    // This shared map stores sounds by a simple name like "Jump" or "BgSound".
    private static Map<String, Clip> soundMap = new HashMap<>();

    // Made static so you don't need 'new SoundPLayer()'
    public void init() {
        // Load every sound file the game uses.
        loadSound("Punch", "Assets/Sound/Punch.wav");
        loadSound("Walk", "Assets/Sound/Walk.wav");
        loadSound("Jump", "Assets/Sound/Jump.wav");
        loadSound("FemaleHurt", "Assets/Sound/FemaleHurt.wav");
        loadSound("ManHurt", "Assets/Sound/ManHurt.wav");
        loadSound("BgSound", "Assets/Sound/BgSound.wav");
        loadSound("UISound", "Assets/Sound/UISound.wav");
    }

    public void loadSound(String name, String path) {
        // Open the sound file and keep it ready to play later.
        try {
            File file = new File(path);
            if (!file.exists()) {
                System.err.println("CRITICAL: Sound file missing at " + file.getAbsolutePath());
                return;
            }
            AudioInputStream stream = AudioSystem.getAudioInputStream(file);
            Clip clip = AudioSystem.getClip();
            clip.open(stream);
            soundMap.put(name, clip);
        } catch (Exception e) {
            System.err.println("Error loading sound [" + name + "]: " + e.getMessage());
        }
    }

    public void play(String name) {
        // Restart the sound from the beginning each time it is played.
        Clip clip = soundMap.get(name);
        if (clip != null) {
            if (clip.isRunning()) clip.stop(); 
            clip.setFramePosition(0); // Rewind
            clip.start();
        } else {
            System.err.println("Sound key not found in map: " + name);
        }
    }

    // Add to SoundPLayer.java
    public void loop(String name) {
        // Keep this sound repeating until something stops it.
        Clip clip = soundMap.get(name);
        if (clip != null && !clip.isRunning()) {
            clip.setFramePosition(0);
            clip.loop(Clip.LOOP_CONTINUOUSLY);
        }
    }

    public void stop(String name) {
        // Stop a sound only if it is currently playing.
        Clip clip = soundMap.get(name);
        if (clip != null && clip.isRunning()) {
            clip.stop();
        }
    }
}

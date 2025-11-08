package utils;

import javafx.scene.media.AudioClip;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * SoundManager - Quản lý âm thanh trong game
 * Tách riêng từ AssetManager để dễ control
 */
public class SoundManager {
    private static SoundManager instance;

    private static Map<String, AudioClip> soundEffects;
    private MediaPlayer backgroundMusic;

    private boolean soundEnabled = true;
    private boolean musicEnabled = true;

    private double soundVolume = 2.0;
    private double musicVolume = 0.4;

    private SoundManager() {
        soundEffects = new HashMap<>();
        loadSoundEffects();

    }

    public static SoundManager getInstance() {
        if (instance == null) {
            instance = new SoundManager();
        }
        return instance;
    }

    /**
     * Load all sound effects
     */
    private void loadSoundEffects() {
        // Item sounds
        loadSound("item_correct", "/resources/assets/sounds/effects/item_correct.wav");
        loadSound("item_wrong", "/resources/assets/sounds/effects/item_wrong.wav");
        loadSound("item_pickup", "/resources/assets/sounds/effects/item_pickup.wav");

        // Combo sounds
        loadSound("combo_increase", "/resources/assets/sounds/effects/combo_increase.wav");
        loadSound("combo_break", "/resources/assets/sounds/effects/combo_break.wav");

        // Customer sounds
        loadSound("customer_happy", "/resources/assets/sounds/effects/customer_happy.wav");
        loadSound("customer_angry", "/resources/assets/sounds/effects/customer_angry.wav");

        // Game sounds
        loadSound("timer_warning", "/resources/assets/sounds/effects/timer_warning.wav");
        loadSound("game_over", "/resources/assets/sounds/effects/game_over.wav");
        loadSound("game_start", "/resources/assets/sounds/effects/game_start.wav");

        // UI sounds
        loadSound("button_hover", "/resources/assets/sounds/effects/button_hover.wav");
    }

    /**
     * Load single sound effect
     */
    private void loadSound(String key, String path) {
        try {
            String url = getClass().getResource(path).toExternalForm();
            AudioClip clip = new AudioClip(url);
            soundEffects.put(key, clip);
            System.out.println("🔊 Loaded sound: " + key);
        } catch (Exception e) {
            System.out.println("⚠️  Sound not found: " + path + " (will be silent)");
        }
    }

    /**
     * Play sound effect
     */
    public void play(String soundKey) {
        if (!soundEnabled) return;

        AudioClip clip = soundEffects.get(soundKey);
        if (clip != null) {
            clip.play(soundVolume);
        }
    }

    /**
     * Play sound with custom volume
     */
    public void play(String soundKey, double volume) {
        if (!soundEnabled) return;

        AudioClip clip = soundEffects.get(soundKey);
        if (clip != null) {
            clip.play(volume * soundVolume);
        }
    }

    /**
     * Play sound with rate (pitch)
     */
    public void playWithRate(String soundKey, double rate) {
        if (!soundEnabled) return;

        AudioClip clip = soundEffects.get(soundKey);
        if (clip != null) {
            clip.play(soundVolume, 0.0, rate, 0.0, 1);
        }
    }

    /**
     * Play background music (loop)
     */
    public void playMusic(String musicName) {
        if (!musicEnabled) return;

        try {
            stopMusic();

            String path = "/resources/assets/sounds/music/" + musicName + ".wav";
            String url = Objects.requireNonNull(getClass().getResource(path)).toExternalForm();

            Media media = new Media(url);
            backgroundMusic = new MediaPlayer(media);
            backgroundMusic.setCycleCount(MediaPlayer.INDEFINITE);
            backgroundMusic.setVolume(musicVolume);

            backgroundMusic.setOnError(() -> {
                System.err.println("Music playback error: " + backgroundMusic.getError().getMessage());
            });
            
            backgroundMusic.play();

            System.out.println("Playing music: " + musicName);
        } catch (Exception e) {
            System.err.println("Music not found or cannot play: " + musicName);
            e.printStackTrace();
        }
    }

    /**
     * Stop music
     */
    public void stopMusic() {
        if (backgroundMusic != null) {
            backgroundMusic.stop();
            backgroundMusic.dispose();
            backgroundMusic = null;
        }
    }

    /**
     * Pause music
     */
    public void pauseMusic() {
        if (backgroundMusic != null) {
            backgroundMusic.pause();
        }
    }

    /**
     * Resume music
     */
    public void resumeMusic() {
        if (backgroundMusic != null && musicEnabled) {
            backgroundMusic.play();
        }
    }

    /**
     * Fade out music
     */
    public void fadeOutMusic(double duration) {
        if (backgroundMusic != null) {
            double startVolume = backgroundMusic.getVolume();
            javafx.animation.Timeline timeline = new javafx.animation.Timeline(
                    new javafx.animation.KeyFrame(
                            javafx.util.Duration.seconds(duration),
                            event -> stopMusic(),
                            new javafx.animation.KeyValue(
                                    backgroundMusic.volumeProperty(),
                                    0.0
                            )
                    )
            );
            timeline.play();
        }
    }

    /**
     * Toggle sound on/off
     */
    public void toggleSound() {
        soundEnabled = !soundEnabled;
        System.out.println("🔊 Sound: " + (soundEnabled ? "ON" : "OFF"));
    }

    /**
     * Toggle music on/off
     */
    public void toggleMusic() {
        musicEnabled = !musicEnabled;
        if (musicEnabled) {
            resumeMusic();
        } else {
            pauseMusic();
        }
        System.out.println("🎵 Music: " + (musicEnabled ? "ON" : "OFF"));
    }

    /**
     * Set sound volume (0.0 to 1.0)
     */
    public void setSoundVolume(double volume) {
        soundVolume = Math.max(0.0, Math.min(1.0, volume));
    }

    /**
     * Set music volume (0.0 to 1.0)
     */
    public void setMusicVolume(double volume) {
        musicVolume = Math.max(0.0, Math.min(1.0, volume));
        if (backgroundMusic != null) {
            backgroundMusic.setVolume(musicVolume);
        }
    }

    /**
     * Get sound enabled state
     */
    public boolean isSoundEnabled() {
        return soundEnabled;
    }

    /**
     * Get music enabled state
     */
    public boolean isMusicEnabled() {
        return musicEnabled;
    }

    /**
     * Cleanup
     */
    public void dispose() {
        stopMusic();
        soundEffects.clear();
    }

    // === Convenience methods for common game sounds ===

    public void playCorrect() {
        play("item_correct");
    }

    public void playWrong() {
        play("item_wrong");
    }

    public void playPickup() {
        play("item_pickup", 0.5);
    }

    public void playComboIncrease(int comboLevel) {
        // Higher pitch for higher combo
        double pitch = 1.0 + (comboLevel * 0.1);
        playWithRate("combo_increase", Math.min(pitch, 2.0));
    }

    public void playComboBreak() {
        play("combo_break");
    }

    public void playCustomerHappy() {
        play("customer_happy");
    }

    public void playCustomerAngry() {
        play("customer_angry", 0.7);
    }

    public void playTimerWarning() {
        play("timer_warning");
    }

    public void playGameOver() {
        play("game_over");
    }

    public void playGameStart() {
        play("game_start");
    }

    public void playGameTheme() {
        playMusic("game_theme");
    }

    public void playMenuMusic() {
        playMusic("menu_music");
    }

    public void playButtonHover() {
        play("button_hover", 0.2);
    }
}
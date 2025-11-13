package utils;
import javafx.scene.media.AudioClip;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
/**
 * SoundManager - Quản lý âm thanh trong game
 * Tách riêng từ AssetManager để dễ control
 * Bổ sung chức năng lưu/tải settings từ config file
 */
public class SoundManager {
    private static SoundManager instance;
    private static Map<String, AudioClip> soundEffects;
    private MediaPlayer backgroundMusic;
    private boolean soundEnabled = true;
    private boolean musicEnabled = true;
    private double soundVolume = 1.0;
    private double musicVolume = 0.4;
    private static final String CONFIG_FILE = "config.properties";
    private SoundManager() {
        soundEffects = new HashMap<>();
        loadSettings(); // Load settings from config file first
        loadSoundEffects();
    }
    public static SoundManager getInstance() {
        if (instance == null) {
            instance = new SoundManager();
        }
        return instance;
    }
    /**
     * Load sound settings from config file
     */
    private void loadSettings() {
        try (InputStream input = getClass().getClassLoader().getResourceAsStream(CONFIG_FILE)) {
            if (input != null) {
                Properties prop = new Properties();
                prop.load(input);
                soundEnabled = Boolean.parseBoolean(prop.getProperty("sound.effects.enabled", "true"));
                soundVolume = Double.parseDouble(prop.getProperty("sound.effects.volume", "1.0"));
                musicEnabled = Boolean.parseBoolean(prop.getProperty("sound.music.enabled", "true"));
                musicVolume = Double.parseDouble(prop.getProperty("sound.music.volume", "0.4"));
                System.out.println("🔊 Sound settings loaded - Effects: " + soundVolume + ", Music: " + musicVolume);
            }
        } catch (Exception e) {
            System.out.println("⚠️ Using default sound settings");
        }
    }
    /**
     * Save sound settings to config file
     */
    public void saveSettings() {
        Properties prop = new Properties();
        // Load existing properties first
        try (InputStream input = getClass().getClassLoader().getResourceAsStream(CONFIG_FILE)) {
            if (input != null) {
                prop.load(input);
            }
        } catch (Exception e) {
            System.err.println("Could not load existing config");
        }
        // Update sound settings
        prop.setProperty("sound.effects.enabled", String.valueOf(soundEnabled));
        prop.setProperty("sound.effects.volume", String.format("%.2f", soundVolume));
        prop.setProperty("sound.music.enabled", String.valueOf(musicEnabled));
        prop.setProperty("sound.music.volume", String.format("%.2f", musicVolume));
        // Save to file
        try {
            String configPath = getClass().getClassLoader().getResource(CONFIG_FILE).getPath();
            if (configPath.startsWith("/") && configPath.contains(":")) {
                configPath = configPath.substring(1);
            }
            try (FileOutputStream output = new FileOutputStream(configPath)) {
                prop.store(output, "Supermarket Game Client Configuration");
                System.out.println("💾 Sound settings saved successfully");
            }
        } catch (Exception e) {
            System.err.println("❌ Could not save settings: " + e.getMessage());
            try {
                File configFile = new File("Client/src/resources/config.properties");
                if (!configFile.exists()) {
                    configFile = new File("src/resources/config.properties");
                }
                if (configFile.exists()) {
                    try (FileOutputStream output = new FileOutputStream(configFile)) {
                        prop.store(output, "Supermarket Game Client Configuration");
                        System.out.println("💾 Sound settings saved to: " + configFile.getAbsolutePath());
                    }
                }
            } catch (Exception e2) {
                System.err.println("❌ Could not save to alternative path: " + e2.getMessage());
            }
        }
    }
    private void loadSoundEffects() {
        loadSound("item_correct", "/resources/assets/sounds/effects/item_correct.wav");
        loadSound("item_wrong", "/resources/assets/sounds/effects/item_wrong.wav");
        loadSound("item_pickup", "/resources/assets/sounds/effects/item_pickup.wav");
        loadSound("combo_increase", "/resources/assets/sounds/effects/combo_increase.wav");
        loadSound("combo_break", "/resources/assets/sounds/effects/combo_break.wav");
        loadSound("customer_happy", "/resources/assets/sounds/effects/customer_happy.wav");
        loadSound("customer_angry", "/resources/assets/sounds/effects/customer_angry.wav");
        loadSound("timer_warning", "/resources/assets/sounds/effects/timer_warning.wav");
        loadSound("game_over", "/resources/assets/sounds/effects/game_over.wav");
        loadSound("game_start", "/resources/assets/sounds/effects/game_start.wav");
        loadSound("button_hover", "/resources/assets/sounds/effects/button_hover.wav");
        loadSound("menu_button", "/resources/assets/sounds/effects/menu_button.wav");
    }
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
    public void play(String soundKey) {
        if (!soundEnabled) return;
        AudioClip clip = soundEffects.get(soundKey);
        if (clip != null) {
            clip.play(soundVolume);
        }
    }
    public void play(String soundKey, double volume) {
        if (!soundEnabled) return;
        AudioClip clip = soundEffects.get(soundKey);
        if (clip != null) {
            clip.play(volume * soundVolume);
        }
    }
    public void playWithRate(String soundKey, double rate) {
        if (!soundEnabled) return;
        AudioClip clip = soundEffects.get(soundKey);
        if (clip != null) {
            clip.play(soundVolume, 0.0, rate, 0.0, 1);
        }
    }
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
            backgroundMusic.setOnError(() -> System.err.println("Music playback error: " + backgroundMusic.getError().getMessage()));
            backgroundMusic.play();
            System.out.println("🎵 Playing music: " + musicName);
        } catch (Exception e) {
            System.err.println("⚠️ Music not found or cannot play: " + musicName);
        }
    }
    public void stopMusic() {
        if (backgroundMusic != null) {
            backgroundMusic.stop();
            backgroundMusic.dispose();
            backgroundMusic = null;
        }
    }
    public void pauseMusic() {
        if (backgroundMusic != null) {
            backgroundMusic.pause();
        }
    }
    public void resumeMusic() {
        if (backgroundMusic != null && musicEnabled) {
            backgroundMusic.play();
        }
    }
    public void fadeOutMusic(double duration) {
        if (backgroundMusic != null) {
            javafx.animation.Timeline timeline = new javafx.animation.Timeline(
                    new javafx.animation.KeyFrame(
                            javafx.util.Duration.seconds(duration),
                            event -> stopMusic(),
                            new javafx.animation.KeyValue(backgroundMusic.volumeProperty(), 0.0)
                    )
            );
            timeline.play();
        }
    }
    public void toggleSound() {
        soundEnabled = !soundEnabled;
        System.out.println("🔊 Sound: " + (soundEnabled ? "ON" : "OFF"));
    }
    public void toggleMusic() {
        musicEnabled = !musicEnabled;
        if (musicEnabled) {
            resumeMusic();
        } else {
            pauseMusic();
        }
        System.out.println("🎵 Music: " + (musicEnabled ? "ON" : "OFF"));
    }
    public void setSoundEnabled(boolean enabled) {
        soundEnabled = enabled;
        System.out.println("🔊 Sound: " + (soundEnabled ? "ON" : "OFF"));
    }
    public void setMusicEnabled(boolean enabled) {
        musicEnabled = enabled;
        if (musicEnabled) {
            resumeMusic();
        } else {
            pauseMusic();
        }
        System.out.println("🎵 Music: " + (musicEnabled ? "ON" : "OFF"));
    }
    public void setSoundVolume(double volume) {
        soundVolume = Math.max(0.0, Math.min(1.0, volume));
    }
    public void setMusicVolume(double volume) {
        musicVolume = Math.max(0.0, Math.min(1.0, volume));
        if (backgroundMusic != null) {
            backgroundMusic.setVolume(musicVolume);
        }
    }
    public double getSoundVolume() {
        return soundVolume;
    }
    public double getMusicVolume() {
        return musicVolume;
    }
    public boolean isSoundEnabled() {
        return soundEnabled;
    }
    public boolean isMusicEnabled() {
        return musicEnabled;
    }
    public void dispose() {
        stopMusic();
        soundEffects.clear();
    }
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

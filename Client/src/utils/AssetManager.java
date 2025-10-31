package utils;

import javafx.scene.image.Image;
import javafx.scene.media.AudioClip;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * AssetManager - Quản lý tất cả assets (images, sounds) của game
 * Singleton pattern để đảm bảo chỉ có 1 instance
 */
public class AssetManager {
    private static AssetManager instance;

    // Cache để lưu assets đã load
    private Map<String, Image> imageCache;
    private Map<String, AudioClip> soundCache;
    private MediaPlayer backgroundMusic;

    // Asset paths
    private static final String IMAGE_BASE = "../resources/assets/images/";
    private static final String SOUND_BASE = "../resources/assets/sounds/";
    private static final String MUSIC_BASE = "../resources/assets/music/";

    // Default fallback (emoji) nếu không có image
    private boolean useFallback = false;

    private AssetManager() {
        imageCache = new HashMap<>();
        soundCache = new HashMap<>();
        loadAssets();
    }

    public static AssetManager getInstance() {
        if (instance == null) {
            instance = new AssetManager();
        }
        return instance;
    }

    /**
     * Load tất cả assets khi khởi động
     */
    private void loadAssets() {
        System.out.println("📦 Loading game assets...");

        // Load item images
        loadItemImages();

        // Load UI images
        loadUIImages();

        // Load sound effects
        loadSoundEffects();

        System.out.println("✅ Assets loaded: " +
                imageCache.size() + " images, " +
                soundCache.size() + " sounds");
    }

    /**
     * Load item images (products)
     */
    private void loadItemImages() {
        String[] items = {
                "milk", "bread", "apple", "carrot",
                "orange", "eggs", "cheese", "meat", "soda"
        };

        for (String item : items) {
            String path = IMAGE_BASE + "items/" + item + ".png";
            loadImage("item_" + item, path);
        }
    }

    /**
     * Load UI images
     */
    private void loadUIImages() {
        // Backgrounds
        loadImage("bg_main", IMAGE_BASE + "backgrounds/menu_bg.png");
        loadImage("bg_game", IMAGE_BASE + "backgrounds/game_bg.png");

        // Customer sprites
        loadImage("customer_happy", IMAGE_BASE + "customers/customer_happy.png");
        loadImage("customer_neutral", IMAGE_BASE + "customers/customer_neutral.png");
        loadImage("customer_angry", IMAGE_BASE + "customers/customer_angry.png");

        // Effects
        loadImage("sparkle", IMAGE_BASE + "effects/sparkle.png");
        loadImage("star", IMAGE_BASE + "effects/combo.png");
        loadImage("coin", IMAGE_BASE + "effects/coin.png");

        // Icons
        loadImage("icon_score", IMAGE_BASE + "icons/score.png");
        loadImage("icon_time", IMAGE_BASE + "icons/time.png");
        loadImage("icon_combo", IMAGE_BASE + "icons/combo.png");
    }

    /**
     * Load sound effects
     */
    private void loadSoundEffects() {
        String[] sounds = {
                "item_correct", "item_wrong", "item_pickup",
                "combo_increase", "combo_break",
                "customer_happy", "customer_angry",
                "timer_warning", "game_over", "button_click"
        };

        for (String sound : sounds) {
            String path = SOUND_BASE + "effects/" + sound + ".wav";
            loadSound(sound, path);
        }
    }

    /**
     * Load một image từ path
     */
    private void loadImage(String key, String path) {
        try {
            InputStream stream = getClass().getResourceAsStream(path);
            if (stream != null) {
                Image image = new Image(stream);
                imageCache.put(key, image);
                System.out.println("  ✓ Loaded image: " + key);
            } else {
                System.out.println("  ⚠ Image not found: " + path);
                useFallback = true;
            }
        } catch (Exception e) {
            System.err.println("  ✗ Failed to load image: " + path);
            System.err.println("    " + e.getMessage());
            useFallback = true;
        }
    }

    /**
     * Load một sound từ path
     */
    private void loadSound(String key, String path) {
        try {
            String url = getClass().getResource(path).toExternalForm();
            AudioClip clip = new AudioClip(url);
            soundCache.put(key, clip);
            System.out.println("  ✓ Loaded sound: " + key);
        } catch (Exception e) {
            System.out.println("  ⚠ Sound not found: " + path);
        }
    }

    /**
     * Get image by key
     */
    public Image getImage(String key) {
        return imageCache.get(key);
    }

    /**
     * Get item image (with fallback to emoji)
     */
    public Image getItemImage(String itemName) {
        String key = "item_" + itemName.toLowerCase();
        Image image = imageCache.get(key);

        if (image == null && useFallback) {
            // Return null to use emoji fallback in UI
            return null;
        }

        return image;
    }

    /**
     * Check if image exists
     */
    public boolean hasImage(String key) {
        return imageCache.containsKey(key);
    }

    /**
     * Play sound effect
     */
    public void playSound(String soundKey) {
        AudioClip clip = soundCache.get(soundKey);
        if (clip != null) {
            clip.play();
        }
    }

    /**
     * Play sound effect with volume
     */
    public void playSound(String soundKey, double volume) {
        AudioClip clip = soundCache.get(soundKey);
        if (clip != null) {
            clip.play(volume);
        }
    }

    /**
     * Play background music (loop)
     */
    public void playBackgroundMusic(String musicName) {
        try {
            stopBackgroundMusic();

            String path = MUSIC_BASE + musicName + ".mp3";
            String url = getClass().getResource(path).toExternalForm();

            Media media = new Media(url);
            backgroundMusic = new MediaPlayer(media);
            backgroundMusic.setCycleCount(MediaPlayer.INDEFINITE);
            backgroundMusic.setVolume(0.5);
            backgroundMusic.play();

            System.out.println("🎵 Playing music: " + musicName);
        } catch (Exception e) {
            System.err.println("⚠ Failed to load music: " + musicName);
        }
    }

    /**
     * Stop background music
     */
    public void stopBackgroundMusic() {
        if (backgroundMusic != null) {
            backgroundMusic.stop();
            backgroundMusic = null;
        }
    }

    /**
     * Set background music volume
     */
    public void setMusicVolume(double volume) {
        if (backgroundMusic != null) {
            backgroundMusic.setVolume(Math.max(0.0, Math.min(1.0, volume)));
        }
    }

    /**
     * Pause background music
     */
    public void pauseMusic() {
        if (backgroundMusic != null) {
            backgroundMusic.pause();
        }
    }

    /**
     * Resume background music
     */
    public void resumeMusic() {
        if (backgroundMusic != null) {
            backgroundMusic.play();
        }
    }

    /**
     * Check if using fallback (no assets loaded)
     */
    public boolean isUsingFallback() {
        return useFallback;
    }

    /**
     * Get emoji fallback for item
     */
    public static String getEmojiForItem(String itemName) {
        switch (itemName.toUpperCase()) {
            case "MILK": return "🥛";
            case "BREAD": return "🍞";
            case "APPLE": return "🍎";
            case "CARROT": return "🥕";
            case "ORANGE": return "🍊";
            case "EGGS": return "🥚";
            case "CHEESE": return "🧀";
            case "MEAT": return "🥩";
            case "SODA": return "🥤";
            default: return "❓";
        }
    }

    /**
     * Cleanup resources
     */
    public void dispose() {
        stopBackgroundMusic();
        imageCache.clear();
        soundCache.clear();
    }
}
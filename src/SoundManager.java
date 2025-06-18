import javax.sound.sampled.*;
import java.io.File;
import java.io.IOException;

public class SoundManager {
    private static Clip startGameSound;
    private static Clip eatPelletSound;
    private static Clip eatGhostSound;
    private static Clip deathSound;
    private static Clip powerUpSound;

    static {
        try {

            startGameSound = loadSound("resources/sounds/sound1.wav");
            eatPelletSound = loadSound("resources/sounds/sound2.wav");
            eatGhostSound = loadSound("resources/sounds/sound3.wav");
            deathSound = loadSound("resources/sounds/sounddie.wav");
            powerUpSound = loadSound("resources/sounds/soundlosing.wav");
        } catch (Exception e) {
            System.err.println("Error download sound: " + e.getMessage());
        }
    }

    private static Clip loadSound(String path) throws IOException, UnsupportedAudioFileException, LineUnavailableException {
        File soundFile = new File(path);
        if (!soundFile.exists()) {
            throw new IOException("Cannot find sound: " + path);
        }
        AudioInputStream ais = AudioSystem.getAudioInputStream(soundFile);
        Clip clip = AudioSystem.getClip();
        clip.open(ais);
        return clip;
    }

    public static void playStartGame() {
        playSound(startGameSound);
    }

    public static void playEatPellet() {
        playSound(eatPelletSound);
    }


    public static void playEatGhost() {
        playSound(eatGhostSound);
    }

    public static void playDeath() {
        playSound(deathSound);
    }

    public static void playPowerUp() {
        playSound(powerUpSound);
    }

    private static void playSound(Clip clip) {
        if (clip != null) {
            clip.setFramePosition(0);
            clip.start();
        }
    }
} 
import javax.swing.*;
import java.awt.*;

public class PowerUp {
    private static final int SIZE = 30;
    private final int x;
    private final int y;
    private final PowerUpType type;
    private boolean isActive = true;

    public enum PowerUpType {
        SPEED("Speed +50%", Color.GREEN),
        INVINCIBILITY("Invincible", Color.YELLOW),
        GHOST_FREEZE("Freeze Ghosts", Color.CYAN);

        final String description;
        final Color color;

        PowerUpType(String description, Color color) {
            this.description = description;
            this.color = color;
        }
    }

    public PowerUp(int x, int y, PowerUpType type) {
        this.x = x;
        this.y = y;
        this.type = type;
    }

    public void draw(Graphics g) {
        if (!isActive) return;
        g.setColor(type.color);
        g.fillOval(x + 5, y + 5, SIZE - 10, SIZE - 10);
    }

    public boolean intersects(Pacman pacman) {
        Rectangle powerUpBounds = new Rectangle(x, y, SIZE, SIZE);
        Rectangle pacmanBounds = new Rectangle(pacman.getX(), pacman.getY(), SIZE, SIZE);
        return powerUpBounds.intersects(pacmanBounds);
    }

    public void apply(GameWindow gameWindow) {
        switch (type) {
            case SPEED:
                Pacman pacman = gameWindow.getPacman();
                pacman.setSpeedMultiplier(1.5);
                new Thread(() -> {
                    try {
                        Thread.sleep(10000); // 10 секунд
                        pacman.resetSpeed();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }).start();
                break;
            case INVINCIBILITY:
                gameWindow.activatePowerMode();
                break;
            case GHOST_FREEZE:
                for (Ghost ghost : gameWindow.getGhosts()) {
                    ghost.freeze();
                }
                new Thread(() -> {
                    try {
                        Thread.sleep(5000); // 5 секунд
                        for (Ghost ghost : gameWindow.getGhosts()) {
                            ghost.unfreeze();
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }).start();
                break;
        }
    }

    public boolean isActive() {
        return isActive;
    }
}
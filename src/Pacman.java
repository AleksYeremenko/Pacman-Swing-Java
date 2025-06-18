import javax.swing.*;
import java.awt.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class Pacman {
    private static final int SIZE = 30;
    private static final int ANIMATION_FRAMES = 4;
    private static final int BASE_MOVEMENT_SPEED = 4;
    private static final int FRAME_DELAY = 50;

    private int x;
    private int y;
    private Direction currentDirection = Direction.RIGHT;
    private Direction nextDirection = Direction.RIGHT;
    private final int[][] maze;
    private int currentFrame = 0;
    private final AtomicBoolean isMoving = new AtomicBoolean(true);
    private final GameWindow gameWindow;
    private final Thread animationThread;
    private double speedMultiplier = 1.0;
    private Thread speedBoostThread;


    public Pacman(GameWindow gameWindow, int[][] maze, int startX, int startY) {
        this.gameWindow = gameWindow;
        this.maze = maze;
        this.x = startX * SIZE;
        this.y = startY * SIZE;

        animationThread = new Thread(() -> {
            while (isMoving.get()) {
                try {
                    Thread.sleep(FRAME_DELAY);
                    currentFrame = (currentFrame + 1) % ANIMATION_FRAMES;
                    gameWindow.repaintGame();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
        animationThread.start();
    }

    public void draw(Graphics g) {
        g.setColor(Color.YELLOW);
        if (currentFrame == 0 || currentFrame == ANIMATION_FRAMES - 1) {
            g.fillOval(x, y, SIZE, SIZE);
        } else {
            int startAngle = 0;
            switch (currentDirection) {
                case UP:
                    startAngle = 90;
                    break;
                case DOWN:
                    startAngle = 270;
                    break;
                case LEFT:
                    startAngle = 180;
                    break;
                case RIGHT:
                    startAngle = 0;
                    break;
            }
            g.fillArc(x, y, SIZE, SIZE, startAngle + 45 - (currentFrame * 5), 270 + (currentFrame * 10));
        }
    }

    public void move() {
        int currentSpeed = (int)(BASE_MOVEMENT_SPEED * speedMultiplier);

        int nextX = x + (nextDirection.dx * currentSpeed);
        int nextY = y + (nextDirection.dy * currentSpeed);

        if (canMove(nextX, nextY)) {
            currentDirection = nextDirection;
            x = nextX;
            y = nextY;
            return;
        }

        nextX = x + (currentDirection.dx * currentSpeed);
        nextY = y + (currentDirection.dy * currentSpeed);

        if (canMove(nextX, nextY)) {
            x = nextX;
            y = nextY;
        } else {

            alignToGrid();
        }
    }

    private void alignToGrid() {

        int gridX = Math.round((float)x / SIZE) * SIZE;
        int gridY = Math.round((float)y / SIZE) * SIZE;

        if (canMove(gridX, gridY)) {
            x = gridX;
            y = gridY;
        }
    }

    private boolean canMove(int newX, int newY) {

        int leftTile = newX / SIZE;
        int rightTile = (newX + SIZE - 1) / SIZE;
        int topTile = newY / SIZE;
        int bottomTile = (newY + SIZE - 1) / SIZE;


        int buffer = 2;
        leftTile = (newX + buffer) / SIZE;
        rightTile = (newX + SIZE - 1 - buffer) / SIZE;
        topTile = (newY + buffer) / SIZE;
        bottomTile = (newY + SIZE - 1 - buffer) / SIZE;

        return !isWall(leftTile, topTile) &&
               !isWall(rightTile, topTile) &&
               !isWall(leftTile, bottomTile) &&
               !isWall(rightTile, bottomTile);
    }

    private boolean isWall(int tileX, int tileY) {
        return tileX >= 0 && tileX < maze[0].length &&
                tileY >= 0 && tileY < maze.length &&
                maze[tileY][tileX] == MazeGenerator.WALL;
    }

    public void setDirection(Direction direction) {
        this.nextDirection = direction;
    }

    public Direction getCurrentDirection() {
        return currentDirection;
    }

    public void setPosition(int x, int y) {
        this.x = x * SIZE;
        this.y = y * SIZE;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public void stop() {
        isMoving.set(false);
        animationThread.interrupt();
    }

    public void setSpeedMultiplier(double multiplier) {
        this.speedMultiplier = multiplier;
    }

    public void resetSpeed() {
        this.speedMultiplier = 1.0;
    }
}
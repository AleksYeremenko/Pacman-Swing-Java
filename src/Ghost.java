import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;

public class Ghost {
    private static final int SIZE = 30;
    private static final int BASE_SPEED = 4;
    private static final int PERSONALITY_CHANGE_TIME = 300;
    
    private int x;
    private int y;
    private final int startX;
    private final int startY;
    private final Color color;
    private final int[][] maze;
    private Direction currentDirection;
    private boolean isVulnerable = false;
    private boolean isFrozen = false;
    private final Random random = new Random();
    private final GhostType type;
    private final GameWindow gameWindow;

    private int personalityTimer = 0;
    private double speedMultiplier = 1.0;
    private int moodCounter = 0;
    private GhostMood currentMood;

    private enum GhostMood {
        AGGRESSIVE(1.2, 0.8),
        CAUTIOUS(0.8, 0.4),
        CHAOTIC(1.0, 1.0),
        SLEEPY(0.6, 0.2);
        
        final double speedMod;
        final double directionChangeProbability;
        
        GhostMood(double speedMod, double directionChangeProbability) {
            this.speedMod = speedMod;
            this.directionChangeProbability = directionChangeProbability;
        }
    }

    public enum GhostType {
        TACTIC,
        HUNTER,
        SCOUT,
        TRICKSTER
    }

    public Ghost(GameWindow gameWindow, int[][] maze, int startX, int startY, Color color, GhostType type) {
        this.gameWindow = gameWindow;
        this.maze = maze;
        this.x = startX * SIZE;
        this.y = startY * SIZE;
        this.startX = startX * SIZE;
        this.startY = startY * SIZE;
        this.color = color;
        this.type = type;
        this.currentDirection = Direction.values()[random.nextInt(Direction.values().length)];
        this.currentMood = GhostMood.values()[random.nextInt(GhostMood.values().length)];
        updatePersonality();
    }

    private void updatePersonality() {
        personalityTimer++;
        if (personalityTimer >= PERSONALITY_CHANGE_TIME) {
            personalityTimer = 0;
            currentMood = GhostMood.values()[random.nextInt(GhostMood.values().length)];
            speedMultiplier = currentMood.speedMod;

            speedMultiplier += (random.nextDouble() - 0.5) * 0.2;
        }
    }

    public void move() {
        if (isFrozen) return;
        
        updatePersonality();

        double actualSpeed = BASE_SPEED * speedMultiplier;
        if (isVulnerable) actualSpeed *= 0.7;
        
        int currentSpeed = (int) Math.round(actualSpeed);

        moodCounter++;

        boolean shouldChangeDirection = false;

        int nextX = x + (currentDirection.dx * currentSpeed);
        int nextY = y + (currentDirection.dy * currentSpeed);
        boolean canMoveForward = canMove(nextX, nextY);

        switch (type) {
            case TACTIC:
                shouldChangeDirection = !canMoveForward || 
                    (random.nextDouble() < currentMood.directionChangeProbability * 0.5 && moodCounter > 50);
                break;
            case HUNTER:
                shouldChangeDirection = !canMoveForward || 
                    (isNearPacman() && random.nextDouble() < currentMood.directionChangeProbability);
                break;
            case SCOUT:
                shouldChangeDirection = !canMoveForward || 
                    (random.nextDouble() < currentMood.directionChangeProbability * 0.7 && moodCounter > 30);
                break;
            case TRICKSTER:
                shouldChangeDirection = !canMoveForward || 
                    (random.nextDouble() < currentMood.directionChangeProbability * 0.3 && moodCounter > 70);
                break;
        }

        if (shouldChangeDirection) {
            moodCounter = 0;
            chooseNewDirection(currentSpeed);
        }

        nextX = x + (currentDirection.dx * currentSpeed);
        nextY = y + (currentDirection.dy * currentSpeed);
        
        if (canMove(nextX, nextY)) {
            x = nextX;
            y = nextY;
        }
    }

    private void chooseNewDirection(int currentSpeed) {
        List<DirectionScore> possibleDirections = new ArrayList<>();

        for (Direction dir : Direction.values()) {
            int newX = x + (dir.dx * currentSpeed);
            int newY = y + (dir.dy * currentSpeed);
            
            if (canMove(newX, newY)) {
                double score = evaluateDirection(dir, newX, newY);
                possibleDirections.add(new DirectionScore(dir, score));
            }
        }
        
        if (!possibleDirections.isEmpty()) {

            possibleDirections.sort((a, b) -> Double.compare(b.score, a.score));

            int maxIndex = Math.min(3, possibleDirections.size());
            currentDirection = possibleDirections.get(random.nextInt(maxIndex)).direction;
        }
    }

    private double evaluateDirection(Direction dir, int newX, int newY) {
        double score = 1.0;
        

        switch (currentMood) {
            case AGGRESSIVE:
                score *= isTowardsPacman(dir) ? 1.5 : 0.7;
                break;
            case CAUTIOUS:
                score *= isTowardsPacman(dir) ? 0.6 : 1.3;
                break;
            case CHAOTIC:
                score *= random.nextDouble() + 0.5;
                break;
            case SLEEPY:
                score *= (dir == currentDirection) ? 1.4 : 0.8;
                break;
        }

        int wallCount = countNearbyWalls(newX, newY);
        score *= (4 - wallCount) / 4.0;
        
        return score;
    }

    private boolean isTowardsPacman(Direction dir) {
        Pacman pacman = gameWindow.getPacman();
        int pacmanX = pacman.getX();
        int pacmanY = pacman.getY();
        
        return (dir.dx > 0 && pacmanX > x) ||
               (dir.dx < 0 && pacmanX < x) ||
               (dir.dy > 0 && pacmanY > y) ||
               (dir.dy < 0 && pacmanY < y);
    }

    private int countNearbyWalls(int newX, int newY) {
        int count = 0;
        int tileX = newX / SIZE;
        int tileY = newY / SIZE;
        
        for (int dy = -1; dy <= 1; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                if (dx == 0 && dy == 0) continue;
                if (isWall(tileX + dx, tileY + dy)) count++;
            }
        }
        return count;
    }

    private boolean isNearPacman() {
        Pacman pacman = gameWindow.getPacman();
        int dx = Math.abs(pacman.getX() - x);
        int dy = Math.abs(pacman.getY() - y);
        return dx < SIZE * 4 && dy < SIZE * 4;
    }

    private static class DirectionScore {
        Direction direction;
        double score;
        
        DirectionScore(Direction direction, double score) {
            this.direction = direction;
            this.score = score;
        }
    }

    public void draw(Graphics g) {
        g.setColor(isVulnerable ? Color.BLUE : color);
        g.fillOval(x, y, SIZE, SIZE);

        // Добавляем глаза
        g.setColor(isVulnerable ? Color.WHITE : Color.WHITE);
        g.fillOval(x + SIZE/4, y + SIZE/4, SIZE/4, SIZE/4);
        g.fillOval(x + SIZE/2, y + SIZE/4, SIZE/4, SIZE/4);

        // Зрачки
        if (!isVulnerable) {
            g.setColor(Color.BLACK);
            int pupilOffset = 2;
            switch (currentDirection) {
                case LEFT:
                    pupilOffset = -2;
                    break;
                case RIGHT:
                    pupilOffset = 2;
                    break;
                case UP:
                    pupilOffset = 0;
                    break;
                case DOWN:
                    pupilOffset = 0;
                    break;
            }
            g.fillOval(x + SIZE/4 + pupilOffset, y + SIZE/4, SIZE/8, SIZE/8);
            g.fillOval(x + SIZE/2 + pupilOffset, y + SIZE/4, SIZE/8, SIZE/8);
        }
    }

    private void moveScared() {
        Pacman pacman = gameWindow.getPacman();
        int pacmanX = pacman.getX();
        int pacmanY = pacman.getY();

        Direction awayDirection = getPathToTarget(
                x + (x - pacmanX),
                y + (y - pacmanY)
        );

        if (awayDirection != null && random.nextDouble() < 0.8) { // 80% шанс убегать
            int newX = x + (awayDirection.dx * (BASE_SPEED - 1));
            int newY = y + (awayDirection.dy * (BASE_SPEED - 1));

            if (canMove(newX, newY) && !willCollideWithOtherGhosts(newX, newY)) {
                currentDirection = awayDirection;
                x = newX;
                y = newY;
                return;
            }
        }

        Direction randomDir = findAlternativeDirection();
        if (randomDir != null) {
            currentDirection = randomDir;
            x += currentDirection.dx * (BASE_SPEED - 1);
            y += currentDirection.dy * (BASE_SPEED - 1);
        }
    }

    private Direction getNextDirection(int pacmanX, int pacmanY) {
        double distanceToPacman = getDistance(pacmanX, pacmanY);

        switch (type) {
            case TACTIC:
                return getPathToTarget(pacmanX, pacmanY);

            case HUNTER:
                Direction pacmanDir = gameWindow.getPacman().getCurrentDirection();
                int targetX = pacmanX + (pacmanDir.dx * 4 * SIZE);
                int targetY = pacmanY + (pacmanDir.dy * 4 * SIZE);
                return getPathToTarget(targetX, targetY);

            case SCOUT:
                if (distanceToPacman < 8 * SIZE) {
                    return getPathToTarget(pacmanX, pacmanY);
                } else {
                    return findAlternativeDirection();
                }

            case TRICKSTER:
                if (distanceToPacman < 6 * SIZE) {
                    // Убегаем в противоположном направлении
                    return getPathToTarget(
                            x - (pacmanX - x),
                            y - (pacmanY - y)
                    );
                } else if (distanceToPacman < 12 * SIZE) {
                    return getPathToTarget(pacmanX, pacmanY);
                }
                return findAlternativeDirection();

            default:
                return findAlternativeDirection();
        }
    }

    private Direction findAlternativeDirection() {

        List<Direction> directions = new ArrayList<>();
        for (Direction dir : Direction.values()) {
            directions.add(dir);
        }
        Collections.shuffle(directions, random);

        for (Direction dir : directions) {
            int newX = x + (dir.dx * BASE_SPEED);
            int newY = y + (dir.dy * BASE_SPEED);

            if (canMove(newX, newY) && !willCollideWithOtherGhosts(newX, newY)) {
                return dir;
            }
        }
        return null;
    }

    private Direction getPathToTarget(int targetX, int targetY) {
        List<Direction> possibleDirections = new ArrayList<>();
        double minDistance = Double.MAX_VALUE;
        Direction bestDirection = null;


        for (Direction dir : Direction.values()) {
            int newX = x + (dir.dx * BASE_SPEED);
            int newY = y + (dir.dy * BASE_SPEED);

            if (canMove(newX, newY) && !willCollideWithOtherGhosts(newX, newY)) {
                double distance = getDistance(newX, newY, targetX, targetY);
                if (distance < minDistance) {
                    minDistance = distance;
                    bestDirection = dir;
                }
                possibleDirections.add(dir);
            }
        }


        if (bestDirection != null) {
            return bestDirection;
        }


        return possibleDirections.isEmpty() ? null :
                possibleDirections.get(random.nextInt(possibleDirections.size()));
    }

    private boolean willCollideWithOtherGhosts(int newX, int newY) {
        Rectangle newBounds = new Rectangle(newX, newY, SIZE, SIZE);
        for (Ghost other : gameWindow.getGhosts()) {
            if (other != this) {
                Rectangle otherBounds = new Rectangle(other.getX(), other.getY(), SIZE, SIZE);
                if (newBounds.intersects(otherBounds)) {
                    return true;
                }
            }
        }
        return false;
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


        if (isWall(leftTile, topTile) || isWall(rightTile, topTile) ||
            isWall(leftTile, bottomTile) || isWall(rightTile, bottomTile)) {
            return false;
        }

        Rectangle newBounds = new Rectangle(newX, newY, SIZE, SIZE);
        for (Ghost other : gameWindow.getGhosts()) {
            if (other != this) {
                Rectangle otherBounds = new Rectangle(other.getX(), other.getY(), SIZE, SIZE);
                if (newBounds.intersects(otherBounds)) {
                    return false;
                }
            }
        }

        return true;
    }

    private boolean isWall(int tileX, int tileY) {
        return tileX >= 0 && tileX < maze[0].length &&
                tileY >= 0 && tileY < maze.length &&
                maze[tileY][tileX] == MazeGenerator.WALL;
    }

    private double getDistance(int x1, int y1, int x2, int y2) {
        return Math.sqrt(Math.pow(x2 - x1, 2) + Math.pow(y2 - y1, 2));
    }

    private double getDistance(int targetX, int targetY) {
        return getDistance(x, y, targetX, targetY);
    }

    public boolean intersects(Pacman pacman) {
        int dx = Math.abs((x + SIZE/2) - (pacman.getX() + SIZE/2));
        int dy = Math.abs((y + SIZE/2) - (pacman.getY() + SIZE/2));
        return dx < SIZE/2 && dy < SIZE/2;
    }

    public void setVulnerable(boolean vulnerable) {
        isVulnerable = vulnerable;
    }

    public void respawn() {
        x = startX;
        y = startY;
        isVulnerable = false;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public void freeze() {
        this.isFrozen = true;
    }

    public void unfreeze() {
        this.isFrozen = false;
    }
}
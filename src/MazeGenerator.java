import java.util.Random;
import java.util.Stack;

public class MazeGenerator {
    public static final int WALL = 1;
    public static final int PATH = 0;
    public static final int PELLET = 2;
    public static final int POWER_PELLET = 3;
    public static final int PACMAN_START = 4;
    public static final int GHOST_START = 5;

    private static final Random random = new Random();

    public static int[][] getMaze(String size) {
        int boardSize = switch (size) {
            case "Small (15x15)" -> 15;
            case "Mid (20x20)" -> 20;
            case "Big (25x25)" -> 25;
            default -> 15;
        };


        int[][] maze = generateMaze(boardSize);


        addGameElements(maze);

        return maze;
    }

    private static int[][] generateMaze(int size) {

        int[][] maze = new int[size][size];
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                maze[i][j] = WALL;
            }
        }


        Stack<Point> stack = new Stack<>();


        int startX = 1;
        int startY = 1;
        maze[startY][startX] = PATH;

        stack.push(new Point(startX, startY));

        while (!stack.empty()) {
            Point current = stack.peek();


            java.util.List<Direction> directions = getPossibleDirections(current, maze);

            if (!directions.isEmpty()) {

                Direction direction = directions.get(random.nextInt(directions.size()));


                Point next = new Point(
                        current.x + direction.dx * 2,
                        current.y + direction.dy * 2
                );


                maze[current.y + direction.dy][current.x + direction.dx] = PATH;
                maze[next.y][next.x] = PATH;

                stack.push(next);
            } else {
                stack.pop();
            }
        }

        return maze;
    }

    private static void addGameElements(int[][] maze) {
        int size = maze.length;


        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                if (maze[i][j] == PATH) {
                    maze[i][j] = PELLET;
                }
            }
        }


        if (maze[1][1] != WALL) maze[1][1] = POWER_PELLET;
        if (maze[1][size-2] != WALL) maze[1][size-2] = POWER_PELLET;
        if (maze[size-2][1] != WALL) maze[size-2][1] = POWER_PELLET;
        if (maze[size-2][size-2] != WALL) maze[size-2][size-2] = POWER_PELLET;


        Point pacmanStart = findRandomPath(maze);
        maze[pacmanStart.y][pacmanStart.x] = PACMAN_START;


        int centerX = size / 2;
        int centerY = size / 2;


        for (int i = centerY - 1; i <= centerY + 1; i++) {
            for (int j = centerX - 1; j <= centerX + 1; j++) {
                if (maze[i][j] != WALL) {
                    maze[i][j] = GHOST_START;
                }
            }
        }
    }

    private static Point findRandomPath(int[][] maze) {
        int size = maze.length;
        Point point;
        do {
            point = new Point(
                    random.nextInt(size - 2) + 1,
                    random.nextInt(size - 2) + 1
            );
        } while (maze[point.y][point.x] == WALL);
        return point;
    }

    private static java.util.List<Direction> getPossibleDirections(Point current, int[][] maze) {
        java.util.List<Direction> directions = new java.util.ArrayList<>();
        int size = maze.length;

        for (Direction dir : Direction.values()) {
            int newX = current.x + dir.dx * 2;
            int newY = current.y + dir.dy * 2;

            if (newX > 0 && newX < size - 1 &&
                    newY > 0 && newY < size - 1 &&
                    maze[newY][newX] == WALL &&
                    maze[current.y + dir.dy][current.x + dir.dx] == WALL) {
                directions.add(dir);
            }
        }

        return directions;
    }

    private static class Point {
        final int x;
        final int y;

        Point(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }

    private enum Direction {
        NORTH(0, -1),
        SOUTH(0, 1),
        EAST(1, 0),
        WEST(-1, 0);

        final int dx;
        final int dy;

        Direction(int dx, int dy) {
            this.dx = dx;
            this.dy = dy;
        }
    }
}
import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

public class GameWindow extends JFrame {
    private static final int CELL_SIZE = 30;
    private static final int GHOST_COUNT = 4;
    private static final int GAME_SPEED = 16;

    private static final int PELLET_POINTS = 10;
    private static final int POWER_PELLET_POINTS = 50;
    private static final int GHOST_POINTS = 200;

    private static final Color[] GHOST_COLORS = {
            Color.RED,    // BLINKY
            Color.PINK,   // PINKY
            Color.CYAN,   // INKY
            Color.ORANGE  // CLYDE
    };

    private final MainMenu mainMenu;
    private final JPanel gamePanel;
    private final JPanel statusPanel;
    private final JLabel scoreLabel;
    private final JLabel timeLabel;
    private final JLabel livesLabel;
    private final JLabel pelletsLeftLabel;

    private final int[][] maze;
    private final Pacman pacman;
    private final List<Ghost> ghosts;
    private final List<PowerUp> powerUps;

    private final AtomicBoolean isGameRunning;
    private boolean isPowerModeActive;
    private int score = 0;
    private int lives = 3;
    private int pelletsLeft = 0;
    private long gameStartTime;
    private Thread powerUpGeneratorThread;
    private Thread gameTimeThread;

    private Point lastEatenPellet;
    private int pelletAnimationFrame = -1;
    private static final int PELLET_ANIMATION_FRAMES = 5;

    public GameWindow(String selectedSize, MainMenu mainMenu) {
        this.mainMenu = mainMenu;
        this.isGameRunning = new AtomicBoolean(true);
        this.ghosts = new ArrayList<>();
        this.powerUps = new ArrayList<>();

        setTitle("Pacman Game");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setResizable(false);

        this.maze = MazeGenerator.getMaze(selectedSize);
        int boardSize = maze.length;

        gamePanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                drawGame(g);
            }
        };
        gamePanel.setPreferredSize(new Dimension(boardSize * CELL_SIZE, boardSize * CELL_SIZE));
        gamePanel.setBackground(ThemeManager.getCurrentTheme().backgroundColor);

        statusPanel = new JPanel();
        statusPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 20, 5));
        statusPanel.setBackground(ThemeManager.getCurrentTheme().backgroundColor);
        
        scoreLabel = new JLabel("Score: 0");
        timeLabel = new JLabel("Time: 0:00");
        livesLabel = new JLabel("Lives: " + lives);
        pelletsLeftLabel = new JLabel("Points left 0");

        scoreLabel.setForeground(ThemeManager.getCurrentTheme().textColor);
        timeLabel.setForeground(ThemeManager.getCurrentTheme().textColor);
        livesLabel.setForeground(ThemeManager.getCurrentTheme().textColor);
        pelletsLeftLabel.setForeground(ThemeManager.getCurrentTheme().textColor);

        statusPanel.add(scoreLabel);
        statusPanel.add(timeLabel);
        statusPanel.add(livesLabel);
        statusPanel.add(pelletsLeftLabel);

        setLayout(new BorderLayout());
        add(statusPanel, BorderLayout.NORTH);
        add(gamePanel, BorderLayout.CENTER);

        int[] pacmanStart = findPacmanStart();
        pacman = new Pacman(this, maze, pacmanStart[0], pacmanStart[1]);
        initializeGhosts();


        setupKeyBindings();

        countPellets();

        startGameThreads();

        SoundManager.playStartGame();

        pack();
        setLocationRelativeTo(null);
    }

    private void startGameThreads() {
        gameStartTime = System.currentTimeMillis();

        gameTimeThread = new Thread(() -> {
            while (isGameRunning.get()) {
                try {
                    updateGameTime();
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
        gameTimeThread.start();

        powerUpGeneratorThread = new Thread(() -> {
            while (isGameRunning.get()) {
                try {
                    Thread.sleep(5000);
                    if (new Random().nextDouble() < 0.25) {
                        generatePowerUp();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
        powerUpGeneratorThread.start();

        startGameLoop();
    }

    private void startGameLoop() {
        Thread gameLoop = new Thread(() -> {
            while (isGameRunning.get()) {
                try {
                    pacman.move();
                    moveGhosts();
                    checkCollisions();
                    repaintGame();
                    Thread.sleep(GAME_SPEED);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
        gameLoop.start();
    }

    private void setupKeyBindings() {
        InputMap inputMap = gamePanel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap actionMap = gamePanel.getActionMap();

        inputMap.put(KeyStroke.getKeyStroke("UP"), "move.up");
        inputMap.put(KeyStroke.getKeyStroke("DOWN"), "move.down");
        inputMap.put(KeyStroke.getKeyStroke("LEFT"), "move.left");
        inputMap.put(KeyStroke.getKeyStroke("RIGHT"), "move.right");

        inputMap.put(KeyStroke.getKeyStroke("W"), "move.up");
        inputMap.put(KeyStroke.getKeyStroke("S"), "move.down");
        inputMap.put(KeyStroke.getKeyStroke("A"), "move.left");
        inputMap.put(KeyStroke.getKeyStroke("D"), "move.right");

        inputMap.put(KeyStroke.getKeyStroke("ESCAPE"), "menu");

        actionMap.put("move.up", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                pacman.setDirection(Direction.UP);
            }
        });

        actionMap.put("move.down", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                pacman.setDirection(Direction.DOWN);
            }
        });

        actionMap.put("move.left", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                pacman.setDirection(Direction.LEFT);
            }
        });

        actionMap.put("move.right", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                pacman.setDirection(Direction.RIGHT);
            }
        });

        actionMap.put("menu", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                returnToMenu();
            }
        });

        gamePanel.setFocusable(true);
        gamePanel.requestFocusInWindow();
    }

    private void drawGame(Graphics g) {
        ThemeManager.Theme currentTheme = ThemeManager.getCurrentTheme();
        
        for (int row = 0; row < maze.length; row++) {
            for (int col = 0; col < maze[row].length; col++) {
                int x = col * CELL_SIZE;
                int y = row * CELL_SIZE;

                switch (maze[row][col]) {
                    case MazeGenerator.WALL:
                        g.setColor(currentTheme.wallColor);
                        g.fillRect(x, y, CELL_SIZE, CELL_SIZE);
                        break;
                    case MazeGenerator.PELLET:
                        g.setColor(currentTheme.pelletColor);
                        g.fillOval(x + CELL_SIZE/3, y + CELL_SIZE/3, CELL_SIZE/3, CELL_SIZE/3);
                        break;
                    case MazeGenerator.POWER_PELLET:
                        g.setColor(currentTheme.pelletColor);
                        g.fillOval(x + CELL_SIZE/4, y + CELL_SIZE/4, CELL_SIZE/2, CELL_SIZE/2);
                        break;
                }
            }
        }

        for (PowerUp powerUp : powerUps) {
            powerUp.draw(g);
        }

        for (Ghost ghost : ghosts) {
            ghost.draw(g);
        }

        pacman.draw(g);

        if (lastEatenPellet != null && pelletAnimationFrame >= 0) {
            int x = lastEatenPellet.x * CELL_SIZE;
            int y = lastEatenPellet.y * CELL_SIZE;
            
            g.setColor(currentTheme.pelletColor);
            int size = (PELLET_ANIMATION_FRAMES - pelletAnimationFrame) * 2;
            g.fillOval(x + CELL_SIZE/2 - size/2, 
                      y + CELL_SIZE/2 - size/2, 
                      size, size);
        }
    }

    public List<Ghost> getGhosts() {
        return ghosts;
    }
    private void checkCollisions() {
        int pacmanTileX = pacman.getX() / CELL_SIZE;
        int pacmanTileY = pacman.getY() / CELL_SIZE;

        if (maze[pacmanTileY][pacmanTileX] == MazeGenerator.PELLET) {
            eatPellet(pacmanTileX, pacmanTileY);
        } else if (maze[pacmanTileY][pacmanTileX] == MazeGenerator.POWER_PELLET) {
            eatPowerPellet(pacmanTileX, pacmanTileY);
        }

        for (Ghost ghost : ghosts) {
            if (ghost.intersects(pacman)) {
                if (isPowerModeActive) {
                    updateScore(GHOST_POINTS);
                    ghost.respawn();
                    SoundManager.playEatGhost();
                } else {
                    handlePacmanDeath();
                }
            }
        }

        powerUps.removeIf(powerUp -> {
            if (powerUp.isActive() && powerUp.intersects(pacman)) {
                powerUp.apply(this);
                SoundManager.playPowerUp();
                return true;
            }
            return false;
        });
    }

    private void eatPellet(int x, int y) {
        maze[y][x] = MazeGenerator.PATH;
        updateScore(PELLET_POINTS);
        pelletsLeft--;
        updatePelletsLeftLabel();

        SoundManager.playEatPellet();

        lastEatenPellet = new Point(x, y);
        pelletAnimationFrame = 0;
        
        new Thread(() -> {
            try {
                while (pelletAnimationFrame < PELLET_ANIMATION_FRAMES) {
                    Thread.sleep(50);
                    pelletAnimationFrame++;
                    repaint(x * CELL_SIZE - 5, y * CELL_SIZE - 5, 
                           CELL_SIZE + 10, CELL_SIZE + 10);
                }
                pelletAnimationFrame = -1;
                lastEatenPellet = null;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();

        checkWinCondition();
    }

    private void eatPowerPellet(int x, int y) {
        maze[y][x] = MazeGenerator.PATH;
        updateScore(POWER_PELLET_POINTS);
        activatePowerMode();
        pelletsLeft--;
        updatePelletsLeftLabel();

        lastEatenPellet = new Point(x, y);
        pelletAnimationFrame = 0;
        
        new Thread(() -> {
            try {
                while (pelletAnimationFrame < PELLET_ANIMATION_FRAMES) {
                    Thread.sleep(70);
                    pelletAnimationFrame++;
                    repaint(x * CELL_SIZE - 10, y * CELL_SIZE - 10, 
                           CELL_SIZE + 20, CELL_SIZE + 20);
                }
                pelletAnimationFrame = -1;
                lastEatenPellet = null;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();

        checkWinCondition();
    }

    private void handlePacmanDeath() {
        lives--;
        livesLabel.setText("Lives: " + lives);

        SoundManager.playDeath();

        if (lives <= 0) {
            gameOver(false);
        } else {
            respawnPacman();
            respawnGhosts();
        }
    }

    private void gameOver(boolean won) {
        isGameRunning.set(false);
        String message;
        if (won) {
            message = String.format("Congratulations! You Won!\n" +
                                  "Score %d\n" +
                                  "Time: %s\n" +
                                  "Enter your name:",
                                  score, timeLabel.getText().substring(6));
        } else {
            message = String.format("Game over!\n" +
                            "Score %d\n" +
                            "Time: %s\n" +
                            "Enter your name:",
                    score, timeLabel.getText().substring(6));
        }

        SwingUtilities.invokeLater(() -> {
            String name = JOptionPane.showInputDialog(this, message);
            if (name != null && !name.trim().isEmpty()) {
                HighScore.addScore(name, score);
            }
            returnToMenu();
        });
    }

    private void returnToMenu() {
        isGameRunning.set(false);
        dispose();
        mainMenu.setVisible(true);
    }

    private void updateGameTime() {
        long currentTime = System.currentTimeMillis() - gameStartTime;
        long seconds = currentTime / 1000;
        long minutes = seconds / 60;
        seconds = seconds % 60;
        timeLabel.setText(String.format("Time: %d:%02d", minutes, seconds));
    }

    private void generatePowerUp() {
        Random random = new Random();
        int x, y;
        do {
            x = random.nextInt(maze.length);
            y = random.nextInt(maze[0].length);
        } while (maze[y][x] != MazeGenerator.PATH);

        PowerUp powerUp = new PowerUp(x * CELL_SIZE, y * CELL_SIZE,
                PowerUp.PowerUpType.values()[random.nextInt(PowerUp.PowerUpType.values().length)]);
        powerUps.add(powerUp);
    }

    private void countPellets() {
        pelletsLeft = 0;
        for (int[] row : maze) {
            for (int cell : row) {
                if (cell == MazeGenerator.PELLET || cell == MazeGenerator.POWER_PELLET) {
                    pelletsLeft++;
                }
            }
        }
        updatePelletsLeftLabel();
    }

    private void updatePelletsLeftLabel() {
        pelletsLeftLabel.setText("Points left: " + pelletsLeft);
    }

    private int[] findPacmanStart() {
        for (int i = 0; i < maze.length; i++) {
            for (int j = 0; j < maze[i].length; j++) {
                if (maze[i][j] == MazeGenerator.PACMAN_START) {
                    return new int[]{j, i};
                }
            }
        }
        return new int[]{1, 1};
    }

    private void initializeGhosts() {
        int ghostIndex = 0;
        for (int i = 0; i < maze.length && ghostIndex < GHOST_COUNT; i++) {
            for (int j = 0; j < maze[i].length && ghostIndex < GHOST_COUNT; j++) {
                if (maze[i][j] == MazeGenerator.GHOST_START) {
                    ghosts.add(new Ghost(this, maze, j, i, GHOST_COLORS[ghostIndex],
                            Ghost.GhostType.values()[ghostIndex]));
                    ghostIndex++;
                }
            }
        }
    }

    private void moveGhosts() {
        for (Ghost ghost : ghosts) {
            ghost.move();
        }
    }

    private void respawnGhosts() {
        for (Ghost ghost : ghosts) {
            ghost.respawn();
        }
    }

    private void respawnPacman() {
        int[] start = findPacmanStart();
        pacman.setPosition(start[0], start[1]);
    }

    public void repaintGame() {
        gamePanel.repaint();
    }

    public void updateScore(int points) {
        score += points;
        scoreLabel.setText("Score: " + score);
    }

    public void activatePowerMode() {
        isPowerModeActive = true;
        for (Ghost ghost : ghosts) {
            ghost.setVulnerable(true);
        }

        new Thread(() -> {
            try {
                Thread.sleep(10000); // 10 секунд
                isPowerModeActive = false;
                for (Ghost ghost : ghosts) {
                    ghost.setVulnerable(false);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }

    public Pacman getPacman() {
        return pacman;
    }

    public int[][] getMaze() {
        return maze;
    }

    public boolean isGameRunning() {
        return isGameRunning.get();
    }

    private void checkWinCondition() {
        if (pelletsLeft <= 0) {
            gameOver(true);
        }
    }
} 
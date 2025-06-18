import javax.swing.*;
import javax.swing.plaf.basic.BasicScrollBarUI;
import javax.swing.plaf.basic.BasicComboBoxUI;
import javax.swing.plaf.basic.BasicComboBoxRenderer;
import java.awt.*;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class MainMenu extends JFrame {
    private static final Color PACMAN_YELLOW = Color.YELLOW;
    private static final Color BACKGROUND_COLOR = Color.BLACK;
    private static final Color BUTTON_COLOR = new Color(33, 33, 222);
    private static final Color BUTTON_TEXT_COLOR = new Color(255, 255, 255);

    private Font titleFont;
    private Font buttonFont;
    private Font subtitleFont;
    private Font menuFont;

    private JPanel mainPanel;
    private JButton newGameButton;
    private JButton highScoresButton;
    private JButton exitButton;
    private JComboBox<String> sizeSelector;
    private JComboBox<ThemeManager.Theme> themeSelector;

    public MainMenu() {
        setTitle("Pacman Game");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);

        loadFonts();
        initializeUI();

        setPreferredSize(new Dimension(600, 700));
        pack();
        setLocationRelativeTo(null);
    }

    private void loadFonts() {
        try {
            String path = "resources/PressStart2P-Regular.ttf";
            File fontFile = new File(path);
            if (!fontFile.exists()) {
                System.err.println("File font didn`t find throw the path " + fontFile.getAbsolutePath());
                throw new IOException("File font didn`t find");
            }

            Font customFont = Font.createFont(Font.TRUETYPE_FONT, fontFile);

            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            ge.registerFont(customFont);

            titleFont = customFont.deriveFont(Font.BOLD, 48f);
            buttonFont = customFont.deriveFont(Font.PLAIN, 16f);
            subtitleFont = customFont.deriveFont(Font.PLAIN, 14f);
            menuFont = customFont.deriveFont(Font.PLAIN, 14f);

        } catch (Exception e) {
            System.err.println("error to load font: " + e.getMessage());
            System.err.println("Current directory: " + new File(".").getAbsolutePath());
            e.printStackTrace();
            titleFont = new Font("Arial", Font.BOLD, 48);
            buttonFont = new Font("Arial", Font.BOLD, 18);
            subtitleFont = new Font("Arial", Font.PLAIN, 14);
            menuFont = new Font("Arial", Font.PLAIN, 14);
        }
    }

    private void styleComboBox(JComboBox<?> comboBox) {
        comboBox.setFont(menuFont);
        comboBox.setForeground(ThemeManager.getCurrentTheme().textColor);
        comboBox.setBackground(ThemeManager.getCurrentTheme().backgroundColor);
        comboBox.setPreferredSize(new Dimension(300, 40));
        

        comboBox.setRenderer(new BasicComboBoxRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value,
                    int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                
                if (isSelected) {
                    setBackground(ThemeManager.getCurrentTheme().textColor);
                    setForeground(ThemeManager.getCurrentTheme().backgroundColor);
                } else {
                    setBackground(ThemeManager.getCurrentTheme().backgroundColor);
                    setForeground(ThemeManager.getCurrentTheme().textColor);
                }
                
                setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
                return this;
            }
        });


        comboBox.setUI(new BasicComboBoxUI() {
            @Override
            protected void installDefaults() {
                super.installDefaults();
                comboBox.setBorder(BorderFactory.createLineBorder(
                    ThemeManager.getCurrentTheme().textColor, 1));
            }
        });
    }

    private void initializeUI() {
        mainPanel = new JPanel(new GridBagLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                drawPacmanAnimation(g);
            }
        };
        mainPanel.setBackground(ThemeManager.getCurrentTheme().backgroundColor);
        mainPanel.setPreferredSize(new Dimension(500, 600));
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(20, 50, 20, 50);


        JLabel titleLabel = new JLabel("PACMAN", SwingConstants.CENTER);
        titleLabel.setFont(titleFont);
        titleLabel.setForeground(ThemeManager.getCurrentTheme().textColor);
        gbc.gridx = 0;
        gbc.gridy = 0;
        mainPanel.add(titleLabel, gbc);


        sizeSelector = new JComboBox<>(new String[]{
                "Small (15x15)",
                "Mid (20x20)",
                "Big (25x25)"
        });
        styleComboBox(sizeSelector);
        gbc.gridy = 1;
        mainPanel.add(sizeSelector, gbc);

        themeSelector = new JComboBox<>(ThemeManager.Theme.values());
        styleComboBox(themeSelector);
        themeSelector.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, 
                    int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof ThemeManager.Theme) {
                    setText(((ThemeManager.Theme) value).displayName);
                }
                setBackground(isSelected ? 
                    ThemeManager.getCurrentTheme().textColor : 
                    ThemeManager.getCurrentTheme().backgroundColor);
                setForeground(isSelected ? 
                    ThemeManager.getCurrentTheme().backgroundColor : 
                    ThemeManager.getCurrentTheme().textColor);
                return this;
            }
        });
        themeSelector.addActionListener(e -> {
            ThemeManager.Theme selectedTheme = (ThemeManager.Theme) themeSelector.getSelectedItem();
            ThemeManager.setCurrentTheme(selectedTheme);
            updateColors();
            styleComboBox(sizeSelector);
            styleComboBox(themeSelector);
        });
        gbc.gridy = 2;
        mainPanel.add(themeSelector, gbc);

        addButton("New Game", e -> startNewGame(), gbc, 3);
        addButton("High Score", e -> showHighScores(), gbc, 4);
        addButton("Exit", e -> System.exit(0), gbc, 5);

        setContentPane(mainPanel);
    }

    private void updateColors() {
        ThemeManager.Theme currentTheme = ThemeManager.getCurrentTheme();
        mainPanel.setBackground(currentTheme.backgroundColor);
        for (Component comp : mainPanel.getComponents()) {
            if (comp instanceof JLabel) {
                comp.setForeground(currentTheme.textColor);
            } else if (comp instanceof JButton) {
                comp.setBackground(currentTheme.backgroundColor);
                comp.setForeground(currentTheme.textColor);
            }
        }
        mainPanel.repaint();
    }

    private void addButton(String text, ActionListener listener, GridBagConstraints gbc, int gridy) {
        JButton button = new JButton(text);
        button.setFont(menuFont);
        button.setForeground(ThemeManager.getCurrentTheme().textColor);
        button.setBackground(ThemeManager.getCurrentTheme().backgroundColor);
        button.setFocusPainted(false);
        button.setBorderPainted(true);
        button.addActionListener(listener);
        button.setPreferredSize(new Dimension(300, 50));
        gbc.gridy = gridy;
        mainPanel.add(button, gbc);
    }

    private void setupButtonListeners() {
        newGameButton.addActionListener(e -> showNewGameDialog());
        highScoresButton.addActionListener(e -> showHighScores());
        exitButton.addActionListener(e -> System.exit(0));
    }

    private void showNewGameDialog() {
        UIManager.put("OptionPane.background", BACKGROUND_COLOR);
        UIManager.put("Panel.background", BACKGROUND_COLOR);

        String[] boardSizes = {"Small (15x15)", "Mid (20x20)", "Big (25x25)"};
        JComboBox<String> sizeComboBox = new JComboBox<>(boardSizes);
        sizeComboBox.setBackground(BUTTON_COLOR);
        sizeComboBox.setForeground(BUTTON_TEXT_COLOR);

        int result = JOptionPane.showOptionDialog(
                this,
                sizeComboBox,
                "Choose size of game",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                null,
                null
        );

        if (result == JOptionPane.OK_OPTION) {
            startGame((String) sizeComboBox.getSelectedItem());
        }
    }

    private void showHighScores() {
        JFrame highScoresFrame = new JFrame("High Scores");
        highScoresFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(BACKGROUND_COLOR);
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        

        JLabel titleLabel = new JLabel("HIGH SCORES");
        titleLabel.setFont(titleFont.deriveFont(24f));
        titleLabel.setForeground(PACMAN_YELLOW);
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);

        DefaultListModel<String> listModel = new DefaultListModel<>();
        List<HighScore.Score> scores = HighScore.getScores();
        for (HighScore.Score score : scores) {
            listModel.addElement(score.toString());
        }
        
        JList<String> scoresList = new JList<>(listModel);
        scoresList.setFont(buttonFont);
        scoresList.setForeground(BUTTON_TEXT_COLOR);
        scoresList.setBackground(BACKGROUND_COLOR);
        scoresList.setSelectionBackground(BUTTON_COLOR);
        scoresList.setSelectionForeground(BUTTON_TEXT_COLOR);

        JScrollPane scrollPane = new JScrollPane(scoresList);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        scrollPane.getVerticalScrollBar().setUI(new BasicScrollBarUI() {
            @Override
            protected void configureScrollBarColors() {
                this.thumbColor = BUTTON_COLOR;
                this.trackColor = BACKGROUND_COLOR;
            }

            @Override
            protected JButton createDecreaseButton(int orientation) {
                return createZeroButton();
            }

            @Override
            protected JButton createIncreaseButton(int orientation) {
                return createZeroButton();
            }

            private JButton createZeroButton() {
                JButton button = new JButton();
                button.setPreferredSize(new Dimension(0, 0));
                button.setMinimumSize(new Dimension(0, 0));
                button.setMaximumSize(new Dimension(0, 0));
                return button;
            }
        });
        scrollPane.setBorder(BorderFactory.createLineBorder(BUTTON_COLOR));

        JButton closeButton = createStyledButton("Close");
        closeButton.addActionListener(e -> highScoresFrame.dispose());

        mainPanel.add(titleLabel, BorderLayout.NORTH);
        mainPanel.add(scrollPane, BorderLayout.CENTER);
        mainPanel.add(closeButton, BorderLayout.SOUTH);
        
        highScoresFrame.add(mainPanel);
        highScoresFrame.setSize(400, 500);
        highScoresFrame.setLocationRelativeTo(this);
        highScoresFrame.setVisible(true);
    }


    private void startGame(String boardSize) {
        this.setVisible(false);


        SwingUtilities.invokeLater(() -> {
            GameWindow gameWindow = new GameWindow(boardSize, this);
            gameWindow.setVisible(true);
        });
    }

    private void startNewGame() {
        String selectedSize = (String) sizeSelector.getSelectedItem();
        if (selectedSize != null) {

            setVisible(false);

            SwingUtilities.invokeLater(() -> {
                GameWindow gameWindow = new GameWindow(selectedSize, this);
                gameWindow.setVisible(true);
            });
        }
    }

    private JButton createStyledButton(String text) {
        JButton button = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                if (getModel().isPressed()) {
                    g2.setColor(BUTTON_COLOR.darker());
                } else if (getModel().isRollover()) {
                    g2.setColor(BUTTON_COLOR.brighter());
                } else {
                    g2.setColor(BUTTON_COLOR);
                }

                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.dispose();

                super.paintComponent(g);
            }
        };

        button.setFont(buttonFont);
        button.setForeground(BUTTON_TEXT_COLOR);
        button.setAlignmentX(Component.CENTER_ALIGNMENT);
        button.setMaximumSize(new Dimension(400, 50));
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setContentAreaFilled(false);
        button.setOpaque(false);

        return button;
    }

    private void drawPacmanAnimation(Graphics g) {
        // Implementation of drawPacmanAnimation method
    }
}
import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import javax.swing.*;
import java.util.List;

public class HighScore {
    private static final String SCORES_FILE = "highscores.dat";
    private static List<Score> scores = new ArrayList<>();

    static {
        loadScores();
    }

    public static void addScore(String name, int score) {
        scores.add(new Score(name, score));
        Collections.sort(scores);

        if (scores.size() > 10) {
            scores = scores.subList(0, 10);
        }
        
        try {
            saveScores();
        } catch (IOException e) {
            showError("Error to save record", e);
        }
    }

    public static List<Score> getScores() {
        return new ArrayList<>(scores);
    }

    private static void loadScores() {
        File file = new File(SCORES_FILE);
        if (!file.exists()) {
            return;
        }

        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
            scores = (List<Score>) ois.readObject();
        } catch (IOException e) {
            showError("error to load record", e);
            scores = new ArrayList<>();
        } catch (ClassNotFoundException e) {
            showError("error format record", e);
            scores = new ArrayList<>();
        }
    }

    private static void saveScores() throws IOException {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(SCORES_FILE))) {
            oos.writeObject(scores);
        }
    }

    private static void showError(String message, Exception e) {
        SwingUtilities.invokeLater(() -> {
            JOptionPane.showMessageDialog(
                null,
                message + "\n" + e.getMessage(),
                "Ошибка",
                JOptionPane.ERROR_MESSAGE
            );
        });
    }

    public static class Score implements Serializable, Comparable<Score> {
        private static final long serialVersionUID = 1L;
        private final String name;
        private final int score;

        public Score(String name, int score) {
            this.name = name;
            this.score = score;
        }

        @Override
        public int compareTo(Score other) {
            return Integer.compare(other.score, this.score);
        }

        @Override
        public String toString() {
            return name + ": " + score;
        }

        public String getName() {
            return name;
        }

        public int getScore() {
            return score;
        }
    }
}
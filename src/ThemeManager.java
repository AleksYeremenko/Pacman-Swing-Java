import java.awt.*;

public class ThemeManager {
    public enum Theme {
        CLASSIC(new Color(0, 0, 0), // фон
                new Color(255, 255, 0), // текст
                new Color(33, 33, 222), // стены
                new Color(255, 255, 255), // точки
                "Classical"),
        
        DARK(new Color(20, 20, 20),
             new Color(0, 255, 0),
             new Color(70, 70, 70),
             new Color(200, 200, 200),
             "Dark"),
        
        LIGHT(new Color(240, 240, 240),
              new Color(0, 0, 255),
              new Color(180, 180, 180),
              new Color(50, 50, 50),
              "Luminous"),
        
        NEON(new Color(25, 0, 50),
             new Color(255, 0, 255),
             new Color(0, 255, 255),
             new Color(255, 255, 0),
             "Neon");

        public final Color backgroundColor;
        public final Color textColor;
        public final Color wallColor;
        public final Color pelletColor;
        public final String displayName;

        Theme(Color backgroundColor, Color textColor, Color wallColor, Color pelletColor, String displayName) {
            this.backgroundColor = backgroundColor;
            this.textColor = textColor;
            this.wallColor = wallColor;
            this.pelletColor = pelletColor;
            this.displayName = displayName;
        }
    }

    private static Theme currentTheme = Theme.CLASSIC;

    public static Theme getCurrentTheme() {
        return currentTheme;
    }

    public static void setCurrentTheme(Theme theme) {
        currentTheme = theme;
    }
} 
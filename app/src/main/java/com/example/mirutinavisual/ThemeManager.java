package com.example.mirutinavisual;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;

public class ThemeManager {
    
    private static final String PREFS_NAME = "theme_preferences";
    private static final String KEY_PRIMARY_COLOR = "primary_color";
    private static final String KEY_ACCENT_COLOR = "accent_color";
    private static final String KEY_FONT_SIZE = "font_size";
    private static final String KEY_HIGH_CONTRAST = "high_contrast";
    private static final String KEY_LARGE_ICONS = "large_icons";
    
    // Colores predefinidos
    public static final int[] THEME_COLORS = {
        Color.parseColor("#2196F3"), // Azul
        Color.parseColor("#4CAF50"), // Verde
        Color.parseColor("#FF9800"), // Naranja
        Color.parseColor("#9C27B0"), // Morado
        Color.parseColor("#E91E63"), // Rosa
        Color.parseColor("#607D8B"), // Gris azulado
        Color.parseColor("#FF5722"), // Rojo naranja
        Color.parseColor("#795548")  // Marrón
    };
    
    public static final String[] THEME_NAMES = {
        "Azul Océano",
        "Verde Naturaleza", 
        "Naranja Energía",
        "Morado Creatividad",
        "Rosa Diversión",
        "Gris Tranquilo",
        "Rojo Pasión",
        "Marrón Tierra"
    };
    
    private SharedPreferences prefs;
    private Context context;
    
    public ThemeManager(Context context) {
        this.context = context;
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
    
    // Métodos para colores
    public void setPrimaryColor(int color) {
        prefs.edit().putInt(KEY_PRIMARY_COLOR, color).apply();
    }
    
    public int getPrimaryColor() {
        return prefs.getInt(KEY_PRIMARY_COLOR, THEME_COLORS[0]); // Azul por defecto
    }
    
    public void setAccentColor(int color) {
        prefs.edit().putInt(KEY_ACCENT_COLOR, color).apply();
    }
    
    public int getAccentColor() {
        return prefs.getInt(KEY_ACCENT_COLOR, Color.parseColor("#FF6B6B"));
    }
    
    // Métodos para accesibilidad
    public void setFontSize(float size) {
        prefs.edit().putFloat(KEY_FONT_SIZE, size).apply();
    }
    
    public float getFontSize() {
        return prefs.getFloat(KEY_FONT_SIZE, 1.0f); // Tamaño normal por defecto
    }
    
    public void setHighContrast(boolean enabled) {
        prefs.edit().putBoolean(KEY_HIGH_CONTRAST, enabled).apply();
    }
    
    public boolean isHighContrastEnabled() {
        return prefs.getBoolean(KEY_HIGH_CONTRAST, false);
    }
    
    public void setLargeIcons(boolean enabled) {
        prefs.edit().putBoolean(KEY_LARGE_ICONS, enabled).apply();
    }
    
    public boolean isLargeIconsEnabled() {
        return prefs.getBoolean(KEY_LARGE_ICONS, false);
    }
    
    // Método para aplicar tema a una vista
    public void applyTheme(android.view.View view) {
        // Aplicar color primario como fondo si es un botón o card
        if (view instanceof android.widget.Button) {
            view.setBackgroundColor(getPrimaryColor());
        } else if (view instanceof androidx.cardview.widget.CardView) {
            ((androidx.cardview.widget.CardView) view).setCardBackgroundColor(getPrimaryColor());
        }
    }
    
    // Método para obtener color con contraste automático
    public int getContrastColor(int backgroundColor) {
        // Calcular luminancia del color de fondo
        double luminance = (0.299 * Color.red(backgroundColor) + 
                           0.587 * Color.green(backgroundColor) + 
                           0.114 * Color.blue(backgroundColor)) / 255;
        
        // Retornar blanco o negro según la luminancia
        return luminance > 0.5 ? Color.BLACK : Color.WHITE;
    }
    
    // Método para obtener color más claro
    public int getLighterColor(int color, float factor) {
        int red = (int) ((Color.red(color) * (1 - factor) / 255 + factor) * 255);
        int green = (int) ((Color.green(color) * (1 - factor) / 255 + factor) * 255);
        int blue = (int) ((Color.blue(color) * (1 - factor) / 255 + factor) * 255);
        return Color.argb(Color.alpha(color), red, green, blue);
    }
    
    // Método para obtener color más oscuro
    public int getDarkerColor(int color, float factor) {
        int red = (int) (Color.red(color) * (1 - factor));
        int green = (int) (Color.green(color) * (1 - factor));
        int blue = (int) (Color.blue(color) * (1 - factor));
        return Color.argb(Color.alpha(color), red, green, blue);
    }
    
    // Método para resetear tema a valores por defecto
    public void resetToDefault() {
        prefs.edit().clear().apply();
    }
}

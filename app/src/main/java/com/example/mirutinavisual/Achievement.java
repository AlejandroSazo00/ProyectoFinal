package com.example.mirutinavisual;

public class Achievement {
    private String id;
    private String name;
    private String description;
    private String iconUrl;
    private String category; // "daily", "streak", "special"
    private int requiredValue; // cantidad necesaria para desbloquear
    private boolean unlocked;
    private long unlockedDate;
    private String pictogramId; // ID del pictograma de recompensa

    public Achievement() {
        // Constructor vacío requerido por Firebase
    }

    public Achievement(String id, String name, String description, String iconUrl, 
                      String category, int requiredValue, String pictogramId) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.iconUrl = iconUrl;
        this.category = category;
        this.requiredValue = requiredValue;
        this.pictogramId = pictogramId;
        this.unlocked = false;
        this.unlockedDate = 0;
    }

    // Getters y Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getIconUrl() { return iconUrl; }
    public void setIconUrl(String iconUrl) { this.iconUrl = iconUrl; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public int getRequiredValue() { return requiredValue; }
    public void setRequiredValue(int requiredValue) { this.requiredValue = requiredValue; }

    public boolean isUnlocked() { return unlocked; }
    public void setUnlocked(boolean unlocked) { this.unlocked = unlocked; }

    public long getUnlockedDate() { return unlockedDate; }
    public void setUnlockedDate(long unlockedDate) { this.unlockedDate = unlockedDate; }

    public String getPictogramId() { return pictogramId; }
    public void setPictogramId(String pictogramId) { this.pictogramId = pictogramId; }
}

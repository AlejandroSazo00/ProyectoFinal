package com.example.mirutinavisual;

public class Activity {
    private String id;
    private String name;
    private String time;
    private int pictogramId;
    private String pictogramKeyword;
    private boolean completed;
    private long createdAt;
    private String userId;

    public Activity() {
        // Constructor vacío requerido para Firebase
    }

    public Activity(String name, String time, int pictogramId, String pictogramKeyword, String userId) {
        this.name = name;
        this.time = time;
        this.pictogramId = pictogramId;
        this.pictogramKeyword = pictogramKeyword;
        this.userId = userId;
        this.completed = false;
        this.createdAt = System.currentTimeMillis();
    }

    // Getters y Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public int getPictogramId() {
        return pictogramId;
    }

    public void setPictogramId(int pictogramId) {
        this.pictogramId = pictogramId;
    }

    public String getPictogramKeyword() {
        return pictogramKeyword;
    }

    public void setPictogramKeyword(String pictogramKeyword) {
        this.pictogramKeyword = pictogramKeyword;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getPictogramUrl() {
        return "https://api.arasaac.org/api/pictograms/" + pictogramId + "?download=false";
    }

    @Override
    public String toString() {
        return "Activity{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", time='" + time + '\'' +
                ", pictogramId=" + pictogramId +
                ", completed=" + completed +
                '}';
    }
}

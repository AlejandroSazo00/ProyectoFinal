package com.example.mirutinavisual;

public class SequenceStep {
    private String id;
    private String name;
    private String description;
    private int pictogramId;
    private String pictogramKeyword;
    private int stepNumber;
    private boolean completed;
    private String audioText;
    
    public SequenceStep() {
        // Constructor vacío requerido por Firebase
    }
    
    public SequenceStep(String id, String name, String description, int pictogramId, 
                       String pictogramKeyword, int stepNumber) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.pictogramId = pictogramId;
        this.pictogramKeyword = pictogramKeyword;
        this.stepNumber = stepNumber;
        this.completed = false;
        this.audioText = name + ". " + description;
    }
    
    // Getters y Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public int getPictogramId() { return pictogramId; }
    public void setPictogramId(int pictogramId) { this.pictogramId = pictogramId; }
    
    public String getPictogramKeyword() { return pictogramKeyword; }
    public void setPictogramKeyword(String pictogramKeyword) { this.pictogramKeyword = pictogramKeyword; }
    
    public int getStepNumber() { return stepNumber; }
    public void setStepNumber(int stepNumber) { this.stepNumber = stepNumber; }
    
    public boolean isCompleted() { return completed; }
    public void setCompleted(boolean completed) { this.completed = completed; }
    
    public String getAudioText() { return audioText; }
    public void setAudioText(String audioText) { this.audioText = audioText; }
}

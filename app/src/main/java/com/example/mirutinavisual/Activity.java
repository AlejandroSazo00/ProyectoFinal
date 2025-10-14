package com.example.mirutinavisual;

import java.util.List;
import java.util.ArrayList;

public class Activity {
    private String id;
    private String name;
    private String time;
    private int pictogramId;
    private String pictogramKeyword;
    private boolean completed;
    private long createdAt;
    private String userId;
    
    // Nuevos campos para secuencias
    private boolean isSequence;
    private List<SequenceStep> steps;
    private int currentStepIndex;

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
        this.isSequence = false;
        this.steps = new ArrayList<>();
        this.currentStepIndex = 0;
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
    
    // Métodos para secuencias
    public boolean isSequence() { return isSequence; }
    public void setSequence(boolean sequence) { isSequence = sequence; }
    
    public List<SequenceStep> getSteps() { return steps; }
    public void setSteps(List<SequenceStep> steps) { this.steps = steps; }
    
    public int getCurrentStepIndex() { return currentStepIndex; }
    public void setCurrentStepIndex(int currentStepIndex) { this.currentStepIndex = currentStepIndex; }
    
    public SequenceStep getCurrentStep() {
        if (steps != null && currentStepIndex < steps.size()) {
            return steps.get(currentStepIndex);
        }
        return null;
    }
    
    public boolean hasNextStep() {
        return steps != null && currentStepIndex < steps.size() - 1;
    }
    
    public boolean hasPreviousStep() {
        return currentStepIndex > 0;
    }
    
    public void nextStep() {
        if (hasNextStep()) {
            currentStepIndex++;
        }
    }
    
    public void previousStep() {
        if (hasPreviousStep()) {
            currentStepIndex--;
        }
    }
    
    public void addStep(SequenceStep step) {
        if (steps == null) {
            steps = new ArrayList<>();
        }
        steps.add(step);
    }
    
    public int getTotalSteps() {
        return steps != null ? steps.size() : 0;
    }
    
    public int getCompletedStepsCount() {
        if (steps == null) return 0;
        int count = 0;
        for (SequenceStep step : steps) {
            if (step.isCompleted()) count++;
        }
        return count;
    }
    
    public float getProgressPercentage() {
        if (getTotalSteps() == 0) return 0;
        return (float) getCompletedStepsCount() / getTotalSteps() * 100;
    }

    @Override
    public String toString() {
        return "Activity{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", time='" + time + '\'' +
                ", pictogramId=" + pictogramId +
                ", completed=" + completed +
                ", isSequence=" + isSequence +
                ", totalSteps=" + getTotalSteps() +
                '}';
    }
}

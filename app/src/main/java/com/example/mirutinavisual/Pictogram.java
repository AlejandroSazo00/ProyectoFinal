package com.example.mirutinavisual;

import java.util.List;

public class Pictogram {
    private int id;
    private List<String> keywords;
    private String imageUrl;

    public Pictogram() {
        // Constructor vacío requerido para Firebase
    }

    public Pictogram(int id, List<String> keywords) {
        this.id = id;
        this.keywords = keywords;
        this.imageUrl = "https://api.arasaac.org/api/pictograms/" + id + "?download=false";
    }

    // Getters y Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
        this.imageUrl = "https://api.arasaac.org/api/pictograms/" + id + "?download=false";
    }

    public List<String> getKeywords() {
        return keywords;
    }

    public void setKeywords(List<String> keywords) {
        this.keywords = keywords;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getDownloadUrl() {
        return "https://api.arasaac.org/api/pictograms/" + id + "?download=true";
    }

    @Override
    public String toString() {
        return "Pictogram{" +
                "id=" + id +
                ", keywords=" + keywords +
                ", imageUrl='" + imageUrl + '\'' +
                '}';
    }
}

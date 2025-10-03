package com.example.mirutinavisual;

import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ArasaacApiService {
    
    private static final String BASE_URL = "https://api.arasaac.org/api/pictograms";
    private static final String SEARCH_URL = BASE_URL + "/es/search/";
    
    private ExecutorService executorService;
    
    public interface PictogramSearchCallback {
        void onSuccess(List<Pictogram> pictograms);
        void onError(String error);
    }
    
    public ArasaacApiService() {
        executorService = Executors.newFixedThreadPool(3);
    }
    
    public void searchPictograms(String searchTerm, PictogramSearchCallback callback) {
        executorService.execute(() -> {
            try {
                String encodedTerm = URLEncoder.encode(searchTerm, "UTF-8");
                String urlString = SEARCH_URL + encodedTerm;
                
                // Debug: mostrar URL
                System.out.println("ARASAAC URL: " + urlString);
                
                URL url = new URL(urlString);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setRequestProperty("User-Agent", "MiRutinaVisual/1.0");
                connection.setConnectTimeout(10000); // 10 segundos
                connection.setReadTimeout(15000); // 15 segundos
                
                int responseCode = connection.getResponseCode();
                System.out.println("Response Code: " + responseCode);
                
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    BufferedReader reader = new BufferedReader(
                        new InputStreamReader(connection.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();
                    
                    String jsonResponse = response.toString();
                    System.out.println("JSON Response: " + jsonResponse.substring(0, Math.min(200, jsonResponse.length())));
                    
                    List<Pictogram> pictograms = parsePictograms(jsonResponse);
                    System.out.println("Pictograms found: " + pictograms.size());
                    callback.onSuccess(pictograms);
                    
                } else {
                    callback.onError("Error del servidor: " + responseCode);
                }
                
                connection.disconnect();
                
            } catch (IOException e) {
                callback.onError("Error de conexión: " + e.getMessage());
            } catch (Exception e) {
                callback.onError("Error inesperado: " + e.getMessage());
            }
        });
    }
    
    private List<Pictogram> parsePictograms(String jsonResponse) {
        List<Pictogram> pictograms = new ArrayList<>();
        
        try {
            JSONArray jsonArray = new JSONArray(jsonResponse);
            
            // Limitar a 12 resultados para no sobrecargar la interfaz
            int maxResults = Math.min(jsonArray.length(), 12);
            
            for (int i = 0; i < maxResults; i++) {
                JSONObject pictogramJson = jsonArray.getJSONObject(i);
                
                int id = pictogramJson.getInt("_id");
                
                // Obtener keywords
                List<String> keywords = new ArrayList<>();
                if (pictogramJson.has("keywords")) {
                    JSONArray keywordsArray = pictogramJson.getJSONArray("keywords");
                    for (int j = 0; j < keywordsArray.length(); j++) {
                        JSONObject keywordObj = keywordsArray.getJSONObject(j);
                        if (keywordObj.has("keyword")) {
                            keywords.add(keywordObj.getString("keyword"));
                        }
                    }
                }
                
                // Si no hay keywords, usar un placeholder
                if (keywords.isEmpty()) {
                    keywords.add("Pictograma " + id);
                }
                
                Pictogram pictogram = new Pictogram(id, keywords);
                pictograms.add(pictogram);
            }
            
        } catch (JSONException e) {
            e.printStackTrace();
        }
        
        return pictograms;
    }
    
    public void loadPictogramImage(Pictogram pictogram, ImageView imageView) {
        if (pictogram != null && imageView != null) {
            Glide.with(imageView.getContext())
                    .load(pictogram.getImageUrl())
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .placeholder(R.drawable.ic_image_placeholder)
                    .error(R.drawable.ic_image_error)
                    .into(imageView);
        }
    }
    
    public void shutdown() {
        if (executorService != null) {
            executorService.shutdown();
        }
    }
}

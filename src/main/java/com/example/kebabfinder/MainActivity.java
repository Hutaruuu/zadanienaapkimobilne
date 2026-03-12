package com.example.kebabfinder;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import org.json.JSONArray;
import org.json.JSONObject;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapController;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

public class MainActivity extends AppCompatActivity {

    private MapView map;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        Configuration.getInstance().setUserAgentValue(getPackageName());

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        map = findViewById(R.id.map);
        map.setTileSource(TileSourceFactory.MAPNIK);
        map.setMultiTouchControls(true);


        GeoPoint startPoint = new GeoPoint(52.73382891305464, 15.239256491377445);
        MapController mapController = (MapController) map.getController();
        mapController.setZoom(10);
        mapController.setCenter(startPoint);


        double delta = 0.05;
        double minLat = startPoint.getLatitude() - delta;
        double maxLat = startPoint.getLatitude() + delta;
        double minLon = startPoint.getLongitude() - delta;
        double maxLon = startPoint.getLongitude() + delta;


        EditText searchInput = findViewById(R.id.search_input);
        Button searchButton = findViewById(R.id.search_button);

        searchButton.setOnClickListener(v -> {
            String query = searchInput.getText().toString().trim();
            if (!query.isEmpty()) {
                new Thread(() -> {
                    try {

                        String bboxParam = String.format("&viewbox=%f,%f,%f,%f&bounded=1", minLon, maxLat, maxLon, minLat);
                        String urlStr = "https://nominatim.openstreetmap.org/search?q=" +
                                query.replace(" ", "%20") +
                                "&format=json&limit=15" + bboxParam;

                        java.net.URL url = new java.net.URL(urlStr);
                        java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
                        conn.setRequestMethod("GET");
                        conn.setRequestProperty("User-Agent", getPackageName());

                        java.io.InputStream is = conn.getInputStream();
                        java.util.Scanner scanner = new java.util.Scanner(is).useDelimiter("\\A");
                        String response = scanner.hasNext() ? scanner.next() : "";
                        scanner.close();

                        JSONArray results = new JSONArray(response);

                        if (results.length() > 0) {
                            runOnUiThread(() -> {
                                map.getOverlays().clear();
                                try {
                                    for (int i = 0; i < results.length(); i++) {
                                        JSONObject place = results.getJSONObject(i);
                                        double lat = place.getDouble("lat");
                                        double lon = place.getDouble("lon");
                                        String name = place.getString("display_name");

                                        Marker marker = new Marker(map);
                                        marker.setPosition(new GeoPoint(lat, lon));
                                        marker.setTitle(name);
                                        map.getOverlays().add(marker);
                                    }


                                    JSONObject first = results.getJSONObject(0);
                                    double lat = first.getDouble("lat");
                                    double lon = first.getDouble("lon");
                                    map.getController().setCenter(new GeoPoint(lat, lon));
                                    map.getController().setZoom(15);
                                    map.invalidate();
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            });
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }).start();
            }
        });


        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }
}
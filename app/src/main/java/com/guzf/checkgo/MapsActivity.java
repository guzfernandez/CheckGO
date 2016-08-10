package com.guzf.checkgo;

import android.Manifest;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, LocationListener {

    private GoogleMap mMap;
    private String provider;
    private LocationManager locationManager;
    private Location currentLoc = null;
    private Marker currentMarker;
    private RequestQueue queue;
    private Marker markerCurrent;
    private MarkerOptions markerOptions;
    private TextView tvPuntos, tvNombre, tvTime, tvGameOver, tvRanking;
    private User user;
    private List<Point> points;
    private List<Check> checks;
    private boolean finished = false;
    private int partidaId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        queue = Volley.newRequestQueue(this);

        points = new ArrayList<>();
        checks = new ArrayList<>();

        Button btnCheck = (Button) findViewById(R.id.btnCheck);
        tvPuntos = (TextView)findViewById(R.id.tvPuntos);
        tvNombre = (TextView)findViewById(R.id.tvNombre);
        tvTime = (TextView)findViewById(R.id.tvTime);
        tvGameOver = (TextView)findViewById(R.id.tvGameOver);
        tvRanking = (TextView)findViewById(R.id.tvRanking);

        Intent i = getIntent();
        user = (User) i.getSerializableExtra("user");

        crearPartida();

        if(user != null){
            tvNombre.setText(user.getNombre());
            if(user.getPuntaje() == 1){
                tvPuntos.setText(user.getPuntaje() + " punto");
            }
            else{
                tvPuntos.setText(user.getPuntaje() + " puntos");
            }
            //getChecks(user.getId());
        }

        btnCheck.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (currentLoc != null) {
                    if (currentMarker != null) {
                        System.out.println(currentMarker.getId() + ", " + currentMarker.getTitle() + ", " + currentMarker.getPosition().latitude + ", " + currentMarker.getPosition().longitude);

                        LatLng marker = currentMarker.getPosition();

                        Location currentPos = new Location("");
                        currentPos.setLatitude(currentLoc.getLatitude());
                        currentPos.setLongitude(currentLoc.getLongitude());

                        Location currentMark = new Location("");
                        currentMark.setLatitude(marker.latitude);
                        currentMark.setLongitude(marker.longitude);

                        float distancia = currentPos.distanceTo(currentMark);

                        if(finished){
                            Toast.makeText(MapsActivity.this, "La partida ha terminado", Toast.LENGTH_SHORT).show();
                        }
                        else if(currentMarker.getTitle().equals("Posición actual")){
                            Toast.makeText(MapsActivity.this, "No puedes hacer check en tu posición actual.", Toast.LENGTH_SHORT).show();
                        }
                        else if(distancia <= 100){
                            System.out.println(currentMarker.getTitle());
                            checkIn(user.getId(), currentMarker.getTitle());
                        }
                        else{
                            Toast.makeText(MapsActivity.this, "La distancia no puede ser mayor a 100 metros.\nDistancia: " + Math.round(distancia) + " metros.", Toast.LENGTH_LONG).show();
                        }
                    } else {
                        Toast.makeText(MapsActivity.this, "Selecciona un lugar.", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    System.out.println("No hay lugar.");
                }
            }
        });

        tvRanking.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(MapsActivity.this, RankingActivity.class));
            }
        });

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        Criteria criteria = new Criteria();
        criteria.setAccuracy(Criteria.ACCURACY_FINE);
        provider = locationManager.getBestProvider(criteria, false);

        Location location = getLastKnownLocation();
        if(location != null){
            updateLocation(location);
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationManager.requestLocationUpdates(provider, 0, 0, this);
        }
    }

    private void crearPartida(){
        String url = "http://10.0.2.2:3000/api/partida/new?user=" + user.getId();

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, url, null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            partidaId = response.getInt("id");
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        Toast.makeText(MapsActivity.this, "Para ganar la partida, debes tener la mayor cantidad de checks posibles.", Toast.LENGTH_LONG).show();
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        error.printStackTrace();
                        Toast.makeText(MapsActivity.this, "Error del servidor", Toast.LENGTH_SHORT).show();
                    }
                });
        queue.add(request);

        startTimer();
    }

    /*private void getChecks(int idUser){
        String url = "http://10.0.2.2:3000/api/points/"+idUser+"/points";

        JsonArrayRequest request = new JsonArrayRequest(url,
                new Response.Listener<JSONArray>() {
                    @Override
                    public void onResponse(JSONArray response) {
                        try {
                            for (int i = 0; i < response.length(); i++) {
                                JSONObject object = (JSONObject) response.get(i);

                                Check check = new Check();
                                check.setIdUser(object.getInt("iduser"));
                                check.setIdPoint(object.getInt("idpoint"));
                                check.setCheckdate(object.getString("checkdate"));

                                checks.add(check);
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                error.printStackTrace();
            }
        });
        queue.add(request);
    }*/

    private void checkIn(int idUser, String markerTitle){
        Point point = null;
        //Check check = null;
        int i = 0;
        //boolean checkValido = true;

        while(i < points.size()){
            if(points.get(i).getDescripcion().equals(markerTitle)){
                point = points.get(i);
                i = points.size();
            }
            i++;
        }

        /*i = 0;
        while(i < checks.size() && point != null){
            if(checks.get(i).getIdPoint() == point.getId()){
                check = checks.get(i);
                i = checks.size();
            }
            i++;
        }*/

        /*if(check != null) {
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
            Date date = null;

            try {
                date = format.parse(check.getCheckdate());
                System.out.println(date);
            } catch (ParseException e) {
                e.printStackTrace();
            }

            if (date != null) {
                long time = System.currentTimeMillis() - date.getTime();

                if(time < 3600000){
                    checkValido = false;
                    Toast.makeText(MapsActivity.this, "No ha terminado el tiempo de espera", Toast.LENGTH_SHORT).show();
                }
            }
        }*/

        if(point != null/* && checkValido*/) {
            final ProgressDialog pDialog = new ProgressDialog(this);
            pDialog.setTitle("Cargando");
            pDialog.setMessage("Espere...");
            pDialog.setCancelable(false);
            pDialog.show();

            String url = "http://10.0.2.2:3000/api/check?user=" + idUser + "&point=" + point.getId() + "&partida="+partidaId;

            final Point finalPoint = point;
            JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, url, null,
                    new Response.Listener<JSONObject>() {
                        @Override
                        public void onResponse(JSONObject response) {
                            pDialog.dismiss();

                            user.setPuntaje(user.getPuntaje()+1);
                            if(user.getPuntaje() == 1){
                                tvPuntos.setText(user.getPuntaje() + " punto");
                            }
                            else{
                                tvPuntos.setText(user.getPuntaje() + " puntos");
                            }
                            Toast.makeText(MapsActivity.this, "Check en " + currentMarker.getTitle() + ".", Toast.LENGTH_SHORT).show();
                            currentMarker.remove();

                            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
                            Date date = new Date();
                            String datetime = dateFormat.format(date);

                            Check check = new Check();
                            check.setIdUser(user.getId());
                            check.setIdPoint(finalPoint.getId());
                            check.setCheckdate(datetime);

                            checks.add(check);
                        }
                    },
                    new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            pDialog.dismiss();
                            error.printStackTrace();
                            Toast.makeText(MapsActivity.this, "Error del servidor", Toast.LENGTH_SHORT).show();
                        }
                    });
            queue.add(request);
        }
    }

    private void checkGPS(){
        boolean enabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);

        if (!enabled) {
            AlertDialog.Builder builder = new AlertDialog.Builder(MapsActivity.this);
            builder.setTitle("GPS");
            builder.setMessage("Para acceder al mapa, debes activar la ubicación.");
            builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    startActivity(intent);
                }
            });
            builder.show();
        }
    }

    private Location getLastKnownLocation() {
        locationManager = (LocationManager) getApplicationContext().getSystemService(LOCATION_SERVICE);
        List<String> providers = locationManager.getProviders(true);
        Location bestLocation = null;
        for (String provider : providers) {
            Location l = null;

            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                l = locationManager.getLastKnownLocation(provider);
            }
            if (l == null) {
                continue;
            }
            if (bestLocation == null || l.getAccuracy() < bestLocation.getAccuracy()) {
                bestLocation = l;
            }
        }

        return bestLocation;
    }

    private void updateLocation(Location location){
        if(location != null){
            double latitude = location.getLatitude();
            double longitude = location.getLongitude();

            System.out.println("Latitude: "+latitude + ", Longitude: "+longitude);

            currentLoc = location;

            if(markerCurrent != null) {
                markerCurrent.setPosition(new LatLng(latitude, longitude));
            }
            else{
                LatLng current = new LatLng(latitude, longitude);
                markerOptions = new MarkerOptions();
                markerOptions.position(current);
                markerOptions.title("Posición actual");
                markerOptions.icon(BitmapDescriptorFactory.fromResource(R.mipmap.location));
            }
        }
        else{
            Toast.makeText(this, "No hay ubicación por el momento", Toast.LENGTH_SHORT).show();
        }
    }

    private void getPoints(){
        String url = "http://10.0.2.2:3000/api/points";

        JsonArrayRequest request = new JsonArrayRequest(url,
                new Response.Listener<JSONArray>() {
                    @Override
                    public void onResponse(JSONArray response) {
                        try {
                            for (int i = 0; i < response.length(); i++) {
                                JSONObject object = (JSONObject) response.get(i);

                                Point point = new Point();
                                point.setId(object.getInt("id"));
                                point.setDescripcion(object.getString("descripcion"));

                                JSONObject location = object.getJSONObject("location");

                                JSONArray array = location.getJSONArray("coordinates");
                                double[] coords = new double[2];
                                coords[0] = (double)array.get(0);
                                coords[1] = (double)array.get(1);
                                point.setLocation(coords);

                                System.out.println(point.getId() + ", " + point.getDescripcion());

                                points.add(point);
                            }
                            addToMaps();
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                error.printStackTrace();
            }
        });
        queue.add(request);
    }

    private void addToMaps(){
        for (Point p : points) {
            LatLng latLng = new LatLng(p.getLocation()[0], p.getLocation()[1]);
            mMap.addMarker(new MarkerOptions().position(latLng).title(p.getDescripcion()));
        }
    }

    private void startTimer(){
        new CountDownTimer(3600000, 1000) {                 // 3600000 = 1 hora
            public void onTick(long millisUntilFinished) {
                tvTime.setText(""+
                        TimeUnit.MILLISECONDS.toMinutes(millisUntilFinished)+ ":" +
                        (TimeUnit.MILLISECONDS.toSeconds(millisUntilFinished) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millisUntilFinished))));
            }
            public void onFinish() {
                tvTime.setText("00:00");
                tvTime.setTextColor(Color.parseColor("#F44336"));
                finished = true;
                tvGameOver.setVisibility(View.VISIBLE);
            }
        }.start();
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkGPS();

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            System.out.println("No hay permisos");
        }
        else{
            locationManager.requestLocationUpdates(provider, 0, 0, this);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            System.out.println("No hay permisos");
        }
        else{
            locationManager.removeUpdates(this);
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        if(markerOptions != null) {
            markerCurrent = mMap.addMarker(markerOptions);
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(markerCurrent.getPosition(), 16.0f));
        }

        getPoints();

        mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {
                currentMarker = null;
            }
        });

        mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
                System.out.println("Marker: "+marker.getTitle());
                marker.setTitle(marker.getTitle());
                marker.showInfoWindow();
                currentMarker = marker;
                return true;
            }
        });
    }

    @Override
    public void onLocationChanged(Location location) {
        updateLocation(location);
    }

    @Override
    public void onStatusChanged(String s, int i, Bundle bundle) {
        System.out.println("Status changed: "+s);
    }

    @Override
    public void onProviderEnabled(String s) {
        System.out.println("Enabled: "+s);
    }

    @Override
    public void onProviderDisabled(String s) {
        System.out.println("Disabled: "+s);
    }
}

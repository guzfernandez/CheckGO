package com.guzf.checkgo;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.ListView;

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class RankingActivity extends AppCompatActivity {

    private RequestQueue queue;
    private ListView lvRanking;
    private List<User> users;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ranking);

        lvRanking = (ListView)findViewById(R.id.lvRanking);
        queue = Volley.newRequestQueue(this);
        users = new ArrayList<>();

        getRanking();

    }

    private void getRanking(){
        String url = "http://10.0.2.2:3000/api/users/ranking";

        JsonArrayRequest request = new JsonArrayRequest(url,
                new Response.Listener<JSONArray>() {
                    @Override
                    public void onResponse(JSONArray response) {
                        try {
                            for (int i = 0; i < response.length(); i++) {
                                JSONObject object = (JSONObject) response.get(i);

                                User user = new User();
                                user.setId(object.getInt("id"));
                                user.setNombre(object.getString("nombre"));
                                user.setPuntaje(object.getInt("puntaje"));

                                users.add(user);
                            }
                            setAdapter();
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

    private void setAdapter(){
        lvRanking.setAdapter(new CustomAdapter(RankingActivity.this, users));
    }

}

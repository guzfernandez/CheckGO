package com.guzf.checkgo;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

public class LoginActivity extends AppCompatActivity {

    private EditText txtUsername;
    private RequestQueue queue;
    private User user = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        txtUsername = (EditText)findViewById(R.id.txtUsername);
        Button btnLogin = (Button) findViewById(R.id.btnLogin);

        queue = Volley.newRequestQueue(this);

        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String username = txtUsername.getText().toString();
                checkUser(username);
            }
        });

    }

    private void checkUser(String username){
        final ProgressDialog pDialogLogin = new ProgressDialog(this);
        pDialogLogin.setTitle("Iniciando sesión");
        pDialogLogin.setMessage("Espere...");
        pDialogLogin.setCancelable(false);
        pDialogLogin.show();

        String url = "http://10.0.2.2:3000/api/users/"+username+"/details";

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        pDialogLogin.dismiss();

                        try {
                            user = new User();
                            user.setId(response.getInt("id"));
                            user.setNombre(response.getString("nombre"));
                            user.setPuntaje(response.getInt("puntaje"));

                            Intent intent = new Intent(LoginActivity.this, MapsActivity.class);
                            intent.putExtra("user", user);
                            startActivity(intent);
                            finish();
                        } catch (JSONException e) {
                            e.printStackTrace();
                            Toast.makeText(LoginActivity.this, "Usuario no existe", Toast.LENGTH_SHORT).show();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        pDialogLogin.dismiss();
                        error.printStackTrace();
                        Toast.makeText(LoginActivity.this, "Error del servidor", Toast.LENGTH_SHORT).show();
                    }
                });

        queue.add(request);
    }
}

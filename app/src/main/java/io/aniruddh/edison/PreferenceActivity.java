package io.aniruddh.edison;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

public class PreferenceActivity extends AppCompatActivity {

    Button setIP;
    EditText getIP;
    SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preference);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        sharedPreferences = getSharedPreferences(Constants.PREFERENCES, Context.MODE_PRIVATE);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });


        setIP = (Button) findViewById(R.id.button_new);
        getIP = (EditText) findViewById(R.id.ip_edison_ed);

        setIP.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String new_IP = getIP.getText().toString();
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString("edison_ip", new_IP);
                editor.commit();
            }
        });
    }

}

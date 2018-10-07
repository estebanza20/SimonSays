package com.example.ezamoraa.simondice;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

public class MainActivity extends AppCompatActivity {

    private static final String LOG_TAG =
                                MainActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void launchGameActivity(View view) {
        Log.d(LOG_TAG, "Launch Game Activity");
        Intent intent = new Intent(this, GameActivity.class);
        startActivity(intent);
    }

    public void launchHighScoresActivity(View view) {
        Log.d(LOG_TAG, "Launch High Scores");
        Intent intent = new Intent(this, HighScoresActivity.class);
        startActivity(intent);
    }
}

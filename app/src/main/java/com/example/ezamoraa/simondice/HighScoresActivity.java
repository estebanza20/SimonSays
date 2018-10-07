package com.example.ezamoraa.simondice;

import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class HighScoresActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_high_scores);
        updateHighScoresTable();
    }

    private void updateHighScoresTable() {
        SharedPreferences hs_prefs = getSharedPreferences(
                getResources().getString(R.string.hs_prefs), 0
        );

        List<TextView> high_scores = Arrays.asList(
                (TextView)findViewById(R.id.hs1_val),
                (TextView)findViewById(R.id.hs2_val),
                (TextView)findViewById(R.id.hs3_val),
                (TextView)findViewById(R.id.hs4_val),
                (TextView)findViewById(R.id.hs5_val)
        );

        Integer hs_num = hs_prefs.getInt(getResources().getString(R.string.hs_num), 0);
        for (int i=0; i<hs_num && i<high_scores.size(); i++) {
            high_scores.get(i).setText(
                    String.valueOf(hs_prefs.getInt(String.valueOf(i),0))
            );
        }
    }
}

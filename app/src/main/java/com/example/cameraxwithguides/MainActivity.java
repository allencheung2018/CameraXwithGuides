package com.example.cameraxwithguides;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class MainActivity extends AppCompatActivity {

    private Button camera1;
    private Button camera2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        camera1 = findViewById(R.id.button);
        camera2 = findViewById(R.id.button2);

        initEvent();
    }

    private void initEvent() {
        camera1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, Camera2Activity.class);
                intent.putExtra("camera", 0);
                startActivity(intent);
            }
        });

        camera2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, Camera2Activity.class);
                intent.putExtra("camera", 1);
                startActivity(intent);
            }
        });
    }
}
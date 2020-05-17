package com.zhangkuo.glide;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.widget.ImageView;

import com.bumptech.glide.Glide;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Glide.with(this).load("https://misc.aotu.io/ONE-SUNDAY/world_cup_2014_42.webp").into((ImageView) findViewById(R.id.test));
    }
}

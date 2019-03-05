package ru.linkos.veresk_controller;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

public class error_window extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_error_window);
        Intent intent = getIntent();
        String error = intent.getStringExtra(MainActivity.EXTRA_MESSAGE_ERR_NO);
        TextView errorTW = findViewById(R.id.errorText);
        errorTW.setText(error);
    }

    public void toStart(View view){
        Intent restartIntent = new Intent(this, MainActivity.class);
        startActivity(restartIntent);
    }
}

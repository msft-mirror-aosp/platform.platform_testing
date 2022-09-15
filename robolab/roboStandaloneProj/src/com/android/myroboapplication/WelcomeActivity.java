package com.android.myroboapplication;

import static com.android.myroboapplication.R.*;
import android.content.Intent;
import android.view.View;
import android.app.Activity;
import android.os.Bundle;

public class WelcomeActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.welcome_activity);

        final View button = findViewById(R.id.login);
        button.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        startActivity(new Intent(WelcomeActivity.this, LoginActivity.class));
                    }
                });
    }
}

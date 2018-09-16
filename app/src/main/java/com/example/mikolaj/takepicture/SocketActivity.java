package com.example.mikolaj.takepicture;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.TextInputEditText;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class SocketActivity extends AppCompatActivity {

    TextInputEditText editText;
    TextView textView;
    String message;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_socket);
        connectWithWebsocket();

        editText = (TextInputEditText) findViewById(R.id.editText);
        textView = (TextView) findViewById(R.id.textView);
        message = editText.getText().toString().trim();

        Button sendButton = (Button) findViewById(R.id.sendButton);
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendMessage(message);
                textView.setText(message);
            }
        });
    }

    private void sendMessage(String message) {
        this.message = message;
        editText.setText("");
    }

    private void connectWithWebsocket() {
        Connect connect = new Connect();
        connect.getSocket().connect();
        Toast.makeText(this, "Connected.", Toast.LENGTH_SHORT).show();
    }

}

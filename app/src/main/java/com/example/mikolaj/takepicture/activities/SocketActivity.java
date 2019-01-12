package com.example.mikolaj.takepicture.activities;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.design.widget.TextInputEditText;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.telephony.TelephonyManager;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.mikolaj.takepicture.Helper;
import com.example.mikolaj.takepicture.R;
import com.example.mikolaj.takepicture.services.GpsService;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.net.URISyntaxException;
import java.util.UUID;

import io.socket.client.IO;
import io.socket.client.Socket;

public class SocketActivity extends AppCompatActivity {

    private Socket socket;
    {
        try {
            socket = IO.socket("http://192.168.1.13:8000");
        } catch (URISyntaxException e){
            throw new RuntimeException(e);
        }
    }

    private TextInputEditText editText;
    private EditText editPhone;
    private TextView textView;
    private TextView imgTextView;
    private ImageView imgView;
    private String message = " ";
    private String phone = " ";
    private String imagePath = " ";

    private BroadcastReceiver broadcastReceiver;

    private boolean imageSent = false;

    private double lat;
    private double lng;

    @Override
    protected void onResume() {
        super.onResume();
        if(broadcastReceiver == null){
            broadcastReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    Helper.lat = (double)intent.getExtras().get("lat");
                    Helper.lng = (double)intent.getExtras().get("lng");
                }
            };
        }
        registerReceiver(broadcastReceiver, new IntentFilter("location_update"));
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_socket);

//        //Connects to the WebSocket
//        socket.connect();

        if(checkPermission()){
        } else {
            requestPermission();
        }

        editText = findViewById(R.id.editText);
        editPhone = findViewById(R.id.editPhone);
        textView = findViewById(R.id.textView);
        imgTextView = findViewById(R.id.imgTextView);
        imgView = findViewById(R.id.imgView);
        message = editText.getText().toString().trim();

        Button sendButton = findViewById(R.id.sendButton);
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendMessage();
                if(imageSent){
                    textView.append(message + " ");
                }
            }
        });

        ImageButton imageButton = findViewById(R.id.imageButton);
        imageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent galleryintent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(galleryintent, 1);
            }
        });
    }

    private boolean checkPermission(){
        int result = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if(result == PackageManager.PERMISSION_GRANTED){
            return true;
        } else {
            return false;
        }
    }

    private void requestPermission(){
        if(ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)){
            Toast.makeText(this, "Write external storage permission allows us to store images.", Toast.LENGTH_SHORT).show();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults){
        switch(requestCode){
            case 1:
                if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    Log.e("PERM: ", "PERMISSION GRANTED, now you can use local drive");
                } else {
                    Log.e("PERM: ", "PERMISSION DENIED, You cannot use local drive");
                }
                break;
        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data){
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == 1 && resultCode == RESULT_OK && data != null){
            Uri selectedImage = data.getData();
            String[] filePath = {MediaStore.Images.Media.DATA};
            Cursor cursor = getContentResolver().query(selectedImage, filePath, null, null, null);
            cursor.moveToFirst();
            int columnIndex = cursor.getColumnIndex(filePath[0]);
            imagePath = cursor.getString(columnIndex);
            imgTextView.setText(imagePath);
            Bitmap bm = BitmapFactory.decodeFile(imagePath);
            Bitmap scaledBm = Bitmap.createScaledBitmap(bm, 800,600, true);
            imgView.setImageBitmap(scaledBm);
            cursor.close();
        }
    }

    private void sendMessage() {
        message = editText.getText().toString().trim();
        phone = editPhone.getText().toString().trim();

        if((message.equals(" ") || message.equals("")) || phone.equals(" ") || imagePath.equals(" ")){
            Toast.makeText(getApplicationContext(), "All fields are required!", Toast.LENGTH_SHORT).show();
        }
        else {
            socket.connect();
            editText.setText("");
            JSONObject dataToSend = new JSONObject();
            try{
                //dataToSend.put("id", 555);
                UUID hash = UUID.randomUUID();
                dataToSend.put("hash", hash);
                dataToSend.put("phone", phone);
                dataToSend.put("latitude", Helper.lat);
                dataToSend.put("longitude", Helper.lng);
                dataToSend.put("text", message);
                dataToSend.put("picture", encodeImage(imagePath));
                socket.emit("message", dataToSend);
            } catch(JSONException e){
                e.printStackTrace();
            }
            Toast.makeText(getApplicationContext(), "Notification has been sent!", Toast.LENGTH_SHORT).show();
        }
    }

    private String encodeImage(String path)
    {
        File imagefile = new File(path);
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(imagefile);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        Bitmap bm = BitmapFactory.decodeStream(fis);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bm.compress(Bitmap.CompressFormat.JPEG,100,baos);
        byte[] b = baos.toByteArray();
        return Base64.encodeToString(b, Base64.DEFAULT);
    }

    @Override
    protected void onStop(){
        super.onStop();
        unregisterReceiver(broadcastReceiver);
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        socket.disconnect();
    }

}

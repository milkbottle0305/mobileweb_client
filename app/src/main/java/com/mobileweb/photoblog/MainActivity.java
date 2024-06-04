package com.mobileweb.photoblog;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private static final int READ_MEDIA_IMAGES_PERMISSION_CODE = 1001;  // 상수 정의
    private static final int READ_EXTERNAL_STORAGE_PERMISSION_CODE = 1002;
    private static final String UPLOAD_URL = "http://10.0.2.2:8000/api_root/Post/";
    private static final int PICK_IMAGE_REQUEST = 1;

    private Uri imageUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button uploadButton = findViewById(R.id.btn);
        uploadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                requestStoragePermission();
            }
        });
    }

    private void requestStoragePermission() {
        if (ContextCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{Manifest.permission.READ_MEDIA_IMAGES},
                    READ_MEDIA_IMAGES_PERMISSION_CODE);
        } else {
            openImagePicker();
        }
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == READ_EXTERNAL_STORAGE_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openImagePicker();
            } else {
                Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            imageUri = data.getData();
            new ImageUploadTask().execute();
        }
    }

    private class ImageUploadTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... voids) {
            try {
                uploadImage();
            } catch (IOException | JSONException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            // 업로드 완료 후 실행할 코드 (예: 알림 또는 다른 UI 업데이트)
        }
    }

    private void uploadImage() throws IOException, JSONException {
        String imagePath = getRealPathFromURI(imageUri);
        File imageFile = new File(imagePath);
        URL url = new URL(UPLOAD_URL);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "multipart/form-data;boundary=\"boundary\"");
        connection.setDoOutput(true);

        DataOutputStream outputStream = new DataOutputStream(connection.getOutputStream());
        outputStream.writeBytes("--boundary\r\n");
        outputStream.writeBytes("Content-Disposition: form-data; name=\"image\"; filename=\"" + imageFile.getName() + "\"\r\n");
        outputStream.writeBytes("\r\n");
        FileInputStream fileInputStream = new FileInputStream(imageFile);
        byte[] buffer = new byte[4096];
        int bytesRead;
        while ((bytesRead = fileInputStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, bytesRead);
        }
        outputStream.writeBytes("\r\n");
        outputStream.writeBytes("--boundary\r\n");
        outputStream.writeBytes("Content-Disposition: form-data; name=\"author\"\r\n");
        outputStream.writeBytes("\r\n");
        outputStream.writeBytes("1");
        outputStream.writeBytes("\r\n");
        outputStream.writeBytes("--boundary\r\n");
        outputStream.writeBytes("Content-Disposition: form-data; name=\"title\"\r\n");
        outputStream.writeBytes("\r\n");
        outputStream.writeBytes("Android-REST API Test");
        outputStream.writeBytes("\r\n");
        outputStream.writeBytes("--boundary\r\n");
        outputStream.writeBytes("Content-Disposition: form-data; name=\"text\"\r\n");
        outputStream.writeBytes("\r\n");
        outputStream.writeBytes("This is REST Api with Android.");
        outputStream.writeBytes("\r\n");
        outputStream.writeBytes("--boundary\r\n");
        outputStream.writeBytes("Content-Disposition: form-data; name=\"created_date\"\r\n");
        outputStream.writeBytes("\r\n");
        outputStream.writeBytes(getCurrentDateTime());
        outputStream.writeBytes("\r\n");
        outputStream.writeBytes("--boundary\r\n");
        outputStream.writeBytes("Content-Disposition: form-data; name=\"published_date\"\r\n");
        outputStream.writeBytes("\r\n");
        outputStream.writeBytes(getCurrentDateTime());
        outputStream.writeBytes("\r\n");
        outputStream.writeBytes("--boundary--\r\n");

        int responseCode = connection.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_CREATED) {
            Log.d("Upload", "Success");
        } else {
            Log.e("Upload", "Failed with error code " + responseCode);
        }

        outputStream.flush();
        outputStream.close();
        connection.disconnect();
    }

    private String getRealPathFromURI(Uri contentUri) {
        String[] projection = {MediaStore.Images.Media.DATA};
        Cursor cursor = getContentResolver().query(contentUri, projection, null, null, null);
        if (cursor == null) {
            return contentUri.getPath();
        } else {
            cursor.moveToFirst();
            int columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            String path = cursor.getString(columnIndex);
            cursor.close();
            return path;
        }
    }

    private String getCurrentDateTime() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.getDefault());
        return dateFormat.format(new Date());
    }
}

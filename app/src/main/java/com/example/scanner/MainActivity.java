package com.example.scanner;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {
    Button buttonTakePhoto;
    Button buttonSearchGallery;
    static final int REQUEST_IMAGE_CAPTURE = 1;
    static final int REQUEST_LOAD_IMAGE = 2;
    static final int REQUEST_CROP = 3;
    File photoFile = null;
    String currentPhotoPath;
    Bitmap photo;
    Response response = null;
    SendMessage sendMessage = new SendMessage();
    ProgressDialog dialog = null;
    Uri photoURI;
    boolean word = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getSupportActionBar().hide();
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN); //enable full screen
        setContentView(R.layout.activity_main);
        buttonTakePhoto = findViewById(R.id.takePhoto);
        buttonSearchGallery = findViewById(R.id.searchGallery);

        if (!hasCamera()) {
            buttonTakePhoto.setEnabled(false);
        }
    }

    //check if the user has a camera
    private boolean hasCamera() {
        return getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY);
    }

    //search gallery
    public void searchGallery(View v) {
        Intent gallery_intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.INTERNAL_CONTENT_URI);
        startActivityForResult(gallery_intent, REQUEST_LOAD_IMAGE);
    }

    //launching the camera
    public void launchCamera(View v) {
        Intent camera_intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (camera_intent.resolveActivity(getPackageManager()) != null) {
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                System.out.println(ex.getMessage());
            }
            if (photoFile != null) {
                photoURI = FileProvider.getUriForFile(this,
                        "com.example.scanner.provider", photoFile);
                camera_intent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(camera_intent, REQUEST_IMAGE_CAPTURE);
            }
        }
    }


    //if you want to return the image taken
    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CROP && resultCode == RESULT_OK && null != data) {
            try {
                photo = MediaStore.Images.Media.getBitmap(this.getContentResolver(), data.getData());
            } catch (IOException e) {
                e.printStackTrace();
            }

            if (photo.getHeight() < 250 || photo.getWidth() < 250) {
                final Handler handler = new Handler() {
                    @Override
                    public void handleMessage(Message mesg) {
                        throw new RuntimeException();
                    }
                };

                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setCancelable(true);
                builder.setTitle("Please confirm");
                builder.setMessage("The section you chose is very small. Is this only a word?");
                builder.setPositiveButton("One word",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                word = true;
                                handler.sendMessage(handler.obtainMessage());
                            }
                        });
                builder.setNegativeButton("More than one word", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        word = false;
                        handler.sendMessage(handler.obtainMessage());
                    }
                });
                AlertDialog dialog = builder.create();
                dialog.show();
                try{
                    Looper.loop(); }
                catch(RuntimeException e){}
            }

            File sdCardDirectory = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "AFTER-CROP");

            if (!sdCardDirectory.exists()) {
                if (!sdCardDirectory.mkdirs()) {
                    Log.d("MySnaps", "failed to create directory");
                }
            }

            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            String nw = "MFA_CROP_" + timeStamp + ".jpeg";

            File image = new File(sdCardDirectory, nw);
            currentPhotoPath = image.getAbsolutePath();

            try {
                FileOutputStream out = new FileOutputStream(currentPhotoPath);
                photo.compress(Bitmap.CompressFormat.PNG, 100, out);
                out.flush();
                out.close();
            } catch (Exception e) {
                e.printStackTrace();
            }

            dialog = ProgressDialog.show(this, "Loading", "Please wait...", true);
            sendMessage.execute();
        }

        if (requestCode == REQUEST_LOAD_IMAGE && resultCode == RESULT_OK && null != data) {
            try {
                photoURI = data.getData();
                String[] filePathColumn = {MediaStore.Images.Media.DATA};
                assert photoURI != null;
                Cursor cursor = getContentResolver().query(photoURI,
                        filePathColumn, null, null, null);
                assert cursor != null;
                cursor.moveToFirst();
                int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
                currentPhotoPath = cursor.getString(columnIndex);
                cursor.close();
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
        }
        if ((requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) || (requestCode == REQUEST_LOAD_IMAGE && resultCode == RESULT_OK && null != data) ) {
            int permissionCheck = ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.CAMERA);
            if(permissionCheck == PackageManager.PERMISSION_DENIED) {
                if(ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, Manifest.permission.CAMERA)){
                    Toast.makeText(this, "CAMERA permission allows us to access Camera app", Toast.LENGTH_SHORT).show();
                }
                else {
                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.CAMERA}, 1);
                }
            }
            cropImage();

        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch(requestCode) {
            case 1: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "Permission granted", Toast.LENGTH_SHORT).show();
                }
                else {
                    Toast.makeText(this, "Permission canceled", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    private void cropImage() {
        try{
            Intent cropIntent = new Intent("com.android.camera.action.CROP");
            cropIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            cropIntent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            cropIntent.setDataAndType(photoURI, "image/*");
            cropIntent.putExtra("crop", "true");
            cropIntent.putExtra("aspectX", 0);
            cropIntent.putExtra("aspectY", 0);
            cropIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
            Toast.makeText(this, "Select a word or a zone of text", Toast.LENGTH_LONG).show();
            startActivityForResult(cropIntent, REQUEST_CROP);
        }
        catch (ActivityNotFoundException e){
                System.out.println(e.toString());
        }
    }

    private File createImageFile() throws IOException {
        @SuppressLint("SimpleDateFormat") String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "/JPEG_" + timeStamp + "_";
        imageFileName = Environment.getExternalStorageDirectory().getAbsolutePath() + imageFileName + ".jpg";
//        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
//        imageFileName = storageDir + imageFileName + ".jpg";
        File image = new File(imageFileName);
//        File image = File.createTempFile(imageFileName, ".jpg", storageDir);
        currentPhotoPath = image.getAbsolutePath();
        return image;
    }

    @SuppressLint("StaticFieldLeak")
    public class SendMessage extends AsyncTask<String, Void, Void> {
        @Override
        protected Void doInBackground(String... params) {
            String url = "http://192.168.100.5:8080/";
            OkHttpClient client = new OkHttpClient.Builder()
                    .connectTimeout(5, TimeUnit.MINUTES) // connect timeout
                    .writeTimeout(5, TimeUnit.MINUTES) // write timeout
                    .readTimeout(5, TimeUnit.MINUTES) // read timeout
                    .build();
            File file = new File(currentPhotoPath);

            RequestBody formBody = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("file", "image.jpg",
                            RequestBody.create(MediaType.parse("image/jpg"), file))
                    .addFormDataPart("word", String.valueOf(word))
                    .build();

            Request request = new Request.Builder().url(url).post(formBody).build();

            try {
                response = client.newCall(request).execute();
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (!response.isSuccessful()) {
                dialog.dismiss();
                try {
                    throw new IOException("Unexpected code " + response);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            else {
                dialog.dismiss();
                Intent intent = new Intent(MainActivity.this, ActivityResult.class);
                try {
                    JSONObject json = new JSONObject(response.body().string());
                    String imageResult = (String) json.get("image");
                    JSONObject textResult = null, textTranslateResult = null;
                    StringBuilder result = new StringBuilder();
                    HashMap<String, String> translationMap = new HashMap<>();
                    try {
                        textResult = (JSONObject) json.get("text");
                        textTranslateResult = (JSONObject) json.get("translate");
                        for (Iterator<String> it = textResult.keys(); it.hasNext(); ) {
                            String key = it.next();
                            result.append(textResult.get(key)).append(" ");
                        }
                        for (Iterator<String> it = textTranslateResult.keys(); it.hasNext(); ) {
                            String key = it.next();
                            translationMap.put(key, textTranslateResult.get(key).toString());
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    intent.putExtra("text", result.toString());
                    intent.putExtra("translate", translationMap);
                    try {
                        OutputStreamWriter outputStreamWriter = new OutputStreamWriter(MainActivity.this.openFileOutput("config.txt", Context.MODE_PRIVATE));
                        outputStreamWriter.write(imageResult);
                        outputStreamWriter.close();
                    }
                    catch (IOException e) {
                        Log.e("Exception", "File write failed: " + e.toString());
                    }
                } catch (IOException | JSONException e) {
                    e.printStackTrace();
                }
                startActivity(intent);
            }
            return null;
        }
    }

}


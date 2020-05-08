package com.example.scanner;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Base64;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import com.ibm.cloud.sdk.core.security.IamAuthenticator;
import com.ibm.cloud.sdk.core.service.exception.NotFoundException;
import com.ibm.cloud.sdk.core.service.exception.RequestTooLargeException;
import com.ibm.cloud.sdk.core.service.exception.ServiceResponseException;
import com.ibm.watson.language_translator.v3.LanguageTranslator;
import com.ibm.watson.language_translator.v3.model.TranslateOptions;
import com.ibm.watson.language_translator.v3.model.TranslationResult;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class ActivityResult extends AppCompatActivity  implements  PopupMenu.OnMenuItemClickListener{

    Button buttonTranslate;
    Button buttonFinish;
    Button buttonCopy;
    Button buttonSearch;
    TextView viewText;
    ImageView viewImage;
    String textResult;
    String imageResult;
    String textTranslateResult;
    String languageResult;
    IamAuthenticator authenticator = new IamAuthenticator("B63mxFfG8zlNF9BqkUBLBXUtu9OarpOWa1TEvkAWy-6S");
    LanguageTranslator languageTranslator = new LanguageTranslator("2018-05-01", authenticator);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getSupportActionBar().hide();
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN); //enable full screen
        setContentView(R.layout.activity_result);
        buttonTranslate = findViewById(R.id.translate);
        buttonFinish = findViewById(R.id.finish);
        buttonCopy = findViewById(R.id.copy);
        buttonSearch = findViewById(R.id.search);
        viewText = findViewById(R.id.textView);
        viewImage = findViewById(R.id.imageResult);
        textResult = (String) getIntent().getExtras().get("text");
        textTranslateResult = (String) getIntent().getExtras().get("translate");
        try {
            InputStream inputStream = ActivityResult.this.openFileInput("config.txt");

            if ( inputStream != null ) {
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                String receiveString = "";
                StringBuilder stringBuilder = new StringBuilder();

                while ( (receiveString = bufferedReader.readLine()) != null ) {
                    stringBuilder.append(receiveString);
                }

                inputStream.close();
                imageResult = stringBuilder.toString();
            }
        }
        catch (FileNotFoundException e) {
            Log.e("login activity", "File not found: " + e.toString());
        } catch (IOException e) {
            Log.e("login activity", "Can not read file: " + e.toString());
        }

        byte[] data = Base64.decode(imageResult, Base64.DEFAULT);
        Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
        viewImage.setImageBitmap(bitmap);
        viewText.append(textResult);

    }

    public void finish(View v) {
        Intent intent= new Intent(this, MainActivity.class);
        startActivity(intent);
    }

    public void copyToClipboard(View v) {
        Toast.makeText(ActivityResult.this, "Text copied", Toast.LENGTH_SHORT).show();
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("CopiedText", textResult);
        clipboard.setPrimaryClip(clip);
    }

    public void searchOnGoogle(View v) {
        Toast.makeText(ActivityResult.this, "Search " + viewText.getText(), Toast.LENGTH_SHORT).show();
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/#q=" + viewText.getText())));
    }

    public void translate(View v) {
        Context context = new ContextThemeWrapper(this, R.style.PopupMenu);
        PopupMenu popupMenu = new PopupMenu(context, v);
        popupMenu.setOnMenuItemClickListener(this);
        MenuInflater inflater = popupMenu.getMenuInflater();
        inflater.inflate(R.menu.popup_menu, popupMenu.getMenu());
        popupMenu.show();
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_arabic:
                setTargetLanguage("ar");
                return true;
            case R.id.action_bulgarian: {
                setTargetLanguage("bg");
                return true;
            }
            case R.id.action_czech: {
                setTargetLanguage("cs");
                return true;
            }
            case R.id.action_danish: {
                setTargetLanguage("da");
                return true;
            }
            case R.id.action_dutch: {
                setTargetLanguage("nl");
                return true;
            }
            case R.id.action_french: {
                setTargetLanguage("fr");
                return true;
            }
            case R.id.action_german: {
                setTargetLanguage("de");
                return true;
            }
            case R.id.action_greek: {
                setTargetLanguage("el");
                return true;
            }
            case R.id.action_hungarian: {
                setTargetLanguage("hu");
                return true;
            }
            case R.id.action_japanese: {
                setTargetLanguage("ja");
                return true;
            }
            case R.id.action_portuguese: {
                setTargetLanguage("pt");
                return true;
            }
            case R.id.action_romanian: {
                setTargetLanguage("ro");
                return true;
            }
            case R.id.action_spanish: {
                setTargetLanguage("es");
                return true;
            }
            case R.id.action_turkish: {
                setTargetLanguage("tr");
                return true;
            }
        }
        return false;
    }

    public void setTargetLanguage(String language) {
        languageResult = language;
        if(!textResult.isEmpty()){
            Translator translator = new Translator();
            translator.execute();
            Toast.makeText(ActivityResult.this, "Text translated", Toast.LENGTH_SHORT).show();
        }
    }


   public class Translator extends AsyncTask<String, Void, Void> {
       @Override
       public Void doInBackground(String... params) {
               try {
                   languageTranslator.setServiceUrl("https://api.us-south.language-translator.watson.cloud.ibm.com/instances/51f99d7d-5354-44e5-9e33-f6e461285d73");
                   TranslateOptions translateOptions = new TranslateOptions.Builder()
                           .addText(textResult)
                           .modelId("en-" + languageResult)
                           .build();

                   TranslationResult result = languageTranslator.translate(translateOptions)
                           .execute().getResult();
                   textTranslateResult = result.getTranslations().get(0).getTranslation();
                   viewText.setText(textTranslateResult);
                   // Invoke a Language Translator method
               } catch (NotFoundException e) {
                   System.out.println(e.getMessage());
                   // Handle Not Found (404) exception
               } catch (RequestTooLargeException e) {
                   System.out.println(e.getMessage());
                   // Handle Request Too Large (413) exception
               } catch (ServiceResponseException e) {
                   System.out.println(e.getMessage());
                   // Base class for all exceptions caused by error responses from the service
                   System.out.println("Service returned status code "
                           + e.getStatusCode() + ": " + e.getMessage());
               }
           return null;
       }
   }
}

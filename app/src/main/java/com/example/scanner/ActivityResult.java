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
import android.text.method.ScrollingMovementMethod;
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
import java.util.ArrayList;
import java.util.HashMap;

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
    String[] lines;
    String[] sensitiveCase;
    HashMap<String, String> translationMap;
    HashMap<String, String> speechMap;
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
        translationMap = (HashMap<String, String>) getIntent().getExtras().get("translate");
        speechMap = (HashMap<String, String>) getIntent().getExtras().get("speech");
        lines = textResult.split("\\r?\\n");
        sensitiveCase = textResult.split(" ");
        viewText.setMovementMethod(new ScrollingMovementMethod());
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
            try {
                Thread.sleep(1000); //1000 milliseconds is one second.
            }
            catch (InterruptedException e)
            {
                e.printStackTrace();
            }
            Toast.makeText(ActivityResult.this, "Text translated", Toast.LENGTH_SHORT).show();
        }
    }


   public class Translator extends AsyncTask<String, Void, Void> {
       @Override
       public Void doInBackground(String... params) {
               try {
                   languageTranslator.setServiceUrl("https://api.us-south.language-translator.watson.cloud.ibm.com/instances/51f99d7d-5354-44e5-9e33-f6e461285d73");
                   TranslateOptions.Builder builder = new TranslateOptions.Builder();
                   textTranslateResult = "";
                   int noWords = 0;
                   int[] wordsPerLine = new int[lines.length];
                   ArrayList<String> newLines = new ArrayList<String>();
                   String auxLine= "";
                   boolean oneLine = false;
                   for(int i = 0; i < speechMap.size(); i++) {
                       if(!speechMap.get(Integer.toString(i)).contains("NN")){
                           oneLine = true;
                           break;
                       }
                   }
                   if(oneLine) {
                       for(int i = 0; i < lines.length; i++) {
                           if (lines[i].length() == 0) continue;
                           wordsPerLine[i] = lines[i].split(" ").length;
                           noWords += wordsPerLine[i];
                           if ((!speechMap.get(Integer.toString(noWords - 1)).contains("NNS")|| !speechMap.get(Integer.toString(noWords - 1)).contains("NNP")) && i < lines.length - 1) {
                               auxLine += lines[i] + " " + lines[i + 1];
                               i++;
                               wordsPerLine[i] = lines[i].split(" ").length;
                               noWords += wordsPerLine[i];
                               if (i == lines.length - 1) {
                                   newLines.add(auxLine);
                               }
                               else if (!speechMap.get(Integer.toString(noWords - 1)).contains("NNS")|| !speechMap.get(Integer.toString(noWords - 1)).contains("NNP")) {
                                   auxLine = auxLine.substring(0, auxLine.length() - lines[i].length());
                                   noWords -= wordsPerLine[i];
                                   i--;
                               }
                           } else {
                               if (auxLine.length() > 0) {
                                   newLines.add(auxLine);
                               }
                               newLines.add(lines[i]);
                               wordsPerLine[i] = lines[i].split(" ").length;
                               auxLine = "";
                           }
                       }

                   }
                   else {
                       for(int i = 0; i < lines.length; i++) {
                           if (lines[i].length() == 0) continue;
                           wordsPerLine[i] = lines[i].split(" ").length;
                           noWords += wordsPerLine[i];
                           newLines.add(lines[i]);
                       }
                   }

                   for(int i = 0; i < newLines.size(); i++) {
                       if(languageResult.equals("ro")) {
                           builder.addText(newLines.get(i).toLowerCase());
                       }
                       else {
                           builder.addText(newLines.get(i));
                       }
                   }

                   builder.modelId("en-" + languageResult);
                   TranslateOptions translateOptions = builder
                           .build();

                   TranslationResult result = languageTranslator.translate(translateOptions)
                           .execute().getResult();

                   textTranslateResult="";
                   String translation = translationMap.get(languageResult);
                   String backup[] = translation.split("\\r?\\n");
                   int index = 0;
                   String[] translations;
                   for(int i = 0; i < result.getTranslations().size(); i++){
                       if (result.getTranslations().get(i).getTranslation().equals(lines[i])) {
                           translations = backup[i].split(" ");
                       }
                       else {
                           String translatedLine = result.getTranslations().get(i).getTranslation();
                           translations = translatedLine.split(" ");
                       }
                       int wordsProcessed = 0;
                       while(wordsProcessed < translations.length) {
                           boolean smallCase = false;
                           boolean upperCase = false;
                           for (int k = wordsProcessed; k < wordsProcessed + wordsPerLine[index] && k < translations.length; k++) {
                               if(languageResult.equals("ro")){
                                   if(k < sensitiveCase.length){
                                       smallCase = (sensitiveCase[k].equals(sensitiveCase[k].toLowerCase()));
                                       upperCase = (sensitiveCase[k].equals(sensitiveCase[k].toUpperCase()));
                                   }
                                   if(smallCase && !upperCase){
                                       textTranslateResult += translations[k] + " ";
                                   }
                                   else if (upperCase && !smallCase) {
                                       textTranslateResult += translations[k].toUpperCase() + " ";
                                   }
                                   else {
                                       textTranslateResult += translations[k].substring(0, 1).toUpperCase() + translations[k].substring(1) + " ";
                                   }
                               }
                               else {
                                   textTranslateResult += translations[k] + " ";
                               }
                           }
                           wordsProcessed += wordsPerLine[index];
                           index++;
                           if(wordsProcessed < translations.length) {
//                               if (result.getTranslations().size() < lines.length){
//                                   textTranslateResult += "\n ";
//                               }
                               for (int k = wordsProcessed; k < translations.length; k++) {
                                   if(languageResult.equals("ro") ){
                                       if(k < sensitiveCase.length){
                                           smallCase = (sensitiveCase[k].equals(sensitiveCase[k].toLowerCase()));
                                           upperCase = (sensitiveCase[k].equals(sensitiveCase[k].toUpperCase()));
                                       }
                                       if(smallCase && !upperCase){
                                           textTranslateResult += translations[k] + " ";
                                       }
                                       else if (upperCase && !smallCase) {
                                           textTranslateResult += translations[k].toUpperCase() + " ";
                                       }
                                       else {
                                           textTranslateResult += translations[k].substring(0, 1).toUpperCase() + translations[k].substring(1) + " ";
                                       }
                                   }
                                   else {
                                       textTranslateResult += translations[k] + " ";
                                   }
                                   wordsProcessed++;
                               }
                           }
                           textTranslateResult +=  "\n";
                       }
                   }
                   if(oneLine) {
                       if (textTranslateResult.split("\\r?\\n").length < lines.length) {
                           String[] words = textTranslateResult.replace("\\r?\\n", " ").split(" ");
                           textTranslateResult = "";
                           int wordsLine = words.length / lines.length;
                           int count = 0;
//                           while (count < words.length) {
//                               for (int i = 0; i < wordsLine; i++) {
//                                   textTranslateResult += words[count] + " ";
//                                   count++;
//                               }
//                               textTranslateResult += "\n";
//                           }
                           for(int j = 0; j < wordsPerLine.length; j++) {
                               for (int i = 0; i < wordsPerLine[j]; i++) {
                                   textTranslateResult += words[count] + " ";
                                   count++;
                               }
                               textTranslateResult += "\n";
                           }
                           if(count < words.length) {
                               for (int i = count; i < words.length; i++) {
                                   textTranslateResult += words[i] + " ";
                                   if(i % wordsLine == 0) {
                                       textTranslateResult += "\n";
                                   }
                               }
                           }
                       }
                   }

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

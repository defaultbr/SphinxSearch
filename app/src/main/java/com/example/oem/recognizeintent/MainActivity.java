package com.example.oem.recognizeintent;/* ====================================================================
 * Copyright (c) 2014 Alpha Cephei Inc.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY ALPHA CEPHEI INC. ``AS IS'' AND
 * ANY EXPRESSED OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL CARNEGIE MELLON UNIVERSITY
 * NOR ITS EMPLOYEES BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * ====================================================================
 */


import android.app.Activity;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.nfc.Tag;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;

import edu.cmu.pocketsphinx.Assets;
import edu.cmu.pocketsphinx.Config;
import edu.cmu.pocketsphinx.Decoder;
import edu.cmu.pocketsphinx.Hypothesis;
import edu.cmu.pocketsphinx.RecognitionListener;
import edu.cmu.pocketsphinx.Segment;
import edu.cmu.pocketsphinx.SpeechRecognizer;
import edu.cmu.pocketsphinx.SpeechRecognizerSetup;

import static android.widget.Toast.makeText;

public class MainActivity extends Activity implements
        RecognitionListener {

    static {
        System.loadLibrary("pocketsphinx_jni");
    }

    /* Named searches allow to quickly reconfigure the decoder */
    private   String MENU_SEARCH = "menu";

    /* Keyword we are looking for to activate menu */
    private static final String KEYPHRASE = "oh mighty computer";

    /* Used to handle permission request */
    private static final int PERMISSIONS_REQUEST_RECORD_AUDIO = 1;
    private static final int PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE = 2;

    private SpeechRecognizer recognizer;
    private HashMap<String, Integer> captions;
    private AssetManager am;
    String rawFolderPath = Environment.getExternalStorageDirectory() + "/rawAudio/";
    private String TAG = "SpeechListener";

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_main2);

        File rawFolder = new File(rawFolderPath);
        if(!rawFolder.isDirectory()) {
            rawFolder.mkdirs();
        }


        runRecognizerSetup();


        am = getAssets();


        Button again = (Button) findViewById(R.id.again);
        again.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    convertToSpeech(am.open("again.mp3"));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });




        Button horas = (Button) findViewById(R.id.horas);
        horas.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    convertToSpeech(am.open("horas.mp3"));

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });




        Button inicio_da_pausa = (Button) findViewById(R.id.inicio_da_pausa);
        inicio_da_pausa.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    convertToSpeech(am.open("inicio_da_pausa.mp3"));

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });




        Button gravar = (Button) findViewById(R.id.gravar);
        gravar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
               recognizer.startListening(MENU_SEARCH, 4000);
            }
        });








//        captions = new HashMap<String, Integer>();
//        // Check if user has given permission to record audio
//        int permissionCheck = ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.RECORD_AUDIO);
//        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
//            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, PERMISSIONS_REQUEST_RECORD_AUDIO);
//            return;
//        }
//
//        permissionCheck = ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE);
//        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
//            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE);
//            return;
//        }
//        runRecognizerSetup();
    }

    private void runRecognizerSetup() {
        // Recognizer initialization is a time-consuming and it involves IO,
        // so we execute it in async task
        new AsyncTask<Void, Void, Exception>() {
            @Override
            protected Exception doInBackground(Void... params) {
                try {
                    Assets assets = new Assets(MainActivity.this);
                    File assetDir = assets.syncAssets();
                    setupRecognizer(assetDir);
                } catch (IOException e) {
                    return e;
                }
                return null;
            }

            @Override
            protected void onPostExecute(Exception result) {
                if (result != null) {
                }
            }
        }.execute();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSIONS_REQUEST_RECORD_AUDIO) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                runRecognizerSetup();
            } else {
                finish();
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (recognizer != null) {
            recognizer.cancel();
            recognizer.shutdown();
        }
    }

    /**
     * In partial result we get quick updates about current hypothesis. In
     * keyword spotting mode we can react here, in other modes we need to wait
     * for final result in onResult.
     */
    @Override
    public void onPartialResult(Hypothesis hypothesis) {
        if (hypothesis == null)
            return;

        String text = hypothesis.getHypstr();

        Log.d(TAG, "onPartialResult: " + text);

//        if (text.equals(KEYPHRASE))
//            switchSearch(MENU_SEARCH);
//        else if (text.equals(DIGITS_SEARCH))
//            switchSearch(DIGITS_SEARCH);
//        else if (text.equals(PHONE_SEARCH))
//            switchSearch(PHONE_SEARCH);
//        else if (text.equals(FORECAST_SEARCH))
//            switchSearch(FORECAST_SEARCH);
    }

    /**
     * This callback is called when we stop the recognizer.
     */
    @Override
    public void onResult(Hypothesis hypothesis) {
        String text = null;
        if (hypothesis != null) {
            text = hypothesis.getHypstr();
        }
        Log.d(TAG, "onResult: " + text);
    }

    @Override
    public void onBeginningOfSpeech() {
        Log.d(TAG, "BeginningOfSpeech: ");

    }

    /**
     * We stop recognizer here to get a final result
     */
    @Override
    public void onEndOfSpeech() {
        Log.d(TAG, "EndOfSpeech: ");

        if (!recognizer.getSearchName().equals(KWS_SEARCH));
//            switchSearch(KWS_SEARCH);
    }

//    private void switchSearch(String searchName) {
//        recognizer.stop();
//
//        // If we are not spotting, start listening with timeout (10000 ms or 10 seconds).
//        if (searchName.equals(KWS_SEARCH))
//            recognizer.startListening(searchName);
//        else
//            recognizer.startListening(searchName, 10000);
//
////        String caption = getResources().getString(captions.get(searchName));
//    }

    private void setupRecognizer(File assetsDir) throws IOException {
        // The recognizer can be configured to perform multiple searches
        // of different kind and switch between them



        recognizer = SpeechRecognizerSetup.defaultSetup()
                .setAcousticModel(new File(assetsDir, "en-us-ptm"))
                .setDictionary(new File(assetsDir, "cmudict-en-us.dict"))
                .setRawLogDir(new File(rawFolderPath)) // To disable logging of raw audio comment out this call (takes a lot of space on the device)

                .getRecognizer();
        recognizer.addListener(this);

        /** In your application you might not need to add all those searches.
         * They are added here for demonstration. You can leave just one.
         */

        // Create keyword-activation search.
        recognizer.addKeyphraseSearch(KWS_SEARCH, KEYPHRASE);

        // Create grammar-based search for selection between demos
//        File menuGrammar = new File(assetsDir, "menu.gram");
//        recognizer.addGrammarSearch(MENU_SEARCH, menuGrammar);
//
        // Create grammar-based search for digit recognition
//        File digitsGrammar = new File(assetsDir, "digits.gram");
//        recognizer.addGrammarSearch(DIGITS_SEARCH, digitsGrammar);

        // Create language model search
//        File languageModel = new File(assetsDir, "weather.dmp");
//        recognizer.addNgramSearch(FORECAST_SEARCH, languageModel);

        // Phonetic search
        MENU_SEARCH = PHONE_SEARCH;
        File phoneticModel = new File(assetsDir, "en-phone.dmp");
        recognizer.addAllphoneSearch(PHONE_SEARCH, phoneticModel);
    }

    @Override
    public void onError(Exception error) {
    }

    @Override
    public void onTimeout() {
        Log.d(TAG, "onTimeout: ");
        recognizer.startListening(MENU_SEARCH, 5000);

    }

    //convert an inputstream to text
    public void convertToSpeech(final InputStream stream){
        new AsyncTask<Void, Void, Exception>() {
            @Override
            protected Exception doInBackground(Void... params) {
                try {
                    Assets assets = new Assets(MainActivity.this);
                    File assetsDir = assets.syncAssets();
                    Config c = Decoder.defaultConfig();
                    c.setString("-hmm", new File(assetsDir, "en-us-ptm").getPath());
                    c.setString("-dict", new File(assetsDir, "cmudict-en-us.dict").getPath());
                    c.setBoolean("-allphone_ci", true);
                    c.setString("-lm", new File(assetsDir, "en-phone.dmp").getPath());
                    Decoder d = new Decoder(c);

                    d.startUtt();
                    byte[] b = new byte[4096];
                    try {
                        int nbytes;
                        while ((nbytes = stream.read(b)) >= 0) {
                            ByteBuffer bb = ByteBuffer.wrap(b, 0, nbytes);

                            // Not needed on desktop but required on android
                            bb.order(ByteOrder.LITTLE_ENDIAN);

                            short[] s = new short[nbytes/2];
                            bb.asShortBuffer().get(s);
                            d.processRaw(s, nbytes/2, false, false);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                        System.out.print("ERROR");
                    }
                    d.endUtt();
                    for (Segment seg : d.seg()) {
                        Log.d("converToSpeech", "WORD: " + seg.getWord());
                        //do something with the result here
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return null;
            }

        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }
}
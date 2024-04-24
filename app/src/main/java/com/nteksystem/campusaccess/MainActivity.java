package com.nteksystem.campusaccess;

import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.nfc.NfcAdapter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;

public class MainActivity extends AppCompatActivity {
    private TextView tvbtnmanualtype;
    private StringBuilder enteredNumbers = new StringBuilder();
    private GridLayout gridnumpad;
    private EditText etidcode, etscanneddata;

    private Button btnclear, btncontinue;

    private NfcAdapter nfcAdapter;
    private LinearLayout linearmanual, linearformclose, linearbtnlogout, linearprogress;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        tvbtnmanualtype = findViewById(R.id.tvBtnForm);
        linearmanual = findViewById(R.id.linearManualForm);
        gridnumpad = findViewById(R.id.gridNumberPad);
        etidcode = findViewById(R.id.etRfidCode);
        linearformclose = findViewById(R.id.linearButtonClose);
        btnclear = findViewById(R.id.btnClear);
        etscanneddata = findViewById(R.id.etScannedRfid);
        linearbtnlogout = findViewById(R.id.linearButtonLogout);
        btncontinue = findViewById(R.id.btnContinue);
        linearprogress = findViewById(R.id.linearProgressEntry);
        btncontinue.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String code = etidcode.getText().toString();

                if (code.isEmpty()){
                    showWarningDialog(MainActivity.this);
                }else {
                    showSelectorDialog(MainActivity.this);
                }
            }
        });

        linearbtnlogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showLogoutWarning();
            }
        });
        nfcAdapter =NfcAdapter.getDefaultAdapter(this);

        btnclear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                etidcode.setText("");
            }
        });

        tvbtnmanualtype.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleManualFormVisibility();
            }
        });

        linearformclose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleManualFormVisibility();
            }
        });

        for(int i = 0; i < gridnumpad.getChildCount(); i++){
            View child = gridnumpad.getChildAt(i);

            if (child instanceof Button){
                Button button = (Button) child;
                button.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        onButtonClick((Button) v);
                    }
                });
            }
        }

        if (nfcAdapter == null){
            Toast.makeText(this, "NFC is not supported onn this device!, please check", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
    }


    private void toggleManualFormVisibility() {
        if (linearmanual.getVisibility() == View.GONE) {
            int[] location = new int[2];
            tvbtnmanualtype.getLocationOnScreen(location);
            int startY = location[1] + tvbtnmanualtype.getHeight();

            linearmanual.setVisibility(View.VISIBLE);
            Animation slideUp = new TranslateAnimation(0, 0, startY, 0);
            slideUp.setDuration(500);
            slideUp.setFillAfter(true);
            linearmanual.startAnimation(slideUp);
        } else {
            Animation slideDown = new TranslateAnimation(0, 0, 0, linearmanual.getHeight());
            slideDown.setDuration(500);
            slideDown.setFillAfter(true);
            slideDown.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {}

                @Override
                public void onAnimationEnd(Animation animation) {
                    linearmanual.setVisibility(View.GONE);
                }

                @Override
                public void onAnimationRepeat(Animation animation) {}
            });
            linearmanual.startAnimation(slideDown);
        }
    }


    public void onButtonClick(Button button){

        onNumericButtonClick(button);
    }

    public void onNumericButtonClick(Button button) {
        String buttonText = button.getText().toString();
        String currentText = etidcode.getText().toString();
        etidcode.setText(currentText + buttonText);
    }


    @Override
    protected void onResume() {
        super.onResume();
        enabledNfc();
    }

    @Override
    protected void onPause() {
        super.onPause();
        disableNfc();
    }

    private void enabledNfc(){
        if (nfcAdapter != null){
            Intent intent =new Intent(this, getClass());
            intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);

            PendingIntent pendingIntent = PendingIntent.getActivity(
                    this, 0, intent, 0);

            nfcAdapter.enableForegroundDispatch(this, pendingIntent, null, null);
        }
    }

    private void disableNfc() {
        if (nfcAdapter != null) {
            nfcAdapter.disableForegroundDispatch(this);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(intent.getAction())) {
            byte[] idBytes = intent.getByteArrayExtra(NfcAdapter.EXTRA_ID);

            long decimalRfidValue = bytesToDecimalValue(idBytes);

            etscanneddata.setText(String.valueOf(decimalRfidValue));
            String code = etscanneddata.getText().toString();
            if (code.isEmpty()){
                showWarningDialog(MainActivity.this);
            }else {
                showSelectorDialog(MainActivity.this);
            }
        }

    }

    private void showSelectorDialog(Context context){
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(context);
        View dialogView = getLayoutInflater().inflate(R.layout.action_selector, null);

        builder.setView(dialogView);
        Intent intent = getIntent();
        Button btnshowprofile = dialogView.findViewById(R.id.btnProfile);
        Button btnverified = dialogView.findViewById(R.id.btnVerifiedClient);
        Button btnok = dialogView.findViewById(R.id.btnDialogClose);


        btnshowprofile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String code = etscanneddata.getText().toString();

                new HttpRequestTaskCheck().execute(code);
            }
        });
        btnverified.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String code = etscanneddata.getText().toString();
                new HttpRequestTask().execute(code);
            }
        });


        AlertDialog alertDialog = builder.create();
        alertDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        alertDialog.show();

        btnok.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (alertDialog.isShowing()) {
                    alertDialog.dismiss();
                }
            }
        });
    }

    private long bytesToDecimalValue(byte[] bytes) {
        long decimalValue = 0;
        for (byte b : bytes) {
            decimalValue = (decimalValue << 8) | (b & 0xFF);
        }
        return decimalValue;
    }
    private void showLogoutWarning() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.warning_dialog, null);

        builder.setView(dialogView);
        AlertDialog alertDialog = builder.create();
        alertDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        TextView tvMessage = dialogView.findViewById(R.id.tvMessage);
        Button btnDialogClose = dialogView.findViewById(R.id.btnDialogClose);

        tvMessage.setText("Are you sure you want to logout?");
        btnDialogClose.setText("Yes!");

        btnDialogClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                logoutSession();
                alertDialog.dismiss();
            }
        });

        alertDialog.show();
    }

    private void showWarningDialog(Context context){
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(context);
        View dialogView = getLayoutInflater().inflate(R.layout.warning_dialog, null);

        builder.setView(dialogView);
        Intent intent = getIntent();

        TextView tvmessage = dialogView.findViewById(R.id.tvMessage);

        tvmessage.setText("Please provide identification card code!");

        AlertDialog alertDialog = builder.create();
        alertDialog.show();

    }

    private class HttpRequestTaskCheck extends AsyncTask<String, Void, String>{

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            linearprogress.setVisibility(View.VISIBLE);
        }

        @Override
        protected String doInBackground(String... params) {
            String code = params[0];
            String apiUrl ="http://192.168.1.154/jhctc/API/verifyRecord?idcode=" + code;

            try {
                URL url = new URL(apiUrl);
                HttpURLConnection urlConnection = (HttpURLConnection)url.openConnection();

                try {
                    InputStream in = urlConnection.getInputStream();
                    BufferedReader reader =new BufferedReader(new InputStreamReader(in));
                    StringBuilder stringBuilder = new StringBuilder();

                    String line;
                    while ((line = reader.readLine()) != null){
                        stringBuilder.append(line).append("\n");
                    }

                    return stringBuilder.toString();
                }finally {
                    urlConnection.disconnect();
                }
            }catch (IOException e){
                e.printStackTrace();
                return null;
            }
        }

        @Override
        protected void onPostExecute(String result) {
            if (result != null){
                handleJsonDataCheck(result);
                linearprogress.setVisibility(View.GONE);
            }else {
                errorDialog(MainActivity.this);
                linearprogress.setVisibility(View.GONE);

            }
        }
    }

    private class HttpRequestTask extends AsyncTask<String, Void, String>{

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            linearprogress.setVisibility(View.VISIBLE);
        }

        @Override
        protected String doInBackground(String... params) {
            String code = params[0];
            String apiUrl ="http://192.168.1.154/jhctc/API/entry?idcode=" + code;

            try {
                URL url = new URL(apiUrl);
                HttpURLConnection urlConnection = (HttpURLConnection)url.openConnection();

                try {
                    InputStream in = urlConnection.getInputStream();
                    BufferedReader reader =new BufferedReader(new InputStreamReader(in));
                    StringBuilder stringBuilder = new StringBuilder();

                    String line;
                    while ((line = reader.readLine()) != null){
                        stringBuilder.append(line).append("\n");
                    }

                    return stringBuilder.toString();
                }finally {
                    urlConnection.disconnect();
                }
            }catch (IOException e){
                e.printStackTrace();
                return null;
            }
        }

        @Override
        protected void onPostExecute(String result) {
            if (result != null){
                handleJsonData(result);
                linearprogress.setVisibility(View.GONE);
            }else {
                errorDialog(MainActivity.this);
                linearprogress.setVisibility(View.GONE);

            }
        }
    }

    private void handleJsonData(String jsonString){
        try {
            JSONObject jsonObject = new JSONObject(jsonString);

            if (jsonObject.has("status")){
                String status = jsonObject.getString("status");

                String serverurl = "http://192.168.1.154/jhctc/";


                if (status.equals("success")){
                    if (jsonObject.has("information")){
                        JSONObject clientObject = jsonObject.getJSONObject("information");
                        String position = clientObject.getString("type");

                        if (position.equals("Student")){
                            String username = clientObject.getString("username");
                            String clientpos = clientObject.getString("type");
                            String rfidcode = clientObject.getString("rfid");
                            String idnumber = clientObject.getString("id_number");
                            String department = clientObject.getString("department");
                            String entrytime = clientObject.getString("in_time");
                            String image = clientObject.getString("image");
                            String fullimage = serverurl + image;
                            showClientInformationDialog(username, clientpos, rfidcode, idnumber, department, entrytime, fullimage);

                        }else if (position.equals("Faculty")){
                            String username = clientObject.getString("username");
                            String clientpos = clientObject.getString("type");
                            String rfidcode = clientObject.getString("rfid");
                            String idnumber = clientObject.getString("id_number");
                            String department = clientObject.getString("department");
                            String entrytime = clientObject.getString("in_time");
                            String image = clientObject.getString("image");
                            String fullimage = serverurl + image;
                            showClientInformationDialog(username, clientpos, rfidcode, idnumber, department, entrytime, fullimage);

                        }else if (position.equals("Staff")){
                            String username = clientObject.getString("username");
                            String clientpos = clientObject.getString("type");
                            String rfidcode = clientObject.getString("rfid");
                            String idnumber = clientObject.getString("id_number");
                            String department = clientObject.getString("department");
                            String entrytime = clientObject.getString("in_time");
                            String image = clientObject.getString("image");
                            String fullimage = serverurl + image;
                            showClientInformationDialog(username, clientpos, rfidcode, idnumber, department, entrytime, fullimage);

                        }else {
                            String username = clientObject.getString("username");
                            String clientpos = clientObject.getString("type");
                            String rfidcode = clientObject.getString("rfid");
                            String entrytime = clientObject.getString("in_time");
                            String image = clientObject.getString("image");
                            String fullimage = serverurl + image;
                            showGuestInformationDialog(username, clientpos, rfidcode, entrytime, fullimage);
                        }
                    }
                }else if (status.equals("error")){
                    errorDialog(MainActivity.this);
                }else if (status.equals("blocked")) {
                    blockedDialog(MainActivity.this);
                }else if (status.equals("failed")){
                    failedDialog(MainActivity.this);
                }else {
                    warningDialog(MainActivity.this);
                }
            }
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    private void handleJsonDataCheck(String jsonString){
        try {
            JSONObject jsonObject = new JSONObject(jsonString);

            if (jsonObject.has("status")){
                String status = jsonObject.getString("status");

                String serverurl = "http://192.168.1.154/jhctc/";


                if (status.equals("success")){
                    if (jsonObject.has("information")){
                        JSONObject clientObject = jsonObject.getJSONObject("information");
                        String position = clientObject.getString("type");

                        if (position.equals("Student")){
                            String username = clientObject.getString("username");
                            String clientpos = clientObject.getString("type");
                            String rfidcode = clientObject.getString("rfid");
                            String idnumber = clientObject.getString("id_number");
                            String department = clientObject.getString("department");
                            String entrytime = clientObject.getString("in_time");
                            String image = clientObject.getString("image");
                            String fullimage = serverurl + image;
                            showClientInformationDialog(username, clientpos, rfidcode, idnumber, department, entrytime, fullimage);

                        }else if (position.equals("Faculty")){
                            String username = clientObject.getString("username");
                            String clientpos = clientObject.getString("type");
                            String rfidcode = clientObject.getString("rfid");
                            String idnumber = clientObject.getString("id_number");
                            String department = clientObject.getString("department");
                            String entrytime = clientObject.getString("in_time");
                            String image = clientObject.getString("image");
                            String fullimage = serverurl + image;
                            showClientInformationDialog(username, clientpos, rfidcode, idnumber, department, entrytime, fullimage);

                        }else if (position.equals("Staff")){
                            String username = clientObject.getString("username");
                            String clientpos = clientObject.getString("type");
                            String rfidcode = clientObject.getString("rfid");
                            String idnumber = clientObject.getString("id_number");
                            String department = clientObject.getString("department");
                            String entrytime = clientObject.getString("in_time");
                            String image = clientObject.getString("image");
                            String fullimage = serverurl + image;
                            showClientInformationDialog(username, clientpos, rfidcode, idnumber, department, entrytime, fullimage);

                        }else {
                            String username = clientObject.getString("username");
                            String clientpos = clientObject.getString("type");
                            String rfidcode = clientObject.getString("rfid");
                            String entrytime = clientObject.getString("in_time");
                            String image = clientObject.getString("image");
                            String fullimage = serverurl + image;
                            showGuestInformationDialog(username, clientpos, rfidcode, entrytime, fullimage);
                        }
                    }
                }else if (status.equals("error")){
                    errorDialog(MainActivity.this);
                }else if (status.equals("blocked")) {
                    blockedDialog(MainActivity.this);
                }else if (status.equals("failed")){
                    failedDialog(MainActivity.this);
                }else {
                    warningDialog(MainActivity.this);
                }
            }
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    private void showClientInformationDialog(String username, String clientpos, String rfidcode, String idnumber, String department, String entrytime, String imageUrl) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.client_information, null);

        builder.setView(dialogView);
        Intent intent = getIntent();
        TextView usernameTextView = dialogView.findViewById(R.id.tvUsername);
        TextView positionTextView = dialogView.findViewById(R.id.tvPosition);
        TextView rfidTextView = dialogView.findViewById(R.id.tvRfidCode);
        TextView idNumberTextView = dialogView.findViewById(R.id.tvStudentId);
        TextView departmentTextView = dialogView.findViewById(R.id.tvDepartment);
        TextView entryTimeTextView = dialogView.findViewById(R.id.tvEntryTime);
        TextView tvlabel = dialogView.findViewById(R.id.tvLabel);
        ImageView imgprofile = dialogView.findViewById(R.id.imgProfile);

        usernameTextView.setText(username);
        positionTextView.setText(clientpos);
        rfidTextView.setText(rfidcode);
        idNumberTextView.setText(idnumber);
        departmentTextView.setText(department);
        entryTimeTextView.setText(entrytime);
        tvlabel.setText("Entry Time");
        new LoadImageTask(imgprofile).execute(imageUrl);

        AlertDialog alertDialog = builder.create();
        alertDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        alertDialog.show();

        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (alertDialog.isShowing()) {
                    alertDialog.dismiss();
                }
            }
        }, 5000);
    }

    private void showGuestInformationDialog(String username, String clientpos, String rfidcode, String entrytime, String imageUrl) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.guest_information, null);

        builder.setView(dialogView);
        Intent intent = getIntent();
        TextView usernameTextView = dialogView.findViewById(R.id.tvUsername);
        TextView positionTextView = dialogView.findViewById(R.id.tvPosition);
        TextView rfidTextView = dialogView.findViewById(R.id.tvRfidCode);
        TextView entryTimeTextView = dialogView.findViewById(R.id.tvEntryTime);
        TextView tvlabel = dialogView.findViewById(R.id.tvLabel);
        ImageView imgprofile = dialogView.findViewById(R.id.imgProfile);

        usernameTextView.setText(username);
        positionTextView.setText(clientpos);
        rfidTextView.setText(rfidcode);
        entryTimeTextView.setText(entrytime);
        tvlabel.setText("Entry Time");
        new LoadImageTask(imgprofile).execute(imageUrl);

        AlertDialog alertDialog = builder.create();
        alertDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        alertDialog.show();

        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (alertDialog.isShowing()) {
                    alertDialog.dismiss();
                }
            }
        }, 5000);
    }

    private static class LoadImageTask extends AsyncTask<String, Void, Bitmap> {
        private final WeakReference<ImageView> imageViewReference;

        LoadImageTask(ImageView imageView) {
            imageViewReference = new WeakReference<>(imageView);
        }

        @Override
        protected Bitmap doInBackground(String... strings) {
            String imageUrl = strings[0];
            try {
                URL url = new URL(imageUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setDoInput(true);
                connection.connect();
                InputStream input = connection.getInputStream();
                return BitmapFactory.decodeStream(input);
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            if (imageViewReference != null && bitmap != null) {
                ImageView imageView = imageViewReference.get();
                if (imageView != null) {
                    imageView.setImageBitmap(bitmap);
                }
            }
        }
    }

    private void failedDialog(Context context){
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(context);
        View dialogView = getLayoutInflater().inflate(R.layout.warning_dialog, null);

        builder.setView(dialogView);
        Intent intent = getIntent();

        TextView tvmessage = dialogView.findViewById(R.id.tvMessage);

        tvmessage.setText("This client has already record. and cannot do duplicate entry");

        AlertDialog alertDialog = builder.create();
        alertDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        alertDialog.show();

        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (alertDialog.isShowing()) {
                    alertDialog.dismiss();
                }
            }
        }, 3000);
    }
    private void errorDialog(Context context){
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(context);
        View dialogView = getLayoutInflater().inflate(R.layout.warning_dialog, null);

        builder.setView(dialogView);
        Intent intent = getIntent();

        TextView tvmessage = dialogView.findViewById(R.id.tvMessage);

        tvmessage.setText("Sorry! this client is not registered to enter the campus!");

        AlertDialog alertDialog = builder.create();
        alertDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        alertDialog.show();

        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (alertDialog.isShowing()) {
                    alertDialog.dismiss();
                }
            }
        }, 3000);
    }

    private void warningDialog(Context context){
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(context);
        View dialogView = getLayoutInflater().inflate(R.layout.warning_dialog, null);

        builder.setView(dialogView);
        Intent intent = getIntent();

        TextView tvmessage = dialogView.findViewById(R.id.tvMessage);

        tvmessage.setText("Please check connection to server!");

        AlertDialog alertDialog = builder.create();
        alertDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        alertDialog.show();

        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (alertDialog.isShowing()) {
                    alertDialog.dismiss();
                }
            }
        }, 3000);
    }
    private void blockedDialog(Context context){
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(context);
        View dialogView = getLayoutInflater().inflate(R.layout.warning_dialog, null);

        builder.setView(dialogView);
        Intent intent = getIntent();

        TextView tvmessage = dialogView.findViewById(R.id.tvMessage);

        tvmessage.setText("This user has been blocked.");

        AlertDialog alertDialog = builder.create();
        alertDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        alertDialog.show();

        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (alertDialog.isShowing()) {
                    alertDialog.dismiss();
                }
            }
        }, 3000);
    }
    private void logoutSession() {
        SharedPreferences sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove("device_setup");
        editor.remove("user_logged_in");
        editor.apply();

        Intent intent = new Intent(this, WelcomeScreen.class);
        startActivity(intent);
        finish();
    }



}
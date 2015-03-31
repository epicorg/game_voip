package com.gamevoip.epicorg.gamevoip;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

import com.google.android.gms.internal.lo;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.util.HashMap;

import communication.CommunicationManager;
import interaction.CustomAlertDialog;
import communication.ServerCommunicationReciver;
import data.LoginData;

/**
 * A login screen that offers login via email/password.
 */
public class LoginActivity extends Activity{

    /**
     * Keep track of the login task to ensure we can cancel it if requested.
     */
    private UserLoginTask mAuthTask = null;
    private LoginActivity thisActivity = this;
    private CommunicationManager communicationManager;
    private HashMap<Integer,View> views = new HashMap<Integer, View>();
    private SharedPreferences myPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        //inizializzazione communcaztionManager
        communicationManager = CommunicationManager.getInstance();
        //communicationManager.init();
        //communicationManager.setContext(this.getApplicationContext());
        views.put(R.id.username,(TextView) findViewById(R.id.username));
        views.put(R.id.password,(TextView)findViewById(R.id.password));
        views.put(R.id.login_form,findViewById(R.id.login_form));
        views.put(R.id.login_progress,findViewById(R.id.login_progress));

        myPreferences = getPreferences(MODE_PRIVATE);
        if(myPreferences.getBoolean("Remember", false)){
            ((TextView)views.get(R.id.username)).setText(myPreferences.getString("Username","user"));
            ((TextView)views.get(R.id.password)).setText(myPreferences.getString("Password", "pass"));
            Log.d("USER_REMEMBER", myPreferences.getString("Username","user"));
            Log.d("PASS_REMEMBER", myPreferences.getString("Password","pass"));
            attemptLogin(findViewById(R.id.login));
        }
    }

    public void notRegistered(View view){

        Intent intent = new Intent(this, RegistrationActivity.class);
        startActivity(intent);
    }

    public void rememberMe(View view){
        CheckBox rememberBox = (CheckBox) findViewById(R.id.remeberMeBox);
        myPreferences = getPreferences(MODE_PRIVATE);
        SharedPreferences.Editor editor = myPreferences.edit();
        Log.d("REMEMBER_ME", String.valueOf(rememberBox.isChecked()));
        if(rememberBox.isChecked())
            editor.putBoolean("Remember" , true);
        else
            editor.putBoolean("Remember", false);
        editor.commit();
    }

    public void attemptLogin(View view) {
        if (mAuthTask != null) {
            return;
        }

        ((TextView)views.get(R.id.username)).setError(null);
        ((TextView)views.get(R.id.password)).setError(null);
        LoginData loginData = getData();
        boolean cancel = loginData.checkData(getApplicationContext(), views);

        if (cancel) {
            //non fare il login
        } else {
            showProgress(true);
            mAuthTask = new UserLoginTask(loginData);
            mAuthTask.execute((Void) null);
        }
    }

    private LoginData getData(){
        String username =  ((TextView)views.get(R.id.username)).getText().toString();
        String password =  ((TextView)views.get(R.id.password)).getText().toString();
        return new LoginData(username,password);
    }

    /**
     * Mostra una barra di caricamento e nasconde i campi riempiti
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    public void showProgress(final boolean show) {
        final TextView mLoginFormView =  (TextView)views.get(R.id.login_form);
        final TextView mProgressView =  (TextView)views.get(R.id.login_progress);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

            mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
            mLoginFormView.animate().setDuration(shortAnimTime).alpha(
                    show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
                }
            });

            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mProgressView.animate().setDuration(shortAnimTime).alpha(
                    show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
                }
            });
        } else {
            // The ViewPropertyAnimator APIs are not available, so simply show
            // and hide the relevant UI components.
            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }
    /**
     * Task asincrono di Login nel quale viene gestita a logica di comunicazione col server
     */
    public class UserLoginTask extends AsyncTask<Void, Void, Boolean> {

        private LoginData loginData;
        private String error;

        public UserLoginTask(LoginData loginData) {
            this.loginData = loginData;
        }

        @Override
        protected Boolean doInBackground(Void... params) {

            System.out.println(loginData.getUsername());
            System.out.println(loginData.getPassword());

            JSONObject loginRequest = new JSONObject();
            try {
                Socket socket = new Socket(InetAddress.getByName("192.168.1.4"), 7007);
                PrintWriter printWriter = new PrintWriter(socket.getOutputStream(),true);
                loginRequest.put("service", "LOGIN");
                loginRequest.put("username", loginData.getUsername());
                loginRequest.put("password", loginData.getPassword());
                printWriter.println(loginRequest.toString());
                Log.d("Richiesta",loginRequest.toString());
                BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                JSONObject response = new JSONObject(reader.readLine());
                Log.d("Risposta", response.toString());

                reader.close();
                boolean value = response.getBoolean("value");
                if(!value){
                    error = response.getString("Description");
                }
                return value;

            } catch (IOException e) {
                e.printStackTrace();
                error = getString(R.string.error_server_unreachable);
                return false;
            } catch (JSONException e) {
                e.printStackTrace();
                error = getString(R.string.error_communication);
                return false;
            }
        }

        @Override
        protected void onPostExecute(final Boolean success) {
            mAuthTask = null;
            if (success) {
                if(myPreferences.getBoolean("Remember",false)) {
                    SharedPreferences.Editor editor = myPreferences.edit();
                    editor.putString("Username", loginData.getUsername());
                    editor.putString("Password", loginData.getPassword());
                    editor.commit();
                    Log.d("REMEMBER", "fields saved");
                }
                Intent intent = new Intent(thisActivity, CallActivity.class);
                intent.putExtra("Username", loginData.getUsername());
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
            } else {
                showErrorDialog();
                showProgress(false);

            }
            showProgress(false);
        }

        private void showErrorDialog() {
            new CustomAlertDialog(getString(R.string.dialog_error)
                    ,error, getString(R.string.dialog_try_again),
                    thisActivity).show();
        }

        @Override
        protected void onCancelled() {
            mAuthTask = null;
            showProgress(false);
        }
    }
}

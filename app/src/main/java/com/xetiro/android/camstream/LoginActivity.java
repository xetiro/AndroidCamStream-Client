package com.xetiro.android.camstream;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.xetiro.android.camstream.network.ServerClient;
import com.xetiro.android.camstream.network.ServerResultCallback;

/**
 * This activity is prompts the user to login with the server. After successfully login, the user
 * continues to the MainActivity where the camera will start streaming to the server.
 * <p>
 * Created by xetiro (aka Ruben Geraldes) on 2020/08/31.
 */
public class LoginActivity extends AppCompatActivity implements ServerResultCallback  {
    private static String TAG = "LoginActivityDebug";

    private TextView mUsernameTextView;
    private TextView mPasswordTextView;
    private TextView mServerIpTextView;
    private TextView mServerPortTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Grab the textviews to later get the text contents when the user press login
        mUsernameTextView = findViewById(R.id.editTextUsername);
        mPasswordTextView = findViewById(R.id.editTextPassword);
        mServerIpTextView = findViewById(R.id.editTextServerIp);
        mServerPortTextView = findViewById(R.id.editTextServerPort);

        // Set the callback listener for the Login button
        Button loginButton = findViewById(R.id.loginButton);
        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Grab user info and try to login with the server
                String username = mUsernameTextView.getText().toString();
                String password = mPasswordTextView.getText().toString();
                String serverIp = mServerIpTextView.getText().toString();
                String serverPort = mServerPortTextView.getText().toString();
                Log.d(TAG, "username = " + username + " | password = " + password + " | ip = " + serverIp + " | port = " + serverPort);
                login(username, password, serverIp, serverPort);
            }
        });
    }

    private void login(String username, String password, String serverIp, String serverPort) {
        // Very basic sanity check. Not robust enough to use outside the scope of prototype experiment.
        if(username.length() > 0 && password.length() > 0 && serverIp.length() > 0 && serverPort.length() > 0) {
            ServerClient clientInstance = ServerClient.getInstance();
            int port = Integer.parseInt(serverPort);
            clientInstance.init(username, password, serverIp, port);
            clientInstance.connect();
        } else {
            Toast invalidLogin = Toast.makeText(this, R.string.toast_invalid_login, Toast.LENGTH_LONG);
            invalidLogin.setGravity(Gravity.CENTER, 0, 0);
            invalidLogin.show();
        }
    }

    @Override
    public void onResume() {
        Log.d(TAG, "onResume");
        super.onResume();
        ServerClient.getInstance().registerCallback(this);
    }

    @Override
    public void onPause() {
        Log.d(TAG, "onPause");
        super.onPause();
        ServerClient.getInstance().unregisterCallback();
    }

    public void onConnected(boolean success) {
        Log.d(TAG, "ServerResultCallback-onConnected: " + success);
        if(success) {
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
        } else {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast toast = Toast.makeText(getApplicationContext(), R.string.toast_authentication_failed, Toast.LENGTH_LONG);
                    toast.setGravity(Gravity.CENTER, 0, 0);
                    toast.show();
                }
            });

        }
    }
}

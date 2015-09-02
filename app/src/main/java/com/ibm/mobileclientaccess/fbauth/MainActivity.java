/*
   Copyright 2015 IBM Corp.
    Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at
        http://www.apache.org/licenses/LICENSE-2.0
    Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
package com.ibm.mobileclientaccess.fbauth;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.ibm.mobileclientaccess.clientsdk.android.auth.facebook.MCAFacebookAuthenticationManager;
import com.ibm.mobilefirstplatform.clientsdk.android.core.api.BMSClient;
import com.ibm.mobilefirstplatform.clientsdk.android.core.api.Response;
import com.ibm.mobilefirstplatform.clientsdk.android.core.api.ResponseListener;
import com.ibm.mobilefirstplatform.clientsdk.android.security.api.AuthorizationManager;

import org.json.JSONObject;

import java.net.MalformedURLException;

public class MainActivity extends Activity implements
        View.OnClickListener,
//        AuthenticationContext,
        ResponseListener
{

    private TextView info;
//    private LoginButton loginButton;

    private Button connectBtn;

   // private static final String ACCESS_TOKEN_KEY = "accessToken";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

//        FacebookSdk.sdkInitialize(getApplicationContext());
//        callbackManager = CallbackManager.Factory.create();

        setContentView(R.layout.activity_main);

        info = (TextView)findViewById(R.id.info);
        connectBtn = (Button)findViewById(R.id.connect);
        connectBtn.setOnClickListener(this);
        findViewById(R.id.disconnect).setOnClickListener(this);

        try {
            BMSClient.getInstance().initialize(this, "http://ilans-mbp.haifa.ibm.com:9080","ilan1234");
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        AuthorizationManager.getInstance().obtainAuthorizationHeader(this, new ResponseListener() {
            @Override
            public void onSuccess(final Response response) {
                final TextView tmpInfo = info;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                    tmpInfo.setText("Connection Success!!!!");
                    }
                });
                Log.e("Cirill", "onSuccess");
            }

            @Override
            public void onFailure(Response response, Throwable t, JSONObject extendedInfo){
                if (response != null){
                    Log.e("onFailure Response", response.toString());
                }

                if (t != null){
                    Log.e("onFailure Exception",t.toString());
                }
                //Log.e("Cirill", "onFailure");
            }
        });
        // Register with default delegate
        MCAFacebookAuthenticationManager.getInstance().registerWithDefaultAuthenticationHandler(this);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        MCAFacebookAuthenticationManager.getInstance().onActivityResultCalled(requestCode, resultCode, data);
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.connect) {
            AuthorizationManager.getInstance().obtainAuthorizationHeader(this,this);
//            JSONObject obj = new JSONObject();
//            try {
//                obj.put("facebookAppId", "928371050557393");
//                MCAFacebookAuthenticationManager.getInstance().onAuthenticationChallengeReceived(this, obj);
//            } catch (JSONException e) {
//                e.printStackTrace();
//            }
        }
        else if (v.getId() == R.id.disconnect) {
            //TODO: do disconnect?
        }
    }

//    @Override
//    public void submitAuthenticationChallengeAnswer(JSONObject answer) {
//        try {
//            info.setText(answer.getString(ACCESS_TOKEN_KEY));
//        } catch (JSONException e) {
//            info.setText("Error");
//            e.printStackTrace();
//        }
//    }
//
//    @Override
//    public void submitAuthenticationChallengeSuccess() {
//        info.setText("submitAuthenticationChallengeSuccess called");
//    }
//
//    @Override
//    public void submitAuthenticationChallengeFailure(JSONObject info) {
//        this.info.setText("submitAuthenticationChallengeFailure called");
//    }

    //ResponseListener
    @Override
    public void onSuccess(Response response) {
        final TextView tmpInfo = this.info;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                tmpInfo.setText("ResponseListener::onSuccess called");
            }
        });
    }

    @Override
    public void onFailure(Response response, Throwable t, JSONObject extendedInfo) {
        final TextView tmpInfo = this.info;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                tmpInfo.setText("ResponseListener::onFailure called");
            }
        });
    }
}

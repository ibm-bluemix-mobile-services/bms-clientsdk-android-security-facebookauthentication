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
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.ibm.mobileclientaccess.clientsdk.android.auth.facebook.MCAFacebookAuthenticationManager;
import com.ibm.mobilefirstplatform.clientsdk.android.core.api.BMSClient;
import com.ibm.mobilefirstplatform.clientsdk.android.core.api.Response;
import com.ibm.mobilefirstplatform.clientsdk.android.core.api.ResponseListener;
import com.ibm.mobilefirstplatform.clientsdk.android.security.api.AuthorizationManager;

import org.json.JSONObject;

import java.net.MalformedURLException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class MainActivity extends Activity implements ResponseListener
{

    //private final String

    private final String backendRoute = "https://AsafAppUnAuth.stage1.mybluemix.net?subzone=dev";
    private final String backendGUID = "a23e3fed-b3e7-4bc6-8662-80fa1fac446f";

    private TextView infoTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        infoTextView = (TextView)findViewById(R.id.info);

        /*
            There may be issues with the hash key for the app, because it may not be correct when using from command line
            https://developers.facebook.com/docs/android/getting-started#release-key-hash (troubleshoot section)
            Add this code (and remove after getting the correct key (debug? release)) for this will print to log the correct hash code.
         */
        try {
            PackageInfo info = getPackageManager().getPackageInfo(
                    getPackageName(),
                    PackageManager.GET_SIGNATURES);
            for (Signature signature : info.signatures) {
                MessageDigest md = MessageDigest.getInstance("SHA");
                md.update(signature.toByteArray());
                Log.d("KeyHash:", Base64.encodeToString(md.digest(), Base64.DEFAULT));
            }
        } catch (PackageManager.NameNotFoundException e) {
        } catch (NoSuchAlgorithmException e) {
        }

        try {
            //Register to the server with backendroute and GUID
            BMSClient.getInstance().initialize(this, backendRoute,backendGUID);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        // Register the default delegate for Facebook
        MCAFacebookAuthenticationManager.getInstance().registerWithDefaultAuthenticationHandler(this);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        setStatus("Checking facebook login data...");
        MCAFacebookAuthenticationManager.getInstance().onActivityResultCalled(requestCode, resultCode, data);
    }

    //ResponseListener
    @Override
    public void onSuccess(Response response) {
        setStatus("Connected to Facebook - OK");

        final TextView viewById = (TextView) findViewById(R.id.user);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                viewById.setText("User: " + AuthorizationManager.getInstance().getUserIdentity().getDisplayName());
            }
        });
    }

    @Override
    public void onFailure(Response response, Throwable t, JSONObject extendedInfo) {
        setStatus("Connection to Facebook - Failed");
    }


    public void onLogin(View view){
        setStatus("Obtain Authorization Header...");
        AuthorizationManager.getInstance().obtainAuthorizationHeader(this, this);
    }

    private void setStatus(final String text){
        final TextView tmpInfo = this.infoTextView;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                tmpInfo.setText(text);
            }
        });
    }
}

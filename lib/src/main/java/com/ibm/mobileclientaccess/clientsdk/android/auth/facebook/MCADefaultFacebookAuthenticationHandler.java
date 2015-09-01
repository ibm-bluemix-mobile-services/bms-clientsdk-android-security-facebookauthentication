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
package com.ibm.mobileclientaccess.clientsdk.android.auth.facebook;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;

import java.util.Arrays;
import java.util.List;

/**
 * Created by iklein on 8/5/15.
 */
public class MCADefaultFacebookAuthenticationHandler implements
        MCAFacebookAuthentication
{

    private static String TAG = "FBOauth";
    private List<String> permissionNeeds = Arrays.asList("public_profile");
    private CallbackManager callbackmanager;

    private Context ctx;

    public MCADefaultFacebookAuthenticationHandler(Context ctx) {
        this.ctx = ctx;
        callbackmanager = CallbackManager.Factory.create();
    }

    @Override
    public void handleAuthentication(Context context, String appId) {
        // Verify that the app Id defined in the .plist file is identical to the one requested by the IMF server.
        if(!(appId.equals(FacebookSdk.getApplicationId()))) {
            MCAFacebookAuthenticationManager.getInstance().onFacebookAuthenticationFailure(null);
            return;
        }

        AccessToken accessToken = AccessToken.getCurrentAccessToken();
        // Use the Facebook SDK to login.
        if (accessToken != null && !accessToken.isExpired())
        {
            String token = accessToken.getToken();
            Log.d(TAG, "Token alerady available = " + token);
            MCAFacebookAuthenticationManager.getInstance().onFacebookAccessTokenReceived(token);
            return;
        }

        Log.d(TAG, "not loggedin, continue");

        if (context instanceof Activity) {
            // Set permissions
            LoginManager.getInstance().logInWithReadPermissions((Activity) context, permissionNeeds);

            LoginManager.getInstance().registerCallback(callbackmanager,
                    new FacebookCallback<LoginResult>() {
                        @Override
                        public void onSuccess(LoginResult loginResult) {
                            String token = AccessToken.getCurrentAccessToken().getToken();
                            MCAFacebookAuthenticationManager.getInstance().onFacebookAccessTokenReceived(token);
                        }

                        @Override
                        public void onCancel() {
                            Log.d(TAG, "On cancel");
                            MCAFacebookAuthenticationManager.getInstance().onFacebookAuthenticationFailure(null);
                        }

                        @Override
                        public void onError(FacebookException error) {
                            Log.d(TAG, error.toString());
                            MCAFacebookAuthenticationManager.getInstance().onFacebookAuthenticationFailure(null);
                        }
                    });
        }

        else{
            MCAFacebookAuthenticationManager.getInstance().onFacebookAuthenticationFailure(null);
        }
    }

    @Override
    public void onActivityResultCalled(int requestCode, int resultCode, Intent data) {
        callbackmanager.onActivityResult(requestCode, resultCode, data);
    }
}

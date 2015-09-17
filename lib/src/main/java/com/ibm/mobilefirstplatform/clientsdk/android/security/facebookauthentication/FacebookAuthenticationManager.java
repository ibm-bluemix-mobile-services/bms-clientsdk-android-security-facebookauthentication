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
package com.ibm.mobilefirstplatform.clientsdk.android.security.facebookauthentication;

import android.content.Context;
import android.content.Intent;

import com.facebook.CallbackManager;
import com.facebook.FacebookSdk;
import com.ibm.mobilefirstplatform.clientsdk.android.core.api.BMSClient;
import com.ibm.mobilefirstplatform.clientsdk.android.logger.api.Logger;
import com.ibm.mobilefirstplatform.clientsdk.android.security.api.AuthenticationContext;
import com.ibm.mobilefirstplatform.clientsdk.android.security.api.AuthenticationListener;

import org.json.JSONException;
import org.json.JSONObject;

public class FacebookAuthenticationManager implements
        AuthenticationListener
{
    private Logger logger;

    private FacebookAuthenticationListener facebookAuthenticationListener;

    private static final String FACEBOOK_REALM = "wl_facebookRealm";
    private static final String FACEBOOK_APP_ID_KEY = "facebookAppId";
    private static final String ACCESS_TOKEN_KEY = "accessToken";

    private CallbackManager fbCallbackmanager;

    //singelton
    private static final Object lock = new Object();
    private static volatile FacebookAuthenticationManager instance;
    private AuthenticationContext authContext;

    public static FacebookAuthenticationManager getInstance() {
        FacebookAuthenticationManager tempManagerInstance = instance;
        if (tempManagerInstance == null) {
            synchronized (lock) {    // While we were waiting for the lock, another
                tempManagerInstance = instance;        // thread may have instantiated the object.
                if (tempManagerInstance == null) {
                    tempManagerInstance = new FacebookAuthenticationManager();
                    instance = tempManagerInstance;
                }
            }
        }
        return tempManagerInstance;
    }

    /**
     * private constructor for singletons
     */
    private FacebookAuthenticationManager() {
        this.logger = Logger.getInstance(FacebookAuthenticationManager.class.getSimpleName());
        fbCallbackmanager = CallbackManager.Factory.create();
    }

    /**
     * register the default Handler for handling FB OAuth requests.
     * @param ctx - needed context for Facebook SDK initialization
     */
    public void registerDefaultAuthenticationListener(Context ctx) {
        registerAuthenticationListener(ctx, new DefaultFacebookAuthenticationListener());
    }

    public void registerAuthenticationListener(Context ctx, FacebookAuthenticationListener handler) {
        facebookAuthenticationListener = handler;

        // Initialize SDK before setContentView(Layout ID)
        FacebookSdk.sdkInitialize(ctx);

        //register as authListener
        BMSClient.getInstance().registerAuthenticationListener(FACEBOOK_REALM, this);
    }

    public void onActivityResultCalled(int requestCode, int resultCode, Intent data) {
        facebookAuthenticationListener.onActivityResultCalled(requestCode, resultCode, data);
    }

    public void onFacebookAccessTokenReceived(String facebookAccessToken) {
        JSONObject object = new JSONObject();
        try {
            object.put(ACCESS_TOKEN_KEY, facebookAccessToken);
            authContext.submitAuthenticationChallengeAnswer(object);
        } catch (JSONException e) {
            logger.error("Error in onFacebookAccessTokenReceived: " + e.getLocalizedMessage());
        }
    }

    public void onFacebookAuthenticationFailure(JSONObject userInfo) {
        authContext.submitAuthenticationFailure(userInfo);
        authContext = null;
    }

    void setAuthenticationContext(AuthenticationContext authContext) {
        this.authContext = authContext;
    }


    @Override
    public void onAuthenticationChallengeReceived(AuthenticationContext authContext, JSONObject challenge, Context context) {
        try {
            String appId = challenge.getString(FACEBOOK_APP_ID_KEY);
            setAuthenticationContext(authContext);
            facebookAuthenticationListener.handleAuthentication(context, appId);
        } catch (JSONException e) {
            logger.error("Error handling FB AuthenticationChallengeReceived: " + e.getLocalizedMessage());
        }
    }

    @Override
    public void onAuthenticationSuccess(Context ctx, JSONObject info) {
        authContext = null;
    }

    @Override
    public void onAuthenticationFailure(Context ctx, JSONObject info) {
        authContext = null;
    }
}

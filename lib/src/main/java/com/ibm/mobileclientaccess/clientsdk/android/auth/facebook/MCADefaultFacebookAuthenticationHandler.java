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

import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.login.LoginBehavior;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.ibm.mobilefirstplatform.clientsdk.android.logger.api.Logger;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.List;

/**
 * Created by iklein on 8/5/15.
 */
public class MCADefaultFacebookAuthenticationHandler implements
        MCAFacebookAuthentication
{
    private Logger logger;
    private List<String> permissionNeeds = Arrays.asList("public_profile");
    private CallbackManager callbackmanager;

    public static final String AUTH_CANCEL_CODE = "100";
    public static final String AUTH_ERROR_CODE = "101";

    private Context ctx;

    public MCADefaultFacebookAuthenticationHandler(Context ctx) {
        this.ctx = ctx;
        this.logger = Logger.getInstance(MCADefaultFacebookAuthenticationHandler.class.getSimpleName());
        callbackmanager = CallbackManager.Factory.create();
    }

    @Override
    public void handleAuthentication(Context context, String appId) {
        // Verify that the app Id defined in the .plist file is identical to the one requested by the IMF server.
        if(!(appId.equals(FacebookSdk.getApplicationId()))) {
            JSONObject obj = null;
            try {
                obj = createFailureResponse(AUTH_ERROR_CODE, "FBAuth - AppId is not the same" );
            } catch (JSONException e) {
                logger.error("error creating JSON message");
            }
            MCAFacebookAuthenticationManager.getInstance().onFacebookAuthenticationFailure(obj);
            return;
        }

        AccessToken accessToken = AccessToken.getCurrentAccessToken();
        // Use the Facebook SDK to login.
        if (accessToken != null && !accessToken.isExpired())
        {
            String token = accessToken.getToken();
            logger.debug("Token alerady available = " + token);
            MCAFacebookAuthenticationManager.getInstance().onFacebookAccessTokenReceived(token);
            return;
        }

        logger.debug("not loggedin, continue");

        if (context instanceof Activity) {
            // Set permissions
            LoginManager.getInstance().logInWithReadPermissions((Activity) context, permissionNeeds);

            LoginManager.getInstance().setLoginBehavior(LoginBehavior.NATIVE_WITH_FALLBACK);

            LoginManager.getInstance().registerCallback(callbackmanager,
                    new FacebookCallback<LoginResult>() {
                        @Override
                        public void onSuccess(LoginResult loginResult) {
                            logger.debug("LoginManager success loggedin");
                            String token = AccessToken.getCurrentAccessToken().getToken();
                            MCAFacebookAuthenticationManager.getInstance().onFacebookAccessTokenReceived(token);
                        }

                        @Override
                        public void onCancel() {
                            JSONObject obj = null;
                            try {
                                obj = createFailureResponse(AUTH_CANCEL_CODE, "LoginManager::onCancel called" );
                            } catch (JSONException e) {
                                logger.error("error creating JSON message");
                            }
                            MCAFacebookAuthenticationManager.getInstance().onFacebookAuthenticationFailure(obj);
                        }

                        @Override
                        public void onError(FacebookException error) {
                            logger.debug(error.toString());

                            JSONObject obj = null;
                            try {
                                obj = createFailureResponse(AUTH_ERROR_CODE, error.toString() );
                            } catch (JSONException e) {
                                logger.error("error creating JSON message");
                            }
                            MCAFacebookAuthenticationManager.getInstance().onFacebookAuthenticationFailure(obj);
                        }
                    });
        }
        else{
            JSONObject obj = null;
            try {
                obj = createFailureResponse(AUTH_ERROR_CODE, "The context provided is not ActivityContext, cannot proceed" );
            } catch (JSONException e) {
                logger.error("error creating JSON message");
            }
            MCAFacebookAuthenticationManager.getInstance().onFacebookAuthenticationFailure(obj);
        }
    }

    @Override
    public void onActivityResultCalled(int requestCode, int resultCode, Intent data) {
        logger.debug("FB, onActivityResultCalled called");
        callbackmanager.onActivityResult(requestCode, resultCode, data);
    }

    private JSONObject createFailureResponse(String code, String msg) throws JSONException{
        JSONObject obj = new JSONObject();
        obj.put("errorCode", code);
        obj.put("msg", msg);
        return obj;
    }
}

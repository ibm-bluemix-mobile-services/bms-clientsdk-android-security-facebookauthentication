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
import com.ibm.mobilefirstplatform.clientsdk.android.core.api.BMSClient;
import com.ibm.mobilefirstplatform.clientsdk.android.logger.api.Logger;
import com.ibm.mobilefirstplatform.clientsdk.android.security.api.AuthenticationContext;
import com.ibm.mobilefirstplatform.clientsdk.android.security.api.AuthenticationListener;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.List;

/**
 * Class implementation for managing the Facebook OAuth listener.
 * This class registers to AuthenticationListener in order to handle authentication requests
 */
public class FacebookAuthenticationManager implements
        AuthenticationListener
{
    private Logger logger;

    private static final String FACEBOOK_REALM = "wl_facebookRealm";
    private static final String FACEBOOK_APP_ID_KEY = "facebookAppId";
    private static final String ACCESS_TOKEN_KEY = "accessToken";

    private List<String> permissionNeeds = Arrays.asList("public_profile");
    private CallbackManager callbackmanager;

    /**
     * Default return code when cancel is pressed during fb authentication (info)
     */
    public static final String AUTH_CANCEL_CODE = "100";

    /**
     * Default return code when error occures (info)
     */
    public static final String AUTH_ERROR_CODE = "101";

    //singelton
    private static final Object lock = new Object();
    private static volatile FacebookAuthenticationManager instance;
    private AuthenticationContext authContext;

    /**
     * Manager singleton - used for registering and handling authentication
     * @return the FacebookAuthenticationManager singelton
     */
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
     * private constructor for singleton
     */
    private FacebookAuthenticationManager() {
        this.logger = Logger.getInstance(FacebookAuthenticationManager.class.getSimpleName());
        callbackmanager = CallbackManager.Factory.create();
    }

    //////////////////////////////// Public API /////////////////////////////////////////
    /**
     * Supply context for initialization of Facebook sdk
     *
     * @param ctx - needed to init Facebook code - can be application context
     */
    public void register(Context ctx) {
        // Initialize Facebook SDK
        FacebookSdk.sdkInitialize(ctx);

        //register as authListener
        BMSClient.getInstance().registerAuthenticationListener(FACEBOOK_REALM, this);
    }

    /**
     * Signs-in to Facebook as identity provider and sends the access token back to the authentication handler.
     *
     * @param appId   The Facebook app id.
     * @param context context to pass for request resources
     */
    public void handleAuthentication(Context context, String appId) {
        // Verify that the app Id defined in the .plist file is identical to the one requested by the IMF server.
        if(!(appId.equals(FacebookSdk.getApplicationId()))) {
            JSONObject obj = null;
            try {
                obj = createFailureResponse(AUTH_ERROR_CODE, "Facebook OAuth - AppId is not equal to " +  appId);
            } catch (JSONException e) {
                logger.error("error creating JSON message");
            }
            FacebookAuthenticationManager.getInstance().onFacebookAuthenticationFailure(obj);
            return;
        }

        AccessToken accessToken = AccessToken.getCurrentAccessToken();
        // Use the Facebook SDK to login.
        if (accessToken != null && !accessToken.isExpired())
        {
            String token = accessToken.getToken();
            logger.debug("Token alerady available = " + token);
            FacebookAuthenticationManager.getInstance().onFacebookAccessTokenReceived(token);
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
                            FacebookAuthenticationManager.getInstance().onFacebookAccessTokenReceived(token);
                        }

                        @Override
                        public void onCancel() {
                            JSONObject obj = null;
                            try {
                                obj = createFailureResponse(AUTH_CANCEL_CODE, "LoginManager::onCancel called" );
                            } catch (JSONException e) {
                                logger.error("error creating JSON message");
                            }
                            FacebookAuthenticationManager.getInstance().onFacebookAuthenticationFailure(obj);
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
                            FacebookAuthenticationManager.getInstance().onFacebookAuthenticationFailure(obj);
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
            FacebookAuthenticationManager.getInstance().onFacebookAuthenticationFailure(obj);
        }
    }

    /**
     * When the Facebook activity ends, it sends a result code, and that result needs to be transferred to the facebook code,
     * @param requestCode the intent request code
     * @param resultCode the result
     * @param data the data (if any)
     */
    public void onActivityResultCalled(int requestCode, int resultCode, Intent data) {
        logger.debug("FB, onActivityResultCalled called");
        callbackmanager.onActivityResult(requestCode, resultCode, data);
    }
    //////////////////////////////// Public API /////////////////////////////////////////

    private JSONObject createFailureResponse(String code, String msg) throws JSONException{
        JSONObject obj = new JSONObject();
        obj.put("errorCode", code);
        obj.put("msg", msg);
        return obj;
    }

    /**
     * Called when the authentication process has succeeded for Facebook, now we send the token as a response to BM
     * authentication challenge.
     * @param facebookAccessToken the token response
     */
    public void onFacebookAccessTokenReceived(String facebookAccessToken) {
        JSONObject object = new JSONObject();
        try {
            object.put(ACCESS_TOKEN_KEY, facebookAccessToken);
            authContext.submitAuthenticationChallengeAnswer(object);
        } catch (JSONException e) {
            logger.error("Error in onFacebookAccessTokenReceived: " + e.getLocalizedMessage());
        }
    }

    /**
     * Called when the authentication process has failed for Facebook.
     * @param userInfo error data
     */
    public void onFacebookAuthenticationFailure(JSONObject userInfo) {
        authContext.submitAuthenticationFailure(userInfo);
        authContext = null;
    }

    private void setAuthenticationContext(AuthenticationContext authContext) {
        this.authContext = authContext;
    }

    @Override
    public void onAuthenticationChallengeReceived(AuthenticationContext authContext, JSONObject challenge, Context context) {
        try {
            String appId = challenge.getString(FACEBOOK_APP_ID_KEY);
            setAuthenticationContext(authContext);
            handleAuthentication(context, appId);
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

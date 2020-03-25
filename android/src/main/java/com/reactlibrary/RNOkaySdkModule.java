
package com.reactlibrary;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.facebook.react.bridge.ActivityEventListener;
import com.facebook.react.bridge.BaseActivityEventListener;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.ReadableMapKeySetIterator;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableNativeArray;
import com.facebook.react.bridge.WritableNativeMap;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itransition.protectoria.psa_multitenant.data.SpaStorage;
import com.itransition.protectoria.psa_multitenant.protocol.scenarios.linking.LinkingScenarioListener;
import com.itransition.protectoria.psa_multitenant.protocol.scenarios.unlinking.UnlinkingScenarioListener;
import com.itransition.protectoria.psa_multitenant.state.ApplicationState;
import com.protectoria.psa.PsaManager;
import com.protectoria.psa.api.PsaConstants;
import com.protectoria.psa.api.converters.PsaIntentUtils;
import com.protectoria.psa.api.entities.PsaEnrollResultData;
import com.protectoria.psa.api.entities.SpaAuthorizationData;
import com.protectoria.psa.api.entities.SpaEnrollData;
import com.protectoria.psa.dex.common.data.enums.PsaType;
import com.protectoria.psa.dex.common.ui.PageTheme;
import com.protectoria.psa.ui.activities.authorization.AuthorizationActivity;
import com.reactlibrary.storage.SpaStorageImpl;

import java.util.HashMap;
import java.util.Map;


public class RNOkaySdkModule extends ReactContextBaseJavaModule {

    private final ReactApplicationContext reactContext;

    private PsaManager psaManager;
    private Promise mPickerPromise;

    private final ActivityEventListener mActivityEventListener = new BaseActivityEventListener() {

        @Override
        public void onActivityResult(Activity activity, int requestCode, int resultCode, Intent data) {
            if (requestCode == PsaConstants.ACTIVITY_REQUEST_CODE_PSA_ENROLL) {
                if (resultCode == Activity.RESULT_OK) {
                    mPickerPromise.resolve(parseEnrollmentData(data));
                } else {
                    mPickerPromise.reject("" + resultCode);
                }
            }

            if (requestCode == PsaConstants.ACTIVITY_REQUEST_CODE_PSA_AUTHORIZATION) {
                if (resultCode == Activity.RESULT_OK
                        || resultCode == AuthorizationActivity.RESULT_OK_CONSUMED_PUSH) {
                    mPickerPromise.resolve(resultCode);
                } else {
                    mPickerPromise.reject("" + resultCode);
                }
            }
        }
    };

    private final LinkingScenarioListener mLinkingScenarioListener = new LinkingScenarioListener() {
        @Override
        public void onLinkingCompletedSuccessful(long l, String s) {
            mPickerPromise.resolve("Linking completed");
        }

        @Override
        public void onLinkingFailed(ApplicationState applicationState) {
            mPickerPromise.resolve("Error status" + applicationState);
        }
    };

    private final UnlinkingScenarioListener mUnlinkingScenarioListener = new UnlinkingScenarioListener() {
        @Override
        public void onUnlinkingCompletedSuccessful() {
            mPickerPromise.resolve("Unlinking completed");
        }

        @Override
        public void onUnlinkingFailed(@NonNull ApplicationState applicationState) {
            mPickerPromise.reject("Error status" + applicationState);
        }
    };


    public RNOkaySdkModule(ReactApplicationContext reactContext) {
        super(reactContext);
        psaManager = PsaManager.init(reactContext, new CrashlyticsExceptionLogger());
        this.reactContext = reactContext;
        reactContext.addActivityEventListener(mActivityEventListener);
    }


    @ReactMethod
    public void init(String endpoint, final Promise promise) {
        psaManager.setPssAddress(endpoint);
        promise.resolve("Success endpoint");
    }


    @ReactMethod
    public void enrollProcedure(final ReadableMap data, final Promise promise) {
        Activity activity = reactContext.getCurrentActivity();
        mPickerPromise = promise;

        ReadableMap spaEnrollDataMap = data.getMap("SpaEnrollData");
        String appPns = spaEnrollDataMap.getString("appPns");
        String pubPss = spaEnrollDataMap.getString("pubPss");
        String installationId = spaEnrollDataMap.getString("installationId");
        ReadableMap pageThemeMap = spaEnrollDataMap.getMap("pageTheme");
        PsaType psaType = PsaType.OKAY;
        SpaEnrollData enrollData;
        if(pageThemeMap != null) {
            PageTheme pageTheme = initPageTheme(pageThemeMap, promise);
            enrollData = new SpaEnrollData(appPns, pubPss, installationId, pageTheme, psaType);
        }
        else {
            enrollData = new SpaEnrollData(appPns, pubPss, installationId, null, psaType);
        }
        PsaManager.startEnrollmentActivity(activity, enrollData);
    }


    @ReactMethod
    public void authorization(final ReadableMap data, final Promise promise) {
        Activity activity = reactContext.getCurrentActivity();
        mPickerPromise = promise;

        ReadableMap spaEnrollDataMap = data.getMap("SpaAuthorizationData");
        int sessionId = spaEnrollDataMap.getInt("sessionId");
        String appPNS = spaEnrollDataMap.getString("appPNS");
        ReadableMap pageThemeMap = spaEnrollDataMap.getMap("pageTheme");
        PsaType psaType = PsaType.OKAY;

        SpaAuthorizationData authorizationData;
        if(pageThemeMap != null) {
            PageTheme pageTheme = initPageTheme(pageThemeMap, promise);
            authorizationData = new SpaAuthorizationData(sessionId, appPNS, pageTheme, psaType);
        }
        else {
            authorizationData = new SpaAuthorizationData(sessionId, appPNS, null, psaType);
        }
        PsaManager.startAuthorizationActivity(activity, authorizationData);
    }

    @ReactMethod
    public void linkTenant(String linkingCode, final ReadableMap data, final Promise promise) {
        try {
            mPickerPromise = promise;
            ReadableMap spaStorageMap = data.getMap("SpaStorage");
            SpaStorage spaStorage = new SpaStorageImpl(reactContext);
            spaStorage.putAppPNS(spaStorageMap.getString("appPNS"));
            spaStorage.putExternalId(spaStorageMap.getString("externalId"));
            spaStorage.putPubPssBase64(spaStorageMap.getString("pubPss"));
            spaStorage.putEnrollmentId(spaStorageMap.getString("enrollmentId"));
            spaStorage.putInstallationId(spaStorageMap.getString("installationId"));
            psaManager.linkTenant(linkingCode, spaStorage, mLinkingScenarioListener);
        } catch ( Exception e ) {
            promise.reject(e);
        }
    }

    @ReactMethod
    public void unlinkTenant(long tenantId, final ReadableMap data, final Promise promise) {
        try {
            mPickerPromise = promise;
            ReadableMap spaStorageMap = data.getMap("SpaStorage");
            SpaStorage spaStorage = new SpaStorageImpl(reactContext);
            spaStorage.putAppPNS(spaStorageMap.getString("appPNS"));
            spaStorage.putExternalId(spaStorageMap.getString("externalId"));
            spaStorage.putPubPssBase64(spaStorageMap.getString("pubPss"));
            spaStorage.putEnrollmentId(spaStorageMap.getString("enrollmentId"));
            spaStorage.putInstallationId(spaStorageMap.getString("installationId"));
            PsaManager.getInstance().unlinkTenant(tenantId, spaStorage, mUnlinkingScenarioListener);
        } catch ( Exception e ) {
            promise.reject(e);
        }

    }


    @ReactMethod
    public void isEnrolled(final Promise promise) {
        promise.resolve(PsaManager.getInstance().isEnrolled());
    }


    @ReactMethod
    public void isReadyForAuthorization(final Promise promise) {
        promise.resolve(PsaManager.getInstance().isReadyForAuthorization());
    }


    @ReactMethod
    public void permissionRequest(final Promise promise) {
        String[] permissions = PsaManager.getRequiredPermissions();
        WritableArray writableArray = new WritableNativeArray();
        for (String permission : permissions) {
            writableArray.pushString(permission);
        }
        if (writableArray.size() == 0) {
            promise.reject("No permissions found.");
        }
        promise.resolve(writableArray);
    }


    public PageTheme initPageTheme(ReadableMap pageThemeMap, Promise promise) {
        ObjectMapper mapper = new ObjectMapper();
        PageTheme pageTheme = new PageTheme();
        try {
            pageTheme = mapper.convertValue(toMap(pageThemeMap), PageTheme.class);
        } catch (Exception e) {
            promise.reject("Invalid object property");
        }
        return pageTheme;
    }

    public static Map<String, Object> toMap(@Nullable ReadableMap readableMap) {
      if (readableMap == null) {
          return null;
      }

      ReadableMapKeySetIterator iterator = readableMap.keySetIterator();
      if (!iterator.hasNextKey()) {
          return null;
      }

      Map<String, Object> result = new HashMap<>();
      while (iterator.hasNextKey()) {
          String key = iterator.nextKey();
          if(key.contains("Color")){
              result.put(key, Color.parseColor(readableMap.getString(key)));
          } else {
              result.put(key, readableMap.getString(key));
          }
      }
      return result;
  }

  private WritableNativeMap parseEnrollmentData(Intent data) {
      PsaEnrollResultData psaEnrollResultData = PsaIntentUtils.enrollResultFromIntent(data);
      WritableNativeMap enrollmentData = new WritableNativeMap();
      enrollmentData.putString("enrollmentId", psaEnrollResultData.getEnrollmentId());
      enrollmentData.putString("externalId", psaEnrollResultData.getExternalId());
      return enrollmentData;
  }

    @Override
    public String getName() {
        return "RNOkaySdk";
    }
}
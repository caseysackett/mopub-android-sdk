package com.mopub.mobileads;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.util.Log;
import com.inmobi.androidsdk.IMAdInterstitial;
import com.inmobi.androidsdk.IMAdInterstitialListener;
import com.inmobi.androidsdk.IMAdRequest;
import com.inmobi.androidsdk.IMAdRequest.ErrorCode;
import com.mopub.mobileads.CustomEventInterstitial;
import com.mopub.mobileads.MoPubErrorCode;

import java.util.HashMap;
import java.util.Map;

/*
 * Tested with InMobi SDK 3.7.0.
 */
class InMobiInterstitial extends CustomEventInterstitial implements IMAdInterstitialListener {
    private CustomEventInterstitialListener mInterstitialListener;
    private IMAdInterstitial mInMobiInterstitial;

    /*
     * Abstract methods from CustomEventInterstitial
     */
    @Override
    protected void loadInterstitial(Context context, CustomEventInterstitialListener interstitialListener,
            Map<String, Object> localExtras, Map<String, String> serverExtras) {
        mInterstitialListener = interstitialListener;
        
        Activity activity = null;
        if (context instanceof Activity) {
            activity = (Activity) context;
        } else {
        	 activity = (Activity)localExtras.get("activity");
        }
        
        if (activity == null) {
            mInterstitialListener.onInterstitialFailed(MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);
            return;
        }
        
        String appId = serverExtras.get("app_id");
        if(appId == null) {
        	try {
	        	ApplicationInfo ai = context.getPackageManager().getApplicationInfo(context.getPackageName(),PackageManager.GET_META_DATA);
	            appId = ai.metaData.get("inmobi_interstitial_ads_app_id").toString();
        	} catch(Throwable t) {
        		Log.e("MoPub", "Could not find inmobi_interstitial_ads_app_id in meta-data in Android manifest");
        	}
        }
        if(appId == null || appId.length() == 0) {
            Log.d("MoPub", "InMobi interstitial ad app_id is missing.");
        	if(mInterstitialListener != null) {
        		mInterstitialListener.onInterstitialFailed(MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);
        	}
        	return;
        }

        /*
         * You may also pass this String down in the serverExtras Map by specifying Custom Event Data
         * in MoPub's web interface.
         */
        mInMobiInterstitial = new IMAdInterstitial(activity, appId);
        
        mInMobiInterstitial.setIMAdInterstitialListener(this);

        IMAdRequest imAdRequest = new IMAdRequest();
        Map<String, String> requestParameters = new HashMap<String, String>();
        requestParameters.put("tp", "c_mopub");
        imAdRequest.setRequestParams(requestParameters);

        mInMobiInterstitial.loadNewAd(imAdRequest);
    }
    
    @Override
    protected void showInterstitial() {
        Log.d("MoPub", "Showing InMobi interstitial ad.");
    	if(mInMobiInterstitial != null) {
    		mInMobiInterstitial.show();
    	}
    }

    @Override
    protected void onInvalidate() {
    	if(mInMobiInterstitial != null) {
    		mInMobiInterstitial.setIMAdInterstitialListener(null);
    	}
    }

    /*
     * IMAdListener implementation
     */
    @Override
    public void onAdRequestLoaded(IMAdInterstitial adInterstitial) {
        Log.d("MoPub", "InMobi interstitial ad loaded successfully.");
        if(mInterstitialListener != null) {
        	mInterstitialListener.onInterstitialLoaded();
        }
    }
    
    @Override
    public void onAdRequestFailed(IMAdInterstitial adInterstitial, ErrorCode errorCode) {
        Log.d("MoPub", "InMobi interstitial ad failed to load.");
        if(mInterstitialListener != null) {
        	mInterstitialListener.onInterstitialFailed(MoPubErrorCode.NETWORK_NO_FILL);
        }
    }

    @Override
    public void onShowAdScreen(IMAdInterstitial adInterstitial) {
        if(mInterstitialListener != null) {
        	mInterstitialListener.onInterstitialShown();
        }
    }
    
    @Override
    public void onLeaveApplication(IMAdInterstitial adInterstitial) {
        /*
         * Because InMobi does not have an onClick equivalent, we use onLeaveApplication
         * as a click notification.
         */
        Log.d("MoPub", "InMobi interstitial ad leaving application.");
        if(mInterstitialListener != null) {
        	mInterstitialListener.onInterstitialClicked();
        }
    }

    @Override
    public void onDismissAdScreen(IMAdInterstitial adInterstitial) {
        Log.d("MoPub", "InMobi interstitial ad dismissed.");
        if(mInterstitialListener != null) {
        	mInterstitialListener.onInterstitialDismissed();
        }
    }
}

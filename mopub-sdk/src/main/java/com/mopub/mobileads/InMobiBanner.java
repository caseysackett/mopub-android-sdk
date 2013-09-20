package com.mopub.mobileads;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import com.inmobi.androidsdk.IMAdListener;
import com.inmobi.androidsdk.IMAdRequest;
import com.inmobi.androidsdk.IMAdRequest.ErrorCode;
import com.inmobi.androidsdk.IMAdView;
import com.mopub.mobileads.CustomEventBanner;
import com.mopub.mobileads.MoPubErrorCode;

import java.util.HashMap;
import java.util.Map;

/*
 * Tested with InMobi SDK 3.7.0.
 */
class InMobiBanner extends CustomEventBanner implements IMAdListener {
    private CustomEventBannerListener mBannerListener;
    private IMAdView mInMobiBanner;

    /*
     * Abstract methods from CustomEventBanner
     */
    @Override
    protected void loadBanner(Context context, CustomEventBannerListener bannerListener,
            Map<String, Object> localExtras, Map<String, String> serverExtras) {
        mBannerListener = bannerListener;
        
        Activity activity = null;
        if (context instanceof Activity) {
            activity = (Activity) context;
        } else {
        	activity = (Activity)localExtras.get("activity");
        }
        
        if (activity == null) {
        	if(mBannerListener != null) {
        		mBannerListener.onBannerFailed(MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);
        	}
            return;
        }
        
        String appId = serverExtras.get("app_id");
        if(appId == null || appId.length() == 0) {
        	if(mBannerListener != null) {
        		mBannerListener.onBannerFailed(MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);
        	}
        	return;
        }
        
        /*
         * You may also pass this String down in the serverExtras Map by specifying Custom Event Data
         * in MoPub's web interface.
         */
        mInMobiBanner = new IMAdView(activity, IMAdView.INMOBI_AD_UNIT_320X50, appId);
        
        mInMobiBanner.setIMAdListener(this);

        IMAdRequest imAdRequest = new IMAdRequest();
        Map<String, String> requestParameters = new HashMap<String, String>();
        requestParameters.put("tp", "c_mopub");
        imAdRequest.setRequestParams(requestParameters);

        mInMobiBanner.loadNewAd(imAdRequest);
    }

    @Override
    protected void onInvalidate() {
    	if(mInMobiBanner != null) {
    		mInMobiBanner.setIMAdListener(null);
    	}
    }

    /*
     * IMAdListener implementation
     */
    @Override
    public void onAdRequestCompleted(IMAdView adView) {
        if (mInMobiBanner != null && mBannerListener != null) {
            Log.d("MoPub", "InMobi banner ad loaded successfully. Showing ad...");
            mBannerListener.onBannerLoaded(mInMobiBanner);
        } else {
        	if(mBannerListener != null) {
        		mBannerListener.onBannerFailed(MoPubErrorCode.NETWORK_INVALID_STATE);
        	}
        }
    }

    @Override
    public void onAdRequestFailed(IMAdView adView, ErrorCode errorCode) {
        Log.d("MoPub", "InMobi banner ad failed to load.");
    	if(mBannerListener != null) {
    		mBannerListener.onBannerFailed(MoPubErrorCode.NETWORK_NO_FILL);
    	}
    }

    @Override
    public void onDismissAdScreen(IMAdView adView) {
        Log.d("MoPub", "InMobi banner ad modal dismissed.");
    }

    @Override
    public void onLeaveApplication(IMAdView adView) {
        /*
         * Because InMobi does not have an onClick equivalent, we use onLeaveApplication
         * as a click notification.
         */
        Log.d("MoPub", "InMobi banner ad leaving application.");
    	if(mBannerListener != null) {
    		mBannerListener.onBannerClicked();
    	}
    }

    @Override
    public void onShowAdScreen(IMAdView adView) {
        Log.d("MoPub", "InMobi banner ad modal shown.");
    }
}

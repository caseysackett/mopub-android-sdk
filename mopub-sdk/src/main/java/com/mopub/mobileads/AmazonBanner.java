package com.mopub.mobileads;

import java.util.Map;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.util.Log;
import android.view.ViewGroup.LayoutParams;

import com.amazon.device.ads.Ad;
import com.amazon.device.ads.AdError;
import com.amazon.device.ads.AdLayout;
import com.amazon.device.ads.AdListener;
import com.amazon.device.ads.AdProperties;
import com.amazon.device.ads.AdRegistration;
import com.amazon.device.ads.AdSize;
import com.amazon.device.ads.AdTargetingOptions;

/*
 * Tested with Amazon SDK 4.0.8
 */
public class AmazonBanner extends CustomEventBanner implements AdListener {
    private CustomEventBannerListener mBannerListener;
    private AdLayout mAmazonAdView;
    
    /*
     * Abstract methods from CustomEventBanner
     */
    @Override
    public void loadBanner(Context context, CustomEventBannerListener bannerListener,
            Map<String, Object> localExtras, Map<String, String> serverExtras) {
        mBannerListener = bannerListener;
        
        Activity activity = null;
        if (context instanceof Activity) {
            activity = (Activity) context;
        } else {
            // You may also pass in an Activity Context in the localExtras map and retrieve it here.
        	activity = (Activity)localExtras.get("activity");
        }
        
        if (activity == null) {
        	if(mBannerListener != null) {
        		mBannerListener.onBannerFailed(MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);
        	}
            return;
        }
        
        int widthPixels = context.getResources().getDisplayMetrics().widthPixels;
        AdSize adSize = AdSize.SIZE_320x50;
        if(widthPixels == 600) adSize = AdSize.SIZE_600x90; 	// Kindle Fire
        if(widthPixels == 800) adSize = AdSize.SIZE_600x90; 	// Kindle Fire HD 7"
        if(widthPixels == 728) adSize = AdSize.SIZE_728x90;		// Only if it matches exactly, since 728 scrolls on 800px wide Kindle Fire HD 7"
        if(widthPixels >= 1024) adSize = AdSize.SIZE_1024x50; 	// Kindle Fire HD
        
        String appId = serverExtras.get("app_id");
        if(appId == null) {
        	try {
	        	ApplicationInfo ai = context.getPackageManager().getApplicationInfo(context.getPackageName(),PackageManager.GET_META_DATA);
	            appId = ai.metaData.get("amazon_ads_app_id").toString();
        	} catch(Throwable t) {
        		Log.e("MoPub", "Could not find amazon_ads_app_id in meta-data in Android manifest");
        	}
        }
        if(appId == null) {
        	Log.e("AmazonBanner", "app_id is null");
        	if(mBannerListener != null) {
        		mBannerListener.onBannerFailed(MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);
        	}
            return;
        }
        
        AdRegistration.setAppKey(appId);
        //AdRegistration.enableTesting(true);
        //AdRegistration.enableLogging(true);
        
        mAmazonAdView = new AdLayout(activity, adSize);
        mAmazonAdView.setListener(this);
        LayoutParams layoutParams = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        mAmazonAdView.setLayoutParams(layoutParams);
        
        AdTargetingOptions adTargetingOptions = new AdTargetingOptions();
        adTargetingOptions.enableGeoLocation(true);
                
        mAmazonAdView.loadAd(adTargetingOptions); // async task to retrieve an ad    
    }

    @Override
    public void onInvalidate() {
    	if(mAmazonAdView != null) {
    		mAmazonAdView.setListener(null);
    	}
    }

	@Override
	public void onAdFailedToLoad(Ad arg0, AdError arg1) {
	  	if(mBannerListener != null) {
	  		mBannerListener.onBannerFailed(MoPubErrorCode.NETWORK_NO_FILL);
	  	}
	}

	@Override
	public void onAdLoaded(Ad arg0, AdProperties arg1) {
		if (mAmazonAdView != null && mBannerListener != null) {
			//Log.d("MoPub", "Amazon banner ad loaded successfully. Showing ad...");
			mBannerListener.onBannerLoaded(mAmazonAdView);
		} else if (mBannerListener != null) {
	    	//Log.e("AmazonBanner", "Ad Loaded but banner listener is null");
			mBannerListener.onBannerFailed(MoPubErrorCode.NETWORK_INVALID_STATE);
		}
	}

	@Override
	public void onAdCollapsed(Ad arg0) {
        if (mBannerListener != null) {
        	mBannerListener.onBannerCollapsed();
        }
	}

	@Override
	public void onAdDismissed(Ad arg0) {
	}

	@Override
	public void onAdExpanded(Ad arg0) {
        if (mBannerListener != null) {
        	mBannerListener.onBannerExpanded();
        }
	}

}

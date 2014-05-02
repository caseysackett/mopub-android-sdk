package com.mopub.mobileads;

import java.util.HashMap;
import java.util.Map;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.util.Log;

import com.inmobi.commons.InMobi;
import com.inmobi.commons.InMobi.LOG_LEVEL;
import com.inmobi.monetization.IMBanner;
import com.inmobi.monetization.IMBannerListener;
import com.inmobi.monetization.IMErrorCode;
import com.mopub.common.MoPub;
import com.mopub.common.util.Views;

/*
 * Tested with InMobi SDK 4.1.1
 */
public class InMobiBanner extends CustomEventBanner implements IMBannerListener {

	@Override
	protected void loadBanner(Context context,
			CustomEventBannerListener bannerListener,
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
        if(appId == null) {
        	try {
	        	ApplicationInfo ai = context.getPackageManager().getApplicationInfo(context.getPackageName(),PackageManager.GET_META_DATA);
	            appId = ai.metaData.get("inmobi_ads_app_id").toString();
        	} catch(Throwable t) {
        		Log.e("MoPub", "Could not find inmobi_ads_app_id in meta-data in Android manifest");
        	}
        }
        if(appId == null || appId.length() == 0) {
            Log.d("MoPub", "InMobi banner ad app_id is missing.");
        	if(mBannerListener != null) {
        		mBannerListener.onBannerFailed(MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);
        	}
        	return;
        }
        
		if (!isAppInitialized) {
			InMobi.initialize(activity, appId);
            isAppInitialized = true;
		}

		/*
		 * You may also pass this String down in the serverExtras Map by
		 * specifying Custom Event Data in MoPub's web interface.
		 */
		iMBanner = new IMBanner(activity, appId, IMBanner.INMOBI_AD_UNIT_320X50);

        Map<String, String> map = new HashMap<String, String>();
        map.put("tp", "c_mopub");
        map.put("tp-ver", MoPub.SDK_VERSION);
        iMBanner.setRequestParams(map);
		InMobi.setLogLevel(LOG_LEVEL.VERBOSE);
		iMBanner.setIMBannerListener(this);
		iMBanner.setRefreshInterval(-1);
		iMBanner.loadBanner();

	}

	private CustomEventBannerListener mBannerListener;
	private IMBanner iMBanner;
	private static boolean isAppInitialized = false;

	/*
	 * Abstract methods from CustomEventBanner
	 */

	@Override
	public void onInvalidate() {
		if (iMBanner != null) {
            iMBanner.setIMBannerListener(null);
            Views.removeFromParent(iMBanner);
            iMBanner.destroy();
		}
	}

	@Override
	public void onBannerInteraction(IMBanner imBanner, Map<String, String> map) {
		mBannerListener.onBannerClicked();
	}

	@Override
	public void onBannerRequestFailed(IMBanner imBanner, IMErrorCode imErrorCode) {

		if (imErrorCode == IMErrorCode.INTERNAL_ERROR) {
			mBannerListener.onBannerFailed(MoPubErrorCode.INTERNAL_ERROR);
		} else if (imErrorCode == IMErrorCode.INVALID_REQUEST) {
			mBannerListener
					.onBannerFailed(MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);
		} else if (imErrorCode == IMErrorCode.NETWORK_ERROR) {
			mBannerListener
					.onBannerFailed(MoPubErrorCode.NETWORK_INVALID_STATE);
		} else if (imErrorCode == IMErrorCode.NO_FILL) {
			mBannerListener.onBannerFailed(MoPubErrorCode.NO_FILL);
		} else {
			mBannerListener.onBannerFailed(MoPubErrorCode.UNSPECIFIED);
		}
	}

	@Override
	public void onBannerRequestSucceeded(IMBanner imBanner) {
		if (iMBanner != null) {
			mBannerListener.onBannerLoaded(imBanner);

		} else {
			mBannerListener.onBannerFailed(null);
		}
	}

	@Override
	public void onDismissBannerScreen(IMBanner imBanner) {
		mBannerListener.onBannerCollapsed();
	}

	@Override
	public void onLeaveApplication(IMBanner imBanner) {

	}

	@Override
	public void onShowBannerScreen(IMBanner imBanner) {
		mBannerListener.onBannerExpanded();
	}

}

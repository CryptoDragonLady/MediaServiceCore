package com.liskovsoft.youtubeapi.app;

import com.liskovsoft.sharedutils.helpers.Helpers;
import com.liskovsoft.sharedutils.mylogger.Log;
import com.liskovsoft.youtubeapi.app.models.AppInfo;
import com.liskovsoft.youtubeapi.app.models.ClientData;
import com.liskovsoft.youtubeapi.app.models.cached.AppInfoCached;
import com.liskovsoft.youtubeapi.app.models.cached.ClientDataCached;
import com.liskovsoft.youtubeapi.app.playerdata.PlayerDataExtractor;
import com.liskovsoft.youtubeapi.app.playerdata.PlayerUrlCache;
import com.liskovsoft.youtubeapi.common.helpers.AppConstants;

import java.util.Arrays;
import java.util.List;

public class AppServiceIntCached extends AppServiceInt {
    private static final String TAG = AppServiceIntCached.class.getSimpleName();
    private static final long CACHE_REFRESH_PERIOD_MS = 10 * 60 * 60 * 1_000; // check updated core files every 10 hours
    private static final int PLAYER_EXTRACTOR_CACHE_SIZE = 4;
    private AppInfoCached mAppInfo;
    private ClientDataCached mClientData;
    private PlayerDataExtractor mPlayerDataExtractor;
    private final PlayerUrlCache<PlayerDataExtractor> mPlayerDataExtractors =
            new PlayerUrlCache<>(PLAYER_EXTRACTOR_CACHE_SIZE);
    private long mAppInfoUpdateTimeMs;
    private final Object mAppInfoSync = new Object();
    private final Object mPlayerSync = new Object();
    private final Object mClientDataSync = new Object();

    @Override
    protected AppInfo getAppInfo(String userAgent) {
        synchronized (mAppInfoSync) {
            return getAppInfoSync(userAgent);
        }
    }

    private AppInfo getAppInfoSync(String userAgent) {
        if (mAppInfo != null && System.currentTimeMillis() - mAppInfoUpdateTimeMs < CACHE_REFRESH_PERIOD_MS) {
            return mAppInfo;
        }

        Log.d(TAG, "updateAppInfoData");

        AppInfo appInfo = super.getAppInfo(userAgent);

        mAppInfo = AppInfoCached.from(appInfo);
        mAppInfoUpdateTimeMs = System.currentTimeMillis();

        return mAppInfo;
    }

    @Override
    public PlayerDataExtractor getPlayerDataExtractor(String playerUrl) {
        synchronized (mPlayerSync) {
            return getPlayerDataExtractorSync(playerUrl);
        }
    }

    @Override
    public PlayerDataExtractor getPlayerDataExtractor() {
        synchronized (mPlayerSync) {
            return getPlayerDataExtractorSync(null);
        }
    }

    private PlayerDataExtractor getPlayerDataExtractorSync(String playerUrl) {
        String appPlayerUrl = check(mAppInfo) ? mAppInfo.getPlayerUrl() : null;
        String savedPlayerUrl = check(getData().getAppInfo()) ? getData().getAppInfo().getPlayerUrl() : null;
        List<String> candidates = PlayerScriptCandidates.resolve(
                playerUrl,
                Arrays.asList(appPlayerUrl, savedPlayerUrl, AppConstants.playerUrls.get(0))
        );

        if (playerUrl != null) {
            return candidates.isEmpty() ? null : getExactPlayerDataExtractor(candidates.get(0));
        }

        if (mPlayerDataExtractor != null) {
            return mPlayerDataExtractor;
        }

        firstValidExtractor(appPlayerUrl, candidates);

        return mPlayerDataExtractor;
    }

    private PlayerDataExtractor getExactPlayerDataExtractor(String playerUrl) {
        PlayerDataExtractor cachedExtractor = mPlayerDataExtractors.get(playerUrl);
        if (cachedExtractor != null) {
            return cachedExtractor;
        }

        PlayerDataExtractor extractor = super.getPlayerDataExtractor(playerUrl);
        if (!extractor.validate()) {
            return null;
        }

        mPlayerDataExtractors.put(playerUrl, extractor);
        return extractor;
    }

    @Override
    protected ClientData getClientData(String clientUrl) {
        synchronized (mClientDataSync) {
            return getClientDataSync(clientUrl);
        }
    }

    private ClientData getClientDataSync(String clientUrl) {
        if (mClientData != null && Helpers.equals(clientUrl, mClientData.getClientUrl())) {
            return mClientData;
        }

        ClientDataCached clientDataCached = getData().getClientData();

        if (clientDataCached != null && Helpers.equals(clientUrl, clientDataCached.getClientUrl())) {
            mClientData = clientDataCached;
            return mClientData;
        }

        Log.d(TAG, "updateClientData");

        ClientData clientData = super.getClientData(clientUrl);

        mClientData = ClientDataCached.from(clientUrl, clientData);

        if (check(mClientData)) {
            getData().setClientData(mClientData);
        }

        return mClientData;
    }

    @Override
    public void invalidateCache() {
        mAppInfo = null;
        // Don't reset Player's cache. It's too heavy to recreate often.
        // Better do it inside MediaServiceData after the update
    }

    @Override
    public boolean isPlayerCacheActual() {
        synchronized (mPlayerSync) {
            return mPlayerDataExtractor != null || !mPlayerDataExtractors.isEmpty();
        }
    }

    private boolean check(AppInfoCached appInfo) {
        return appInfo != null && appInfo.validate();
    }

    private boolean check(ClientDataCached clientData) {
        return clientData != null && clientData.validate();
    }

    private void firstValidExtractor(String appPlayerUrl, List<String> playerUrls) {
        String actualTimestamp = null;

        for (String url : playerUrls) {
            PlayerDataExtractor extractor = mPlayerDataExtractors.get(url);
            if (extractor == null) {
                extractor = super.getPlayerDataExtractor(url);
            }

            if (extractor.validate()) {
                mPlayerDataExtractor = extractor;
                mPlayerDataExtractors.put(url, extractor);

                if (Helpers.equals(url, appPlayerUrl)) {
                    getData().setAppInfo(mAppInfo);
                    getData().setFailedAppInfo(null);
                } else {
                    getData().setFailedAppInfo(mAppInfo);
                    getData().setAppInfo(null);
                }

                if (actualTimestamp != null) {
                    extractor.setSignatureTimestamp(actualTimestamp);
                }

                break;
            }

            // Try to fetch the actual timestamp for old players. Needed for history (tracking) and possibly more.
            // NOTE: the older player may not work on newer timestamp
            if (Helpers.equals(url, appPlayerUrl)) {
                actualTimestamp = extractor.getSignatureTimestamp();
            }
        }
    }
}

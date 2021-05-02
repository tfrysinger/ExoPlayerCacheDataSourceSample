package com.reddragon.customcachesample;

import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.net.Uri;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource;
import com.google.android.exoplayer2.upstream.cache.CacheDataSource;
import com.google.android.exoplayer2.upstream.cache.LeastRecentlyUsedCacheEvictor;
import com.google.android.exoplayer2.upstream.cache.SimpleCache;
import com.google.android.exoplayer2.util.Util;

import java.io.File;

import timber.log.Timber;

@SuppressWarnings("deprecation")
public class Utils {
    public static final int NO_BACKGROUND_RESOURCE = -1;
    public static final String MEDIA_CACHE_DIRECTORY = "media_cache";
    public static final int MAX_CACHE_SIZE_IN_MB = 1000;
    public static final int DEFAULT_MEDIA_CACHE_FRAGMENT_SIZE_IN_BYTES = (1024 * 1024 * 10);
    public static final int LOCATION_LOCAL = 0;
    public static final int LOCATION_REMOTE = 1;
    public static final int LOCATION_UNKNOWN = 2;
    private static final String LOCAL_FILE_AUTHORITIES = "com.android.providers.downloads.documents:com.android.providers.media.documents:com.android.externalstorage.documents";

    private static SimpleCache mediaCache;
    private static ProgressiveMediaSource.Factory cachedMediaSourceFactory;
    private static ProgressiveMediaSource.Factory defaultMediaSourceFactory;

    public static void showAlertDialog(int resTitle, int resMessage, AppCompatActivity activity) {
        showAlertDialog(resTitle, resMessage, activity, false, NO_BACKGROUND_RESOURCE, null, null);
    }

    public static SimpleCache getMediaCache() {
        if ( mediaCache == null ) {
            Context context = CustomCacheApp.getInstance().getApplicationContext();
            File contentDirectory = new File(context.getCacheDir(), MEDIA_CACHE_DIRECTORY);
            mediaCache = new SimpleCache(contentDirectory, new LeastRecentlyUsedCacheEvictor(MAX_CACHE_SIZE_IN_MB*1024*1024));
        }
        return mediaCache;
    }


    public static void showAlertDialog(int resTitle,
                                       int resMessage,
                                       AppCompatActivity activity,
                                       boolean quitActivityOnOk,
                                       int backgroundDrawableResource,
                                       Runnable okRunnable,
                                       Runnable cancelRunnable){
        AlertDialog alertDialog;
        try {
            AlertDialog.Builder builder = new AlertDialog.Builder(activity).
                    setIconAttribute(android.R.attr.alertDialogIcon).
                    setTitle(resTitle).
                    setMessage(resMessage);
            builder.setPositiveButton(activity.getString(R.string.alert_ok), (DialogInterface dialog, int which) -> {
                if ( quitActivityOnOk ) {
                    activity.finish();
                }
                if ( okRunnable != null ){
                    okRunnable.run();
                }
            });
            if ( cancelRunnable != null ) {
                builder.setNegativeButton(activity.getString(R.string.alert_cancel), (DialogInterface dialog, int which) -> cancelRunnable.run());
            }

            alertDialog = builder.create();
            if ( backgroundDrawableResource > NO_BACKGROUND_RESOURCE) {
                alertDialog.getWindow().setBackgroundDrawableResource(backgroundDrawableResource);
            }
            alertDialog.setCanceledOnTouchOutside(false);
            alertDialog.show();
        } catch (Resources.NotFoundException e) {
            Timber.d("Lifecycle: Exception creating showAlertDialog: %s", e.getMessage());
        }
    }

    public static ProgressiveMediaSource.Factory getCachedMediaSourceFactory(Context context,
                                                                             CacheDataSource.EventListener eventListener) {
        if ( cachedMediaSourceFactory == null ) {
            cachedMediaSourceFactory = new ProgressiveMediaSource.Factory(
                    new CustomCacheDataSourceFactory(context,
                            DEFAULT_MEDIA_CACHE_FRAGMENT_SIZE_IN_BYTES,
                            eventListener));
        }
        return cachedMediaSourceFactory;
    }

    public static ProgressiveMediaSource.Factory getDefaultMediaSourceFactory(Context context) {
        if ( defaultMediaSourceFactory == null ) {
            defaultMediaSourceFactory = new ProgressiveMediaSource.Factory(
                   new DefaultHttpDataSource.Factory().setUserAgent(Util.getUserAgent(context, context.getString(R.string.app_name))));
        }
        return defaultMediaSourceFactory;
    }

    public static int fileLocationType(Uri uri) {
        try {
            if ( (uri.getAuthority() != null && LOCAL_FILE_AUTHORITIES.contains( uri.getAuthority().toLowerCase() )) ||
                    (uri.getScheme() != null && "file".equalsIgnoreCase(uri.getScheme()))) {
                return LOCATION_LOCAL;
            } else {
                return LOCATION_REMOTE;
            }

        } catch (Exception e) {
            Timber.d("Got exception determining fileLocationType for uri: %s, message was: %s", uri.toString(), e.getMessage());
        }
        return LOCATION_UNKNOWN;
    }

}

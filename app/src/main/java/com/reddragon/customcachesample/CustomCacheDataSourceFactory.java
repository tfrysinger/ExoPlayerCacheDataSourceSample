package com.reddragon.customcachesample;

import android.content.Context;
import android.net.Uri;

import androidx.annotation.Nullable;

import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DataSpec;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource;
import com.google.android.exoplayer2.upstream.FileDataSource;
import com.google.android.exoplayer2.upstream.TransferListener;
import com.google.android.exoplayer2.upstream.cache.CacheDataSink;
import com.google.android.exoplayer2.upstream.cache.CacheDataSource;
import com.google.android.exoplayer2.util.Util;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;

public class CustomCacheDataSourceFactory implements DataSource.Factory {
    private final DefaultHttpDataSource.Factory defaultDatasourceFactory;
    private final CacheDataSource.EventListener eventListener;
    private final long maxCacheFragmentSize;

    public CustomCacheDataSourceFactory(Context context,
                                        long maxCacheFragmentSize,
                                        CacheDataSource.EventListener eventListener) {
        this.maxCacheFragmentSize = maxCacheFragmentSize;
        this.eventListener = eventListener;

        defaultDatasourceFactory =
                        new DefaultHttpDataSource.Factory().setUserAgent(Util.getUserAgent(context, context.getString(R.string.app_name)));


    }

    @Override
    public @NotNull DataSource createDataSource() {
        CacheDataSource dataSource = new CacheDataSource(Utils.getMediaCache(),
                defaultDatasourceFactory.createDataSource(),
                new FileDataSource(),
                new CacheDataSink(Utils.getMediaCache(), maxCacheFragmentSize),
                CacheDataSource.FLAG_BLOCK_ON_CACHE | CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR,
                eventListener);
        return new CustomCacheDataSource(dataSource);
    }

    /**
     * Class specifically to turn off the flag that would not cache streamed docs.
     */
    private static class CustomCacheDataSource implements DataSource {
        private final CacheDataSource cacheDataSource;
        CustomCacheDataSource(CacheDataSource cacheDataSource) {
            this.cacheDataSource = cacheDataSource;
        }

        @Override
        public void addTransferListener(@NotNull TransferListener transferListener) {
            cacheDataSource.addTransferListener(transferListener);
        }

        @Override
        public long open(DataSpec dataSpec) throws IOException {
            return cacheDataSource.open(dataSpec
                    .buildUpon()
                    .setFlags(dataSpec.flags & ~DataSpec.FLAG_DONT_CACHE_IF_LENGTH_UNKNOWN)
                    .build());
        }

        @Nullable
        @Override
        public Uri getUri() {
            return cacheDataSource.getUri();
        }

        @Override
        public void close() throws IOException {
            cacheDataSource.close();
        }

        @Override
        public int read(byte @NotNull [] target, int offset, int length) throws IOException {
            return cacheDataSource.read(target, offset, length);
        }
    }
}

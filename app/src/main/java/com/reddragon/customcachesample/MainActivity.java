package com.reddragon.customcachesample;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.upstream.cache.CacheDataSource;

import org.jetbrains.annotations.NotNull;

import timber.log.Timber;

import static com.google.android.exoplayer2.Player.STATE_BUFFERING;
import static com.google.android.exoplayer2.Player.STATE_ENDED;
import static com.google.android.exoplayer2.Player.STATE_IDLE;
import static com.google.android.exoplayer2.Player.STATE_READY;
import static com.reddragon.customcachesample.Utils.LOCATION_REMOTE;

public class MainActivity extends AppCompatActivity
                          implements CacheDataSource.EventListener,
                                     Player.EventListener {
    private static final int REQUEST_CODE_NEW_MEDIA_FROM_DEVICE = 100;
    EditText videoToPlay;
    Uri chosenVideoUri;
    SimpleExoPlayer basicExoPlayer;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Timber.d("Lifecycle: onCreate() called");
        setContentView(R.layout.activity_main);
        Button findVideoButton = findViewById(R.id.selectVideoButton);
        Button playVideoButton = findViewById(R.id.playVideo);
        playVideoButton.setOnClickListener(view -> playVideo());
        findVideoButton.setOnClickListener(view -> findVideo());
        videoToPlay = findViewById(R.id.chosenVideo);
        chosenVideoUri = Uri.parse(videoToPlay.getText().toString());
        videoToPlay.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                try {
                    chosenVideoUri = Uri.parse(s.toString());
                    videoToPlay.setEnabled(true);
                    videoToPlay.setAlpha(1.0f);
                } catch (Exception e) {
                    chosenVideoUri = null;
                    videoToPlay.setEnabled(false);
                    videoToPlay.setAlpha(.35f);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
        basicExoPlayer = new SimpleExoPlayer.Builder(this).build();
        basicExoPlayer.addListener(this);
        PlayerView playerView = findViewById(R.id.video_view);
        playerView.setPlayer(basicExoPlayer);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        releaseResources();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Uri uri;

        if ( resultCode != Activity.RESULT_OK) {
            return;
        }

        if ( requestCode == REQUEST_CODE_NEW_MEDIA_FROM_DEVICE ) {
            if ( data == null ) {
                // This is an error in this case - we should have a Uri to the chosen video
                Utils.showAlertDialog(R.string.alert_no_video_chosen_title,
                        R.string.alert_no_video_chosen_title,
                        this);
                return;

            }
            // Means the user selected a video using SAF.
            uri = data.getData();
            if (uri == null) {
                Utils.showAlertDialog(R.string.alert_no_video_chosen_title,
                        R.string.alert_no_video_chosen_title,
                        this);
                return;
            }
            videoToPlay.setText(uri.toString());
        }
    }

    // Player Listener
    @Override
    public void onPlayerError(@NotNull ExoPlaybackException error) {
        Timber.d("Lifecycle: onPlayerError: %s", error.toString());
    }

    @Override
    public void onPlaybackStateChanged(int state) {
        String stateMsg = "UNKNOWN";
        switch (state) {
            case STATE_IDLE:
                stateMsg = "IDLE";
                break;
            case STATE_BUFFERING:
                stateMsg = "BUFFERING";
                break;
            case STATE_ENDED:
                stateMsg = "ENDED";
                break;
            case STATE_READY:
                stateMsg = "READY";
                break;
        }
        Timber.d("Lifecycle: onPlaybackStateChanged: %s", stateMsg);
    }

    @Override
    public void onIsLoadingChanged(boolean isLoading) {
        //Timber.d("Lifecycle: onIsLoadingChanged: %s", isLoading ? "true" : "false");
    }

    @Override
    public void onIsPlayingChanged(boolean isPlaying) {
        Timber.d("Lifecycle: onIsPlayingChanged: %s", isPlaying ? "true" : "false");
    }

    // Cache Listener
    @Override
    public void onCachedBytesRead(long cacheSizeBytes, long cachedBytesRead) {
        Timber.d("Lifecycle: onCachedBytesRead called, cacheSizeBytes is %d, cachedBytesRead is %d", cacheSizeBytes, cachedBytesRead);
    }

    @Override
    public void onCacheIgnored(int reason) {
        Timber.d("Lifecycle: onCachedBytesIgnored, reason code: %d", reason);
    }

    private void releaseResources() {
        try {
            basicExoPlayer.stop();
            basicExoPlayer.release();
        } catch (Exception e) {
            Timber.d("Lifecycle: Exception stopping Exoplayer: %s", e.getMessage());
        }
    }

    private void playVideo() {
        if ( chosenVideoUri != null) {
            basicExoPlayer.setPlayWhenReady(false);
            MediaItem mediaItem = MediaItem.fromUri(chosenVideoUri);
            MediaSource mediaSource;
            if (Utils.fileLocationType(chosenVideoUri) == LOCATION_REMOTE) {
                mediaSource = Utils.getCachedMediaSourceFactory(this, this).createMediaSource(mediaItem);
            } else {
                mediaSource = Utils.getDefaultMediaSourceFactory(this).createMediaSource(mediaItem);
            }
            basicExoPlayer.setMediaSource(mediaSource);
            basicExoPlayer.setPlayWhenReady(true);
            basicExoPlayer.prepare();
        }
    }

    private void findVideo() {
        Intent contentSelectionIntent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        contentSelectionIntent.addCategory(Intent.CATEGORY_OPENABLE);
        contentSelectionIntent.setType("*/*");
        contentSelectionIntent.putExtra(Intent.EXTRA_MIME_TYPES, getString(R.string.video_media_type));
        // Now see if user has at least one app that can handle this
        if ( contentSelectionIntent.resolveActivity(getPackageManager()) == null) {
            Utils.showAlertDialog(R.string.alert_no_package_exists_title,
                    R.string.alert_no_package_exists_message,
                    this);
            return;
        }
        startActivityForResult(contentSelectionIntent, REQUEST_CODE_NEW_MEDIA_FROM_DEVICE);
    }
}
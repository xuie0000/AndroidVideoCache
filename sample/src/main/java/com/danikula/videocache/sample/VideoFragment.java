package com.danikula.videocache.sample;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.VideoView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.danikula.videocache.CacheListener;
import com.danikula.videocache.HttpProxyCacheServer;

import java.io.File;

public class VideoFragment extends Fragment implements CacheListener {

    private static final String LOG_TAG = "VideoFragment";

    public static VideoFragment build(String url) {
        VideoFragment fragment = new VideoFragment();
        Bundle bundle = new Bundle();
        bundle.putString("url", url);
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        url = getArguments().getString("url", "");
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_video, container, false);
        cacheStatusImageView = view.findViewById(R.id.cacheStatusImageView);
        videoView = view.findViewById(R.id.videoView);
        progressBar = view.findViewById(R.id.progressBar);

        return view;

    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        checkCachedState();
        startVideo();
        progressBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                int videoPosition = videoView.getDuration() * progressBar.getProgress() / 100;
                videoView.seekTo(videoPosition);
            }
        });
    }

    String url;

    ImageView cacheStatusImageView;
    VideoView videoView;
    SeekBar progressBar;

    private final VideoProgressUpdater updater = new VideoProgressUpdater();

    private void checkCachedState() {
        HttpProxyCacheServer proxy = App.getProxy(getActivity());
        boolean fullyCached = proxy.isCached(url);
        setCachedState(fullyCached);
        if (fullyCached) {
            progressBar.setSecondaryProgress(100);
        }
    }

    private void startVideo() {
        HttpProxyCacheServer proxy = App.getProxy(getActivity());
        proxy.registerCacheListener(this, url);
        String proxyUrl = proxy.getProxyUrl(url);
        Log.d(LOG_TAG, "Use proxy url " + proxyUrl + " instead of original url " + url);
        videoView.setVideoPath(proxyUrl);
        videoView.start();
    }

    @Override
    public void onResume() {
        super.onResume();
        updater.start();
    }

    @Override
    public void onPause() {
        super.onPause();
        updater.stop();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        videoView.stopPlayback();
        App.getProxy(getActivity()).unregisterCacheListener(this);
    }

    @Override
    public void onCacheAvailable(File file, String url, int percentsAvailable) {
        progressBar.setSecondaryProgress(percentsAvailable);
        setCachedState(percentsAvailable == 100);
        Log.d(LOG_TAG, String.format("onCacheAvailable. percents: %d, file: %s, url: %s", percentsAvailable, file, url));
    }

    private void updateVideoProgress() {
        int videoProgress = videoView.getCurrentPosition() * 100 / videoView.getDuration();
        progressBar.setProgress(videoProgress);
    }

    private void setCachedState(boolean cached) {
        int statusIconId = cached ? R.drawable.ic_cloud_done : R.drawable.ic_cloud_download;
        cacheStatusImageView.setImageResource(statusIconId);
    }

    private final class VideoProgressUpdater extends Handler {

        public void start() {
            sendEmptyMessage(0);
        }

        public void stop() {
            removeMessages(0);
        }

        @Override
        public void handleMessage(Message msg) {
            updateVideoProgress();
            sendEmptyMessageDelayed(0, 500);
        }
    }
}

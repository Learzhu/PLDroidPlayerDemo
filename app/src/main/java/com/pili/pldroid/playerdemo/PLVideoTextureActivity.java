package com.pili.pldroid.playerdemo;

import android.content.pm.ActivityInfo;
import android.nfc.Tag;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import com.pili.pldroid.player.AVOptions;
import com.pili.pldroid.player.PLMediaPlayer;
import com.pili.pldroid.player.widget.PLVideoTextureView;
import com.pili.pldroid.player.widget.PLVideoView;
import com.pili.pldroid.playerdemo.utils.Utils;
import com.pili.pldroid.playerdemo.widget.MediaController;

import java.util.logging.Logger;

/**
 * This is a demo activity of PLVideoTextureView
 */
public class PLVideoTextureActivity extends AppCompatActivity {

    private static final int MESSAGE_ID_RECONNECTING = 0x01;

    private MediaController mMediaController;
    private PLVideoTextureView mVideoView;
    private Toast mToast = null;
    private String mVideoPath = null;
    private int mRotation = 0;
    private int mDisplayAspectRatio = PLVideoTextureView.ASPECT_RATIO_FIT_PARENT; //default
    private View mLoadingView;
    private boolean mIsActivityPaused = true;

    private static final String TAG = "PLVideoTextureActivity";
    /**
     * 视频的方向
     */
    private int mVideoRotation;
    private boolean needResume;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_pl_video_texture);
        mVideoView = (PLVideoTextureView) findViewById(R.id.VideoView);

        mLoadingView = findViewById(R.id.LoadingView);
        mVideoView.setBufferingIndicator(mLoadingView);
        mLoadingView.setVisibility(View.VISIBLE);

        mVideoPath = getIntent().getStringExtra("videoPath");

        // If you want to fix display orientation such as landscape, you can use the code show as follow
        //
        // if (this.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
        //     mVideoView.setPreviewOrientation(0);
        // }
        // else if (this.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
        //     mVideoView.setPreviewOrientation(270);
        // }

        mVideoPath = getIntent().getStringExtra("videoPath");

        AVOptions options = new AVOptions();

        int isLiveStreaming = getIntent().getIntExtra("liveStreaming", 1);
        // the unit of timeout is ms
        options.setInteger(AVOptions.KEY_PREPARE_TIMEOUT, 10 * 1000);
        options.setInteger(AVOptions.KEY_GET_AV_FRAME_TIMEOUT, 10 * 1000);
        // Some optimization with buffering mechanism when be set to 1
        options.setInteger(AVOptions.KEY_LIVE_STREAMING, isLiveStreaming);
        if (isLiveStreaming == 1) {
            options.setInteger(AVOptions.KEY_DELAY_OPTIMIZATION, 1);
        }

        // 1 -> hw codec enable, 0 -> disable [recommended]
        int codec = getIntent().getIntExtra("mediaCodec", 0);
        options.setInteger(AVOptions.KEY_MEDIACODEC, codec);

        // whether start play automatically after prepared, default value is 1
        options.setInteger(AVOptions.KEY_START_ON_PREPARED, 0);

        mVideoView.setAVOptions(options);

        // You can mirror the display
        // mVideoView.setMirror(true);

        // You can also use a custom `MediaController` widget
        mMediaController = new MediaController(this, false, isLiveStreaming == 1);
        mVideoView.setMediaController(mMediaController);

        mVideoView.setOnCompletionListener(mOnCompletionListener);
        mVideoView.setOnErrorListener(mOnErrorListener);

        /**
         * 插入的部分为了 实现横竖屏切换  防止竖着拍摄的视频横向播放被放大
         */
        mVideoView.setOnInfoListener(mOnInfoListener);
//        mVideoView.setOnVideoSizeChangedListener(mOnVideoSizeChangedListener);
        mVideoView.setOnBufferingUpdateListener(mOnBufferingUpdateListener);
        mVideoView.setOnSeekCompleteListener(mOnSeekCompleteListener);
        mVideoView.setVideoPath(mVideoPath);
        mVideoView.setDisplayAspectRatio(PLVideoView.ASPECT_RATIO_PAVED_PARENT);
        mVideoView.setOnPreparedListener(mOnPreparedListener);
        mVideoView.setOnVideoSizeChangedListener(new PLMediaPlayer.OnVideoSizeChangedListener() {
            @Override
            public void onVideoSizeChanged(PLMediaPlayer plMediaPlayer, int width, int height) {
//                Logger.i("width:" + width + "---heightL:" + height);
                Log.i("PLVideo", "width:" + width + "---heightL:" + height);
//                if (width > height) {
//                    //视频是横屏 旋转方向
//                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
//                }
                if (width > height && mVideoRotation == 0) {
                    //旋转方向
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                }
                //如果视频角度是90度
                if (mVideoRotation == 90) {
                    //旋转视频
                    mVideoView.setDisplayOrientation(270);
                }

            }
        });

        mVideoView.setVideoPath(mVideoPath);
        mVideoView.start();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mToast = null;
        mVideoView.pause();
        mIsActivityPaused = true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        mIsActivityPaused = false;
        mVideoView.start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mVideoView.stopPlayback();
    }

    public void onClickRotate(View v) {
        mRotation = (mRotation + 90) % 360;
        mVideoView.setDisplayOrientation(mRotation);
    }

    public void onClickSwitchScreen(View v) {
        mDisplayAspectRatio = (mDisplayAspectRatio + 1) % 5;
        mVideoView.setDisplayAspectRatio(mDisplayAspectRatio);
        switch (mVideoView.getDisplayAspectRatio()) {
            case PLVideoTextureView.ASPECT_RATIO_ORIGIN:
                showToastTips("Origin mode");
                break;
            case PLVideoTextureView.ASPECT_RATIO_FIT_PARENT:
                showToastTips("Fit parent !");
                break;
            case PLVideoTextureView.ASPECT_RATIO_PAVED_PARENT:
                showToastTips("Paved parent !");
                break;
            case PLVideoTextureView.ASPECT_RATIO_16_9:
                showToastTips("16 : 9 !");
                break;
            case PLVideoTextureView.ASPECT_RATIO_4_3:
                showToastTips("4 : 3 !");
                break;
            default:
                break;
        }
    }

    private PLMediaPlayer.OnErrorListener mOnErrorListener = new PLMediaPlayer.OnErrorListener() {
        @Override
        public boolean onError(PLMediaPlayer mp, int errorCode) {
            boolean isNeedReconnect = false;
            switch (errorCode) {
                case PLMediaPlayer.ERROR_CODE_INVALID_URI:
                    showToastTips("Invalid URL !");
                    break;
                case PLMediaPlayer.ERROR_CODE_404_NOT_FOUND:
                    showToastTips("404 resource not found !");
                    break;
                case PLMediaPlayer.ERROR_CODE_CONNECTION_REFUSED:
                    showToastTips("Connection refused !");
                    break;
                case PLMediaPlayer.ERROR_CODE_CONNECTION_TIMEOUT:
                    showToastTips("Connection timeout !");
                    isNeedReconnect = true;
                    break;
                case PLMediaPlayer.ERROR_CODE_EMPTY_PLAYLIST:
                    showToastTips("Empty playlist !");
                    break;
                case PLMediaPlayer.ERROR_CODE_STREAM_DISCONNECTED:
                    showToastTips("Stream disconnected !");
                    isNeedReconnect = true;
                    break;
                case PLMediaPlayer.ERROR_CODE_IO_ERROR:
                    showToastTips("Network IO Error !");
                    isNeedReconnect = true;
                    break;
                case PLMediaPlayer.ERROR_CODE_UNAUTHORIZED:
                    showToastTips("Unauthorized Error !");
                    break;
                case PLMediaPlayer.ERROR_CODE_PREPARE_TIMEOUT:
                    showToastTips("Prepare timeout !");
                    isNeedReconnect = true;
                    break;
                case PLMediaPlayer.ERROR_CODE_READ_FRAME_TIMEOUT:
                    showToastTips("Read frame timeout !");
                    isNeedReconnect = true;
                    break;
                case PLMediaPlayer.MEDIA_ERROR_UNKNOWN:
                    break;
                default:
                    showToastTips("unknown error !");
                    break;
            }
            // Todo pls handle the error status here, reconnect or call finish()
            if (isNeedReconnect) {
                sendReconnectMessage();
            } else {
                finish();
            }
            // If you want to retry, do like this:
            // mVideoView.setVideoPath(mVideoPath);
            // mVideoView.start();

            // Return true means the error has been handled
            // If return false, then `onCompletion` will be called
            return true;
        }
    };

    private PLMediaPlayer.OnCompletionListener mOnCompletionListener = new PLMediaPlayer.OnCompletionListener() {
        @Override
        public void onCompletion(PLMediaPlayer plMediaPlayer) {
            showToastTips("Play Completed !");
            finish();
        }
    };

    private void showToastTips(final String tips) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mToast != null) {
                    mToast.cancel();
                }
                mToast = Toast.makeText(PLVideoTextureActivity.this, tips, Toast.LENGTH_SHORT);
                mToast.show();
            }
        });
    }

    private void sendReconnectMessage() {
        showToastTips("正在重连...");
        mLoadingView.setVisibility(View.VISIBLE);
        mHandler.removeCallbacksAndMessages(null);
        mHandler.sendMessageDelayed(mHandler.obtainMessage(MESSAGE_ID_RECONNECTING), 500);
    }

    protected Handler mHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what != MESSAGE_ID_RECONNECTING) {
                return;
            }
            if (mIsActivityPaused || !Utils.isLiveStreamingAvailable()) {
                finish();
                return;
            }
            if (!Utils.isNetworkAvailable(PLVideoTextureActivity.this)) {
                sendReconnectMessage();
                return;
            }
            mVideoView.setVideoPath(mVideoPath);
            mVideoView.start();
        }
    };
    /**
     * 以下部分为实现横竖屏
     */
    private PLMediaPlayer.OnBufferingUpdateListener mOnBufferingUpdateListener = new PLMediaPlayer.OnBufferingUpdateListener() {
        @Override
        public void onBufferingUpdate(PLMediaPlayer plMediaPlayer, int precent) {
        }
    };

    private PLMediaPlayer.OnSeekCompleteListener mOnSeekCompleteListener = new PLMediaPlayer.OnSeekCompleteListener() {
        @Override
        public void onSeekComplete(PLMediaPlayer plMediaPlayer) {
//            Logger.d("onSeekComplete !");
            Log.d(TAG, "onSeekComplete !");
        }

    };

    private PLMediaPlayer.OnPreparedListener mOnPreparedListener = new PLMediaPlayer.OnPreparedListener() {
        @Override
        public void onPrepared(PLMediaPlayer plMediaPlayer) {


        }
    };
    private PLMediaPlayer.OnInfoListener mOnInfoListener = new PLMediaPlayer.OnInfoListener() {
        @Override
        public boolean onInfo(PLMediaPlayer plMediaPlayer, int what, int extra) {
            switch (what) {
                case PLMediaPlayer.MEDIA_INFO_BUFFERING_START:
//                    Logger.i("正在缓冲----");
                    Log.i(TAG, "正在缓冲----");
                    //开始缓存，暂停播放
                    if (isPlaying()) {
//                        stopPlayer();
                        if (mVideoView != null) {
                            mVideoView.pause();
                        }
                        needResume = true;
                    }
                    mLoadingView.setVisibility(View.VISIBLE);
                    break;
                case PLMediaPlayer.MEDIA_INFO_BUFFERING_END:
                case PLMediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START:
//                    Logger.i("缓冲完成-- --");
                    Log.i(TAG, "缓冲完成-- --");
                    //缓存完成，继续播放
                    if (needResume)
//                        startPlayer();
                        if (mVideoView != null) {
                            mVideoView.start();
                        }
                    mLoadingView.setVisibility(View.GONE);
                    break;
                case PLMediaPlayer.MEDIA_INFO_BUFFERING_BYTES_UPDATE:
                    //显示 下载速度
//                    Logger.e("download rate:" + extra);
                    Log.e(TAG, "download rate:" + extra);
                    //mListener.onDownloadRateChanged(arg2);
                    break;
            }
//            Logger.i("onInfo:" + what + "___" + extra);
            Log.i(TAG, "onInfo:" + what + "___" + extra);
            return false;
        }
    };

    private boolean isPlaying() {
        return mMediaController.isShowing();
    }

}

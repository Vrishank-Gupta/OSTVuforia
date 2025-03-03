package com.vrishank.gupta.exploreworld.VideoPlayback.app.VideoPlayback;

import java.util.concurrent.locks.ReentrantLock;

import com.vrishank.gupta.exploreworld.VideoPlayback.R;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.media.MediaPlayer.OnVideoSizeChangedListener;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.GestureDetector.OnDoubleTapListener;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.widget.MediaController;
import android.widget.VideoView;


public class FullscreenPlayback extends Activity implements OnPreparedListener,
    SurfaceHolder.Callback, OnVideoSizeChangedListener, OnErrorListener,
    OnCompletionListener
{
    private static final String LOGTAG = "FullscreenPlayback";
    
    private VideoView mVideoView = null;
    private MediaPlayer mMediaPlayer = null;
    private SurfaceHolder mHolder = null;
    private MediaController mMediaController = null;
    private String mMovieName = "";
    private int mSeekPosition = 0;
    private int mRequestedOrientation = 0;
    private GestureDetector mGestureDetector = null;
    private boolean mShouldPlayImmediately = false;
    private SimpleOnGestureListener mSimpleListener = null;
    private ReentrantLock mMediaPlayerLock = null;
    private ReentrantLock mMediaControllerLock = null;
    
    
    protected void prepareViewForMediaPlayer()
    {
        mVideoView = (VideoView) findViewById(R.id.surface_view);
        
        setRequestedOrientation(mRequestedOrientation);
        
        mHolder = mVideoView.getHolder();
        mHolder.addCallback(this);
    }
    
    
    protected void onCreate(Bundle savedInstanceState)
    {
        Log.d(LOGTAG, "Fullscreen.onCreate");
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.fullscreen_layout);
        
        mMediaControllerLock = new ReentrantLock();
        mMediaPlayerLock = new ReentrantLock();
        
        prepareViewForMediaPlayer();
        
        mSeekPosition = getIntent().getIntExtra("currentSeekPosition", 0);
        mMovieName = getIntent().getStringExtra("movieName");
        mRequestedOrientation = getIntent().getIntExtra("requestedOrientation",
            0);
        mShouldPlayImmediately = getIntent().getBooleanExtra(
            "shouldPlayImmediately", false);
        
        // Create a gesture detector that will handle single and double taps:
        mSimpleListener = new SimpleOnGestureListener();
        mGestureDetector = new GestureDetector(getApplicationContext(),
            mSimpleListener);
        
        // We assign the actions for the single and double taps:
        mGestureDetector.setOnDoubleTapListener(new OnDoubleTapListener()
        {
            public boolean onDoubleTap(MotionEvent e)
            {
                return false;
            }
            
            
            public boolean onDoubleTapEvent(MotionEvent e)
            {
                return false;
            }
            
            
            public boolean onSingleTapConfirmed(MotionEvent e)
            {
                boolean result = false;
                mMediaControllerLock.lock();
                // This simply toggles the MediaController visibility:
                if (mMediaController != null)
                {
                    if (mMediaController.isShowing())
                        mMediaController.hide();
                    else
                        mMediaController.show();
                    
                    result = true;
                }
                mMediaControllerLock.unlock();
                
                return result;
            }
        });
        
    }
    
    
    private void createMediaPlayer()
    {
        mMediaPlayerLock.lock();
        mMediaControllerLock.lock();
        
        try
        {
            mMediaPlayer = new MediaPlayer();
            mMediaController = new MediaController(this);
            

             mMediaPlayer.setDataSource(mMovieName);
            
            mMediaPlayer.setDisplay(mHolder);
            mMediaPlayer.prepareAsync();
            mMediaPlayer.setOnPreparedListener(this);
            mMediaPlayer.setOnVideoSizeChangedListener(this);
            mMediaPlayer.setOnErrorListener(this);
            mMediaPlayer.setOnCompletionListener(this);
            mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            
        } catch (Exception e)
        {
            Log.e(LOGTAG,
                "Error while creating the MediaPlayer: " + e.toString());
            
            prepareForTermination();
            
            destroyMediaPlayer();
            
            finish();
        }
        
        mMediaControllerLock.unlock();
        mMediaPlayerLock.unlock();
    }
    
    
    public boolean onTouchEvent(MotionEvent event)
    {
        return mGestureDetector.onTouchEvent(event);
    }
    
    
    public void onPrepared(MediaPlayer mediaplayer)
    {
        // Log.d( LOGTAG, "Fullscreen.onPrepared");
        
        mMediaControllerLock.lock();
        mMediaPlayerLock.lock();
        
        if ((mMediaController != null) && (mVideoView != null)
            && (mMediaPlayer != null))
        {
            if (mVideoView.getParent() != null)
            {
                mMediaController.setMediaPlayer(player_interface);
                
                View anchorView = mVideoView.getParent() instanceof View ? (View) mVideoView
                    .getParent() : mVideoView;
                mMediaController.setAnchorView(anchorView);
                mVideoView.setMediaController(mMediaController);
                mMediaController.setEnabled(true);
                
                try
                {
                    mMediaPlayer.seekTo(mSeekPosition);
                } catch (Exception e)
                {
                    mMediaPlayerLock.unlock();
                    mMediaControllerLock.unlock();
                    Log.e(LOGTAG, "Could not seek to a position");
                }
                
                if (mShouldPlayImmediately)
                {
                    try
                    {
                        mMediaPlayer.start();
                        mShouldPlayImmediately = false;
                    } catch (Exception e)
                    {
                        mMediaPlayerLock.unlock();
                        mMediaControllerLock.unlock();
                        Log.e(LOGTAG, "Could not start playback");
                    }
                }
                
                mMediaController.show();
            }
        }
        
        mMediaPlayerLock.unlock();
        mMediaControllerLock.unlock();
    }
    
    
    private void destroyMediaPlayer()
    {
        mMediaControllerLock.lock();
        if (mMediaController != null)
        {
            mMediaController.removeAllViews();
            mMediaController = null;
        }
        mMediaControllerLock.unlock();
        
        mMediaPlayerLock.lock();
        if (mMediaPlayer != null)
        {
            try
            {
                mMediaPlayer.stop();
            } catch (Exception e)
            {
                mMediaPlayerLock.unlock();
                Log.e(LOGTAG, "Could not stop playback");
            }
            mMediaPlayer.release();
            mMediaPlayer = null;
        }
        mMediaPlayerLock.unlock();
    }
    
    
    private void destroyView()
    {
        mVideoView = null;
        mHolder = null;
    }
    
    
    protected void onDestroy()
    {
        // Log.d( LOGTAG, "Fullscreen.onDestroy");
        
        prepareForTermination();
        
        super.onDestroy();
        
        destroyMediaPlayer();
        
        mMediaPlayerLock = null;
        mMediaControllerLock = null;
    }
    
    
    protected void onResume()
    {
        // Log.d( LOGTAG, "Fullscreen.onResume");
        super.onResume();
        
        prepareViewForMediaPlayer();
    }
    
    
    public void onConfigurationChanged(Configuration config)
    {
        super.onConfigurationChanged(config);
    }
    
    
    private void prepareForTermination()
    {
        mMediaControllerLock.lock();
        if (mMediaController != null)
        {
            mMediaController.hide();
            mMediaController.removeAllViews();
        }
        mMediaControllerLock.unlock();
        
        mMediaPlayerLock.lock();
        if (mMediaPlayer != null)
        {
            mSeekPosition = mMediaPlayer.getCurrentPosition();
            
            boolean wasPlaying = mMediaPlayer.isPlaying();
            if (wasPlaying)
            {
                try
                {
                    mMediaPlayer.pause();
                } catch (Exception e)
                {
                    mMediaPlayerLock.unlock();
                    Log.e(LOGTAG, "Could not pause playback");
                }
            }
            
            Intent i = new Intent();
            i.putExtra("movieName", mMovieName);
            i.putExtra("currentSeekPosition", mSeekPosition);
            i.putExtra("playing", wasPlaying);
            setResult(Activity.RESULT_OK, i);
        }
        mMediaPlayerLock.unlock();
    }
    
    
    public void onBackPressed()
    {
        prepareForTermination();
        super.onBackPressed();
    }
    
    
    protected void onPause()
    {
        // Log.d( LOGTAG, "Fullscreen.onPause");
        super.onPause();
        
        prepareForTermination();
        
        destroyMediaPlayer();
        
        destroyView();
    }
    
    
    public void surfaceCreated(SurfaceHolder holder)
    {
        createMediaPlayer();
    }
    
    
    public void surfaceChanged(SurfaceHolder holder, int format, int width,
        int height)
    {
    }
    
    
    public void surfaceDestroyed(SurfaceHolder holder)
    {
    }
    
    private MediaController.MediaPlayerControl player_interface = new MediaController.MediaPlayerControl()
    {
        public int getBufferPercentage()
        {
            return 100;
        }
        
        
        public int getCurrentPosition()
        {
            int result = 0;
            mMediaPlayerLock.lock();
            if (mMediaPlayer != null)
                result = mMediaPlayer.getCurrentPosition();
            mMediaPlayerLock.unlock();
            return result;
        }
        
        
        public int getDuration()
        {
            int result = 0;
            mMediaPlayerLock.lock();
            if (mMediaPlayer != null)
                result = mMediaPlayer.getDuration();
            mMediaPlayerLock.unlock();
            return result;
        }
        
        
        public boolean isPlaying()
        {
            boolean result = false;
            
            mMediaPlayerLock.lock();
            if (mMediaPlayer != null)
                result = mMediaPlayer.isPlaying();
            mMediaPlayerLock.unlock();
            return result;
        }
        
        
        public void pause()
        {
            mMediaPlayerLock.lock();
            if (mMediaPlayer != null)
            {
                try
                {
                    mMediaPlayer.pause();
                } catch (Exception e)
                {
                    mMediaPlayerLock.unlock();
                    Log.e(LOGTAG, "Could not pause playback");
                }
            }
            mMediaPlayerLock.unlock();
        }
        
        
        public void seekTo(int pos)
        {
            mMediaPlayerLock.lock();
            if (mMediaPlayer != null)
            {
                try
                {
                    mMediaPlayer.seekTo(pos);
                } catch (Exception e)
                {
                    mMediaPlayerLock.unlock();
                    Log.e(LOGTAG, "Could not seek to position");
                }
            }
            mMediaPlayerLock.unlock();
        }
        
        
        public void start()
        {
            mMediaPlayerLock.lock();
            if (mMediaPlayer != null)
            {
                try
                {
                    mMediaPlayer.start();
                } catch (Exception e)
                {
                    mMediaPlayerLock.unlock();
                    Log.e(LOGTAG, "Could not start playback");
                }
            }
            mMediaPlayerLock.unlock();
        }
        
        
        public boolean canPause()
        {
            return true;
        }
        
        
        public boolean canSeekBackward()
        {
            return true;
        }
        
        
        public boolean canSeekForward()
        {
            return true;
        }
        
        
        public int getAudioSessionId()
        {
            return 0;
        }
    };
    
    
    public void onVideoSizeChanged(MediaPlayer mp, int width, int height)
    {
    }
    
    
    public boolean onError(MediaPlayer mp, int what, int extra)
    {
        if (mp == mMediaPlayer)
        {
            String errorDescription;
            
            switch (what)
            {
                case MediaPlayer.MEDIA_ERROR_NOT_VALID_FOR_PROGRESSIVE_PLAYBACK:
                    errorDescription = "The video is streamed and its container is not valid for progressive playback";
                    break;
                
                case MediaPlayer.MEDIA_ERROR_SERVER_DIED:
                    errorDescription = "Media server died";
                    break;
                
                case MediaPlayer.MEDIA_ERROR_UNKNOWN:
                    errorDescription = "Unspecified media player error";
                    break;
                
                default:
                    errorDescription = "Unknown error " + what;
                    break;
            }
            
            Log.e(LOGTAG, "Error while opening the file for fullscreen. "
                + "Unloading the media player (" + errorDescription + ", "
                + extra + ")");
            
            prepareForTermination();
            
            destroyMediaPlayer();
            
            finish();
            
            return true;
        }
        
        return false;
    }


    @Override
    public void onCompletion(MediaPlayer mp)
    {
        prepareForTermination();
        finish();
    }
}

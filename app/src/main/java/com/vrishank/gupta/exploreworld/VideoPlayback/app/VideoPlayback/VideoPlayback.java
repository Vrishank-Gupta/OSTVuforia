package com.vrishank.gupta.exploreworld.VideoPlayback.app.VideoPlayback;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.GestureDetector;
import android.view.GestureDetector.OnDoubleTapListener;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.RelativeLayout;

import com.qualcomm.vuforia.CameraDevice;
import com.qualcomm.vuforia.ObjectTracker;
import com.qualcomm.vuforia.State;
import com.qualcomm.vuforia.TargetFinder;
import com.qualcomm.vuforia.TargetSearchResult;
import com.qualcomm.vuforia.Trackable;
import com.qualcomm.vuforia.Tracker;
import com.qualcomm.vuforia.TrackerManager;
import com.qualcomm.vuforia.Vuforia;
import com.vrishank.gupta.exploreworld.Application1.AppControl;
import com.vrishank.gupta.exploreworld.Application1.SampleApplicationException;
import com.vrishank.gupta.exploreworld.Application1.SampleApplicationSession;
import com.vrishank.gupta.exploreworld.Application1.utils.LoadingDialogHandler;
import com.vrishank.gupta.exploreworld.Application1.utils.SampleApplicationGLView;
import com.vrishank.gupta.exploreworld.Application1.utils.Texture;
import com.vrishank.gupta.exploreworld.VideoPlayback.R;
import com.vrishank.gupta.exploreworld.VideoPlayback.app.VideoPlayback.VideoPlayerHelper.MEDIA_STATE;
import com.vrishank.gupta.exploreworld.VideoPlayback.ui.ActivityList.AboutScreen;
import com.vrishank.gupta.exploreworld.VideoPlayback.ui.SampleAppMenu.SampleAppMenu;
import com.vrishank.gupta.exploreworld.VideoPlayback.ui.SampleAppMenu.SampleAppMenuGroup;
import com.vrishank.gupta.exploreworld.VideoPlayback.ui.SampleAppMenu.SampleAppMenuInterface;

import java.util.Vector;


public class VideoPlayback extends Activity implements
        AppControl, SampleAppMenuInterface
{
    private static final String LOGTAG = "VideoPlayback";
    
    SampleApplicationSession vuforiaAppSession;
    
    Activity mActivity;
    
    private GestureDetector mGestureDetector = null;
    private SimpleOnGestureListener mSimpleListener = null;
    
    public static final int NUM_TARGETS = 2;
    public static final int STONES = 0;
    public static final int CHIPS = 1;
    private VideoPlayerHelper mVideoPlayerHelper = null;
    private int mSeekPosition;
    private boolean mWasPlaying;
    private String mMovieName;
    
    private boolean mReturningFromFullScreen = false;
    
    private SampleApplicationGLView mGlView;
    
    private VideoPlaybackRenderer mRenderer;
    
    private Vector<Texture> mTextures;

    private RelativeLayout mUILayout;
    
    private boolean mPlayFullscreenVideo = false;
    
    private SampleAppMenu mSampleAppMenu;
    
    private LoadingDialogHandler loadingDialogHandler = new LoadingDialogHandler(
        this);
    
    private AlertDialog mErrorDialog;
    
    boolean mIsDroidDevice = false;
    boolean mIsInitialized = false;

    boolean mFinderStarted = false;
    static final int UPDATE_ERROR_NO_NETWORK_CONNECTION = -3;
    static final int UPDATE_ERROR_SERVICE_NOT_AVAILABLE = -4;
    private boolean mExtendedTracking = false;
    private static final String kAccessKey = "2d131fce3791b5f03f3ccd0dbdf9e5e6d99cf2fd";
    private static final String kSecretKey = "d0a6c51077ad70d519f2c3337e5533ea8e1295d8";
    private int mInitErrorCode = 0;

    protected void onCreate(Bundle savedInstanceState)
    {
        Log.d(LOGTAG, "onCreate");
        super.onCreate(savedInstanceState);
        
        vuforiaAppSession = new SampleApplicationSession(this);
        
        mActivity = this;
        
        startLoadingAnimation();
        
        vuforiaAppSession
            .initAR(this, ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        
        mTextures = new Vector<Texture>();
        loadTextures();
        mSimpleListener = new SimpleOnGestureListener();
        mGestureDetector = new GestureDetector(getApplicationContext(),
            mSimpleListener);
        
        mVideoPlayerHelper = null;
        mSeekPosition = 0;
        mWasPlaying = false;
        mMovieName = "";


        mVideoPlayerHelper = new VideoPlayerHelper();
        mVideoPlayerHelper.init();
        mVideoPlayerHelper.setActivity(this);

        
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
                boolean isSingleTapHandled = false;

                if (mRenderer!= null && mRenderer.isTapOnScreenInsideTarget(e.getX(),
                    e.getY()))
                {
                    if (mVideoPlayerHelper.isPlayableOnTexture())
                    {
                        if ((mVideoPlayerHelper.getStatus() == MEDIA_STATE.PAUSED)
                            || (mVideoPlayerHelper.getStatus() == MEDIA_STATE.READY)
                            || (mVideoPlayerHelper.getStatus() == MEDIA_STATE.STOPPED)
                            || (mVideoPlayerHelper.getStatus() == MEDIA_STATE.REACHED_END))
                        {
                            if ((mVideoPlayerHelper.getStatus() == MEDIA_STATE.REACHED_END))
                                mSeekPosition = 0;

                            mVideoPlayerHelper.play(mPlayFullscreenVideo,
                                mSeekPosition);
                            mSeekPosition = VideoPlayerHelper.CURRENT_POSITION;
                        } else if (mVideoPlayerHelper.getStatus() == MEDIA_STATE.PLAYING)
                        {
                            mVideoPlayerHelper.pause();
                        }
                    } else if (mVideoPlayerHelper.isPlayableFullscreen())
                    {

                        mVideoPlayerHelper.play(true,
                            VideoPlayerHelper.CURRENT_POSITION);
                    }

                    isSingleTapHandled = true;
                }

                return isSingleTapHandled;
            }
        });
    }
    
    

    private void loadTextures()
    {
        mTextures.add(Texture.loadTextureFromApk(
                "VideoPlayback/VuforiaSizzleReel_1.png", getAssets()));
        mTextures.add(Texture.loadTextureFromApk("VideoPlayback/play.png",
            getAssets()));
        mTextures.add(Texture.loadTextureFromApk("VideoPlayback/busy.png",
            getAssets()));
        mTextures.add(Texture.loadTextureFromApk("VideoPlayback/error.png",
            getAssets()));
    }
    
    
    protected void onResume()
    {
        Log.d(LOGTAG, "onResume");
        super.onResume();
        
        if (mIsDroidDevice)
        {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }
        
        try
        {
            vuforiaAppSession.resumeAR();
        } catch (SampleApplicationException e)
        {
            Log.e(LOGTAG, e.getString());
        }
        
        // Resume the GL view
        if (mGlView != null)
        {
            mGlView.setVisibility(View.VISIBLE);
            mGlView.onResume();
        }
        
        // Reload all the movies
        if (mRenderer != null)
        {

            if (!mReturningFromFullScreen)
            {
                mRenderer.requestLoad(mMovieName, mSeekPosition,
                    false);
            } else
            {
                mRenderer.requestLoad(mMovieName, mSeekPosition,
                    mWasPlaying);
            }

        }
        
        mReturningFromFullScreen = false;
    }
    
    // Called when returning from the full screen player
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        if (requestCode == 1)
        {
            
            mActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            
            if (resultCode == RESULT_OK)
            {
                String movieBeingPlayed = data.getStringExtra("movieName");
                mReturningFromFullScreen = true;


                if (movieBeingPlayed.compareTo(mMovieName) == 0)
                {
                    mSeekPosition = data.getIntExtra(
                        "currentSeekPosition", 0);
                    mWasPlaying = data.getBooleanExtra("playing", false);
                }

            }
        }
    }
    
    
    public void onConfigurationChanged(Configuration config)
    {
        Log.d(LOGTAG, "onConfigurationChanged");
        super.onConfigurationChanged(config);
        
        vuforiaAppSession.onConfigurationChanged();
    }
    
    
    protected void onPause()
    {
        Log.d(LOGTAG, "onPause");
        super.onPause();
        
        if (mGlView != null)
        {
            mGlView.setVisibility(View.INVISIBLE);
            mGlView.onPause();
        }

        if (mVideoPlayerHelper.isPlayableOnTexture()) {
            mSeekPosition = mVideoPlayerHelper.getCurrentPosition();
            mWasPlaying = mVideoPlayerHelper.getStatus() == MEDIA_STATE.PLAYING ? true : false;
        }
        if (mVideoPlayerHelper != null)
            mVideoPlayerHelper.unload();

        
        mReturningFromFullScreen = false;
        
        try
        {
            vuforiaAppSession.pauseAR();
        } catch (SampleApplicationException e)
        {
            Log.e(LOGTAG, e.getString());
        }
    }
    
    
    protected void onDestroy()
    {
        Log.d(LOGTAG, "onDestroy");
        super.onDestroy();
        
        if (mVideoPlayerHelper != null)
            mVideoPlayerHelper.deinit();
        mVideoPlayerHelper = null;

        try
        {
            vuforiaAppSession.stopAR();
        } catch (SampleApplicationException e)
        {
            Log.e(LOGTAG, e.getString());
        }
        
        // Unload texture:
        mTextures.clear();
        mTextures = null;
        
        System.gc();
    }

    
    public void onBackPressed()
    {
//        pauseAll(-1);
        super.onBackPressed();
    }
    
    
    private void startLoadingAnimation()
    {
        LayoutInflater inflater = LayoutInflater.from(this);
        mUILayout = (RelativeLayout) inflater.inflate(R.layout.camera_overlay,
            null, false);
        
        mUILayout.setVisibility(View.VISIBLE);
        mUILayout.setBackgroundColor(Color.BLACK);
        
        loadingDialogHandler.mLoadingDialogContainer = mUILayout
            .findViewById(R.id.loading_indicator);
        
        loadingDialogHandler
            .sendEmptyMessage(LoadingDialogHandler.SHOW_LOADING_DIALOG);
        
        addContentView(mUILayout, new LayoutParams(LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT));
    }
    
    
    private void initApplicationAR()
    {
        int depthSize = 16;
        int stencilSize = 0;
        boolean translucent = Vuforia.requiresAlpha();
        
        mGlView = new SampleApplicationGLView(this);
        mGlView.init(translucent, depthSize, stencilSize);
        
        mRenderer = new VideoPlaybackRenderer(this, vuforiaAppSession);
        mRenderer.setTextures(mTextures);

        mRenderer.setVideoPlayerHelper(mVideoPlayerHelper);

        
        mGlView.setRenderer(mRenderer);


        float[] temp = { 0f, 0f, 0f };
        mRenderer.targetPositiveDimensions.setData(temp);
        mRenderer.videoPlaybackTextureID[0] = -1;

        
    }
    
    

    public boolean onTouchEvent(MotionEvent event)
    {
        boolean result = false;
        if ( mSampleAppMenu != null )
            result = mSampleAppMenu.processEvent(event);
        
        // Process the Gestures
        if (!result)
            mGestureDetector.onTouchEvent(event);
        
        return result;
    }

    public void startFinderIfStopped()
    {
        if(!mFinderStarted)
        {
            mFinderStarted = true;

            // Get the object tracker:
            TrackerManager trackerManager = TrackerManager.getInstance();
            ObjectTracker objectTracker = (ObjectTracker) trackerManager
                    .getTracker(ObjectTracker.getClassType());

            // Initialize target finder:
            TargetFinder targetFinder = objectTracker.getTargetFinder();

            targetFinder.clearTrackables();
            targetFinder.startRecognition();
        }
    }


    public void stopFinderIfStarted()
    {
        if(mFinderStarted)
        {
            mFinderStarted = false;

            // Get the object tracker:
            TrackerManager trackerManager = TrackerManager.getInstance();
            ObjectTracker objectTracker = (ObjectTracker) trackerManager
                    .getTracker(ObjectTracker.getClassType());

            // Initialize target finder:
            TargetFinder targetFinder = objectTracker.getTargetFinder();

            targetFinder.stop();
        }
    }


    @Override
    public boolean doInitTrackers()
    {
        TrackerManager tManager = TrackerManager.getInstance();
        Tracker tracker;

        // Indicate if the trackers were initialized correctly
        boolean result = true;

        tracker = tManager.initTracker(ObjectTracker.getClassType());
        if (tracker == null)
        {
            Log.e(
                    LOGTAG,
                    "Tracker not initialized. Tracker already initialized or the camera is already started");
            result = false;
        } else
        {
            Log.i(LOGTAG, "Tracker successfully initialized");
        }

        return result;
    }


    @Override
    public boolean doLoadTrackersData()
    {
        Log.d(LOGTAG, "initCloudReco");

        // Get the object tracker:
        TrackerManager trackerManager = TrackerManager.getInstance();
        ObjectTracker objectTracker = (ObjectTracker) trackerManager
                .getTracker(ObjectTracker.getClassType());

        // Initialize target finder:
        TargetFinder targetFinder = objectTracker.getTargetFinder();

        // Start initialization:
        if (targetFinder.startInit(kAccessKey, kSecretKey))
        {
            targetFinder.waitUntilInitFinished();
        }

        int resultCode = targetFinder.getInitState();
        if (resultCode != TargetFinder.INIT_SUCCESS)
        {
            if(resultCode == TargetFinder.INIT_ERROR_NO_NETWORK_CONNECTION)
            {
                mInitErrorCode = UPDATE_ERROR_NO_NETWORK_CONNECTION;
            }
            else
            {
                mInitErrorCode = UPDATE_ERROR_SERVICE_NOT_AVAILABLE;
            }

            Log.e(LOGTAG, "Failed to initialize target finder.");
            return false;
        }

        // Use the following calls if you would like to customize the color of
        // the UI
        // targetFinder->setUIScanlineColor(1.0, 0.0, 0.0);
        // targetFinder->setUIPointColor(0.0, 0.0, 1.0);

        return true;
    }


    @Override
    public boolean doDeinitTrackers()
    {
        // Indicate if the trackers were deinitialized correctly
        boolean result = true;

        TrackerManager tManager = TrackerManager.getInstance();
        tManager.deinitTracker(ObjectTracker.getClassType());

        return result;
    }

    @Override
    public boolean doStartTrackers()
    {
        // Indicate if the trackers were started correctly
        boolean result = true;

        // Start the tracker:
        TrackerManager trackerManager = TrackerManager.getInstance();
        ObjectTracker objectTracker = (ObjectTracker) trackerManager
                .getTracker(ObjectTracker.getClassType());
        objectTracker.start();

        // Start cloud based recognition if we are in scanning mode:
        TargetFinder targetFinder = objectTracker.getTargetFinder();
        targetFinder.startRecognition();
        mFinderStarted = true;

        return result;
    }


    @Override
    public boolean doStopTrackers()
    {
        // Indicate if the trackers were stopped correctly
        boolean result = true;

        TrackerManager trackerManager = TrackerManager.getInstance();
        ObjectTracker objectTracker = (ObjectTracker) trackerManager
                .getTracker(ObjectTracker.getClassType());

        if(objectTracker != null)
        {
            objectTracker.stop();

            // Stop cloud based recognition:
            TargetFinder targetFinder = objectTracker.getTargetFinder();
            targetFinder.stop();
            mFinderStarted = false;

            // Clears the trackables
            targetFinder.clearTrackables();
        }
        else
        {
            result = false;
        }

        return result;
    }

    @Override
    public boolean doUnloadTrackersData()
    {
        return true;
    }

    @Override
    public void onInitARDone(SampleApplicationException exception)
    {
        
        if (exception == null)
        {
            initApplicationAR();
            
            mRenderer.mIsActive = true;
            
            // Now add the GL surface view. It is important
            // that the OpenGL ES surface view gets added
            // BEFORE the camera is started and video
            // background is configured.
            addContentView(mGlView, new LayoutParams(LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT));
            
            // Sets the UILayout to be drawn in front of the camera
            mUILayout.bringToFront();
            
            // Hides the Loading Dialog
            loadingDialogHandler
                .sendEmptyMessage(LoadingDialogHandler.HIDE_LOADING_DIALOG);
            
            // Sets the layout background to transparent
            mUILayout.setBackgroundColor(Color.TRANSPARENT);
            
            try
            {
                vuforiaAppSession.startAR(CameraDevice.CAMERA.CAMERA_DEFAULT);
            } catch (SampleApplicationException e)
            {
                Log.e(LOGTAG, e.getString());
            }
            
            boolean result = CameraDevice.getInstance().setFocusMode(
                CameraDevice.FOCUS_MODE.FOCUS_MODE_CONTINUOUSAUTO);
            
            if (!result)
                Log.e(LOGTAG, "Unable to enable continuous autofocus");
            
            mSampleAppMenu = new SampleAppMenu(this, this, "ExploreWorld",
                mGlView, mUILayout, null);
            setSampleAppMenuSettings();
            
            mIsInitialized = true;
            
        } else
        {
            Log.e(LOGTAG, exception.getString());
            showInitializationErrorMessage(exception.getString());
        }
        
    }
    
    
    // Shows initialization error messages as System dialogs
    public void showInitializationErrorMessage(String message)
    {
        final String errorMessage = message;
        runOnUiThread(new Runnable()
        {
            public void run()
            {
                if (mErrorDialog != null)
                {
                    mErrorDialog.dismiss();
                }
                
                // Generates an Alert Dialog to show the error message
                AlertDialog.Builder builder = new AlertDialog.Builder(
                    VideoPlayback.this);
                builder
                    .setMessage(errorMessage)
                    .setTitle(getString(R.string.INIT_ERROR))
                    .setCancelable(false)
                    .setIcon(0)
                    .setPositiveButton("OK",
                        new DialogInterface.OnClickListener()
                        {
                            public void onClick(DialogInterface dialog, int id)
                            {
                                finish();
                            }
                        });
                
                mErrorDialog = builder.create();
                mErrorDialog.show();
            }
        });
    }


    @Override
    public void onQCARUpdate(State state)
    {
        // Get the tracker manager:
        TrackerManager trackerManager = TrackerManager.getInstance();

        // Get the object tracker:
        ObjectTracker objectTracker = (ObjectTracker) trackerManager
                .getTracker(ObjectTracker.getClassType());

        // Get the target finder:
        TargetFinder finder = objectTracker.getTargetFinder();

        // Check if there are new results available:
        final int statusCode = finder.updateSearchResults();

        // Show a message if we encountered an error:
        if (statusCode < 0)
        {

            boolean closeAppAfterError = (
                    statusCode == UPDATE_ERROR_NO_NETWORK_CONNECTION ||
                            statusCode == UPDATE_ERROR_SERVICE_NOT_AVAILABLE);
            Log.e(LOGTAG,"ci sono problemi!");
            //showErrorMessage(statusCode, state.getFrame().getTimeStamp(), closeAppAfterError);

        } else if (statusCode == TargetFinder.UPDATE_RESULTS_AVAILABLE)
        {
            // Process new search results
            if (finder.getResultCount() > 0)
            {
                TargetSearchResult result = finder.getResult(0);

                // Check if this target is suitable for tracking:
                if (result.getTrackingRating() > 0)
                {
                    Trackable trackable = finder.enableTracking(result);

                    if (mExtendedTracking)
                        trackable.startExtendedTracking();
                }
            }
        }
    }
    
    final private static int CMD_BACK = -1;
    final private static int CMD_FULLSCREEN_VIDEO = 1;
    
    
    // This method sets the menu's settings
    private void setSampleAppMenuSettings()
    {
        SampleAppMenuGroup group;
        
        group = mSampleAppMenu.addGroup("", false);
        group.addTextItem(getString(R.string.menu_back), -1);
        
        group = mSampleAppMenu.addGroup("", true);
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH)
        {
            group.addSelectionItem(getString(R.string.menu_playFullscreenVideo),
                CMD_FULLSCREEN_VIDEO, mPlayFullscreenVideo);
        }
        
        mSampleAppMenu.attachMenu();
    }
    
    
    @Override
    public boolean menuProcess(int command)
    {
        
        boolean result = true;
        
        switch (command)
        {
            case CMD_BACK:
                Intent intent = new Intent(VideoPlayback.this,AboutScreen.class);
                intent.putExtra("ACTIVITY_TO_LAUNCH",
                    "app.VideoPlayback.VideoPlayback");
                intent.putExtra("ABOUT_TEXT_TITLE", "ExploreWorld");
                intent.putExtra("ABOUT_TEXT", "VideoPlayback/VP_about.html");
                startActivity(intent);
                //finish();
                break;
            
            case CMD_FULLSCREEN_VIDEO:
                mPlayFullscreenVideo = !mPlayFullscreenVideo;
                
                if (mVideoPlayerHelper.getStatus() == MEDIA_STATE.PLAYING)
                    {
                        mVideoPlayerHelper.pause();

                        mVideoPlayerHelper.play(true,
                            mSeekPosition);
                    }

                break;
            
        }
        
        return result;
    }

    public void setUrl(String url){
        if(mMovieName!=url) {
            mMovieName = url;

        }
    }
}

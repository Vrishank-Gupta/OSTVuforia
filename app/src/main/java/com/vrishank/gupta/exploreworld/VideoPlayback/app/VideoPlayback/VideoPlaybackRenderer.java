package com.vrishank.gupta.exploreworld.VideoPlayback.app.VideoPlayback;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Vector;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.annotation.SuppressLint;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.os.SystemClock;
import android.util.DisplayMetrics;
import android.util.Log;

import com.qualcomm.vuforia.ImageTarget;
import com.qualcomm.vuforia.Matrix44F;
import com.qualcomm.vuforia.Renderer;
import com.qualcomm.vuforia.State;
import com.qualcomm.vuforia.Tool;
import com.qualcomm.vuforia.TrackableResult;
import com.qualcomm.vuforia.VIDEO_BACKGROUND_REFLECTION;
import com.qualcomm.vuforia.Vec2F;
import com.qualcomm.vuforia.Vec3F;
import com.qualcomm.vuforia.Vuforia;
import com.vrishank.gupta.exploreworld.Application1.SampleApplicationSession;
import com.vrishank.gupta.exploreworld.Application1.utils.SampleMath;
import com.vrishank.gupta.exploreworld.Application1.utils.SampleUtils;
import com.vrishank.gupta.exploreworld.Application1.utils.Texture;
import com.vrishank.gupta.exploreworld.VideoPlayback.app.VideoPlayback.VideoPlayerHelper.MEDIA_STATE;
import com.vrishank.gupta.exploreworld.VideoPlayback.app.VideoPlayback.VideoPlayerHelper.MEDIA_TYPE;


public class VideoPlaybackRenderer implements GLSurfaceView.Renderer
{
    private static final String LOGTAG = "VideoPlaybackRenderer";
    
    SampleApplicationSession vuforiaAppSession;
    
    private int videoPlaybackShaderID = 0;
    private int videoPlaybackVertexHandle = 0;
    private int videoPlaybackNormalHandle = 0;
    private int videoPlaybackTexCoordHandle = 0;
    private int videoPlaybackMVPMatrixHandle = 0;
    private int videoPlaybackTexSamplerOESHandle = 0;
    int[] videoPlaybackTextureID = new int[1];
    
    private int keyframeShaderID = 0;
    private int keyframeVertexHandle = 0;
    private int keyframeNormalHandle = 0;
    private int keyframeTexCoordHandle = 0;
    private int keyframeMVPMatrixHandle = 0;
    private int keyframeTexSampler2DHandle = 0;


    private float videoQuadTextureCoords[] = { 0.0f, 0.0f, 1.0f, 0.0f, 1.0f,
            1.0f, 0.0f, 1.0f, };
    
    private float videoQuadTextureCoordsTransformed[] = { 0.0f, 0.0f,
            1.0f, 0.0f, 1.0f, 1.0f, 0.0f, 1.0f, };
    
    Vec3F targetPositiveDimensions = new Vec3F();

    static int NUM_QUAD_VERTEX = 4;
    static int NUM_QUAD_INDEX = 6;
    
    double quadVerticesArray[] = { -1.0f, -1.0f, 0.0f, 1.0f, -1.0f, 0.0f, 1.0f,
            1.0f, 0.0f, -1.0f, 1.0f, 0.0f };
    
    double quadTexCoordsArray[] = { 0.0f, 0.0f, 1.0f, 0.0f, 1.0f, 1.0f, 0.0f,
            1.0f };
    
    double quadNormalsArray[] = { 0, 0, 1, 0, 0, 1, 0, 0, 1, 0, 0, 1, };
    
    short quadIndicesArray[] = { 0, 1, 2, 2, 3, 0 };
    
    Buffer quadVertices, quadTexCoords, quadIndices, quadNormals;
    
    public boolean mIsActive = false;
    
    private float[] mTexCoordTransformationMatrix = null;
    private VideoPlayerHelper mVideoPlayerHelper = null;
    private String mMovieName = null;
    private MEDIA_TYPE mCanRequestType = null;
    private int mSeekPosition = 0;
    private boolean mShouldPlayImmediately = false;
    private long mLostTrackingSince = 0;
    private boolean mLoadRequested = false;
    
    VideoPlayback mActivity;
    
    Matrix44F modelViewMatrix = new Matrix44F();
    
    private Vector<Texture> mTextures;
    
    boolean isTracking = false;
    MEDIA_STATE currentStatus = MEDIA_STATE.NOT_READY;
    
    float videoQuadAspectRatio = 1.0f;
    float keyframeQuadAspectRatio = 1.0f;

    private String url="";
    
    
    public VideoPlaybackRenderer(VideoPlayback activity,
        SampleApplicationSession session)
    {
        mActivity = activity;
        vuforiaAppSession = session;

        mVideoPlayerHelper = null;
        mMovieName = "";
        mCanRequestType = MEDIA_TYPE.ON_TEXTURE_FULLSCREEN;
        mSeekPosition = 0;
        mShouldPlayImmediately = false;
        mLostTrackingSince = -1;
        mLoadRequested = false;
        mTexCoordTransformationMatrix = new float[16];

        targetPositiveDimensions = new Vec3F();
        modelViewMatrix = new Matrix44F();
    }
    
    
    public void setVideoPlayerHelper(VideoPlayerHelper newVideoPlayerHelper)
    {
        mVideoPlayerHelper = newVideoPlayerHelper;
    }
    
    
    public void requestLoad(String movieName, int seekPosition,
        boolean playImmediately)
        {
            mMovieName = movieName;
            mSeekPosition = seekPosition;
            mShouldPlayImmediately = playImmediately;
            mLoadRequested = true;
    }
    
    
    public void onSurfaceCreated(GL10 gl, EGLConfig config)
    {
        initRendering();
        
        Vuforia.onSurfaceCreated();

        if (mVideoPlayerHelper != null)
        {
            if (!mVideoPlayerHelper
                .setupSurfaceTexture(videoPlaybackTextureID[0]))
                mCanRequestType = MEDIA_TYPE.FULLSCREEN;
            else
                mCanRequestType = MEDIA_TYPE.ON_TEXTURE_FULLSCREEN;

            if (mLoadRequested)
            {
                mVideoPlayerHelper.load(mMovieName,
                    mCanRequestType, mShouldPlayImmediately,
                    mSeekPosition);
                mLoadRequested = false;
            }
        }
    }

    public void onSurfaceChanged(GL10 gl, int width, int height)
    {
        Vuforia.onSurfaceChanged(width, height);

        if (mLoadRequested && mVideoPlayerHelper != null)
        {
            mVideoPlayerHelper.load(mMovieName, mCanRequestType,
                mShouldPlayImmediately, mSeekPosition);
            mLoadRequested = false;
        }
    }
    
    
    public void onDrawFrame(GL10 gl)
    {
        if (!mIsActive)
            return;

        if (mVideoPlayerHelper != null)
        {
            if (mVideoPlayerHelper.isPlayableOnTexture())
            {
                if (mVideoPlayerHelper.getStatus() == MEDIA_STATE.PLAYING)
                {
                    mVideoPlayerHelper.updateVideoData();
                }

                mVideoPlayerHelper
                    .getSurfaceTextureTransformMatrix(mTexCoordTransformationMatrix);
                setVideoDimensions(
                    mVideoPlayerHelper.getVideoWidth(),
                    mVideoPlayerHelper.getVideoHeight(),
                    mTexCoordTransformationMatrix);
            }

            setStatus(mVideoPlayerHelper.getStatus().getNumericType());
        }
        
        renderFrame();

        if (isTracking)
        {
            mLostTrackingSince = -1;
        } else
        {
            if (mLostTrackingSince < 0)
                mLostTrackingSince = SystemClock.uptimeMillis();
            else
            {
                if ((SystemClock.uptimeMillis() - mLostTrackingSince) > 2000)
                {
                    if (mVideoPlayerHelper != null)
                        mVideoPlayerHelper.pause();
                }
            }
        }

    }
    
    
    @SuppressLint("InlinedApi")
    void initRendering()
    {
        Log.d(LOGTAG, "VideoPlayback VideoPlaybackRenderer initRendering");
        
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, Vuforia.requiresAlpha() ? 0.0f
            : 1.0f);
        
        for (Texture t : mTextures)
        {
            GLES20.glGenTextures(1, t.mTextureID, 0);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, t.mTextureID[0]);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
            GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA,
                t.mWidth, t.mHeight, 0, GLES20.GL_RGBA,
                GLES20.GL_UNSIGNED_BYTE, t.mData);
        }
        

        GLES20.glGenTextures(1, videoPlaybackTextureID, 0);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
            videoPlaybackTextureID[0]);
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
            GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
            GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 0);
        
        videoPlaybackShaderID = SampleUtils.createProgramFromShaderSrc(
                VideoPlaybackShaders.VIDEO_PLAYBACK_VERTEX_SHADER,
                VideoPlaybackShaders.VIDEO_PLAYBACK_FRAGMENT_SHADER);
        videoPlaybackVertexHandle = GLES20.glGetAttribLocation(
                videoPlaybackShaderID, "vertexPosition");
        videoPlaybackNormalHandle = GLES20.glGetAttribLocation(
                videoPlaybackShaderID, "vertexNormal");
        videoPlaybackTexCoordHandle = GLES20.glGetAttribLocation(
                videoPlaybackShaderID, "vertexTexCoord");
        videoPlaybackMVPMatrixHandle = GLES20.glGetUniformLocation(
                videoPlaybackShaderID, "modelViewProjectionMatrix");
        videoPlaybackTexSamplerOESHandle = GLES20.glGetUniformLocation(
                videoPlaybackShaderID, "texSamplerOES");
        
        keyframeShaderID = SampleUtils.createProgramFromShaderSrc(
            KeyFrameShaders.KEY_FRAME_VERTEX_SHADER,
            KeyFrameShaders.KEY_FRAME_FRAGMENT_SHADER);
        keyframeVertexHandle = GLES20.glGetAttribLocation(keyframeShaderID,
                "vertexPosition");
        keyframeNormalHandle = GLES20.glGetAttribLocation(keyframeShaderID,
            "vertexNormal");
        keyframeTexCoordHandle = GLES20.glGetAttribLocation(keyframeShaderID,
                "vertexTexCoord");
        keyframeMVPMatrixHandle = GLES20.glGetUniformLocation(keyframeShaderID,
                "modelViewProjectionMatrix");
        keyframeTexSampler2DHandle = GLES20.glGetUniformLocation(
            keyframeShaderID, "texSampler2D");
        
        keyframeQuadAspectRatio = (float) mTextures
            .get(0).mHeight / (float) mTextures.get(0).mWidth;
        
        quadVertices = fillBuffer(quadVerticesArray);
        quadTexCoords = fillBuffer(quadTexCoordsArray);
        quadIndices = fillBuffer(quadIndicesArray);
        quadNormals = fillBuffer(quadNormalsArray);
        
    }
    
    
    private Buffer fillBuffer(double[] array)
    {
        ByteBuffer bb = ByteBuffer.allocateDirect(4 * array.length); // each
                                                                     // float
                                                                     // takes 4
                                                                     // bytes
        bb.order(ByteOrder.LITTLE_ENDIAN);
        for (double d : array)
            bb.putFloat((float) d);
        bb.rewind();
        
        return bb;
        
    }
    
    
    private Buffer fillBuffer(short[] array)
    {
        ByteBuffer bb = ByteBuffer.allocateDirect(2 * array.length); // each
                                                                     // short
                                                                     // takes 2
                                                                     // bytes
        bb.order(ByteOrder.LITTLE_ENDIAN);
        for (short s : array)
            bb.putShort(s);
        bb.rewind();
        
        return bb;
        
    }
    
    
    private Buffer fillBuffer(float[] array)
    {
        ByteBuffer bb = ByteBuffer.allocateDirect(4 * array.length); // each
                                                                     // float
                                                                     // takes 4
                                                                     // bytes
        bb.order(ByteOrder.LITTLE_ENDIAN);
        for (float d : array)
            bb.putFloat(d);
        bb.rewind();
        
        return bb;
        
    }
    
    
    @SuppressLint("InlinedApi")
    void renderFrame()
    {


        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
        
        State state = Renderer.getInstance().begin();
        
        Renderer.getInstance().drawVideoBackground();
        
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        

        GLES20.glEnable(GLES20.GL_CULL_FACE);
        GLES20.glCullFace(GLES20.GL_BACK);
        if (Renderer.getInstance().getVideoBackgroundConfig().getReflection() == VIDEO_BACKGROUND_REFLECTION.VIDEO_BACKGROUND_REFLECTION_ON)
            GLES20.glFrontFace(GLES20.GL_CW); // Front camera
        else
            GLES20.glFrontFace(GLES20.GL_CCW); // Back camera
            
        float temp[] = { 0.0f, 0.0f, 0.0f };

        isTracking = false;
        targetPositiveDimensions.setData(temp);
        
        for (int tIdx = 0; tIdx < state.getNumTrackableResults() && tIdx<1; tIdx++)
        {
            TrackableResult trackableResult = state.getTrackableResult(tIdx);
            
            ImageTarget imageTarget = (ImageTarget) trackableResult
                .getTrackable();

            String new_url=imageTarget.getMetaData();

            if (mVideoPlayerHelper != null && !new_url.equals(url))
            {
                mActivity.setUrl(new_url);

                mVideoPlayerHelper.unload();
                mVideoPlayerHelper.load(new_url, mCanRequestType,
                        mShouldPlayImmediately, mSeekPosition);
                mLoadRequested = false;

                url=new_url;
            }

            modelViewMatrix = Tool
                .convertPose2GLMatrix(trackableResult.getPose());
            
            isTracking = true;
            
            targetPositiveDimensions = imageTarget.getSize();
            
            temp[0] = targetPositiveDimensions.getData()[0] / 2.0f;
            temp[1] = targetPositiveDimensions.getData()[1] / 2.0f;
            targetPositiveDimensions.setData(temp);
            
            if ((currentStatus == VideoPlayerHelper.MEDIA_STATE.READY)
                || (currentStatus == VideoPlayerHelper.MEDIA_STATE.REACHED_END)
                || (currentStatus == VideoPlayerHelper.MEDIA_STATE.NOT_READY)
                || (currentStatus == VideoPlayerHelper.MEDIA_STATE.ERROR))
            {
//                float[] modelViewMatrixKeyframe = Tool.convertPose2GLMatrix(
//                    trackableResult.getPose()).getData();
//                float[] modelViewProjectionKeyframe = new float[16];
//                // Matrix.translateM(modelViewMatrixKeyframe, 0, 0.0f, 0.0f,
//                // targetPositiveDimensions[currentTarget].getData()[0]);
//
//                // Here we use the aspect ratio of the keyframe since it
//                // is likely that it is not a perfect square
//
//                float ratio = 1.0f;
//                if (mTextures.get(0).mSuccess)
//                    ratio = keyframeQuadAspectRatio;
//                else
//                    ratio = targetPositiveDimensions.getData()[1]
//                            / targetPositiveDimensions.getData()[0];
//
//                Matrix.scaleM(modelViewMatrixKeyframe, 0,
//                    targetPositiveDimensions.getData()[0],
//                    targetPositiveDimensions.getData()[0]
//                        * ratio,
//                    targetPositiveDimensions.getData()[0]);
//                Matrix.multiplyMM(modelViewProjectionKeyframe, 0,
//                    vuforiaAppSession.getProjectionMatrix().getData(), 0,
//                    modelViewMatrixKeyframe, 0);
//
//                GLES20.glUseProgram(keyframeShaderID);
//
//                // Prepare for rendering the keyframe
//                GLES20.glVertexAttribPointer(keyframeVertexHandle, 3,
//                    GLES20.GL_FLOAT, false, 0, quadVertices);
//                GLES20.glVertexAttribPointer(keyframeNormalHandle, 3,
//                    GLES20.GL_FLOAT, false, 0, quadNormals);
//                GLES20.glVertexAttribPointer(keyframeTexCoordHandle, 2,
//                    GLES20.GL_FLOAT, false, 0, quadTexCoords);
//
//                GLES20.glEnableVertexAttribArray(keyframeVertexHandle);
//                GLES20.glEnableVertexAttribArray(keyframeNormalHandle);
//                GLES20.glEnableVertexAttribArray(keyframeTexCoordHandle);
//
//                GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
//
//                // The first loaded texture from the assets folder is the
//                // keyframe
//                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,
//                    mTextures.get(0).mTextureID[0]);
//                GLES20.glUniformMatrix4fv(keyframeMVPMatrixHandle, 1, false,
//                    modelViewProjectionKeyframe, 0);
//                GLES20.glUniform1i(keyframeTexSampler2DHandle, 0);
//
//                // Render
//                GLES20.glDrawElements(GLES20.GL_TRIANGLES, NUM_QUAD_INDEX,
//                    GLES20.GL_UNSIGNED_SHORT, quadIndices);
//
//                GLES20.glDisableVertexAttribArray(keyframeVertexHandle);
//                GLES20.glDisableVertexAttribArray(keyframeNormalHandle);
//                GLES20.glDisableVertexAttribArray(keyframeTexCoordHandle);
//
//                GLES20.glUseProgram(0);
            } else
            {
                float[] modelViewMatrixVideo = Tool.convertPose2GLMatrix(
                    trackableResult.getPose()).getData();
                float[] modelViewProjectionVideo = new float[16];
                // Matrix.translateM(modelViewMatrixVideo, 0, 0.0f, 0.0f,
                // targetPositiveDimensions[currentTarget].getData()[0]);

                Matrix.scaleM(modelViewMatrixVideo, 0,
                    targetPositiveDimensions.getData()[0],
                    targetPositiveDimensions.getData()[0]
                        * videoQuadAspectRatio,
                    targetPositiveDimensions.getData()[0]);
                Matrix.multiplyMM(modelViewProjectionVideo, 0,
                    vuforiaAppSession.getProjectionMatrix().getData(), 0,
                    modelViewMatrixVideo, 0);
                
                GLES20.glUseProgram(videoPlaybackShaderID);
                
                GLES20.glVertexAttribPointer(videoPlaybackVertexHandle, 3,
                        GLES20.GL_FLOAT, false, 0, quadVertices);
                GLES20.glVertexAttribPointer(videoPlaybackNormalHandle, 3,
                        GLES20.GL_FLOAT, false, 0, quadNormals);

                GLES20.glVertexAttribPointer(videoPlaybackTexCoordHandle,
                    2, GLES20.GL_FLOAT, false, 0,
                    fillBuffer(videoQuadTextureCoordsTransformed));
                
                GLES20.glEnableVertexAttribArray(videoPlaybackVertexHandle);
                GLES20.glEnableVertexAttribArray(videoPlaybackNormalHandle);
                GLES20.glEnableVertexAttribArray(videoPlaybackTexCoordHandle);
                
                GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
                
                GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                    videoPlaybackTextureID[0]);
                GLES20.glUniformMatrix4fv(videoPlaybackMVPMatrixHandle, 1,
                    false, modelViewProjectionVideo, 0);
                GLES20.glUniform1i(videoPlaybackTexSamplerOESHandle, 0);
                
                // Render
                GLES20.glDrawElements(GLES20.GL_TRIANGLES, NUM_QUAD_INDEX,
                    GLES20.GL_UNSIGNED_SHORT, quadIndices);
                
                GLES20.glDisableVertexAttribArray(videoPlaybackVertexHandle);
                GLES20.glDisableVertexAttribArray(videoPlaybackNormalHandle);
                GLES20.glDisableVertexAttribArray(videoPlaybackTexCoordHandle);
                
                GLES20.glUseProgram(0);
                
            }

            if ((currentStatus == VideoPlayerHelper.MEDIA_STATE.READY)
                || (currentStatus == VideoPlayerHelper.MEDIA_STATE.REACHED_END)
                || (currentStatus == VideoPlayerHelper.MEDIA_STATE.PAUSED)
                || (currentStatus == VideoPlayerHelper.MEDIA_STATE.NOT_READY)
                || (currentStatus == VideoPlayerHelper.MEDIA_STATE.ERROR))
            {
                float[] modelViewMatrixButton = Tool.convertPose2GLMatrix(
                    trackableResult.getPose()).getData();
                float[] modelViewProjectionButton = new float[16];
                
                GLES20.glDepthFunc(GLES20.GL_LEQUAL);
                
                GLES20.glEnable(GLES20.GL_BLEND);
                GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA,
                    GLES20.GL_ONE_MINUS_SRC_ALPHA);
                
                Matrix
                    .translateM(
                        modelViewMatrixButton,
                        0,
                        0.0f,
                        0.0f,
                            targetPositiveDimensions.getData()[1] / 10.98f);
                Matrix
                        .scaleM(
                        modelViewMatrixButton,
                        0,
                        (targetPositiveDimensions.getData()[1] / 2.0f),
                        (targetPositiveDimensions.getData()[1] / 2.0f),
                        (targetPositiveDimensions.getData()[1] / 2.0f));
                Matrix.multiplyMM(modelViewProjectionButton, 0,
                    vuforiaAppSession.getProjectionMatrix().getData(), 0,
                    modelViewMatrixButton, 0);
                
                GLES20.glUseProgram(keyframeShaderID);
                
                GLES20.glVertexAttribPointer(keyframeVertexHandle, 3,
                    GLES20.GL_FLOAT, false, 0, quadVertices);
                GLES20.glVertexAttribPointer(keyframeNormalHandle, 3,
                    GLES20.GL_FLOAT, false, 0, quadNormals);
                GLES20.glVertexAttribPointer(keyframeTexCoordHandle, 2,
                    GLES20.GL_FLOAT, false, 0, quadTexCoords);
                
                GLES20.glEnableVertexAttribArray(keyframeVertexHandle);
                GLES20.glEnableVertexAttribArray(keyframeNormalHandle);
                GLES20.glEnableVertexAttribArray(keyframeTexCoordHandle);
                
                GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
                
                switch (currentStatus)
                {
                    case READY:
                        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,
                            mTextures.get(1).mTextureID[0]);
                        break;
                    case REACHED_END:
                        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,
                            mTextures.get(1).mTextureID[0]);
                        break;
                    case PAUSED:
                        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,
                            mTextures.get(1).mTextureID[0]);
                        break;
                    case NOT_READY:
                        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,
                            mTextures.get(2).mTextureID[0]);
                        break;
                    case ERROR:
                        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,
                            mTextures.get(3).mTextureID[0]);
                        break;
                    default:
                        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,
                            mTextures.get(2).mTextureID[0]);
                        break;
                }
                GLES20.glUniformMatrix4fv(keyframeMVPMatrixHandle, 1, false,
                    modelViewProjectionButton, 0);
                GLES20.glUniform1i(keyframeTexSampler2DHandle, 0);
                
                GLES20.glDrawElements(GLES20.GL_TRIANGLES, NUM_QUAD_INDEX,
                    GLES20.GL_UNSIGNED_SHORT, quadIndices);
                
                GLES20.glDisableVertexAttribArray(keyframeVertexHandle);
                GLES20.glDisableVertexAttribArray(keyframeNormalHandle);
                GLES20.glDisableVertexAttribArray(keyframeTexCoordHandle);
                
                GLES20.glUseProgram(0);
                
                GLES20.glDepthFunc(GLES20.GL_LESS);
                GLES20.glDisable(GLES20.GL_BLEND);
            }
            
            SampleUtils.checkGLError("VideoPlayback renderFrame");
        }
        
        GLES20.glDisable(GLES20.GL_DEPTH_TEST);
        
        Renderer.getInstance().end();
        
    }
    
    
    boolean isTapOnScreenInsideTarget(float x, float y)
    {
        Vec3F intersection;
        // Vec3F lineStart = new Vec3F();
        // Vec3F lineEnd = new Vec3F();
        
        DisplayMetrics metrics = new DisplayMetrics();
        mActivity.getWindowManager().getDefaultDisplay().getMetrics(metrics);
        intersection = SampleMath.getPointToPlaneIntersection(SampleMath
            .Matrix44FInverse(vuforiaAppSession.getProjectionMatrix()),
            modelViewMatrix, metrics.widthPixels, metrics.heightPixels,
            new Vec2F(x, y), new Vec3F(0, 0, 0), new Vec3F(0, 0, 1));

        if ((intersection.getData()[0] >= -(targetPositiveDimensions
            .getData()[0]))
            && (intersection.getData()[0] <= (targetPositiveDimensions
                .getData()[0]))
            && (intersection.getData()[1] >= -(targetPositiveDimensions
                .getData()[1]))
            && (intersection.getData()[1] <= (targetPositiveDimensions
                .getData()[1])))
            return true;
        else
            return false;
    }
    
    
    void setVideoDimensions(float videoWidth, float videoHeight,
        float[] textureCoordMatrix)
    {

        videoQuadAspectRatio = videoHeight / videoWidth;
        
        float mtx[] = textureCoordMatrix;
        float tempUVMultRes[] = new float[2];

        tempUVMultRes = uvMultMat4f(
            videoQuadTextureCoordsTransformed[0],
            videoQuadTextureCoordsTransformed[1],
            videoQuadTextureCoords[0], videoQuadTextureCoords[1], mtx);
        videoQuadTextureCoordsTransformed[0] = tempUVMultRes[0];
        videoQuadTextureCoordsTransformed[1] = tempUVMultRes[1];
        tempUVMultRes = uvMultMat4f(
            videoQuadTextureCoordsTransformed[2],
            videoQuadTextureCoordsTransformed[3],
            videoQuadTextureCoords[2], videoQuadTextureCoords[3], mtx);
        videoQuadTextureCoordsTransformed[2] = tempUVMultRes[0];
        videoQuadTextureCoordsTransformed[3] = tempUVMultRes[1];
        tempUVMultRes = uvMultMat4f(
            videoQuadTextureCoordsTransformed[4],
            videoQuadTextureCoordsTransformed[5],
            videoQuadTextureCoords[4], videoQuadTextureCoords[5], mtx);
        videoQuadTextureCoordsTransformed[4] = tempUVMultRes[0];
        videoQuadTextureCoordsTransformed[5] = tempUVMultRes[1];
        tempUVMultRes = uvMultMat4f(
            videoQuadTextureCoordsTransformed[6],
            videoQuadTextureCoordsTransformed[7],
            videoQuadTextureCoords[6], videoQuadTextureCoords[7], mtx);
        videoQuadTextureCoordsTransformed[6] = tempUVMultRes[0];
        videoQuadTextureCoordsTransformed[7] = tempUVMultRes[1];

        // textureCoordMatrix = mtx;
    }
    
    
    float[] uvMultMat4f(float transformedU, float transformedV, float u,
        float v, float[] pMat)
    {
        float x = pMat[0] * u + pMat[4] * v /* + pMat[ 8]*0.f */+ pMat[12]
            * 1.f;
        float y = pMat[1] * u + pMat[5] * v /* + pMat[ 9]*0.f */+ pMat[13]
            * 1.f;
        // float z = pMat[2]*u + pMat[6]*v + pMat[10]*0.f + pMat[14]*1.f; // We
        // float w = pMat[3]*u + pMat[7]*v + pMat[11]*0.f + pMat[15]*1.f;
        
        float result[] = new float[2];
        // transformedU = x;
        // transformedV = y;
        result[0] = x;
        result[1] = y;
        return result;
    }
    
    
    void setStatus(int value)
    {
        switch (value)
        {
            case 0:
                currentStatus = VideoPlayerHelper.MEDIA_STATE.REACHED_END;
                break;
            case 1:
                currentStatus = VideoPlayerHelper.MEDIA_STATE.PAUSED;
                break;
            case 2:
                currentStatus = VideoPlayerHelper.MEDIA_STATE.STOPPED;
                break;
            case 3:
                currentStatus = VideoPlayerHelper.MEDIA_STATE.PLAYING;
                break;
            case 4:
                currentStatus = VideoPlayerHelper.MEDIA_STATE.READY;
                break;
            case 5:
                currentStatus = VideoPlayerHelper.MEDIA_STATE.NOT_READY;
                break;
            case 6:
                currentStatus = VideoPlayerHelper.MEDIA_STATE.ERROR;
                break;
            default:
                currentStatus = VideoPlayerHelper.MEDIA_STATE.NOT_READY;
                break;
        }
    }


    boolean isTracking()
    {
        return isTracking;
    }


    public void setTextures(Vector<Texture> textures)
    {
        mTextures = textures;
    }
    
}

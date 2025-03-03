package com.vrishank.gupta.exploreworld.Application1.utils;

import java.lang.ref.WeakReference;

import android.app.Activity;
import android.os.Handler;
import android.os.Message;
import android.view.View;


public final class LoadingDialogHandler extends Handler
{
    private final WeakReference<Activity> mActivity;
    public static final int HIDE_LOADING_DIALOG = 0;
    public static final int SHOW_LOADING_DIALOG = 1;
    
    public View mLoadingDialogContainer;
    
    
    public LoadingDialogHandler(Activity activity)
    {
        mActivity = new WeakReference<Activity>(activity);
    }
    
    
    public void handleMessage(Message msg)
    {
        Activity imageTargets = mActivity.get();
        if (imageTargets == null)
        {
            return;
        }
        
        if (msg.what == SHOW_LOADING_DIALOG)
        {
            mLoadingDialogContainer.setVisibility(View.VISIBLE);
            
        } else if (msg.what == HIDE_LOADING_DIALOG)
        {
            mLoadingDialogContainer.setVisibility(View.GONE);
        }
    }
    
}

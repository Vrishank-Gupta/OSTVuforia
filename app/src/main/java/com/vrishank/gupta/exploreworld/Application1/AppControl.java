package com.vrishank.gupta.exploreworld.Application1;

import com.qualcomm.vuforia.State;


public interface AppControl
{
    
    boolean doInitTrackers();

    boolean doLoadTrackersData();
    
    
    boolean doStartTrackers();
    
    
    boolean doStopTrackers();
    
    
    boolean doUnloadTrackersData();
    
    
    boolean doDeinitTrackers();

    void onInitARDone(SampleApplicationException e);

    void onQCARUpdate(State state);
    
}

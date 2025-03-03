package com.vrishank.gupta.exploreworld.Application1;

public class SampleApplicationException extends Exception
{
    
    private static final long serialVersionUID = 2L;
    
    public static final int INITIALIZATION_FAILURE = 0;
    public static final int VUFORIA_ALREADY_INITIALIZATED = 1;
    public static final int TRACKERS_INITIALIZATION_FAILURE = 2;
    public static final int LOADING_TRACKERS_FAILURE = 3;
    public static final int UNLOADING_TRACKERS_FAILURE = 4;
    public static final int TRACKERS_DEINITIALIZATION_FAILURE = 5;
    public static final int CAMERA_INITIALIZATION_FAILURE = 6;

    private int mCode = -1;
    private String mString = "";
    
    
    public SampleApplicationException(int code, String description)
    {
        super(description);
        mCode = code;
        mString = description;
    }


    public String getString()
    {
        return mString;
    }
}

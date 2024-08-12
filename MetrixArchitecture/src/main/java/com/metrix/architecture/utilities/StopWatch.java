package com.metrix.architecture.utilities;

/**
 * StopWatch class to track the start/stop time
 * @author elin
 *
 */
public class StopWatch {
    
    private long mStartTime = 0;
    private long mStopTime = 0;
    private long mElapsed = 0;
    private boolean mRunning = false;

    
    public void start() {
        this.mStartTime = System.nanoTime();
        this.mRunning = true;
    }
    
    public void stop() {
        this.mStopTime = System.nanoTime();
        this.mRunning = false;
    }

    public void reset() {
        this.mStartTime = 0;
        this.mStopTime = 0;
        this.mRunning = false;
    }    
        
    /**
     * Get elaspsed time in microseconds
     * @return
     */
    public long getElapsedTimeMicro() {
        if (mRunning) {
            mElapsed = ((System.nanoTime() - mStartTime) / 1000);
        }
        else {
            mElapsed = ((mStopTime - mStartTime) / 1000);
        }
        return mElapsed;
    }
          
    /**
     * Get elaspsed time in milliseconds
     * @return
     */
    public long getElapsedTimeMilli() {
        if (mRunning) {
            mElapsed = ((System.nanoTime() - mStartTime) / 1000000);
        }
        else {
            mElapsed = ((mStopTime - mStartTime) / 1000000);
        }
        return mElapsed;
    }
}
package com.clearwaterrevival.ukasz.phonecallsblocker;

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.Context;
import android.content.SharedPreferences;

/**
 * Created by Łukasz on 2017-03-05.
 * Service class which allows working app in background.
 * Allows start and stop detecting calls.
 */
public class CallDetectService extends JobService
{
//    private CallDetector callDetector;

    /**
     * This method runs on starting the job service of detecting calls.
     * Creates a {@link CallDetector} object and calls a start method for it.
     *
     * @param params {@link JobParameters} parameters of the job to schedule
     * @return true
     */
    @Override
    public boolean onStartJob(JobParameters params)
    {
//        if(callDetector == null)
//        {
//            callDetector = new CallDetector(this);
//        }
//        else
//        {
//            callDetector.stop();
//
//        }
//        callDetector.start();
        if(StartActivity.callDetector != null)
        {
            StartActivity.callDetector.stop();
            StartActivity.callDetector.start();
        }
        else
        {
            StartActivity.callDetector = new CallDetector(getApplicationContext());
            StartActivity.callDetector.start();
        }

        jobFinished(params, false);
        return true;
    }

    /**
     *
     * This method runs when we disable a job service of detecting calls.
     * Calls a stop method for {@link CallDetector} object.
     *
     * @param params {@link JobParameters} parameters of the job to finish
     * @return false
     */
    @Override
    public boolean onStopJob(JobParameters params)
    {
        SharedPreferences data = getApplicationContext().getSharedPreferences("data", Context.MODE_PRIVATE);
        boolean callDetectorEnabled = data.getBoolean("detectEnabled", false);
        //Stop only if call detection service is not enabled
//        if(callDetector != null && !callDetectorEnabled)
//        {
//            callDetector.stop();
//            callDetector = null;
//        }
        if(StartActivity.callDetector != null && !callDetectorEnabled)
        {
            StartActivity.callDetector.stop();
            StartActivity.callDetector = null;
        }
        jobFinished(params, false);
        return false;
    }
}

package com.monitorapp.services;

import android.app.Service;
import android.content.Intent;
import android.media.MediaRecorder;
import android.os.IBinder;
import android.util.Log;

import com.monitorapp.BuildConfig;
import com.monitorapp.db_utils.DatabaseHelper;
import com.monitorapp.db_utils.UserIDStore;

import java.io.IOException;
import java.sql.Date;
import java.text.SimpleDateFormat;

public class NoiseDetectorService extends Service {

    private MediaRecorder mRecorder = null;

    /* start value in dB */
    public static float dbCount = 40;
    Thread tMic;
    DatabaseHelper dbHelper;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public int onStartCommand(Intent intent, int flags, int startId) {
        dbHelper = DatabaseHelper.getHelper(getApplicationContext());
        if (mRecorder == null) {
            try {
                mRecorder = new MediaRecorder();
                mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
                mRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
                mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
                mRecorder.setOutputFile("/dev/null/");
                mRecorder.prepare();
                mRecorder.start();

            } catch (IOException exception) {
                exception.printStackTrace();
            }

            tMic = new Thread() {
                public void run() {

                    while (true) {
                        try {
                            if (Thread.interrupted()) {
                                break;
                            }
                            int volume = mRecorder.getMaxAmplitude();
                            if (volume > 0)
                                dbCount = 20 * (float) (Math.log10(volume));

                            SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss");
                            Date date = new Date(System.currentTimeMillis());

                            if (BuildConfig.DEBUG) {
                                Log.d("MICROPHONE", volume + " " + dbCount);
                            }
                            dbHelper.addRecordNoiseDetectorData(sdf.format(date), UserIDStore.id(getApplicationContext()), volume, dbCount);

                            if (!Thread.interrupted()) {
                                Thread.sleep(1000);
                            }

                        } catch (InterruptedException e) {
                            e.printStackTrace();
                            Thread.currentThread().interrupt();
                            break;
                        }
                    }
                }
            };
            tMic.start();

        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        tMic.interrupt();

        if (mRecorder != null) {
            mRecorder.stop();
            mRecorder.reset();
            mRecorder.release();
            mRecorder = null;
        }
        super.onDestroy();
    }
}
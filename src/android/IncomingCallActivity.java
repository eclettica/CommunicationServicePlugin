package it.linup.cordova.plugin.communication;

import android.content.res.AssetFileDescriptor;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import android.app.Activity;

import android.content.res.Resources;


import it.linup.cordova.plugin.communication.services.CommunicationService;

import java.io.IOException;

public class IncomingCallActivity extends Activity {

    private static IncomingCallActivity instance;

    public static void requestDestroy() {
        if (instance != null) {
            if (player != null && player.isPlaying()) {
                player.stop();
                player.release();
            }
            instance.finish();
        }
    }

    private static MediaPlayer player;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);

        CommunicationService.setIncomingCallActivity(this);

        super.onCreate(savedInstanceState);
        String package_name = getApplication().getPackageName();
        Resources res = getApplication().getResources();
        setContentView(res.getIdentifier("incoming_call", "layout", package_name));

        // findViewById(R.id.btnAccept).setOnClickListener(new View.OnClickListener() {
        findViewById(res.getIdentifier("btnAccept", "id", package_name)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                IncomingCallActivity.this.sendEvent("onAcceptCall");
                CommunicationService.setIncomingCallActivity(null);
                finish();
            }
        });
        // findViewById(R.id.btnCancel).setOnClickListener(new View.OnClickListener() {
        findViewById(res.getIdentifier("btnCancel", "id", package_name)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                IncomingCallActivity.this.sendEvent("onCancelCall");
                CommunicationService.setIncomingCallActivity(null);
                finish();
            }
        });

        String info = getIntent().getStringExtra("info");
        String status = getIntent().getStringExtra("status");

        // ((TextView) findViewById(R.id.tvName)).setText(info);
        // ((TextView) findViewById(R.id.tvStatus)).setText(status);
        ((TextView) findViewById(res.getIdentifier("tvName", "id", package_name))).setText(info);
        ((TextView) findViewById(res.getIdentifier("tvStatus", "id", package_name))).setText(status);

        try {
            //AssetFileDescriptor afd = getAssets().openFd("iphone_old_phone.mp3");
            AssetFileDescriptor afd = getAssets().openFd("old_phone.mp3");
            player = new MediaPlayer();
            player.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
            player.prepare();
            player.setLooping(true);
            player.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
        instance = this;
    }

    private void sendEvent(String event) {
        try {

            CommunicationService.callEvent(event, "");

            if (player != null && player.isPlaying()) {
                player.stop();
                player.release();
            }
            CommunicationService.setIncomingCallActivity(null);
            this.finish();
        }catch (Exception e){
            e.printStackTrace();
        }

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        instance = null;
        CommunicationService.setIncomingCallActivity(null);
    }

    @Override
    public void finish() {
        super.finish();
        instance = null;
        CommunicationService.setIncomingCallActivity(null);
    }
}

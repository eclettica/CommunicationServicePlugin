package it.linup.cordova.plugin.communication;

import android.content.res.AssetFileDescriptor;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import android.app.Activity;

import it.linup.cordova.plugin.WebSocketNativeModule;

//import com.idra.R;

import java.io.IOException;

public class IncomingCallActivity extends Activity {

    private static IncomingCallActivity instance;

    public static void requestDestroy() {
        if (instance != null) {
            /*if (player != null && player.isPlaying()) {
                player.stop();
                player.release();
            }*/
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

        super.onCreate(savedInstanceState);
        String package_name = getApplication().getPackageName();
        setContentView(getApplication().getResources().getIdentifier("incoming_call", "layout", package_name));

        /*findViewById(R.id.btnAccept).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                IncomingCallActivity.this.sendEvent("onAcceptCall");
                finish();
            }
        });*/

        /*findViewById(R.id.btnCancel).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                IncomingCallActivity.this.sendEvent("onCancelCall");
                finish();
            }
        });*/

        String info = getIntent().getStringExtra("info");
        String status = getIntent().getStringExtra("status");

        /*((TextView) findViewById(R.id.tvName)).setText(info);
        ((TextView) findViewById(R.id.tvStatus)).setText(status);*/

        /*try {
            //AssetFileDescriptor afd = getAssets().openFd("iphone_old_phone.mp3");
            AssetFileDescriptor afd = getAssets().openFd("old_phone.mp3");
            player = new MediaPlayer();
            player.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
            player.prepare();
            player.setLooping(true);
            player.start();
        } catch (IOException e) {
            e.printStackTrace();
        }*/
        instance = this;
    }

    private void sendEvent(String event) {
        try {
            WebSocketNativeModule ws = WebSocketNativeModule.getInstance();
            if (ws != null) {
                ws.sendEvent(event, "");
            }

            /*if (player != null && player.isPlaying()) {
                player.stop();
                player.release();
            }*/

            this.finish();
        }catch (Exception e){
            e.printStackTrace();
        }

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        instance = null;
    }

    @Override
    public void finish() {
        super.finish();
        instance = null;
    }
}

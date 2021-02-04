package it.linup.cordova.plugin.communication.services;

import android.app.ActivityManager;
import android.app.Service;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.os.Vibrator;
import android.util.Log;

import android.os.Binder;

//import com.idra.modules.DbSql.DBManager;
//import com.idra.modules.DbSql.DbCustomLogic;

import it.linup.cordova.plugin.communication.IncomingCallActivity;
import it.linup.cordova.plugin.communication.CommunicationServicePlugin;
import it.linup.cordova.plugin.communication.CommunicationServicePlugin.Event;

import it.linup.cordova.plugin.utils.LogUtils;

import it.linup.cordova.plugin.utils.FileUtils;

import it.linup.cordova.plugin.services.WebsocketListnerInterface;
import it.linup.cordova.plugin.services.WebsocketService;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.net.URI;
import java.util.Date;

import io.sqlc.SQLiteManager;


//import org.pgsqlite.SQLitePlugin;
//import org.pgsqlite.SQLitePluginPackage;

public class CommunicationService extends Service implements WebsocketListnerInterface {

    private boolean isConnected = false;
    protected static boolean requestHeartBit = false;
    protected static int failedHeartBit = 0;
    protected static JSONArray lstUser;
    protected static boolean isEnableHearbitCheck;
    private Handler m_handler;
    private Runnable m_handlerTask;
    public static boolean reconnect = true;
    public  static Context mContext;
    private int counter = 0;

    private CommunicationServicePlugin _plugin = null;


    public static String tag="COMMUNICATIONSERVICE - CommunicationService";

	// Binder given to clients
    private final IBinder binder = new ForegroundBinder();

    private SQLiteManager sqliteManager = SQLiteManager.instance();


    /**
     * Class used for the client Binder.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with IPC.
     */
    public class ForegroundBinder extends Binder
    {
        public CommunicationService getService()
        {
            // Return this instance of ForegroundService
            // so clients can call public methods
            return CommunicationService.this;
        }
    }


    /**
     * Creates an IntentService.  Invoked by your subclass's constructor.
     *
     */
    public CommunicationService() {

    }

    public void setPlugin(CommunicationServicePlugin plugin) {
        this._plugin = plugin;
    }


    private static enum CommunicationServiceSingleton {
        INSTANCE;

        CommunicationService singleton = new CommunicationService();

        public CommunicationService getSingleton() {
            return singleton;
        }

    }

    public static CommunicationService instance() {
        return CommunicationService.CommunicationServiceSingleton.INSTANCE.getSingleton();
    }

    public void onCreate() {
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MYSERVICE:ALLARM");
        super.onCreate();
        mContext = getApplicationContext();
        wl.acquire();
        LogUtils.printLog(tag," >>>onCreate()");
        WebsocketService.instance().addListner(this);
    }

    @Override
    public IBinder onBind(Intent intent) {

        return binder;
    }

    /*@Override
    protected void onHandleIntent(@Nullable Intent intent) {
        LogUtils.printLog(tag,"WEBSOCKETSERVICE onHandleIntent");
        this.uri = intent.getStringExtra("uri");
        _onStartCommand();
    }*/

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        _onStartCommand();

        // PREFERISCO UTILIZZARE LO START_STICKY PERCHE' NEL CASO DI CAMBIO SERVER ATTRVERSO IL CONNECT DI WebSocketNativeModulo riscrivo l'uri e reinizializzo tutto
        return START_STICKY;

        //return START_REDELIVER_INTENT;
    }


    private void _onStartCommand(){
        LogUtils.printLog(tag," onStartCommand ");
	    WebsocketService.instance().addListner(this);
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        LogUtils.printLog(tag," onTaskRemoved " + reconnect);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        LogUtils.printLog(tag," onDestroy "  + reconnect);
    }

    public void startActivity(){
        //Context context = cordova.getActivity().getApplicationContext();
        Intent intent = new Intent(mContext, IncomingCallActivity.class);
        mContext.startActivity(intent);
    }

    public void onEvent(String event, String data) {
        LogUtils.printLog(tag, event + " " + data);
        this.counter++;
        if(this.counter == 2) {
            this.startActivity();
        }
        if(this._plugin != null) {
            this._plugin.fireEvent(CommunicationServicePlugin.Event.MESSAGE, data);
        }
    }

}

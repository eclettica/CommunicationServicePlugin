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
import it.linup.cordova.plugin.communication.services.CommunicationServiceSqlUtil;
import it.linup.cordova.plugin.communication.services.CommunicationMessageService;

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

import io.sqlc.SQLiteAndroidDatabaseCallback;



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
        String dbName = FileUtils.readFromFile("dbName", this);
        if(dbName != null && !dbName.trim().equals("")) {
            this.startDatabase(dbName, null);
        }
    }

    public void setDbName(String dbName) {
        if(dbName != null && !dbName.trim().equals(""))
            FileUtils.writeToFile("dbName", dbName, mContext);

        this.startDatabase(dbName, null);
    }

    private void startDatabase(String dbName, JSONObject options) {
        if(dbName == null)
            return;
        if(options == null) {
            options = new JSONObject();
            try {
                options.put("name", dbName);
            } catch(Exception e){}
        }

        CommunicationServiceSqlUtil.setDbName(dbName, options);
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
        /*this.counter++;
        if(this.counter == 2) {
            this.startActivity();
        }*/
        switch (event) {
            case "onWebsocketConnect":
                this.manageOnConnection();
                break;
            case "onWebsocketMessage":
                this.manageMessage(data);
                break;
            case "onWebsocketException":
                break;
            case "onWebsocketClose":
                break;
            default:
                LogUtils.printLog(tag, event + " event not found");
        }
        if(this._plugin != null) {
            this._plugin.fireEvent(CommunicationServicePlugin.Event.MESSAGE, data);
        }
    }

    /**
     * Questo metodo viene chiamato a valle della connessione alla websocket.
     * Qui possono essere inviate le chiamate per richiedere al server nuovi messaggi
     * e per richiedere la lista aggiornata degli utenti.
     * Inoltre qui si può innescare il controllo su eventuali notifiche [lettura, ricezione]
     * e messaggi non ancora inviati
     */
    private void manageOnConnection() {
        this.sentQueued();
        this.askForMessages();
        this.reloadUserList();
    }

    /**
     * Questo metodo deve effettuare
     * 1 - la ricerca di messaggi non inviati e inviarli;
     * 2 - la ricerca di messaggi ricevuti non notificati e inviarli;
     * 3 - la ricerca di messaggi letti non notificati e inviarli;
     */
    private void sentQueued() {

    }

    /**
     * Questo metodo serve per richiedere dal server eventuali messaggi non ricevuti
     */
    private void askForMessages() {

    }

    /**
     * Questo metodo serve per aggiornare la lista degli utenti
     */
    private void reloadUserList() {

    }

    private void manageMessage(String message) {

        JSONObject jobj = null;


        try {
            try {
                jobj = new JSONObject(message);
            } catch (Exception e) {
                if (message.equals("not logged")) {
                    jobj = new JSONObject();
                    jobj.put("message", message);
                    jobj.put("event", message);
                }
            }
            String event = jobj.getString("event");

            switch (event) {
                case "newmessage":
                // GESTIONE  DEL CASO IN CUI LA MAIN ACTIVITY  E' STATA UCCISA O IN BACKHGROUND
                if (_plugin == null || !_plugin.active)
                {
                    try {
                        Vibrator v = (Vibrator) mContext.getSystemService(Context.VIBRATOR_SERVICE);
                        v.vibrate(1000);
                        LogUtils.printLog(tag," MESSAGE RECEIVED");
                    } catch (Exception e) {
                        LogUtils.printLog(tag,"ACTIVITY ERROR ON  MESSAGE RECEIVED VIBRATION");
                    }

                }
                // FINE GESTIONE  DEL CASO IN CUI LA MAIN ACTIVITY  E' STATA UCCISA O IN BACKHGROUND

                this.addMessage(jobj);

                break;

            }
        } catch(JSONException jex) {

        }

    }

    /**
     * In questo metodo devono essere gestiti tutti i passaggi legati al flusso di ricezione di un messaggio
     * - salvare il messaggio e flaggarlo come ricevuto
     * - cercare la conversazione relativa e, qualora non esiste, crearla;
     * - calcolare e aggiornare il numero di messaggi non letti per la conversazione
     * e il testo dell'ultimo messaggio
     * - generare gli eventi per comunicare alla parte JS dell'arrivo di un messaggio e
     * il nuovo valore del contatore;
     * - inviare al backend un messaggio di avvenuta ricezione
     * @param jobj
     */
    private void addMessage(JSONObject jobj) {
        //String insertMessageQuery = CommunicationServiceSqlUtil.getInsertMessageQuery();
        //salvare il messaggio e flaggarlo come ricevuto
        CommunicationMessageService.saveMessage(jobj, new SQLiteAndroidDatabaseCallback(){
            public void error(String error){
                LogUtils.printLog(tag, "dbquery callback error " + error);
            }
            public void success(JSONArray arr) {
                //cercare la conversazione relativa e, qualora non esiste, crearla
            }
        });

    }

}

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

import java.lang.reflect.Field;

import android.os.Binder;

//import com.idra.modules.DbSql.DBManager;
//import com.idra.modules.DbSql.DbCustomLogic;

import it.linup.cordova.plugin.communication.IncomingCallActivity;
import it.linup.cordova.plugin.communication.CommunicationServicePlugin;
import it.linup.cordova.plugin.communication.CommunicationServicePlugin.Event;
import it.linup.cordova.plugin.communication.services.CommunicationServiceSqlUtil;
import it.linup.cordova.plugin.communication.services.CommunicationMessageService;

import it.linup.cordova.plugin.communication.models.MessageWrapper;
import it.linup.cordova.plugin.communication.models.SqlMessageWrapper;
import it.linup.cordova.plugin.communication.models.SqlChatWrapper;


import it.linup.cordova.plugin.utils.LogUtils;

import it.linup.cordova.plugin.utils.FileUtils;

import it.linup.cordova.plugin.services.WebsocketListnerInterface;
import it.linup.cordova.plugin.services.WebsocketService;
import it.linup.cordova.plugin.services.NotificationService;


import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.net.URI;
import java.util.Date;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.LinkedList;

import io.sqlc.SQLiteManager;

import io.sqlc.SQLiteAndroidDatabaseCallback;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.PluginResult;
import com.google.gson.Gson;

import it.linup.cordova.plugin.communication.services.CommunicationMessageService.QuerySelectObj;

//import org.pgsqlite.SQLitePlugin;
//import org.pgsqlite.SQLitePluginPackage;

public class CommunicationService extends Service implements WebsocketListnerInterface {

    private boolean isConnected = false;
    public static boolean isCalling = false;
    public static String fromId;
    public static String fromName;
    protected static boolean requestHeartBit = false;
    protected static int failedHeartBit = 0;
    protected static JSONArray lstUser;
    protected static boolean isEnableHearbitCheck;
    private Handler m_handler;
    private Runnable m_handlerTask;
    public static boolean reconnect = true;
    public  static Context mContext;
    private int counter = 0;

    private String userId;

    private String currentSessionId;

    public static CommunicationServicePlugin _plugin = null;

    public static Class _mainClass = null;

    public static final String USERSLIST = "userslist";
    public static final String UPDATEUSERS = "updateusers";
    public static final String FORCELOGOUT = "forcelogout";
    public static final String NEWMESSAGE = "newmessage";
    public static final String SENDMESSAGE = "sendmessage";
    public static final String DOWNLOADMESSAGE = "downloadmessage";
    public static final String CALL = "call";
    public static final String READMESSAGE = "readmessage";
	public static final String RECEIVEMESSAGE = "receivemessage";
    public static final String LEAVEGROUP = "leavegroup";
    public static final String ENTERGROUP = "entergroup";
    public static final String NEWCHAT = "newchat";


    private Map<String, UserSessionFacade> users;


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
        this.users = new HashMap<String, UserSessionFacade>();
    }

    public void setPlugin(CommunicationServicePlugin plugin) {

        _plugin = plugin;
    }

    public void setMainClass(Class mainClass) {

        _mainClass = mainClass;
    }

    public CommunicationServicePlugin getPlugin() {
        return _plugin;
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


    private void _onStartCommand() {
        LogUtils.printLog(tag," onStartCommand ");
	    WebsocketService.instance().addListner(this);
        String dbName = FileUtils.readFromFile("dbName", this);
        if(dbName != null && !dbName.trim().equals("")) {
            this.startDatabase(dbName, null);
        }
        this.reloadUserList();
    }

    public void setCurrentId(String userId) {
        this.userId = userId;
    }

    public void setDbName(String dbName) {
        if(dbName != null && !dbName.trim().equals(""))
            FileUtils.writeToFile("dbName", dbName, mContext);

        this.startDatabase(dbName, null);
    }

    public void cleanDbName() {
        FileUtils.writeToFile("dbName", "", mContext);
        CommunicationServiceSqlUtil.clanDbName();
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

    public static void startActivity(String info, String status){
        //Context context = cordova.getActivity().getApplicationContext();
        mContext.startActivity(new Intent(mContext, IncomingCallActivity.class)
                .putExtra("info", info)
                .putExtra("status", status));
    }

    public void onEvent(String event, String data) {
        LogUtils.printLog(tag, event + " " + data);
        switch (event) {
            case "onWebsocketConnect":
                this.manageOnConnection();
                break;
            case "onWebsocketMessage":
                this.manageMessage(data);
                break;
            case "onWebsocketForceLogout":
                this.forcelogout();
                break;
            case "onWebsocketException":
                break;
            case "onWebsocketClose":
                break;
            default:
                LogUtils.printLog(tag, event + " event not found");
        }
        if(_plugin != null) {
            _plugin.fireEvent(CommunicationServicePlugin.Event.MESSAGE, data);
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

    private void forcelogout() {
        setDbName(null);
    }

    /**
     * Questo metodo deve effettuare
     * 1 - la ricerca di messaggi non inviati e inviarli;
     * 2 - la ricerca di messaggi ricevuti non notificati e inviarli;
     * 3 - la ricerca di messaggi letti non notificati e inviarli;
     */
    private void sentQueued() {
        LogUtils.printLog(tag,"sentQueued ");

            Integer limit = 1000;
            Integer page = 0;
            List<QuerySelectObj> conds = new LinkedList<QuerySelectObj>();
            QuerySelectObj qso = new QuerySelectObj("fromId", "=", this.userId);
            conds.add(qso);
            qso = new QuerySelectObj("isSent", "=", "false");
            conds.add(qso);

            CommunicationMessageService.getChatMessages(page, limit, conds, new SQLiteAndroidDatabaseCallback() {

                public void error(String error) {
                    LogUtils.printLog(tag, "search messages dbquery callback error " + error);
                }

                public void success(JSONArray arr) {
                    LogUtils.printLog(tag, "search messages " + arr);
                }
            });
    }

    /**
     * Questo metodo serve per richiedere dal server eventuali messaggi non ricevuti
     */
    private void askForMessages() {

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
                    WebsocketService.instance().closeSocket();
                } else {
                    this.currentSessionId = message;
                    return;
                }
            }
            String event = jobj.getString("event");

            switch (event) {
                case FORCELOGOUT:
                    FileUtils.writeToFile("websocketserviceuri", "", this.getApplicationContext());
                    WebsocketService.instance().closeSocket();
                    break;
                case UPDATEUSERS:
                    this.updateUsers(jobj.getJSONArray("data"));
                    break;
                case NEWMESSAGE:
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

                    this.addMessage(jobj, null);

                    break;
                case ENTERGROUP:
                    this.enterGroup(jobj, null);
                    break;
                case SENDMESSAGE: {
                        JSONObject data = jobj.getJSONObject("data");
                        data = data.getJSONObject("result");
                        this.receiveSendMessage(data);
                    }
                    break;
                case RECEIVEMESSAGE: {
                    JSONObject data = jobj.getJSONObject("data");
                    data = data.getJSONObject("result");
                    this.receiveReceiveMessage(data);
                }
                break;
                case READMESSAGE: {
                    JSONObject data = jobj.getJSONObject("data");
                    data = data.getJSONObject("result");
                    this.receiveReadMessage(data);
                }
                break;

                case CALL:
                    JSONObject data = jobj.getJSONObject("data");
                    String status = data.getString("status");

                    if("calling".equals(status)) {
                        CommunicationService.isCalling = true;
                        CommunicationService.fromId = data.getString("from");
                        CommunicationService.fromName = data.getString("fromCompleteName");
                        int delay=500;
                        Boolean isActive = null;
                        try {
                            String package_name = getApplication().getPackageName();
                            Class<?> clazz = Class.forName(package_name + "MainActivity");
                            Field f = clazz.getField("active");
                            isActive = f.getBoolean(null);
                        } catch(Exception e) {
                            LogUtils.printLog(tag,"exception... " + e.getMessage());
                            e.printStackTrace();
                        }
                        if (isActive == null || !isActive)
                        {
                            LogUtils.printLog(tag,"APP RIAVVIATA");
                            delay=4000;
                            restartApp();
                        }
                        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                startActivity(CommunicationService.fromName, "videocall");
                            }
                        }, delay);

                    } else if("reject".equals(status)) {
                        if(CommunicationService.isCalling) {
                            CommunicationService.isCalling = false;
                            CommunicationService.fromId = null;
                            CommunicationService.fromName = null;
                        }
                    } else if("leave".equals(status)) {
                        if(CommunicationService.isCalling) {
                            CommunicationService.isCalling = false;
                            CommunicationService.fromId = null;
                            CommunicationService.fromName = null;
                        }
                    }
                    if (_plugin != null) {
                        //Gson gson = new Gson();
                        _plugin.generateEvent(CALL, message);
                    }
                    break;
                default:
                    LogUtils.printLog(tag, "Event not found " + event + " " + jobj.toString());
            }
        } catch(JSONException jex) {
            LogUtils.printLog(tag, "exception " + jex.getMessage());
        }

    }

    public static void receiveSendMessage(JSONObject data) {
        CommunicationMessageService.receiveSendMessage(data);
    }
    public static void receiveReceiveMessage(JSONObject data) {
        CommunicationMessageService.receiveReceiveMessage(data);
    }
    public static void receiveReadMessage(JSONObject data) {
        CommunicationMessageService.receiveReadMessage(data);
    }

    public static void enterGroup(JSONObject jobj, CallbackContext cbc) {
        LogUtils.printLog(tag, "enterGroup " + jobj.toString());
        Gson gson = new Gson();
        JSONObject data = jobj.optJSONObject("data");
        JSONObject result = null;
        if(data != null)
            result = data.optJSONObject("result");
        else
            result = jobj;
        if(result == null)
            result = jobj;
        //SqlChatWrapper r = gson.fromJson(jobj.toString(), SqlChatWrapper.class);
        SqlChatWrapper r = new SqlChatWrapper();
        try {
            r.uuid = result.getString("id");
            r.chatName = result.getString("groupName");
            r.chatDescription = result.getString("groupMessageDescription");
            r.lastRandom = "";
            r.lastMessage = "";
            r.lastUser = "-";
            r.isGroup = true;
            r.timestamp = new Date().getTime();
            r.numNotRead = 0;
        } catch(JSONException ex) {
            LogUtils.printLog(tag, "exception!!!!! " + ex.getMessage());
            if (cbc != null)
                cbc.error(ex.getMessage());
        }
        CommunicationServicePlugin pg = _plugin;
        CommunicationMessageService.findChat(r.uuid, true, new SQLiteAndroidDatabaseCallback() {
            public void error(String error) {
                LogUtils.printLog(tag, "dbquery callback error " + error);
                if (cbc != null)
                    cbc.error(error);
            }
            public void success(JSONArray arr) {
                JSONArray chatArr = null;
                try {
                    chatArr = CommunicationMessageService.extractResult(arr);
                } catch(JSONException ex) {
                    chatArr = null;
                } catch(Exception e) {
                    if(cbc != null)
                        cbc.error(e.getMessage());
                    return;
                }
                if(chatArr == null || chatArr.length() <= 0) {
                    CommunicationService.instance().addChat(r, cbc, new SQLiteAndroidDatabaseCallback() {
                        public void error(String error) {
                            LogUtils.printLog(tag, "dbquery callback error " + error);

                        }
                        public void success(JSONArray arr) {
                            if (pg != null) {
                                Gson gson = new Gson();
                                pg.generateEvent(NEWCHAT, gson.toJson(r));
                            }
                        }
                    });
                } else {
                    if (pg != null) {
                        Gson gson = new Gson();
                        pg.generateEvent(NEWCHAT, gson.toJson(r));
                    }
                }
            }
        });
    }

    public static void vibrate() {
        vibrate(1000);
    }

    public static void vibrate(Integer duration) {
        if(duration == null)
            duration = 1000;
        try {
            Vibrator v = (Vibrator) mContext.getSystemService(Context.VIBRATOR_SERVICE);
            v.vibrate(duration);
            LogUtils.printLog(tag," MESSAGE RECEIVED");
        } catch (Exception e) {
            LogUtils.printLog(tag,"ACTIVITY ERROR ON  MESSAGE RECEIVED VIBRATION");
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
     * - inviare al backend
     * un messaggio di avvenuta ricezione
     * @param jobj
     */
    public void addMessage(JSONObject jobj, CallbackContext cbc) {
        //String insertMessageQuery = CommunicationServiceSqlUtil.getInsertMessageQuery();
        //salvare il messaggio e flaggarlo come ricevuto
        LogUtils.printLog(tag, "FASE0 saveMessageAndChat ");
        CommunicationServicePlugin pg = _plugin;
        CommunicationMessageService.saveMessageAndChat(jobj, new SQLiteAndroidDatabaseCallback(){
            public void error(String error){
                LogUtils.printLog(tag, "dbquery callback error " + error);
                if(cbc != null) {
                    cbc.error(error);
                }
            }
            public void success(JSONArray arr) {
                //deve inviare al backend l'avvenuta ricezione
                try {
                    SqlMessageWrapper s = CommunicationMessageService.extractMessage(jobj);
                    ReceiveReadMessagesReq r = new ReceiveReadMessagesReq();
                    if(s.isGroup)
                        r.groupId = s.groupId;
                    r.lastRandom = Long.parseLong(s.randomId);
                    r.userUuid = s.fromId;
                    sendToWebsocket(DOWNLOADMESSAGE, r);
                    LogUtils.printLog(tag, "FASE7 message and chat saved ");
                    if (cbc != null) {
                        LogUtils.printLog(tag, "FASE8 call callback ");
                        PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, arr);
                        cbc.sendPluginResult(pluginResult);
                    } else {
                        LogUtils.printLog(tag, "FASE8 generate event... " + (pg != null ? "not null" : "null"));
                        if (pg != null) {
                            Gson gson = new Gson();
                            pg.generateEvent(NEWMESSAGE, gson.toJson(s));
                        }
                    }
                } catch(JSONException ex) {
                    if (cbc != null) {
                        cbc.error(ex.getMessage());
                    }
                }

            }
        });
    }

    public void addChat(JSONObject jobj, CallbackContext cbc) {
        Gson gson = new Gson();
        SqlChatWrapper r = gson.fromJson(jobj.toString(), SqlChatWrapper.class);
        addChat(r, cbc, null);
    }

    public void addChat(SqlChatWrapper r, CallbackContext cbc, SQLiteAndroidDatabaseCallback cbcs) {
        r.numNotRead = 0;
        r.lastRandom = "";
        r.lastMessage = "-";
        r.lastUser = "-";
        r.timestamp = new Date().getTime();
        Map<String, Object> map = CommunicationMessageService.convertChatToMap(r);
        CommunicationMessageService.saveChat(map, new  SQLiteAndroidDatabaseCallback() {
            public void error(String error) {
                LogUtils.printLog(tag, "dbquery callback error " + error);
                if (cbc != null) {
                    cbc.error(error);
                }
                if(cbcs != null) {
                    cbcs.error(error);
                }
            }

            public void success(JSONArray arr) {
                LogUtils.printLog(tag, "addChat ok ");
                if(cbc != null) {
                    PluginResult pluginResult = new PluginResult(PluginResult.Status.OK);
                    cbc.sendPluginResult(pluginResult);
                }
                if(cbcs != null) {
                    cbcs.success(arr);
                }
            }
        });
    }

    public static void send(JSONObject message, CallbackContext cbc) {
        LogUtils.printLog(tag, "FASE0 sendMessageAndChat ");

        Gson gson = new Gson();
        SendMessageRequest r = gson.fromJson(message.toString(), SendMessageRequest.class);
        r.isAttachment = r.isAttach;
        r.tempId = r.attachmentId;
        LogUtils.printLog(tag, "FASE1 extract message");
        SqlMessageWrapper s = SqlMessageWrapper.buildFromRequest(r);

        CommunicationMessageService.sendMessageAndChat(s, new SQLiteAndroidDatabaseCallback(){
            public void error(String error) {
                LogUtils.printLog(tag, "dbquery callback error " + error);
                if(cbc != null) {
                    cbc.error(error);
                }
            }
            public void success(JSONArray arr) {
                //deve inviare al backend l'avvenuta ricezione

                sendToWebsocket(SENDMESSAGE, r);
                LogUtils.printLog(tag, "FASE7 message and chat saved ");
                if(cbc != null){
                    PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, arr);
                    cbc.sendPluginResult(pluginResult);
                } else {
                    /*if(CommunicationService.instance()._plugin != null) {
                        Gson gson = new Gson();
                        CommunicationService.instance()._plugin.generateEvent(NEWMESSAGE, gson.toJson(s));
                    }*/
                }

            }
        });
    }

    public void sendSocket(String message, CallbackContext cbc) {
        WebsocketService.instance().asyncSend(message);
        if(cbc != null) {
            PluginResult pluginResult = new PluginResult(PluginResult.Status.OK);
            cbc.sendPluginResult(pluginResult);
        }
    }

    public void checkSocket(CallbackContext cbc) {
        boolean isConnected = WebsocketService.instance().getConnected();
        if(cbc != null) {
            PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, isConnected);
            cbc.sendPluginResult(pluginResult);
        }
    }

    public static void read(Long id, String fromId, String randomId, String groupId, CallbackContext cbc) {
        Map<String, Object> fields = new HashMap<String, Object>();
        fields.put("isRead", true);
        if(id != null) {
            CommunicationMessageService.updateMessage(id, fields, new SQLiteAndroidDatabaseCallback() {
                public void error(String error) {
                    LogUtils.printLog(tag, "dbquery callback error " + error);
                    if (cbc != null) {
                        cbc.error(error);
                    }
                }

                public void success(JSONArray arr) {
                    CommunicationMessageService.findChatCountAndUpdate(fromId, groupId != null ? true : false, null);
                    ReceiveReadMessagesReq r = new ReceiveReadMessagesReq();
                    if (groupId != null)
                        r.groupId = groupId;
                    r.lastRandom = Long.parseLong(randomId);
                    r.userUuid = fromId;
                    sendToWebsocket(READMESSAGE, r);
                }
            });
        } else {
            CommunicationMessageService.updateMessage(fromId, randomId, groupId, fields, new SQLiteAndroidDatabaseCallback() {
                public void error(String error) {
                    LogUtils.printLog(tag, "dbquery callback error " + error);
                    if (cbc != null) {
                        cbc.error(error);
                    }
                }

                public void success(JSONArray arr) {
                    CommunicationMessageService.findChatCountAndUpdate(fromId, groupId != null ? true : false, null);
                    ReceiveReadMessagesReq r = new ReceiveReadMessagesReq();
                    if (groupId != null)
                        r.groupId = groupId;
                    r.lastRandom = Long.parseLong(randomId);
                    r.userUuid = fromId;
                    sendToWebsocket(READMESSAGE, r);
                }
            });
        }
    }

    /*public static void read(Long id, String fromId, String randomId, String groupId, CallbackContext cbc) {
        Map<String, Object> fields = new HashMap<String, Object>();
        fields.put("isRead", true);
        if(id != null) {
            CommunicationMessageService.updateMessage(id, fields, new SQLiteAndroidDatabaseCallback() {
                public void error(String error) {
                    LogUtils.printLog(tag, "dbquery callback error " + error);
                    if (cbc != null) {
                        cbc.error(error);
                    }
                }

                public void success(JSONArray arr) {
                    CommunicationMessageService.findChatCountAndUpdate(fromId, groupId != null ? true : false, null);
                    ReceiveReadMessagesReq r = new ReceiveReadMessagesReq();
                    if (groupId != null)
                        r.groupId = groupId;
                    r.lastRandom = Long.parseLong(randomId);
                    r.userUuid = fromId;
                    sendToWebsocket(READMESSAGE, r);
                }
            });
        } else {
            CommunicationMessageService.updateMessage(fromId, randomId, groupId, fields, new SQLiteAndroidDatabaseCallback() {
                public void error(String error) {
                    LogUtils.printLog(tag, "dbquery callback error " + error);
                    if (cbc != null) {
                        cbc.error(error);
                    }
                }

                public void success(JSONArray arr) {
                    CommunicationMessageService.findChatCountAndUpdate(fromId, groupId != null ? true : false, null);
                    ReceiveReadMessagesReq r = new ReceiveReadMessagesReq();
                    if (groupId != null)
                        r.groupId = groupId;
                    r.lastRandom = Long.parseLong(randomId);
                    r.userUuid = fromId;
                    sendToWebsocket(READMESSAGE, r);
                }
            });
        }
    }*/

    public static void updateMessage(Long id, String fromId, String randomId, String groupId, JSONArray fieldsList, CallbackContext cbc) {
        Map<String, Object> fields = new HashMap<String, Object>();
        JSONObject jobj;
        for(int i = 0; i < fieldsList.length(); i++) {
            try {
                jobj = fieldsList.getJSONObject(i);
                fields.put(jobj.getString("field"), jobj.get("value"));
            } catch(Exception e) {
                LogUtils.printLog(tag, "updateMessage error " + e.getMessage());
                continue;
            }
        }
        if(fields.size() == 0) {
            if(cbc != null)
                cbc.error("non ci sono campi da aggiornare");
        }

        if(id != null) {
            CommunicationMessageService.updateMessage(id, fields, new SQLiteAndroidDatabaseCallback() {
                public void error(String error) {
                    LogUtils.printLog(tag, "dbquery callback error " + error);
                    if (cbc != null) {
                        cbc.error(error);
                    }
                }

                public void success(JSONArray arr) {
                    if(cbc != null)
                        cbc.success(arr);
                }
            });
        } else {
            CommunicationMessageService.updateMessage(fromId, randomId, groupId, fields, new SQLiteAndroidDatabaseCallback() {
                public void error(String error) {
                    LogUtils.printLog(tag, "dbquery callback error " + error);
                    if (cbc != null) {
                        cbc.error(error);
                    }
                }

                public void success(JSONArray arr) {
                    if(cbc != null)
                        cbc.success(arr);
                }
            });
        }
    }

    public static void findChatCountAndUpdate(String fromId, boolean isGroup, SQLiteAndroidDatabaseCallback cbc) {
        CommunicationMessageService.findChatCountAndUpdate(fromId, isGroup, cbc);
    }



    public static class SendMessageRequest {
        public String fromId;
        public String fromName;
        public String uuid;
        public String toName;
        public Boolean isGroup;
        public String message;
        public Long randomId;
        public Long timestamp;
        public String socketSessionFrom;
        public String replyTo;
        public boolean isAttach = false;
        public String attachmentType = null;
        public String attachmentName;
        public String attachmentId;
        public String localPath;
        public boolean isAttachment = false;
        public String tempId;
    }

    public static class ReceiveReadMessagesReq {
        public String userUuid;
        public Long lastRandom;
        public String groupId;
    }

    public static void sendToWebsocket(String event, Object data) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("event", event);
        map.put("data", data);
        Gson gson = new Gson();
        WebsocketService.instance().asyncSend(gson.toJson(map));
    }


    /**
     * GESTIONE OPERATORI
     */

    /**
     * Questo metodo viene invocato per effettuare verso il backend una richiesta
     * degli operatori attualmente connessi
     */

    public void reloadUserList(){
        LogUtils.printLog(tag, "reloadUserList");
        //if(WebsocketService.instance().getIsConnected()) {
            WebsocketService.instance().asyncSend("{\"event\":\""+ USERSLIST + "\",\"data\":{}}");
        //}
    }

    /**
     * Questo metodo viene invocato ogni volta che dal backend arriva la nuova lista degli utenti
     * @param users
     */
    public void updateUsers(JSONArray users) {
        this.users.clear();
        if(users == null)
            return;
        int length = users.length();
        Map<String, UserSessionFacade> userMap = new HashMap<String, UserSessionFacade>();
        for(int i=0; i<length; i++) {
            try {
                JSONObject jObj = users.getJSONObject(i);
                UserSessionFacade usf = UserSessionFacade.buildFromJson(jObj);
                if (usf == null)
                    continue;
                userMap.put(usf.userId, usf);
            } catch(JSONException ex) {
                continue;
            }
        }
        this.users = userMap;
        LogUtils.printLog(tag, "updateUsers... comunicate...");
        this.comunicateUsers();
    }

    public void comunicateUsers() {
        if(_plugin != null) {
            _plugin.generateEvent(UPDATEUSERS, getUsersJSONObject());
        }
    }

    public JSONObject getUsersJSONObject() {
        JSONObject ret = new JSONObject();
        if(this.users != null) {
            for(String userId : this.users.keySet()) {
                if(userId == null)
                    continue;
                JSONObject jo = this.users.get(userId).toJsonObject();
                if(jo == null)
                    continue;
                try {
                    ret.put(userId, jo);
                } catch(JSONException ex) {
                    continue;
                }
            }
        }
        return ret;
    }

    public static class UserSessionFacade {

        public String userId;

        public String name;

        public String surname;

        public String status;

        public String sessionId;

        public List<String> sessionIds;

        public JSONObject toJsonObject() {
            try {
                JSONObject ret = new JSONObject();
                ret.put("userId", userId);
                ret.put("name", name);
                ret.put("surname", surname);
                ret.put("status", status);
                ret.put("sessionId", sessionId);
                JSONArray arr = new JSONArray();
                for (String s : sessionIds)
                    arr.put(s);
                ret.put("sessionIds", arr);
                return ret;
            } catch(JSONException ex) {
                return null;
            }
        }

        public static UserSessionFacade buildFromJson(JSONObject jObj) {
            UserSessionFacade ret = new UserSessionFacade();
            try {
                ret.userId = jObj.getString("userId");
                ret.name = jObj.getString("name");
                ret.surname = jObj.getString("surname");
                ret.status = jObj.getString("status");
                ret.sessionId = jObj.optString("sessionId");
                ret.sessionIds = new LinkedList();
                JSONArray sessionArray = jObj.optJSONArray("sessionIds");
                if(sessionArray != null) {
                    int length = sessionArray.length();
                    for(int i=0; i<length; i++) {
                        ret.sessionIds.add(sessionArray.getString(i));
                    }
                }
                
                return ret;
            } catch(JSONException ex) {
                return null;
            }
        }
    }

    public static void getChatMessages(String uuid, Boolean isGroup, Integer page, Integer limit, List<QuerySelectObj> conds, CallbackContext cbc, SQLiteAndroidDatabaseCallback dbc) {
        CommunicationMessageService.getChatMessages(uuid, isGroup, page, limit, conds, cbc, dbc);
    }

    public static void getAllChats(SQLiteAndroidDatabaseCallback cbc) {
        CommunicationMessageService.getAllChats(cbc);
    }

    public static void getAllMessages(SQLiteAndroidDatabaseCallback cbc) {
        CommunicationMessageService.getAllMessages(cbc);
    }

    public static void getAllChats(CallbackContext cbc) {
        CommunicationMessageService.getAllChats(cbc);
    }

    public static void getAllMessages(CallbackContext cbc) {
        CommunicationMessageService.getAllMessages(cbc);
    }

    public static void generateEvent(String event, String obj) {
        LogUtils.printLog(tag, "generateEvent send to plugin");
        if (_plugin != null) {
            LogUtils.printLog(tag, "generateEvent send to plugin generate event");
            _plugin.generateEvent(event, obj);
        }
    }

    public static void callEvent(String event, String obj) {
        LogUtils.printLog(tag, "generateEvent send to plugin");
        if("onAcceptCall".equals(event)) {
            if(CommunicationService.isCalling) {
                Map<String, Object> ob = new HashMap<String, Object>();
                ob.put("event", "call");
                Map<String, Object> m = new HashMap<String, Object>();
                m.put("status", event);
                m.put("from", CommunicationService.fromId);
                m.put("fromCompleteName", CommunicationService.fromName);
                ob.put("data", m);

                if (_plugin != null) {
                    Gson gson = new Gson();
                    LogUtils.printLog(tag, "generateEvent send to plugin generate event");
                    _plugin.generateEvent(CALL, gson.toJson(ob));
                }
            }
        } else if("onCancelCall".equals(event)) {
            Map<String, Object> m = new HashMap<String, Object>();
            m.put("status", "reject");
            m.put("toUserId", CommunicationService.fromId);
            sendToWebsocket(CALL, m);
        } else {
            if (_plugin != null) {
                LogUtils.printLog(tag, "generateEvent send to plugin generate event");
                _plugin.generateEvent(event, obj);
            }
        }
    }

    private void restartApp() {
        Intent i = getBaseContext().getPackageManager().
                getLaunchIntentForPackage(getBaseContext().getPackageName());
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(i);
    }

    public static void checkNotReadAndUpdateNotification() {
        CommunicationMessageService.checkNotRead(new SQLiteAndroidDatabaseCallback() {

            public void error(String error) {
                LogUtils.printLog(tag, "AAAAAAAA check not read messages error " + error);

            }

            public void success(JSONArray arr) {

            }

            @Override
            public void successObj(Object obj) {
                Integer c = null;
                if(obj != null) {
                    c = (Integer) obj;
                    LogUtils.printLog(tag, "AAAAAAAA check not read successObj " + c);
                    CommunicationService.instance().updateNotification(null, c);
                } else {
                    LogUtils.printLog(tag, "AAAAAAAA check not read successObj NULL!!!");
                }
            }
        });
    }

    private void updateNotification(Boolean b, Integer num) {
        NotificationService.instance().updateNotification(b, this, _mainClass);
    }
}

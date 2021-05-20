package it.linup.cordova.plugin.communication;


import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.ComponentName;
import android.os.IBinder;

import android.os.AsyncTask;
import android.os.Handler;
import android.widget.Toast;
import it.linup.cordova.plugin.communication.services.CommunicationService;
import it.linup.cordova.plugin.communication.services.CommunicationService.ForegroundBinder;
import it.linup.cordova.plugin.communication.services.CommunicationMessageService.QuerySelectObj;
import it.linup.cordova.plugin.services.WebsocketListnerInterface;
import it.linup.cordova.plugin.utils.FileUtils;
import it.linup.cordova.plugin.utils.LogUtils;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaWebView;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import static android.content.Context.BIND_AUTO_CREATE;

import io.sqlc.SQLiteAndroidDatabaseCallback;
import io.sqlc.SQLiteManager;

import com.google.gson.Gson;


import java.util.List;
import java.util.LinkedList;
import java.util.Date;


//import com.facebook.react.modules.core.DeviceEventManagerModule;





public class CommunicationServicePlugin extends CordovaPlugin {

    private static final String DURATION_LONG = "long";
    private String tag = "==:== CommunicationServicePlugin :";

    // Event types for callbacks
    public enum Event { ACTIVATE, DEACTIVATE, FAILURE, MESSAGE }

    // Plugin namespace
    private static final String JS_NAMESPACE = "window.plugins.CommunicationServicePlugin";

    private static Handler m_handler;
    private Runnable m_handlerTask;
    protected static boolean isConnected = false;
    private String info = "";
    protected static boolean requestHeartBit = false;
    protected static int failedHeartBit = 0;
    protected static JSONArray lstUser;
    protected static boolean isEnableHearbitCheck;
    private static String uriToReconnect="";

    private static JSONObject defaultSettings = new JSONObject();

    public static boolean active = true;

    private CallbackContext callbackContext;

    // Service that keeps the app awake
    private CommunicationService service;

    private SQLiteManager sqliteManager = SQLiteManager.instance();


    // Used to (un)bind the service to with the activity
     private final ServiceConnection connection = new ServiceConnection()
     {
         @Override
         public void onServiceConnected (ComponentName name, IBinder service)
         {
             ForegroundBinder binder = (ForegroundBinder) service;
             CommunicationServicePlugin.this.service = binder.getService();
             CommunicationServicePlugin.this.service.setPlugin(CommunicationServicePlugin.this);
             CommunicationServicePlugin.this.service.setMainClass(getMainActivityClass());
         }
 
         @Override
         public void onServiceDisconnected (ComponentName name)
         {
             fireEvent(Event.FAILURE, "'service disconnected'");
         }
     };

     // Flag indicates if the service is bind
     private boolean isBind = false;

    /**
     * Bind the activity to a background service and put them into foreground
     * state.
     */
    private void startService()
    {

        if (isBind)
            return;

        Activity context = cordova.getActivity();

        Intent intent = new Intent(context, CommunicationService.class);
        try {
            context.bindService(intent, connection, BIND_AUTO_CREATE);
            fireEvent(Event.ACTIVATE, null);
            context.startService(intent);
            isBind = true;
        } catch (Exception e) {
            fireEvent(Event.FAILURE, String.format("'%s'", e.getMessage()));
        }


    }

    /**
     * Bind the activity to a background service and put them into foreground
     * state.
     */
    private void stopService()
    {
        Activity context = cordova.getActivity();
        Intent intent    = new Intent(context, CommunicationService.class);

        if (!isBind) return;

        fireEvent(Event.DEACTIVATE, null);
        context.unbindService(connection);
        context.stopService(intent);

        isBind = false;
    }

    public void kill() {
        m_handler.removeCallbacks(m_handlerTask);
        m_handler = null;
    }

    @Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);
        this.startService();
        if(sqliteManager.needContext()) {
            sqliteManager.setContext(this.cordova.getActivity());
        }
    }

    /**
     * Called when the system is about to start resuming a previous activity.
     *
     * @param multitasking Flag indicating if multitasking is turned on for app.
     */
    @Override
    public void onPause(boolean multitasking)
    {
        try {
            active = false;

        } finally {
            //clearKeyguardFlags(cordova.getActivity());
        }
    }

    /**
     * Called when the activity is no longer visible to the user.
     */
    @Override
    public void onStop () {
        //clearKeyguardFlags(cordova.getActivity());
        /*if(isBind && this.service != null) {
            this.service.setPlugin(null);
        }*/
        active = false;
    }

    /**
     * Called when the activity is finishing.
     */
    @Override
    public void onDestroy () {
        //clearKeyguardFlags(cordova.getActivity());
        if(isBind && this.service != null) {
            this.service.setPlugin(null);
        }
        active = false;
    }

    /**
     * Called when the activity will start interacting with the user.
     *
     * @param multitasking Flag indicating if multitasking is turned on for app.
     */
    @Override
    public void onResume (boolean multitasking)
    {
        active = true;
        this.startService();
        //stopService();
        if(isBind && this.service != null) {
            this.service.setPlugin(this);
        }
    }

    @Override
    public boolean execute(String action, JSONArray args,
                           final CallbackContext callbackContext) {
        try {
            JSONObject options = null;
            if (args != null && args.length() > 0)
                options = args.getJSONObject(0);
            switch (action) {
                case "configure":
                    this.configure(options);
                    break;
                case "enable":
                    Boolean isEnable = options.getBoolean("isEnable");
                    if (isEnable != null)
                        this.enable(isEnable);
                    break;
                case "startService":
                    this.startService();
                    break;
                case "loadUsers":
                    this.loadUsers(callbackContext);
                    break;
                case "reloadUsers":
                    this.reloadUsers(callbackContext);
                    break;
                case "addMessage":
                    this.addMessage(options, callbackContext);
                    break;
                case "getChats":
                    this.getChats(options, callbackContext);
                    break;
                case "addChat":
                    this.addChat(options, callbackContext);
                    break;
                case "vibrate":
                    this.vibrate(callbackContext);
                    break;
                case "getMessages":
                    this.getMessages(options, callbackContext);
                    break;
                case "connect":
                    //CommunicationService.instance().startActivity();
                    String userId = options.optString("userId");
                    if(sqliteManager.needContext()) {
                        sqliteManager.setContext(this.cordova.getActivity());
                    }
                    if(userId != null)
                        CommunicationService.instance().setCurrentId(userId);
                    if(userId == null || userId.trim().equals(""))
                        userId = "test";
                    userId += "-v202102181700.db";
                    CommunicationService.instance().setDbName(userId);
                {
                    PluginResult pluginResult = new PluginResult(PluginResult.Status.OK);
                    callbackContext.sendPluginResult(pluginResult);
                }

                    break;
                case "setInfo": {
                    String params = options.getString("params");
                    if (params != null)
                        this.setInfo(params);
                    break;
                }
                case "send": {
                    JSONObject params = options.optJSONObject("params");
                    if (params != null)
                        this.send(params, callbackContext);
                    break;
                }
                case "sendSocket": {
                    String params = options.optString("params");
                    if (params != null)
                        this.sendSocket(params, callbackContext);
                    break;
                }
                case "checkSocket": {
                    this.checkSocket(callbackContext);
                    break;
                }
                case "read": {
                    Long id = options.optLong("id");
                    String fromId = options.optString("fromId");
                    String randomId = options.optString("randomId");
                    String groupId = options.optString("groupId");
                    //if (id != null)
                    this.read(id, fromId, randomId, groupId, callbackContext);
                    break;
                }
                case "updateMessage": {
                    Long id = options.optLong("id");
                    String fromId = options.optString("fromId");
                    String randomId = options.optString("randomId");
                    String groupId = options.optString("groupId");
                    JSONArray fields = options.optJSONArray("fieldList");
                    //if (id != null)
                    this.updateMessage(id, fromId, randomId, groupId, fields, callbackContext);
                    break;
                }
                case "updateChatCount":
                {
                    String fromId = options.optString("uuid");
                    Boolean isGroup = options.optBoolean("isGroup");
                    CommunicationService.findChatCountAndUpdate(fromId, isGroup, new SQLiteAndroidDatabaseCallback() {
                        public void error(String error) {
                            if (callbackContext != null) {
                                callbackContext.error(error);
                            }
                        }

                        public void success(JSONArray arr) {

                        }
                        });
                }
                case "checkModule":
                    this.checkModule();
                    break;
                case "close":
                    this.close();
                    this.callbackContext = null;
                    break;
                case "show": {

                    if(true)
                        break;
                    String message = options.getString("message");
                    String duration = options.getString("duration");
                    this.show(message, duration);
                    //this.connect("");
                    PluginResult pluginResult = new PluginResult(PluginResult.Status.OK);
                    callbackContext.sendPluginResult(pluginResult);
                    break;
                }
                default:
                    callbackContext.error("\"" + action + "\" is not a recognized action.");
                    return false;
            }
        } catch(JSONException e) {
            callbackContext.error("Error encountered: " + e.getMessage());
            return false;
        }

        return true;
    }

    public void show(String message, String duration) {
        Toast toast = Toast.makeText(cordova.getActivity(), message,
                "long".equals(duration) ? Toast.LENGTH_LONG : Toast.LENGTH_SHORT);
        // Display toast
        toast.show();
    }

    /**
     * Update the default settings and configure the notification.
     *
     * @param settings The settings
     * @param update A truthy value means to update the running service.
     */
    private void configure(JSONObject settings)
    {
            this.defaultSettings = settings;
    }

    //@ReactMethod
    public void enable(boolean isEnable) {
        isEnableHearbitCheck = isEnable;
        failedHeartBit = 0;
    }

    //@Override
    public String getName() {
        return "CommunicationServicePlugin";
    }

    private static String uri;


    public static void sendMesg(String mess)
    {
    }

    public void loadUsers(CallbackContext cbc) {
        JSONObject jo = this.service.getUsersJSONObject();
        PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, jo);
        cbc.sendPluginResult(pluginResult);
    }

    public void reloadUsers(CallbackContext cbc) {
        this.service.reloadUserList();
        PluginResult pluginResult = new PluginResult(PluginResult.Status.OK);
        cbc.sendPluginResult(pluginResult);
    }

    public void addMessage(JSONObject message, CallbackContext cbc) {
        if(message != null)
            this.service.addMessage(message, cbc);
        /*PluginResult pluginResult = new PluginResult(PluginResult.Status.OK);
        cbc.sendPluginResult(pluginResult);*/
    }

    public void addChat(JSONObject chat, CallbackContext cbc) {
        if(chat != null)
            this.service.addChat(chat, cbc);
    }

    public void getChats(JSONObject obtions, CallbackContext cbc) {
        this.service.getAllChats(cbc);
    }

    public void vibrate(CallbackContext cbc) {
        this.service.vibrate();
        PluginResult pluginResult = new PluginResult(PluginResult.Status.OK);
        cbc.sendPluginResult(pluginResult);
    }

    public void getMessages(JSONObject options, CallbackContext cbc) {
        String uuid = null;
        LogUtils.printLog(tag,"getMessages " + options);
        if(options != null) {
            uuid = options.optString("uuid", null);
            Boolean isGroup = null;
            if(options.has("isGroup"))
                isGroup = options.optBoolean("isGroup");
            Integer limit = options.optInt("limit", 10);
            Integer page = options.optInt("page", 0);
            LogUtils.printLog(tag,"getMessages " + uuid + " " + isGroup);
            JSONArray conditions = options.optJSONArray("conditions");
            List<QuerySelectObj> conds = null;
            if(conditions != null) {
                Gson gson = new Gson();
                conds = new LinkedList<QuerySelectObj>();
                for(int i=0; i<conditions.length(); i++) {
                    try {
                        QuerySelectObj qso = gson.fromJson(conditions.get(i).toString(), QuerySelectObj.class);
                        conds.add(qso);
                    } catch(JSONException ex) {
                        continue;
                    }
                }
            }
            if(uuid != null && isGroup != null) {
                this.service.getChatMessages(uuid, isGroup, page, limit, conds, cbc, null);
                return;
            }
        }
        this.service.getAllMessages(cbc);
    }

    public String getCallName(String from) {

        String name = "";
        try {
            for (int i = 0; i < lstUser.length(); i++) {
                if (lstUser.getJSONObject(i).has("token")&&lstUser.getJSONObject(i).getString("token").equals(from)) {
                    name = lstUser.getJSONObject(i).getString("name");
                    break;
                }
            }
        } catch (Exception e) {

        }
        return name;
    }


    // DA SPOSTARE IN ALTRI LUOGHI E DA CAPIERE SE SERVE VERAMENTE SE SI TOGLIE L?APP SCHIATTA PERCHE' VIENE CHIAMTA DALLA PARTE NATIVA
    //@ReactMethod
    /*public void doMute(){
        AudioManager audioManager = (AudioManager) getCurrentActivity().getSystemService(Context.AUDIO_SERVICE);
        audioManager.setMode(AudioManager.MODE_IN_CALL);
        if (audioManager.isMicrophoneMute() == false) {
            audioManager.setMicrophoneMute(true);
        } else {
            audioManager.setMicrophoneMute(false);
        }
    }*/


    public Activity getCurrentActivity() {
        return this.cordova.getActivity();
    }

    //@ReactMethod
    public void connect(String uri) {
        startService();
        //WebsocketService.plugin = this;
        //WebsocketService.instance().connect(uri);
    }




    public void updateNotification(Boolean status) {
        try
        {
            LogUtils.printLog(tag,"NOTIFICATION SERVICE " + status);
        }
        catch (Exception e) {
            LogUtils.printLog(tag,"ERRORE IN NOTIFICATION SERVICE");

        }
    }

    public Class getMainActivityClass() {
        Class mainActivity;
        Context context = getApplicationContext();
        String  packageName = context.getPackageName();
        Intent  launchIntent = context.getPackageManager().getLaunchIntentForPackage(packageName);
        String  className = launchIntent.getComponent().getClassName();

        try {
            //loading the Main Activity to not import it in the plugin
            mainActivity = Class.forName(className);
            return mainActivity;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public void sendEvent(String eventName, String params) {
        try {
            LogUtils.printLog(tag,"SendEventToJS " + eventName);
            JSONObject obj = new JSONObject();
            obj.put("event", eventName);
            obj.put("param", params);
            PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, obj);
            this.callbackContext.sendPluginResult(pluginResult);

        } catch (Exception e) {
            LogUtils.printLog(tag,"problem in sendEvent");
            e.printStackTrace();
        }
    }

    public Context getApplicationContext() {
        return this.cordova.getActivity().getApplicationContext();
    }

    //@ReactMethod
    public void setInfo(String params) {
        info = params;
    }

    //@ReactMethod
    public void send(JSONObject message, CallbackContext cbc) {
        this.service.send(message, cbc);
    }

    public void sendSocket(String message, CallbackContext cbc) {
        this.service.sendSocket(message, cbc);
    }

    public void checkSocket(CallbackContext cbc) {
        this.service.checkSocket(cbc);
    }

    public void read(Long id, String fromId, String randomId, String groupId, CallbackContext cbc) {
        this.service.read(id, fromId, randomId, groupId, cbc);
    }

    public void updateMessage(Long id, String fromId, String randomId, String groupId, JSONArray fieldsList, CallbackContext cbc) {
        this.service.updateMessage(id, fromId, randomId, groupId, fieldsList, cbc);
    }

    //@ReactMethod
    public void checkModule() {
        //sendEvent("onCheckModule", "{\"failed:\"" + this.failedHeartBit + ",\"connected\":"+this.isConnected+"}" );
        generateEvent("onCheckModule", "{\"failed:\"" + this.failedHeartBit + ",\"connected\":"+this.isConnected+"}" );
    }

    //@ReactMethod
    public void close() {
        FileUtils.writeToFile("idrauri","", this.getApplicationContext());
    }

    private class SendOperation extends AsyncTask<String, Void, Void> {

        @Override
        protected Void doInBackground(String... params) {
            if(params != null && params.length == 1) {
                //WebsocketService.instance().send(params);
                //_sock.send(params[0]);
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
        }

        @Override
        protected void onPreExecute() {
        }

        @Override
        protected void onProgressUpdate(Void... values) {
        }
    }

    /**
     * Fire event with some parameters inside the web view.
     *
     * @param event The name of the event
     * @param params Optional arguments for the event
     */
    public void fireEvent (Event event, String params)
    {
        String eventName = event.name().toLowerCase();
        Boolean active   = event == Event.ACTIVATE;
        String timestamp = "" + new Date().getTime();
        String str = String.format("%s.on('%s', %s, %s)",
                JS_NAMESPACE, eventName, params, timestamp);

        str = String.format("%s;%s.fireEvent('%s',%s, %s);",
                str, JS_NAMESPACE, eventName, params, timestamp);

        final String js = str;

        cordova.getActivity().runOnUiThread(() -> webView.loadUrl("javascript:" + js));
    }

    public void generateEvent (String eventName, JSONObject params) {
        this.generateEvent(eventName, params.toString());
    }

    public void generateEvent (String eventName, String params)
    {
        String timestamp = "" + new Date().getTime();
        String str = String.format("%s.onEvent('%s',%s, %s);",
                JS_NAMESPACE, eventName, params, timestamp);

        final String js = str;

        cordova.getActivity().runOnUiThread(() -> webView.loadUrl("javascript:" + js));
    }

}


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
import it.linup.cordova.plugin.services.WebsocketListnerInterface;
import it.linup.cordova.plugin.utils.FileUtils;
import it.linup.cordova.plugin.utils.LogUtils;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import static android.content.Context.BIND_AUTO_CREATE;


//import com.facebook.react.modules.core.DeviceEventManagerModule;





public class CommunicationServicePlugin extends CordovaPlugin {

    private static final String DURATION_LONG = "long";
    private String tag = "==:== CommunicationServicePlugin :";

    // Event types for callbacks
    private enum Event { ACTIVATE, DEACTIVATE, FAILURE }

    // Plugin namespace
    private static final String JS_NAMESPACE = "cordova.plugin.CommunicationServicePlugin";

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

     // Used to (un)bind the service to with the activity
     private final ServiceConnection connection = new ServiceConnection()
     {
         @Override
         public void onServiceConnected (ComponentName name, IBinder service)
         {
             ForegroundBinder binder = (ForegroundBinder) service;
             CommunicationServicePlugin.this.service = binder.getService();
         }
 
         @Override
         public void onServiceDisconnected (ComponentName name)
         {
             fireEvent(Event.FAILURE, "'service disconnected'");
         }
     };

     // Flag indicates if the service is bind
     private boolean isBind = false;

    public void kill() {
        m_handler.removeCallbacks(m_handlerTask);
        m_handler = null;
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
        //stopService();
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
                case "connect":
                    if(true)
                        break;
                    String uri = options.getString("uri");
                    LogUtils.printLog(tag,"connect uri: " + uri);
                    this.callbackContext = callbackContext;
                    if (uri != null)
                        this.connect(uri);
                    break;
                case "setInfo": {
                    String params = options.getString("params");
                    if (params != null)
                        this.setInfo(params);
                    break;
                }
                case "send": {
                    String params = options.getString("params");
                    if (params != null)
                        this.send(params);
                    break;
                }
                case "checkModule":
                    this.checkModule();
                    break;
                case "close":
                    this.close();
                    this.callbackContext = null;
                    break;
                case "show": {
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

     /**
     * Bind the activity to a background service and put them into foreground
     * state.
     */
    private void startService()
    {
        Activity context = cordova.getActivity();

        if (isBind)
            return;

        Intent intent = new Intent(context, CommunicationService.class);
        try {
            context.bindService(intent, connection, BIND_AUTO_CREATE);
            fireEvent(Event.ACTIVATE, null);
            context.startService(intent);
        } catch (Exception e) {
            fireEvent(Event.FAILURE, String.format("'%s'", e.getMessage()));
        }

        isBind = true;
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


    public void updateNotification(Boolean status) {
        try
        {
            LogUtils.printLog(tag,"NOTIFICATION SERVICE " + status);
        }
        catch (Exception e) {
            LogUtils.printLog(tag,"ERRORE IN NOTIFICATION SERVICE");

        }
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
    public void send(String params) {
        //if(_sock!=null) {
        //_sock.send(params);
        new SendOperation().execute(params);


    }
    //@ReactMethod
    public void checkModule() {
        sendEvent("onCheckModule", "{\"failed:\"" + this.failedHeartBit + ",\"connected\":"+this.isConnected+"}" );
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
     * Fire vent with some parameters inside the web view.
     *
     * @param event The name of the event
     * @param params Optional arguments for the event
     */
    private void fireEvent (Event event, String params)
    {
        String eventName = event.name().toLowerCase();
        Boolean active   = event == Event.ACTIVATE;

        String str = String.format("%s._setActive(%b)",
                JS_NAMESPACE, active);

        str = String.format("%s;%s.on('%s', %s)",
                str, JS_NAMESPACE, eventName, params);

        str = String.format("%s;%s.fireEvent('%s',%s);",
                str, JS_NAMESPACE, eventName, params);

        final String js = str;

        cordova.getActivity().runOnUiThread(() -> webView.loadUrl("javascript:" + js));
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

}


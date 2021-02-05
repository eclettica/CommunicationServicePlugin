package it.linup.cordova.plugin.communication.services;

import io.sqlc.SQLiteAndroidDatabaseCallback;
import io.sqlc.SQLiteManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import it.linup.cordova.plugin.utils.LogUtils;



public class CommunicationServiceSqlUtil {

    private static String dbName;

    private static JSONObject options;

    private static SQLiteManager sqliteManager = SQLiteManager.instance();

    public static String tag="COMMUNICATIONSERVICE - CommunicationServiceSqlUtil";

    private static boolean messageExist = false;


    static boolean checkMessageExist() {
        return messageExist;
    }

    static void setDbName(String _dbName, JSONObject _options) {
        dbName = _dbName;
        options = _options;
        sqliteManager.startDatabase(dbName, options, null);
        checkAllTables();
    }

    static void checkAllTables() {
        checkMessageTable();
    }

    static void checkMessageTable() {
        String query = "CREATE TABLE IF NOT EXISTS Message (id  INTEGER PRIMARY KEY AUTOINCREMENT, randomId TEXT, fromId TEXT, fromName TEXT, textMsg TEXT, toId TEXT, isGroup BOOLEAN, isAttach BOOLEAN, attachmentId TEXT, attachmentType TEXT, attachmentName TEXT, isSent BOOLEAN, isReceived BOOLEAN, isRead BOOLEAN, isReceivedComunicated BOOLEAN, isReadComunicated BOOLEAN, replyTo TEXT, time INTEGER, clientTime INTEGER)";
        executeSingle(query, null, new SQLiteAndroidDatabaseCallback() {
            public void error(String error){
                LogUtils.printLog(tag, "dbquery callback error " + error);
                messageExist = false;
            }
            public void success(JSONArray arr) {
                messageExist = true;
                executeSingle("SELECT sql FROM sqlite_master WHERE name='Message' ", null, new SQLiteAndroidDatabaseCallback() {
                    public void error(String error) {
                        LogUtils.printLog(tag, "dbquery callback error " + error);
                    }

                    public void success(JSONArray arr) {
                        for (int i = 0 ; i < arr.length(); i++) {
                            try {
                                JSONObject obj = arr.getJSONObject(i);
                                LogUtils.printLog(tag, "res  " + i + ": " + obj.toString());
                            } catch(Exception e) {
                                LogUtils.printLog(tag, "cannot get response for  " + i );
                            }
                        }
                    }
                });

            }
        });
    }


    static void executeSingle(String query, JSONArray params, SQLiteAndroidDatabaseCallback cbc) {
        sqliteManager.executeSingle(dbName, query, params, cbc);
    }

}
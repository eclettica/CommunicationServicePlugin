package it.linup.cordova.plugin.communication.services;

import io.sqlc.SQLiteAndroidDatabaseCallback;
import io.sqlc.SQLiteManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import it.linup.cordova.plugin.utils.LogUtils;
import org.apache.cordova.CallbackContext;




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
        checkChatTable();
    }

    static void checkMessageTable() {
        String query = "CREATE TABLE IF NOT EXISTS Message (id  INTEGER PRIMARY KEY AUTOINCREMENT, randomId TEXT, fromId TEXT, fromName TEXT, textMsg TEXT, toId TEXT, toName TEXT, isGroup BOOLEAN, groupId TEXT, isAttach BOOLEAN, attachmentId TEXT, attachmentType TEXT, attachmentName TEXT, isSent BOOLEAN, isReceived BOOLEAN, isRead BOOLEAN, isReceivedComunicated BOOLEAN, isReadComunicated BOOLEAN, replyTo TEXT, time INTEGER, clientTime INTEGER)";
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

    static void checkChatTable() {
        String query = "CREATE TABLE IF NOT EXISTS Chat (id  INTEGER PRIMARY KEY AUTOINCREMENT, uuid TEXT, lastRandom TEXT, lastMessage TEXT, lastUser TEXT, isGroup BOOLEAN, timestamp INTEGER, numNotRead INTEGER)";
        executeSingle(query, null, new SQLiteAndroidDatabaseCallback() {
            public void error(String error){
                LogUtils.printLog(tag, "dbquery callback error " + error);
                messageExist = false;
            }
            public void success(JSONArray arr) {
                messageExist = true;
                executeSingle("SELECT sql FROM sqlite_master WHERE name='Chat' ", null, new SQLiteAndroidDatabaseCallback() {
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
        LogUtils.printLog(tag, "DBNAME " + dbName + " query: " + query);
        sqliteManager.executeSingle(dbName, query, params, cbc);
    }

    static void executeSingle(String query, JSONArray params, CallbackContext cbc) {
        LogUtils.printLog(tag, "DBNAME " + dbName + " query: " + query);
        sqliteManager.executeSingle(dbName, query, params, cbc);
    }

}
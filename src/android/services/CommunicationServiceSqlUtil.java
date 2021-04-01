package it.linup.cordova.plugin.communication.services;

import io.sqlc.SQLiteAndroidDatabaseCallback;
import io.sqlc.SQLiteManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import it.linup.cordova.plugin.utils.LogUtils;
import org.apache.cordova.CallbackContext;

import java.util.Date;




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

    static void clanDbName() {
        dbName = null;
        options = null;
    }

    static void checkAllTables() {
        checkEvolutionTable();
        checkMessageTable();
        checkChatTable();

        evol20210330();
    }

    static void evol20210330() {
        LogUtils.printLog(tag, "evol20210330 MAKE ");
        String query = "ALTER TABLE Message ADD COLUMN localPath TEXT";
        makeEvolution(1, query, null, new SQLiteAndroidDatabaseCallback() {
            public void error(String error){
                LogUtils.printLog(tag, "evol20210330 error " + error);
                printEvolutions();
            }
            public void success(JSONArray arr) {
                LogUtils.printLog(tag, "evol20210330 ok ");
                printEvolutions();
            }
            public void successObj(Object arr) {
                LogUtils.printLog(tag, "evol20210330 ok ");
                printEvolutions();
            }
        });
    }

    static void checkEvolutionTable() {
        String query = "CREATE TABLE IF NOT EXISTS Evolutions (id  INTEGER PRIMARY KEY AUTOINCREMENT, evolutionNum INTEGER, isApplied BOOLEAN, isError BOOLEAN, errorMsg TEXT, time INTEGER)";
        executeSingle(query, null, new SQLiteAndroidDatabaseCallback() {
            public void error(String error){
                LogUtils.printLog(tag, "dbquery callback error " + error);
                messageExist = false;
            }
            public void success(JSONArray arr) {
                messageExist = true;
                executeSingle("SELECT sql FROM sqlite_master WHERE name='Evolutions' ", null, new SQLiteAndroidDatabaseCallback() {
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

    static void makeEvolution(Integer evolutionNum, String query, JSONArray params, SQLiteAndroidDatabaseCallback sdc) {
        checkEvolution(evolutionNum, new SQLiteAndroidDatabaseCallback() {
            public void error(String error){
                LogUtils.printLog(tag, "dbquery callback error " + error);
                if(sdc != null)
                    sdc.error(error);
            }
            public void success(JSONArray arr) {}

            public void successObj(Object alreadyExistsObj) {
                LogUtils.printLog(tag, "make evolution " + evolutionNum + " success... " + alreadyExistsObj);
                if(alreadyExistsObj == null){
                    LogUtils.printLog(tag, "make evolution " + evolutionNum + " success... no callback");
                    if(sdc != null)
                        sdc.error("No callback...");
                }
                try {
                    Boolean alreadyExists = (Boolean) alreadyExistsObj;
                    if(alreadyExists) {
                        LogUtils.printLog(tag, "make evolution " + evolutionNum + " success... exist!" + alreadyExists);
                        if(sdc != null)
                            sdc.successObj(new Boolean(true));
                    } else {
                        LogUtils.printLog(tag, "make evolution " + evolutionNum + " success... make query!" + query);
                        executeSingle(query, params, new SQLiteAndroidDatabaseCallback() {
                            public void error(String err) {
                                LogUtils.printLog(tag, "evolution dbquery callback error " + err);
                                if(sdc != null)
                                    sdc.error(err);
                            }

                            public void success(JSONArray arr) {
                                LogUtils.printLog(tag, "make evolution " + evolutionNum + " now insert evolution");

                                insertEvolution(evolutionNum, sdc);
                            }
                        });
                    }
                } catch(Exception e) {
                    if(sdc != null)
                        sdc.error(e.getMessage());
                }

            }
                });
    }

    static void insertEvolution(Integer evolutionNum, SQLiteAndroidDatabaseCallback sdc) {
        String query = "INSERT INTO Evolutions (evolutionNum,  isApplied, isError, time) VALUES (?, ?, ?, ?)";
        JSONArray jsonArray = new JSONArray();
        jsonArray.put(evolutionNum);
        jsonArray.put(true);
        jsonArray.put(false);
        jsonArray.put(new Date().getTime());
        executeSingle(query, jsonArray, new SQLiteAndroidDatabaseCallback() {
            public void error(String error) {
                LogUtils.printLog(tag, "dbquery callback error " + error);
                if(sdc != null)
                    sdc.error(error);
            }

            public void success(JSONArray arr) {
                if(sdc != null)
                    sdc.success(arr);
            }
        });
    }

        static void checkEvolution(Integer evolutionNum, SQLiteAndroidDatabaseCallback sdc) {
            LogUtils.printLog(tag, "evolution " + evolutionNum + " check ");
            String query = "SELECT * FROM Evolutions where evolutionNum="+evolutionNum;
        executeSingle(query, null, new SQLiteAndroidDatabaseCallback() {
            public void error(String error){
                LogUtils.printLog(tag, "dbquery callback error " + error);
                sdc.error(error);
            }
            public void success(JSONArray arr) {
                if(arr == null) {
                    LogUtils.printLog(tag, "evolution " + evolutionNum + " arr is null! ");
                    if(sdc != null)
                        sdc.successObj(new Boolean(false));
                    return;
                }
                LogUtils.printLog(tag, "evolution " + evolutionNum + " ----->arr is not null! ");
                LogUtils.printLog(tag, "evolution " + evolutionNum + " ----->arr is not null! " + arr.length());
                for (int i = 0 ; i < arr.length(); i++) {
                    LogUtils.printLog(tag, "evolution " + evolutionNum + " ----->arr is not null! " + arr.length());
                    try {
                        JSONObject obj = arr.getJSONObject(i);
                        LogUtils.printLog(tag, "res  " + i + ": " + obj.toString());
                        String type = obj.getString("type");
                        if("success".equals(type)) {
                            obj = obj.getJSONObject("result");
                            arr = obj.optJSONArray("rows");
                            if(arr == null || arr.length() <= 0) {
                                LogUtils.printLog(tag, "evolution " + evolutionNum + " don't exist... ");
                                if(sdc != null)
                                    sdc.successObj(new Boolean(false));
                                return;
                            } else {
                                LogUtils.printLog(tag, "evolution " + evolutionNum + " already exist... ");
                                if(sdc != null)
                                    sdc.successObj(new Boolean(true));
                                return;
                            }
                        }
                        sdc.error("ricerca in errore " + obj.toString());
                    } catch(Exception e) {
                        LogUtils.printLog(tag, "cannot get response for  " + i );
                        sdc.error(e.getMessage());
                    }
                }
            }
        });
    }

    static void printEvolutions() {
        String query = "SELECT * FROM Evolutions";
        executeSingle(query, null, new SQLiteAndroidDatabaseCallback() {
            public void error(String error){
                LogUtils.printLog(tag, "dbquery callback error " + error);
            }
            public void success(JSONArray arr) {
                if(arr == null) {
                    LogUtils.printLog(tag, "printEvolutions arr is null...");
                    return;
                }
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

    static void checkMessageTable() {
        String query = "CREATE TABLE IF NOT EXISTS Message (id  INTEGER PRIMARY KEY AUTOINCREMENT, randomId TEXT, fromId TEXT, fromName TEXT, textMsg TEXT, toId TEXT, toName TEXT, isGroup BOOLEAN, groupId TEXT, isAttach BOOLEAN, attachmentId TEXT, attachmentType TEXT, attachmentName TEXT, isSent BOOLEAN, isReceived BOOLEAN, isRead BOOLEAN, isReceivedComunicated BOOLEAN, isReadComunicated BOOLEAN, isDownloaded BOOLEAN, replyTo TEXT, time INTEGER, clientTime INTEGER)";
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
        String query = "CREATE TABLE IF NOT EXISTS Chat (id  INTEGER PRIMARY KEY AUTOINCREMENT, uuid TEXT, chatName TEXT, chatDescription TEXT, lastRandom TEXT, lastMessage TEXT, lastUser TEXT, isGroup BOOLEAN, timestamp INTEGER, numNotRead INTEGER)";
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
        if(dbName == null || dbName.trim().equals("")) {
            LogUtils.printLog(tag, "DB is disconnected");
            if(cbc != null)
                cbc.error("DB is disconnected");
            return;
        }
        LogUtils.printLog(tag, "DBNAME " + dbName + " query: " + query);
        sqliteManager.executeSingle(dbName, query, params, cbc);
    }

    static void executeSingle(String query, JSONArray params, CallbackContext cbc) {
        if(dbName == null || dbName.trim().equals("")) {
            LogUtils.printLog(tag, "DB is disconnected");
            if(cbc != null)
                cbc.error("DB is disconnected");
            return;
        }
        LogUtils.printLog(tag, "DBNAME " + dbName + " query: " + query);
        sqliteManager.executeSingle(dbName, query, params, cbc);
    }

}
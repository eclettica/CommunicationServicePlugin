package it.linup.cordova.plugin.communication.services;

import io.sqlc.SQLiteAndroidDatabaseCallback;
import io.sqlc.SQLiteManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.StringBuilder;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import it.linup.cordova.plugin.utils.LogUtils;


import it.linup.cordova.plugin.communication.services.CommunicationServiceSqlUtil;



public class CommunicationMessageService {

    public static boolean messageTable = false;
    private static SQLiteManager sqliteManager = SQLiteManager.instance();

    public static String tag="COMMUNICATIONSERVICE - CommunicationServiceMessage";

    /**
     * TABELLA MESSAGGI:
     * id  INTEGER PRIMARY KEY AUTOINCREMENT,
     * randomId TEXT,
     * fromId TEXT,
     * fromName TEXT,
     * textMsg TEXT,
     * toId TEXT,
     * isGroup BOOLEAN,
     * isAttach BOOLEAN,
     * attachmentId TEXT,
     * attachmentType TEXT,
     * attachmentName TEXT,
     * isSent BOOLEAN,
     * isReceived BOOLEAN,
     * isRead BOOLEAN,
     * isReceivedComunicated BOOLEAN,
     * isReadComunicated BOOLEAN,
     * replyTo TEXT,
     * time INTEGER,
     * clientTime INTEGER
     *
     */

    public static void saveMessage(JSONObject jobj, SQLiteAndroidDatabaseCallback cbc) {
        if(jobj == null) {
            if(cbc != null)
                cbc.error("Oggetto null");
            return;
        }
        try {
            Map<String, Object> map = extractMessage(jobj);
            saveMessage(map, cbc);
        } catch(JSONException ex) {
            if(cbc != null)
                cbc.error(ex.getMessage());
        }
    }


        public static void saveMessage(Map<String, Object> map, SQLiteAndroidDatabaseCallback cbc) {
        if(map == null) {
            if(cbc != null)
                cbc.error("Oggetto null");
            return;
        }
        try {
            QueryObj qo = insertQuery("Message", map);
            CommunicationServiceSqlUtil.executeSingle(qo.query, qo.params, cbc);
        } catch(Exception ex) {
            if(cbc != null)
                cbc.error(ex.getMessage());
        }
    }

    public static Map<String, Object> extractMessage(JSONObject jobj) throws JSONException {
        if(jobj == null)
            return null;
        JSONObject data = jobj.optJSONObject("data");
        JSONObject result = null;
        if(data != null)
            result = data.getJSONObject("result");
        else
            result = jobj;
        JSONObject fromUser = result.getJSONObject("fromUser");
        JSONObject toUser = result.getJSONObject("toUser");

        String textMsg = result.getString("textMessage");
        String replyTo = result.optString("replyTo", "");
        String uuidfrom = fromUser.getString("uuid");
        String namefrom = fromUser.getString("completeName");
        String uuidto = toUser.getString("uuid");
        String nameto = toUser.getString("completeName");
        String random = result.getString("randomId");
        String user = fromUser.getString("name");
        Boolean isGroup = result.getBoolean("isGroup");
        String groupId = result.optString("groupId");
        Boolean isAttach = result.getBoolean("isAttach");
        String attachType = null;
        if (result.has("attachType"))
            attachType = result.getString("attachType");
        String attachmentId = result.getString("attachmentId");
        String attachmentName = result.getString("attachmentName");
        Long time = result.getLong("serverDateTimestamp");

        Map<String, Object> map = new HashMap<String, Object>();
        map.put("randomId", random);
        map.put("fromId", uuidfrom);
        map.put("fromName", namefrom);
        map.put("textMsg", textMsg);
        map.put("toId", uuidto);
        map.put("toName", nameto);
        map.put("isGroup", isGroup);
        map.put("groupId", groupId);
        map.put("isAttach", isAttach);
        if (isAttach) {
            map.put("attachmentId", attachmentId);
            map.put("attachmentType", attachType);
            map.put("attachmentName", attachmentName);
        }
        //map.("isSent" BOOLEAN,
        map.put("isReceived", true);
        //map.("isRead" BOOLEAN,
        //map.("isReceivedComunicated" BOOLEAN,
        //map.("isReadComunicated" BOOLEAN,
        map.put("replyTo", replyTo);
        map.put("time", time);
        map.put("clientTime", new Date().getTime());
        return map;
    }

    public static void saveMessageAndChat(JSONObject jobj, SQLiteAndroidDatabaseCallback cbc) {
        if(jobj == null) {
            if(cbc != null)
                cbc.error("Oggetto null");
            return;
        }
        try {
            Map<String, Object> m = extractMessage(jobj);

            saveMessage(m, new SQLiteAndroidDatabaseCallback() {

                public void error(String error) {
                    LogUtils.printLog(tag, "dbquery callback error " + error);
                    if (cbc != null)
                        cbc.error(error);
                }

                public void success(JSONArray arr) {
                    //cercare la conversazione relativa e, qualora non esiste, crearla
                    try {
                        String id = null;
                        if((Boolean)m.get("isGroup"))
                            id = (String)m.get("groupId");
                        else
                            id = (String)m.get("fromId");
                        findChat(id, (Boolean)m.get("isGroup"), new SQLiteAndroidDatabaseCallback() {
                            public void error(String error) {
                                LogUtils.printLog(tag, "dbquery callback error " + error);
                                if (cbc != null)
                                    cbc.error(error);
                            }
                            public void success(JSONArray arr) {
                                if(arr == null || arr.length() == 0) {
                                    addChat(m, cbc);
                                } else {
                                    //qui è necessario verificare se devo aggiornare la chat
                                }
                            }
                        });

                    } catch (Exception e) {
                        if (cbc != null)
                            cbc.error("Exception " + e.getMessage());
                    }
                }
            });
        } catch(Exception e) {
            if(cbc != null)
                cbc.error("Exception " + e.getMessage());
        }
    }

    public static void findChat(String id, boolean isGroup, SQLiteAndroidDatabaseCallback cbc) {
        //TODO gestire le chat di gruppo
        String query = null;
        if(isGroup)
            query = "SELECT * FROM Chat where groupId=?";
        else
            query = "SELECT * FROM Chat where fromId=?";
        JSONArray arr = new JSONArray();
        arr.put(id);
        CommunicationServiceSqlUtil.executeSingle(query, arr, new SQLiteAndroidDatabaseCallback() {

            public void error(String error) {
                LogUtils.printLog(tag, "search chat dbquery callback error " + error);
                if(cbc != null)
                    cbc.error(error);
            }

            public void success(JSONArray arr) {

                }
            });
    }


    public static void addChat(Map<String, Object> messageMap, SQLiteAndroidDatabaseCallback cbc) {
        Integer numNotRead = 1;
        Map<String, Object> contentValue = new HashMap<String, Object>();

        if((Boolean)messageMap.get("isGroup")) {
            contentValue.put("uuid", messageMap.get("groupId"));
            contentValue.put("isGroup", true);
            contentValue.put("lastuser", messageMap.get("fromName"));
        } else {
            contentValue.put("uuid", messageMap.get("fromId"));
            contentValue.put("isGroup", false);
        }
        contentValue.put("lastRandom", messageMap.get("randomId"));
        contentValue.put("lastMessage", messageMap.get("textMessage"));
        contentValue.put("timestamp", messageMap.get("time"));
        contentValue.put("numNotRead",numNotRead);
        saveChat(contentValue, cbc);
    }

    public static void saveChat(Map<String, Object> chatMap, SQLiteAndroidDatabaseCallback cbc) {
        try {
            QueryObj qo = insertQuery("Chat", chatMap);
            CommunicationServiceSqlUtil.executeSingle(qo.query, qo.params, cbc);
        } catch(Exception ex) {
            if(cbc != null)
                cbc.error(ex.getMessage());
        }
    }

    public static class QueryObj {
        public String query;
        public JSONArray params;
    }


    public static QueryObj insertQuery(String table, Map<String, Object> fields) {
        JSONArray arr = new JSONArray();
        StringBuilder x = new StringBuilder("");

        x.append("INSERT INTO ");
        x.append(table);

        StringBuilder columns = new StringBuilder("");
        StringBuilder values = new StringBuilder("");
        String sep = "";
        for(String columnKey : fields.keySet()) {
            columns.append(sep);
            columns.append(columnKey);
            values.append(sep);
            values.append("?");
            arr.put(fields.get(columnKey));
            sep = ", ";
        }

        x.append("(");
        x.append(columns);
        x.append(") VALUES(");
        x.append(values);
        x.append(");");

        QueryObj ret = new QueryObj();
        ret.params = arr;
        ret.query = x.toString();
        return ret;
    }


}
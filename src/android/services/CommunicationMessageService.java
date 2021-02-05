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


import it.linup.cordova.plugin.communication.services.CommunicationServiceSqlUtil;



public class CommunicationMessageService {

    public static boolean messageTable = false;
    private static SQLiteManager sqliteManager = SQLiteManager.instance();

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
        if(jobj == null)
            return;
        try {
            JSONObject data = jobj.getJSONObject("data");
            JSONObject result = data.getJSONObject("result");
            JSONObject fromUser = result.getJSONObject("fromUser");
            JSONObject toUser = result.getJSONObject("toUser");

            String textMsg = result.getString("textMessage");
            String replyTo = result.getString("replyTo");
            String uuidfrom = fromUser.getString("uuid");
            String namefrom = fromUser.getString("completename");
            String uuidto = toUser.getString("uuid");
            String nameto = toUser.getString("completename");
            String random = result.getString("randomId");
            String user = fromUser.getString("name");
            Boolean isGroup = result.getBoolean("isGroup");
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

            QueryObj qo = insertQuery("Message", map);
            CommunicationServiceSqlUtil.executeSingle(qo.query, qo.params, cbc);
        } catch(JSONException ex) {
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
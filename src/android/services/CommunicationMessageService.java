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
import java.util.List;
import java.util.LinkedList;

import it.linup.cordova.plugin.utils.LogUtils;


import it.linup.cordova.plugin.communication.services.CommunicationServiceSqlUtil;

import it.linup.cordova.plugin.communication.models.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.PluginResult;




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
            SqlMessageWrapper s = extractMessage(jobj);
            s.isReceived = true;
            Map<String, Object> map = convertMessageToMap(s);
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
            LogUtils.printLog(tag, "FASE2 save message");
            QueryObj qo = insertQuery("Message", map);
            CommunicationServiceSqlUtil.executeSingle(qo.query, qo.params, cbc);
        } catch(Exception ex) {
            if(cbc != null)
                cbc.error(ex.getMessage());
        }
    }

    public static SqlMessageWrapper extractMessage(JSONObject jobj) throws JSONException {
        if(jobj == null)
            return null;
        JSONObject data = jobj.optJSONObject("data");
        JSONObject result = null;
        if(data != null)
            result = data.getJSONObject("result");
        else
            result = jobj;
        Gson gson = new Gson();
        MessageWrapper mw = gson.fromJson(result.toString(), MessageWrapper.class);
        SqlMessageWrapper s = SqlMessageWrapper.buildFromRequest(mw);
        return s;

    }

    public static Map<String, Object> convertMessageToMap(SqlMessageWrapper s) {
        ObjectMapper oMapper = new ObjectMapper();
        Map<String, Object> map = oMapper.convertValue(s, Map.class);
        return map;
    }

    public static Map<String, Object> convertChatToMap(SqlChatWrapper s) {
        ObjectMapper oMapper = new ObjectMapper();
        Map<String, Object> map = oMapper.convertValue(s, Map.class);
        return map;
    }


    /**
     * Metodo che viene invocato alla ricezione di un messaggio
     * @param jobj - oggetto JSON che contiene il messaggio ricevuto
     * @param cbc - oggetto relativo alla callback
     */
    public static void saveMessageAndChat(JSONObject jobj, SQLiteAndroidDatabaseCallback cbc) {
        if(jobj == null) {
            if(cbc != null)
                cbc.error("Oggetto null");
            return;
        }
        try {
            LogUtils.printLog(tag, "FASE1 extract message");
            SqlMessageWrapper s = extractMessage(jobj);
            s.isReceived = true;
            s.isDownloaded = true;
            Map<String, Object> m = convertMessageToMap(s);

            saveMessage(m, new SQLiteAndroidDatabaseCallback() {

                public void error(String error) {
                    LogUtils.printLog(tag, "dbquery callback error " + error);
                    if (cbc != null)
                        cbc.error(error);
                }

                public void success(JSONArray arr) {
                    /*if(cbc != null) {
                        cbc.success(arr);
                        return;
                    }*/
                    //cercare la conversazione relativa e, qualora non esiste, crearla
                    LogUtils.printLog(tag, "FASE3 chat search");
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
                                LogUtils.printLog(tag, "FASE4 chat check " + arr);
                                JSONArray chatArr = null;
                                try {
                                    chatArr = extractResult(arr);
                                } catch(JSONException ex) {
                                    chatArr = null;
                                } catch(Exception e) {
                                    if(cbc != null)
                                        cbc.error(e.getMessage());
                                    return;
                                }
                                if(chatArr == null || chatArr.length() <= 0) {
                                    LogUtils.printLog(tag, "FASE5 need create chat ");
                                    addChat(m, cbc);
                                } else {
                                    //qui è necessario verificare se devo aggiornare la chat
                                    JSONObject chatObj = chatArr.optJSONObject(0);
                                    if(chatObj == null) {
                                        LogUtils.printLog(tag, "FASE5 need create chat ");
                                        addChat(m, cbc);
                                        return;
                                    }
                                    LogUtils.printLog(tag, "FASE6 update chat ");
                                    Gson gson = new Gson();
                                    SqlChatWrapper chat = gson.fromJson(chatObj.toString(), SqlChatWrapper.class);

                                    if(s.time > chat.timestamp) {
                                        //è necessario aggiornare

                                        chat.lastRandom = s.randomId;
                                        chat.lastMessage = s.textMsg;
                                        chat.lastUser = s.fromName;
                                        chat.timestamp = s.time;
                                        checkNotReadAndUpdate(chat, cbc);
                                    } else {
                                        if(cbc != null) {
                                            cbc.success(arr);
                                         }
                                    }

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

    /**
     * Metodo che viene invocato quando si sta cercando di inviare un messaggio
     * @param s - wrapper del messaggio
     * @param cbc - oggetto per la callback
     */
    public static void sendMessageAndChat(SqlMessageWrapper s, SQLiteAndroidDatabaseCallback cbc) {
        if(s == null) {
            if(cbc != null)
                cbc.error("Oggetto null");
            return;
        }
        try {

            Map<String, Object> m = convertMessageToMap(s);

            saveMessage(m, new SQLiteAndroidDatabaseCallback() {

                public void error(String error) {
                    LogUtils.printLog(tag, "dbquery callback error " + error);
                    if (cbc != null)
                        cbc.error(error);
                }

                public void success(JSONArray arr) {
                    /*if(cbc != null) {
                        cbc.success(arr);
                        return;
                    }*/
                    //cercare la conversazione relativa e, qualora non esiste, crearla
                    LogUtils.printLog(tag, "FASE3 chat search");
                    try {
                        String id = null;
                        if((Boolean)m.get("isGroup"))
                            id = (String)m.get("groupId");
                        else
                            id = (String)m.get("toId");
                            findChat(id, (Boolean)m.get("isGroup"), new SQLiteAndroidDatabaseCallback() {
                                public void error(String error) {
                                    LogUtils.printLog(tag, "dbquery callback error " + error);
                                    if (cbc != null)
                                        cbc.error(error);
                                }
                                public void success(JSONArray arr) {
                                    LogUtils.printLog(tag, "FASE4 chat check " + arr);
                                    JSONArray chatArr = null;
                                    try {
                                        chatArr = extractResult(arr);
                                    } catch(JSONException ex) {
                                        chatArr = null;
                                    } catch(Exception e) {
                                        if(cbc != null)
                                            cbc.error(e.getMessage());
                                        return;
                                    }
                                    if(chatArr == null || chatArr.length() <= 0) {
                                        LogUtils.printLog(tag, "FASE5 need create chat ");
                                        Map<String, Object> vals = messageToChat(m, true);

                                        saveChat(vals, cbc);
                                    } else {
                                        //qui è necessario verificare se devo aggiornare la chat
                                        JSONObject chatObj = chatArr.optJSONObject(0);
                                        if(chatObj == null) {
                                            LogUtils.printLog(tag, "FASE5 need create chat ");
                                            addChat(m, cbc);
                                            return;
                                        }
                                        LogUtils.printLog(tag, "FASE6 update chat ");
                                        Gson gson = new Gson();
                                        SqlChatWrapper chat = gson.fromJson(chatObj.toString(), SqlChatWrapper.class);
                                        if(s.time > chat.timestamp) {
                                            //è necessario aggiornare

                                            chat.lastRandom = s.randomId;
                                            chat.lastMessage = s.textMsg;
                                            chat.lastUser = s.fromName;
                                            chat.timestamp = s.time;
                                            checkNotReadAndUpdate(chat, cbc);
                                        } else {
                                            //TODO fare update della chat
                                            if(cbc != null) {
                                                cbc.success(arr);
                                            }
                                        }

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

    public static void findChatCountAndUpdate(String groupId, boolean isGroup, SQLiteAndroidDatabaseCallback cbc) {
        findChat(groupId, isGroup, new SQLiteAndroidDatabaseCallback() {
            public void error(String error) {
                LogUtils.printLog(tag, "dbquery callback error " + error);
                if (cbc != null)
                    cbc.error(error);
            }
            public void success(JSONArray arr) {
                JSONArray chatArr = null;
                try {
                    chatArr = extractResult(arr);
                } catch(JSONException ex) {
                    chatArr = null;
                } catch(Exception e) {
                    if(cbc != null)
                        cbc.error(e.getMessage());
                    return;
                }
                if(chatArr == null || chatArr.length() <= 0) {
                    LogUtils.printLog(tag, "chat not fuond");
                    if(cbc != null)
                        cbc.error("chat not fuond");
                } else {
                    //qui è necessario verificare se devo aggiornare la chat
                    JSONObject chatObj = chatArr.optJSONObject(0);
                    if(chatObj == null) {
                        LogUtils.printLog(tag, "chat not fuond");
                        if(cbc != null)
                            cbc.error("chat not fuond");
                    }
                    LogUtils.printLog(tag, "update chat ");
                    Gson gson = new Gson();
                    SqlChatWrapper chat = gson.fromJson(chatObj.toString(), SqlChatWrapper.class);
                    checkNotReadAndUpdate(chat, cbc);
                }
            }
        });
    }

    public static void findChat(String groupId, boolean isGroup, SQLiteAndroidDatabaseCallback cbc) {
        //TODO gestire le chat di gruppo
        String query = null;
        /*if(isGroup)
            query = "SELECT * FROM Chat where groupId=?";
        else
            query = "SELECT * FROM Chat where fromId=?";*/
        query = "SELECT * FROM Chat where uuid=?";
        JSONArray arr = new JSONArray();
        arr.put(groupId);
        CommunicationServiceSqlUtil.executeSingle(query, arr, new SQLiteAndroidDatabaseCallback() {

            public void error(String error) {
                LogUtils.printLog(tag, "search chat dbquery callback error " + error);
                if(cbc != null)
                    cbc.error(error);
            }

            public void success(JSONArray arr) {
                LogUtils.printLog(tag, "search chat dbquery callback " + arr);
                if(cbc != null)
                    cbc.success(arr);
                }
            });
    }

    public static void getAllChats( CallbackContext cbc) {
        getAllChats(null, null, cbc, null);
    }

    public static void getAllChats( SQLiteAndroidDatabaseCallback cbc) {
        getAllChats(null, null, null, cbc);
    }

    public static void getAllChats(Integer page, Integer limit, CallbackContext cbc, SQLiteAndroidDatabaseCallback dbc) {
        Map<String, String> sortMap = new HashMap<String, String>();
        sortMap.put("timestamp", "DESC");
        QueryObj qo = selectQuery("Chat", null,
                page, limit, sortMap);
        /*if(cbc != null) {
            CommunicationServiceSqlUtil.executeSingle(qo.query, qo.params, cbc);
        } else {*/
            CommunicationServiceSqlUtil.executeSingle(qo.query, qo.params, new SQLiteAndroidDatabaseCallback() {

                public void error(String error) {
                    LogUtils.printLog(tag, "search chats dbquery callback error " + error);
                    if(cbc != null) {
                        cbc.error(error);
                    } else if(dbc != null)
                        dbc.error(error);
                }

                public void success(JSONArray arr) {
                    LogUtils.printLog(tag, "search chats " + arr);
                    if(cbc != null) {
                        extractResult(arr, cbc);

                    } else if(dbc != null)
                        dbc.success(arr);
                }
            });
        //}
    }


    public static void getAllMessages( CallbackContext cbc) {
        getAllMessages(null, null, cbc, null);
    }

    public static void getAllMessages( SQLiteAndroidDatabaseCallback cbc) {
        getAllMessages(null, null, null, cbc);
    }

    public static void getAllMessages(Integer page, Integer limit, CallbackContext cbc, SQLiteAndroidDatabaseCallback dbc) {
        Map<String, String> sortMap = new HashMap<String, String>();
        sortMap.put("time", "DESC");
        QueryObj qo = selectQuery("Message", null,
                page, limit, sortMap);
        //if(cbc != null) {
        //    CommunicationServiceSqlUtil.executeSingle(qo.query, qo.params, cbc);
        //} else {
            CommunicationServiceSqlUtil.executeSingle(qo.query, qo.params, new SQLiteAndroidDatabaseCallback() {

                public void error(String error) {
                    LogUtils.printLog(tag, "search messages dbquery callback error " + error);
                    if(cbc != null) {
                        cbc.error(error);
                    } else if(dbc != null)
                        dbc.error(error);
                }

                public void success(JSONArray arr) {
                    LogUtils.printLog(tag, "search messages " + arr);
                    if(cbc != null) {
                        extractResult(arr, cbc);

                    } else if(dbc != null)
                        dbc.success(arr);
                }
            });
        //}
    }

    public static void getChatMessages(String uuid, Boolean isGroup, Integer page, Integer limit, List<QuerySelectObj> conds, CallbackContext cbc, SQLiteAndroidDatabaseCallback dbc) {
        LogUtils.printLog(tag, "getChatMessages " + uuid + " " + isGroup);
        Map<String, String> sortMap = new HashMap<String, String>();
        sortMap.put("time", "DESC");
        QueryGroupObj qg = new QueryGroupObj();
        qg.condition = "and";
        qg.fields = new LinkedList<QuerySelectObj>();
        qg.fields.add(new QuerySelectObj("isGroup", "=", isGroup));
        LogUtils.printLog(tag, "getChatMessages added condition isGroup");
        if(isGroup) {
            qg.groups = null;
            qg.fields.add(new QuerySelectObj("groupId", "=", uuid));
        } else {

            qg.groups = new LinkedList<QueryGroupObj>();
            QueryGroupObj qg1 = new QueryGroupObj();
            qg1.condition = "or";
            qg1.fields = new LinkedList<QuerySelectObj>();
            qg1.fields.add(new QuerySelectObj("fromId", "=", uuid));
            qg1.fields.add(new QuerySelectObj("toId", "=", uuid));
            qg.groups.add(qg1);
            LogUtils.printLog(tag, "getChatMessages added condition uuid");
        }
        if(conds != null) {
            qg.fields.addAll(conds);
        }
        if(qg.groups != null)
            LogUtils.printLog(tag, "getChatMessages generateQuery " + qg.groups.size());
        QueryObj qo = selectQuery("Message", qg,
                page, limit, sortMap);
        LogUtils.printLog(tag, "getChatMessages generatedQuery " + qo.query);

        //if(cbc != null) {
        //    CommunicationServiceSqlUtil.executeSingle(qo.query, qo.params, cbc);
        //} else {
        CommunicationServiceSqlUtil.executeSingle(qo.query, qo.params, new SQLiteAndroidDatabaseCallback() {

            public void error(String error) {
                LogUtils.printLog(tag, "search messages dbquery callback error " + error);
                if(cbc != null) {
                    cbc.error(error);
                } else if(dbc != null)
                    dbc.error(error);
            }

            public void success(JSONArray arr) {
                LogUtils.printLog(tag, "search messages " + arr);
                if(cbc != null) {
                    extractResult(arr, cbc);

                } else if(dbc != null)
                    dbc.success(arr);
            }
        });
        //}
    }

    public static void extractResult(JSONArray arr, CallbackContext cbc) {
        try {
            JSONObject resultObj = extractResultObj(arr);
            if(cbc != null) {
                if(arr != null) {
                    PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, resultObj);
                    cbc.sendPluginResult(pluginResult);
                } else {
                    cbc.error("no result");
                }
            }
        } catch(JSONException ex) {
            if(cbc != null)
                cbc.error(ex.getMessage());
        } catch(Exception e) {
            if(cbc != null) {
                PluginResult pluginResult = new PluginResult(PluginResult.Status.ERROR, arr);
                cbc.sendPluginResult(pluginResult);
            }
        }
    }

    public static JSONArray extractResult(JSONArray arr) throws JSONException, Exception {
        JSONObject obj = extractResultObj(arr);
        arr = obj.getJSONArray("rows");
        return arr;
    }

    public static JSONObject extractResultObj(JSONArray arr) throws JSONException, Exception {
        if(arr == null || arr.length() <= 0)
            return null;
        JSONObject obj = arr.getJSONObject(0);
        if(obj == null)
            return null;
        if(!"success".equals(obj.getString("type")))
            throw new Exception("result is in error");
        obj = obj.getJSONObject("result");
        return obj;
    }


    /*public static void getAllChats(Integer page, Integer limit, SQLiteAndroidDatabaseCallback cbc) {
        //TODO gestire le chat di gruppo
        String query = null;
        query = "SELECT * FROM Chat ORDER BY timestamp DESC";
        if(limit != null) {
            query += " LIMIT " + limit;
            if (page != null) {
                int offset = page * limit;
                query += " OFFSET " + offset;
            }
        }


        JSONArray arr = new JSONArray();
        CommunicationServiceSqlUtil.executeSingle(query, arr, new SQLiteAndroidDatabaseCallback() {

            public void error(String error) {
                LogUtils.printLog(tag, "search chats dbquery callback error " + error);
                if(cbc != null)
                    cbc.error(error);
            }

            public void success(JSONArray arr) {
                LogUtils.printLog(tag, "search chats " + arr);
                if(cbc != null)
                    cbc.success(arr);
            }
        });
    }*/

    public static void checkNotReadAndUpdate(SqlChatWrapper chat, SQLiteAndroidDatabaseCallback cbc) {
        QueryObj qo = new QueryObj();
        qo.query = "SELECT COUNT(*) as c from Message where ";
        if(chat.isGroup) {
            qo.query += "groupId='" + chat.uuid + "' and toId is not null and isRead='false'";
        } else {
            qo.query += "groupId='" + chat.uuid + "' and fromId='" + chat.uuid + "' and isRead='false'";
        }
        qo.params = new JSONArray();

        CommunicationServiceSqlUtil.executeSingle(qo.query, qo.params, new SQLiteAndroidDatabaseCallback() {

            public void error(String error) {
                LogUtils.printLog(tag, "AAAAAAAA check not read messages error " + error);
                if(cbc != null)
                    cbc.error(error);
            }

            public void success(JSONArray arr) {
                LogUtils.printLog(tag, "AAAAAAAA not read messages... " + arr);
                try {
                    arr = extractResult(arr);
                    if (arr == null || arr.length() <= 0) {
                        if (cbc != null)
                            cbc.error("result not found...");
                        return;
                    }
                    JSONObject obj = arr.getJSONObject(0);
                    if (obj == null) {
                        if (cbc != null)
                            cbc.error("result obj not found...");
                        return;
                    }
                    Integer count = obj.getInt("c");
                    chat.numNotRead = count;
                    updateChat(chat, cbc);
                } catch(Exception e) {
                    LogUtils.printLog(tag, "AAAAAAAA check not read messages error " + e.getMessage());
                    if(cbc != null)
                        cbc.error(e.getMessage());
                }
            }
        });

    }

    public static void updateChat(SqlChatWrapper chat, SQLiteAndroidDatabaseCallback cbc) {
        Map<String, Object> chatMap = convertChatToMap(chat);
        QueryGroupObj qg = new QueryGroupObj();
        qg.fields = new LinkedList<QuerySelectObj>();
        QuerySelectObj qso = new QuerySelectObj("id", "=", chat.id);
        qg.fields.add(qso);
        qg.groups = null;
        qg.condition = "and";
        QueryObj qo = updateQuery("Chat", chatMap, qg);
        LogUtils.printLog(tag, "updateChat -  " + chat.id + " " + qo.query);
        if(qo.params != null) {
            String params = "";
            for (int i = 0; i < qo.params.length(); i++) {
                try {
                    params += " " + qo.params.get(i).toString();
                } catch (JSONException e) {
                    continue;
                }
            }
            LogUtils.printLog(tag, "updateChat -  " + params);
        }
        CommunicationServiceSqlUtil.executeSingle(qo.query, qo.params, cbc);
        LogUtils.printLog(tag, "updateChat send to plugin");
        Gson gson = new Gson();
        CommunicationService.generateEvent(CommunicationService.NEWCHAT, gson.toJson(chatMap));
    }

    public static void addChat(Map<String, Object> messageMap, SQLiteAndroidDatabaseCallback cbc) {
        Map<String, Object> contentValue = messageToChat(messageMap);
        saveChat(contentValue, cbc);
    }

    public static Map<String, Object> messageToChat(Map<String, Object> messageMap) {
        return messageToChat(messageMap, false);
    }

    public static Map<String, Object> messageToChat(Map<String, Object> messageMap, boolean isSend) {
        Integer numNotRead = 1;
        Map<String, Object> contentValue = new HashMap<String, Object>();

        if((Boolean)messageMap.get("isGroup")) {
            contentValue.put("uuid", messageMap.get("groupId"));
            contentValue.put("isGroup", true);
            contentValue.put("lastuser", messageMap.get("fromName"));
            contentValue.put("chatName", messageMap.get("toName"));
        } else {
            if(isSend) {
                contentValue.put("chatName", messageMap.get("toName"));
                contentValue.put("uuid", messageMap.get("toId"));
            } else {
                contentValue.put("chatName", messageMap.get("fromName"));
                contentValue.put("uuid", messageMap.get("fromId"));
            }

            contentValue.put("isGroup", false);
            contentValue.put("lastuser", messageMap.get("fromName"));
        }
        contentValue.put("lastRandom", messageMap.get("randomId"));
        contentValue.put("lastMessage", messageMap.get("textMsg"));
        contentValue.put("timestamp", messageMap.get("time"));
        contentValue.put("numNotRead",numNotRead);
        return contentValue;
    }

    public static void saveChat(Map<String, Object> chatMap, SQLiteAndroidDatabaseCallback cbc) {
        try {
            LogUtils.printLog(tag, "FASE6 save chat ");
            QueryObj qo = insertQuery("Chat", chatMap);
            CommunicationServiceSqlUtil.executeSingle(qo.query, qo.params, cbc);
            Gson gson = new Gson();
            CommunicationService.generateEvent(CommunicationService.NEWCHAT, gson.toJson(chatMap));

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

    public static class QuerySelectObj {
        public String fieldKey;
        public String op;
        public Object fieldVal;

        public QuerySelectObj() {}

        public QuerySelectObj(String fieldKey, String op, Object fieldVal) {
            this.fieldVal = fieldVal;
            this.fieldKey = fieldKey;
            this.op = op;
        }
    }

    public static class QueryGroupObj {
        public String condition;
        public List<QuerySelectObj> fields;
        public List<QueryGroupObj> groups;
    }

    public static QueryObj buildGroupWhere(QueryGroupObj group) {
        String q = "";
        String sep = "";
        QueryObj ret = new QueryObj();
        if(group.fields != null && !group.fields.isEmpty()) {
            ret.params = new JSONArray();
            for(QuerySelectObj field : group.fields) {
                q += sep + field.fieldKey + " " + field.op + " ?";
                ret.params.put(field.fieldVal);
                sep = " " + group.condition + " ";
            }
        }
        if(group.groups != null && !group.groups.isEmpty()) {
            if(ret.params == null)
                ret.params = new JSONArray();
            for(QueryGroupObj subgroup : group.groups) {
                QueryObj sbo = buildGroupWhere(subgroup);
                if(sbo != null && sbo.query != null && !sbo.query.trim().equals("")) {
                    q += sep + "( " + sbo.query + " )";
                    if(sbo.params != null)
                        ret.params = putAll(ret.params, sbo.params);
                    sep = " " + group.condition + " ";
                }
            }
        }
        ret.query = q;
        return ret;
    }

    public static QueryObj selectQuery(String table, QueryGroupObj group,
                                       Integer page, Integer limit, Map<String, String> sortMap) {
        JSONArray arr = new JSONArray();
        StringBuilder x = new StringBuilder("");

        x.append("SELECT * FROM ");
        x.append(table);

        if(group != null) {
            QueryObj sbo = buildGroupWhere(group);
            if(sbo.query != null && !sbo.query.trim().equals("")) {
                x.append(" WHERE ");
                x.append(sbo.query);
                if(sbo.params != null)
                    arr = putAll(arr, sbo.params);
            }
        }

        if(sortMap != null && !sortMap.isEmpty()) {
            String sep = " ORDER BY ";
            for(String k : sortMap.keySet()) {
                x.append(sep);
                x.append(k);
                x.append(" ");
                x.append(sortMap.get(k));
                sep = ", ";
            }
        }


        if(limit != null) {
            x.append(" LIMIT " + limit);
            if (page != null) {
                int offset = page * limit;
                x.append(" OFFSET " + offset);
            }
        }


        QueryObj ret = new QueryObj();
        ret.params = arr;
        ret.query = x.toString();
        return ret;
    }

    public static QueryObj updateQuery(String table,
                                       Map<String, Object> fields,
                                       QueryGroupObj group) {
        JSONArray arr = new JSONArray();
        StringBuilder x = new StringBuilder("");
        LogUtils.printLog(tag, " updateQuery ");
        x.append("UPDATE ");
        x.append(table);
        x.append(" SET ");

        String sep = "";
        LogUtils.printLog(tag, " addFields ");
        for(String columnKey : fields.keySet()) {
            x.append(sep);
            x.append(columnKey);
            x.append(" = ?");
            arr.put(fields.get(columnKey));
            sep = ", ";
        }

        /*StringBuilder columns = new StringBuilder("");
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

        x.append(" (");
        x.append(columns);
        x.append(") VALUES(");
        x.append(values);
        x.append(")");*/
        LogUtils.printLog(tag, " addCondition ");
        if(group != null) {
            QueryObj sbo = buildGroupWhere(group);
            if(sbo.query != null && !sbo.query.trim().equals("")) {
                x.append(" WHERE ");
                x.append(sbo.query);
                if(sbo.params != null)
                    arr = putAll(arr, sbo.params);
            }
        }

        QueryObj ret = new QueryObj();
        ret.params = arr;
        ret.query = x.toString();
        LogUtils.printLog(tag, " query: " + ret.query);
        return ret;
    }



    public static JSONArray putAll(JSONArray container, JSONArray toAdd) {
        if(container == null)
            container = new JSONArray();
        if(toAdd == null)
            return container;
        for(int i = 0; i < toAdd.length(); i++) {
            try {
                container.put(toAdd.get(i));
            } catch(JSONException ex) {
                continue;
            }
        }
        return container;
    }

    public static void findMessage(Long id, SQLiteAndroidDatabaseCallback cbc) {
        //TODO gestire le chat di gruppo
        String query = null;
        /*if(isGroup)
            query = "SELECT * FROM Chat where groupId=?";
        else
            query = "SELECT * FROM Chat where fromId=?";*/
        QueryGroupObj group = new QueryGroupObj();
        group.condition = "and";
        group.fields = new LinkedList<QuerySelectObj>();
        QuerySelectObj qso = new QuerySelectObj("id", "=", id);
        QueryObj qo = selectQuery("Message", group, 0, null, null);

        CommunicationServiceSqlUtil.executeSingle(qo.query, qo.params, new SQLiteAndroidDatabaseCallback() {

            public void error(String error) {
                LogUtils.printLog(tag, "search message dbquery callback error " + error);
                if(cbc != null)
                    cbc.error(error);
            }

            public void success(JSONArray arr) {
                LogUtils.printLog(tag, "search message dbquery callback " + arr);
                if(cbc != null)
                    cbc.success(arr);
            }
        });
    }

    public static void updateMessage(Long id, Map<String, Object> fields, SQLiteAndroidDatabaseCallback cbc) {
        //TODO gestire le chat di gruppo
        String query = null;
        /*if(isGroup)
            query = "SELECT * FROM Chat where groupId=?";
        else
            query = "SELECT * FROM Chat where fromId=?";*/
        QueryGroupObj group = new QueryGroupObj();
        group.condition = "and";
        group.fields = new LinkedList<QuerySelectObj>();
        QuerySelectObj qso = new QuerySelectObj("id", "=", id);
        group.fields.add(qso);
        QueryObj qo = updateQuery("Message", fields, group);

        CommunicationServiceSqlUtil.executeSingle(qo.query, qo.params, new SQLiteAndroidDatabaseCallback() {

            public void error(String error) {
                LogUtils.printLog(tag, "search message dbquery callback error " + error);
                if(cbc != null)
                    cbc.error(error);
            }

            public void success(JSONArray arr) {
                LogUtils.printLog(tag, "search message dbquery callback " + arr);
                if(cbc != null)
                    cbc.success(arr);
            }
        });
    }

    public static void receiveSendMessage(JSONObject data) {
            receiveEventMessage(data, "isSent", "randomId", "uuid");

    }
    public static void receiveReceiveMessage(JSONObject data) {
        receiveEventMessage(data, "isReceived", "lastRandom", "userId");

    }
    public static void receiveReadMessage(JSONObject data) {
        receiveEventMessage(data, "isRead", "lastRandom", "userId");
        
    }

    public static void receiveEventMessage(JSONObject data, String eventField, String randomField, String userField) {
        try {
            LogUtils.printLog(tag, "receiveEventMessage " + eventField + " " + randomField + " " + data.toString());
            String randomId = "" + data.getLong(randomField);
            String uuid = data.getString(userField);
            Boolean isGroup = data.optBoolean("isGroup");
            if(isGroup == null)
                isGroup = false;
            searchForMessage(uuid, randomId, isGroup, new SQLiteAndroidDatabaseCallback() {

                public void error(String error) {
                    LogUtils.printLog(tag, "receiveEventMessage - search messages dbquery callback error " + error);

                }

                public void success(JSONArray arr) {
                    try {
                        if (arr == null || arr.length() <= 0)
                            return;
                        JSONObject obj = arr.getJSONObject(0);

                        Long id = obj.getLong("id");

                        Map<String, Object> fields = new HashMap<String, Object>();
                        obj.put(eventField, "true");
                        fields.put(eventField, true);
                        if(eventField.equals("isRead")) {
                            obj.put("isReceived", "true");
                            fields.put("isReceived", true);
                        }

                        updateMessage(id, fields, new SQLiteAndroidDatabaseCallback() {

                            public void error(String error) {
                                LogUtils.printLog(tag, "receiveEventMessage - search messages dbquery callback error " + error);
                            }

                            public void success(JSONArray arr) {
                                Gson gson = new Gson();
                                String evt = CommunicationService.SENDMESSAGE;
                                if(eventField.equals("isRead")) {
                                    evt = CommunicationService.READMESSAGE;
                                } else if(eventField.equals("isReceived")) {
                                    evt = CommunicationService.RECEIVEMESSAGE;
                                }
                                CommunicationService.generateEvent(evt, gson.toJson(obj));
                            }
                        });
                    } catch(JSONException jex) {
                        LogUtils.printLog(tag, "exception " + jex.getMessage());
                    }
                }
            });

        } catch(JSONException jex) {
            LogUtils.printLog(tag, "exception " + jex.getMessage());
        }
    }

    public static void searchForMessage(String uuid, String randomId, Boolean isGroup, SQLiteAndroidDatabaseCallback dbc) {
        LogUtils.printLog(tag, "searchForMessage " + uuid + " " + randomId + " " + isGroup);
        List<QuerySelectObj> conds = new LinkedList<QuerySelectObj>();
        conds.add(new QuerySelectObj("randomId", "=",randomId));
        getChatMessages(uuid, isGroup, 0, 1, conds, null, new SQLiteAndroidDatabaseCallback() {

            public void error(String error) {
                LogUtils.printLog(tag, "searchForMessage - search messages dbquery callback error " + error);
                if(dbc != null)
                    dbc.error(error);
            }

            public void success(JSONArray arr) {
                try {
                    arr = extractResult(arr);
                    if(dbc != null)
                        dbc.success(arr);
                } catch(Exception e) {
                    LogUtils.printLog(tag, "exception " + e.getMessage());
                }
            }
        });
    }

}
package it.linup.cordova.plugin.communication.models;

import java.util.Date;
import java.util.HashMap;
import it.linup.cordova.plugin.communication.models.MessageWrapper;
import it.linup.cordova.plugin.communication.services.CommunicationService.SendMessageRequest;

public class SqlMessageWrapper {

    public Long id;
    public String randomId;
    public String fromId;
    public String fromName;
    public String textMsg;
    public String toId;
    public String toName;
    public boolean isGroup = false;
    public String groupId;
    public boolean isAttach = false;
    public String attachmentType = null;
    public String attachmentName;
    public String attachmentId;
    public boolean isSent = false;
    public boolean isReceived = false;
    public boolean isRead = false;
    public boolean isReceivedComunicated = false;
    public boolean isReadComunicated = false;
    public boolean isDownloaded = false;
    public String replyTo;
    public Long time;
    public Long clientTime;
    public String localPath;

    /*public boolean isAttachment = false;
    public String tempId = null;*/

    public SqlMessageWrapper() {

    }

    public static SqlMessageWrapper buildFromRequest(MessageWrapper mw) {
        SqlMessageWrapper ret = new SqlMessageWrapper();
        ret.attachmentId = mw.attachmentId;
        ret.attachmentName = mw.attachmentName;
        ret.attachmentType = mw.attachType;
        ret.clientTime = new Date().getTime();
        ret.fromId = mw.fromUser != null ? mw.fromUser.get("uuid") : null;
        ret.fromName = mw.fromUser != null ? mw.fromUser.get("completeName") : null;
        ret.groupId = mw.groupId;
        ret.isAttach = mw.isAttach;
        //ret.isAttachment = mw.isAttach;
        //ret.tempId = mw.attachmentId;
        ret.isGroup = mw.isGroup;
        if(ret.isGroup == false)
            ret.groupId = ret.fromId;
        ret.randomId = ""+mw.randomId;
        ret.replyTo = mw.replyTo;
        ret.textMsg = mw.textMessage;
        ret.time = mw.serverDateTimestamp;
        ret.toId = mw.toUser != null ? mw.toUser.get("uuid") : null;
        ret.toName = mw.isGroup ? mw.groupName : mw.toUser != null ? mw.toUser.get("completeName") : null;
        ret.isDownloaded = mw.isDownloaded;
        return ret;
    }

    public static SqlMessageWrapper buildFromRequest(SendMessageRequest mw) {
        SqlMessageWrapper ret = new SqlMessageWrapper();
        ret.fromId = mw.fromId;
        ret.fromName = mw.fromName;
        ret.isGroup = mw.isGroup;
        if(ret.isGroup) {
            ret.groupId = mw.uuid;
        } else {
            ret.toId = mw.uuid;
            ret.groupId = mw.uuid;
        }
        ret.toName = mw.toName;
        ret.textMsg = mw.message;
        ret.randomId = ""+mw.randomId;
        ret.time = mw.timestamp;

        ret.attachmentId = mw.attachmentId;
        ret.attachmentName = mw.attachmentName;
        ret.attachmentType = mw.attachmentType;
        ret.isAttach = mw.isAttach;
        ret.localPath = mw.localPath;

        //ret.isAttachment = mw.isAttach;
        //ret.tempId = mw.attachmentId;

        ret.isDownloaded = true;
        return ret;
    }

}
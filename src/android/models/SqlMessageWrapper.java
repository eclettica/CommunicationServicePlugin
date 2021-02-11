package it.linup.cordova.plugin.communication.models;

import java.util.Date;
import java.util.HashMap;
import it.linup.cordova.plugin.communication.models.MessageWrapper;

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
    public String replyTo;
    public Long time;
    public Long clientTime;

    public SqlMessageWrapper() {

    }

    public SqlMessageWrapper(MessageWrapper mw) {
        this.attachmentId = mw.attachmentId;
        this.attachmentName = mw.attachmentName;
        this.attachmentType = mw.attachType;
        this.clientTime = new Date().getTime();
        this.fromId = mw.fromUser != null ? mw.fromUser.get("uuid") : null;
        this.fromName = mw.fromUser != null ? mw.fromUser.get("completeName") : null;
        this.groupId = mw.groupId;
        this.isAttach = mw.isAttach;
        this.isGroup = mw.isGroup;
        this.randomId = ""+mw.randomId;
        this.replyTo = mw.replyTo;
        this.textMsg = mw.textMessage;
        this.time = mw.serverDateTimestamp;
        this.toId = mw.toUser != null ? mw.toUser.get("uuid") : null;
        this.toName = mw.isGroup ? mw.groupName : mw.toUser != null ? mw.toUser.get("completeName") : null;
    }

}
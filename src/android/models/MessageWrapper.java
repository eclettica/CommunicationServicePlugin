package it.linup.cordova.plugin.communication.models;

import java.util.Date;
import java.util.HashMap;

public class MessageWrapper {

    public Date clientDate;
    public Long randomId;
    public Date serverDate;
    public Long serverDateTimestamp;
    public String textMessage;
    public String replyTo;
    public boolean isRead;
    public boolean isSent;
    public HashMap<String, String> fromUser;
    public HashMap<String, String> toUser;
    public String uuid;
    public boolean isAttach = false;
    public String attachType = null;
    public boolean isGroup = false;
    public String groupId;
    public String attachmentName;
    public String attachmentId;

}
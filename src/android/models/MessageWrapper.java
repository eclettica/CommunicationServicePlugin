package it.linup.cordova.plugin.communication.models;

import java.util.Date;
import java.util.HashMap;

public class MessageWrapper {

    public String clientDate;
    public Long randomId;
    public String serverDate;
    public Long serverDateTimestamp;
    public String textMessage;
    public String replyTo;
    public boolean isRead;
    public boolean isSent;
    public boolean isDownloaded;
    public HashMap<String, String> fromUser;
    public HashMap<String, String> toUser;
    public String uuid;
    public boolean isAttach = false;
    public String attachType = null;
    public boolean isGroup = false;
    public String groupId;
    public String groupName;
    public String attachmentName;
    public String attachmentId;
    public String localPath;
}
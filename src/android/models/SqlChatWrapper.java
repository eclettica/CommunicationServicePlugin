package it.linup.cordova.plugin.communication.models;

import java.util.Date;
import java.util.HashMap;
import it.linup.cordova.plugin.communication.models.MessageWrapper;

public class SqlChatWrapper {


    public Long id;
    public String uuid;
    public String lastRandom;
    public String lastMessage;
    public String lastUser;
    public boolean isGroup = false;
    public Long timestamp;
    public Integer numNotRead;


    public SqlChatWrapper() {

    }


}
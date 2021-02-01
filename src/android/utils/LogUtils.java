package it.linup.cordova.plugin.utils;

import android.os.Environment;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Date;
import java.text.SimpleDateFormat;


/**
 * Created by linhtom on 7/21/17.
 */

public class LogUtils {

    public static boolean isPrintLog = true;

    private static File log = new File(
            Environment.getExternalStorageDirectory() + "/" + "idra" + new Date().toLocaleString() + ".log");
    private static OutputStreamWriter out;



    static {
        configureLog();
    }

    private static void configureLog() {
        Date date = new Date();
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd");
        String strDate= formatter.format(date);
        log = new File(
                Environment.getExternalStorageDirectory() + "/" + "idra-" + strDate + ".log");
        try {
            out = new OutputStreamWriter(new FileOutputStream(log, true));
        } catch (Exception e) {
        }
    }

    public static void writeError(Throwable e) {
        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));
        String exceptionAsString = sw.toString();

        String outPut = "[Exception]\nDate Time: %s\nStackTrace: %s\n-------------\n\n";
        String formated = String.format(outPut, new Date().toLocaleString(), exceptionAsString);

        try {
            if (out == null) {
                configureLog();
            }
            appendLog(formated);
        } catch (Exception e1) {
            configureLog();
            try {
                appendLog(formated);
            } catch (Exception e2) {
                // Do
            }
        }
    }

    public static void writeError(String info, String error) {
        String outPut = "[Info]\nDate Time: %s\n%s\n-------------\n\n";
        String formated = String.format(outPut, new Date().toLocaleString(), info);
        String formated1 = null;
        if (error != null && !error.trim().equals("")) {
            formated1 = String.format(outPut, new Date().toLocaleString(), error);
        }
        try {
            if (out == null) {
                configureLog();
            }
            appendLog(formated);
            if (formated1 != null)
                appendLog(formated1);
        } catch (Exception e1) {
            configureLog();
            try {
                appendLog(formated);
                if (formated1 != null)
                    appendLog(formated1);
            } catch (Exception e) {
                // Donothing
            }
        }
    }

    public static void writeInfo(String info) {

        String outPut = "[Info]\nDate Time: %s\n%s\n-------------\n\n";
        String formated = String.format(outPut, new Date().toLocaleString(), info);
        try {
            if (out == null) {
                configureLog();
            }
            appendLog(formated);
        } catch (Exception e1) {
            configureLog();
            try {
                appendLog(formated);
            } catch (Exception e) {
                // Donothing
            }
        }
    }

    public static void appendLog(String info) throws Exception {
        out.write(info);
        out.flush();
    }

    public static void printLog(String tag,String msg){
        if(isPrintLog){
            System.out.println(tag + "");
            System.out.println("******************************* "+tag+" ************************************");
            System.out.println(tag + " " + msg);
            System.out.println("******************************* "+tag+" ************************************");
            System.out.println(tag + "");
        }
    }

}
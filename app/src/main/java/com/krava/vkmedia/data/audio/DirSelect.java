package com.krava.vkmedia.data.audio;

import android.os.Environment;
import android.preference.PreferenceManager;
import android.widget.Toast;

import com.krava.vkmedia.presentation.VKApplication;

import java.io.File;

/**
 * Created by krava2008 on 30.06.16.
 */

public class DirSelect {
    public static String ext;
    private static String parsedEXT;

    static {
        ext = String.valueOf(System.getenv("SECONDARY_STORAGE"));
    }

    public static String getParsedEXT() {
        parsedEXT = ext;
        if (parsedEXT.contains("null")) {
            parsedEXT = "null";
        } else if (parsedEXT.contains(":")) {
            for (String s : ext.split(":")) {
                if (!(s.contains("usb") || s.contains("Usb") || s.contains("USB"))) {
                    parsedEXT = s;
                }
            }
        }
        return parsedEXT + "/";
    }

    public static File getCustomState(int param) {
        File file;
        if (PreferenceManager.getDefaultSharedPreferences(VKApplication.context).getBoolean("customDirCh", false)) {
            file = new File(getParsedEXT() + PreferenceManager.getDefaultSharedPreferences(VKApplication.context).getString("cacheDirCoffeeNew", "VKCoffee"));
            File err;
            if (!new File(getParsedEXT()).canRead()) {
                Toast.makeText(VKApplication.context, "\u0412\u043e\u0437\u043d\u0438\u043a\u043b\u0430 \u043e\u0448\u0438\u0431\u043a\u0430, \u043d\u0430\u043a\u043e\u043f\u0438\u0442\u0435\u043b\u044c \u043d\u0435\u0434\u043e\u0441\u0442\u0443\u043f\u0435\u043d", Toast.LENGTH_SHORT).show();
                err = new File(String.valueOf(Environment.getExternalStorageDirectory().getAbsolutePath()) + "/" + PreferenceManager.getDefaultSharedPreferences(VKApplication.context).getString("cacheDirCoffeeNew", "VKCoffee"));
                if (err.exists()) {
                    return err;
                }
                err.setWritable(true);
                err.mkdirs();
                return err;
            } else if (getParsedEXT().contains("null")) {
                Toast.makeText(VKApplication.context, "\u0412\u043e\u0437\u043d\u0438\u043a\u043b\u0430 \u043e\u0448\u0438\u0431\u043a\u0430 null", Toast.LENGTH_SHORT).show();
                err = new File(String.valueOf(Environment.getExternalStorageDirectory().getAbsolutePath()) + "/" + PreferenceManager.getDefaultSharedPreferences(VKApplication.context).getString("cacheDirCoffeeNew", "VKCoffee"));
                if (err.exists()) {
                    return err;
                }
                err.setWritable(true);
                err.mkdirs();
                return err;
            } else if (getParsedEXT().contains("usb") && getParsedEXT().contains("Usb")) {
                Toast.makeText(VKApplication.context, "\u0412\u043e\u0437\u043d\u0438\u043a\u043b\u0430 \u043e\u0448\u0438\u0431\u043a\u0430 usb contains", Toast.LENGTH_SHORT).show();
                err = new File(String.valueOf(Environment.getExternalStorageDirectory().getAbsolutePath()) + "/" + PreferenceManager.getDefaultSharedPreferences(VKApplication.context).getString("cacheDirMediaNew", "VKMedia"));
                if (err.exists()) {
                    return err;
                }
                err.setWritable(true);
                err.mkdirs();
                return err;
            } else {
                if (!file.exists()) {
                    file.setWritable(true);
                    file.mkdirs();
                }
                return file;
            }
        }
        file = new File(String.valueOf(Environment.getExternalStorageDirectory().getAbsolutePath()) + "/" + PreferenceManager.getDefaultSharedPreferences(VKApplication.context).getString("cacheDirMediaNew", "VKMedia"));
        if (!file.exists()) {
            file.setWritable(true);
            file.mkdirs();
        }
        return file;
    }
}

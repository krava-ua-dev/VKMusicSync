package com.krava.vkmedia.application.utils;

import android.content.res.Resources;

import com.krava.vkmedia.R;
import com.krava.vkmedia.application.VKApplication;

import java.util.Calendar;

/**
 * Created by krava2008 on 15.05.16.
 */
public class TimeUtils {
    public static final long DAY = 86400000;
    public static final long HOUR = 3600000;
    public static final long MIN = 60000;
    public static final long MONTH = 2592000000L;
    public static final long SEC = 1000;
    public static final long WEEK = 604800000;
    public static final long YEAR = 31536000000L;
    public static final String DEFAULT_STRING_VALUE = "";

    public static Calendar getCalendar() {
        return Calendar.getInstance();
    }

    public static String langDate(long _dt, boolean forceShortMonth) {
        Resources res = VKApplication.context.getResources();
        long dt =  _dt; //* SEC;
        Calendar now = getCalendar();
        Calendar ds = getCalendar();
        ds.set(Calendar.MINUTE, 0);
        ds.set(Calendar.HOUR_OF_DAY, 0);
        ds.set(Calendar.SECOND, 0);
        ds.set(Calendar.MILLISECOND, 0);
        long daystart = ds.getTimeInMillis();
        Calendar c = getCalendar();
        c.setTimeInMillis(dt);
        String r = DEFAULT_STRING_VALUE;
        String str;
        Object[] objArr;
        if (daystart < dt && DAY + daystart >= dt) {
            int i;
            str = "%s %s %d:%02d";
            objArr = new Object[4];
            objArr[0] = res.getString(R.string.today);
            if (c.get(Calendar.HOUR_OF_DAY) == 1) {
                i = R.string.date_at_1am;
            } else {
                i = R.string.date_at;
            }
            objArr[1] = res.getString(i);
            objArr[2] = c.get(Calendar.HOUR_OF_DAY);
            objArr[3] = c.get(Calendar.MINUTE);
            return String.format(str, objArr);
        } else if (DAY + daystart < dt && 172800000 + daystart > dt) {
            str = "%s %s %d:%02d";
            objArr = new Object[4];
            objArr[0] = res.getString(R.string.tomorrow);
            objArr[1] = res.getString(c.get(Calendar.HOUR_OF_DAY) == 1 ? R.string.date_at_1am : R.string.date_at);
            objArr[2] = c.get(Calendar.HOUR_OF_DAY);
            objArr[3] = c.get(Calendar.MINUTE);
            return String.format(str, objArr);
        } else if (daystart - DAY >= dt || daystart < dt) {
            StringBuilder append;
            Object[] objArr2;
            if (c.get(Calendar.YEAR) != now.get(Calendar.YEAR)) {
                r = r + res.getString(R.string.date_format_day_month_year, new Object[]{Integer.valueOf(c.get(Calendar.DATE)), res.getStringArray(R.array.months_short)[Math.min(c.get(Calendar.MONTH), 11)], Integer.valueOf(c.get(Calendar.YEAR))});
            } else {
                append = new StringBuilder().append(r);
                objArr2 = new Object[2];
                objArr2[0] = c.get(Calendar.DATE);
                objArr2[1] = res.getStringArray(forceShortMonth ? R.array.months_short : R.array.months_full)[Math.min(c.get(Calendar.MONTH), 11)];
                r = append.append(res.getString(R.string.date_format_day_month, objArr2)).toString();
            }
            append = new StringBuilder().append(r);
            String str2 = " %s %d:%02d";
            objArr2 = new Object[3];
            objArr2[0] = res.getString(c.get(Calendar.HOUR_OF_DAY) == 1 ? R.string.date_at_1am : R.string.date_at);
            objArr2[1] = c.get(Calendar.HOUR_OF_DAY);
            objArr2[2] = c.get(Calendar.MINUTE);
            return append.append(String.format(str2, objArr2)).toString();
        } else {
            str = "%s %s %d:%02d";
            objArr = new Object[4];
            objArr[0] = res.getString(R.string.yesterday);
            objArr[1] = res.getString(c.get(Calendar.HOUR_OF_DAY) == 1 ? R.string.date_at_1am : R.string.date_at);
            objArr[2] = c.get(Calendar.HOUR_OF_DAY);
            objArr[3] = c.get(Calendar.MINUTE);
            return String.format(str, objArr);
        }
    }

    public static String langDate(long _dt) {
        return langDate(_dt, false);
    }
}

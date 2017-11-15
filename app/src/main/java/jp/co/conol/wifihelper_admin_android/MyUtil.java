package jp.co.conol.wifihelper_admin_android;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import java.util.Calendar;
import java.util.Date;

/**
 * Created by Masafumi_Ito on 2017/09/28.
 */

@SuppressWarnings("ALL")
public class MyUtil {

    public static class Network {

        /**
         * ネットワークに接続されているかをチェック
         * @param context
         * @return 接続：true / 未接続：false
         */
        public static boolean isConnected(Context context) {
            ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo info = cm.getActiveNetworkInfo();

            return info != null && info.isConnected();
        }
    }

    public static class Transform {

        /**
         * Date型 -> Calendar型
         * @param date
         * @return calendar
         */
        public static Calendar dateToCalender(Date date){
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(date);
            return calendar;
        }

        /**
         * Calendar型 -> Date型
         * @param calendar
         * @return date
         */
        public static Date calendarTodate(Calendar calendar){
            Date date = calendar.getTime();
            return date;
        }
    }
}

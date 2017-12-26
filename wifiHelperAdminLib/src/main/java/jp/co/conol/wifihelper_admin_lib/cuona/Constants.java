package jp.co.conol.wifihelper_admin_lib.cuona;

/**
 * Created by m_ito on 2017/12/13.
 */

// package-private
class Constants {
    static class Urls {
        static final String SERVICE_KEY = "H7Pa7pQaVxxG";   // サービスキー
        static final String CDMS_URL = "http://manage-dev.cuona.io";   // デバイスマネージャーURL（開発）
//        static final String CDMS_URL = "https://manage.cuona.io";   // デバイスマネージャーURL（本番）
//        static final String LOG_URL = ""; // ログを送らない
        static final String LOG_URL = CDMS_URL + "/api/device_logs/" + SERVICE_KEY + ".json"; // ログ送信先
    }
}

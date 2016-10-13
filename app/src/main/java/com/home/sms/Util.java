package com.home.sms;

/**
 * Created by Home on 10/12/2016.
 */

public class Util {

    public static final StringBuilder sb = new StringBuilder();

    public static String concat(Object... objects) {
        sb.setLength(0);
        for (Object obj : objects) {
            sb.append(obj);
        }
        return sb.toString();
    }
}

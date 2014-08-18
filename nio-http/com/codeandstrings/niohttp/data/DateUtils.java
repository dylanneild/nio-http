package com.codeandstrings.niohttp.data;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class DateUtils {

    public static SimpleDateFormat RFC_822_DATE_FORMAT
            = new SimpleDateFormat("EEE', 'dd' 'MMM' 'yyyy' 'HH:mm:ss' 'Z", Locale.US);

    public static SimpleDateFormat RFC_822_ZONE_LESS_FORMAT
            = new SimpleDateFormat("EEE', 'dd' 'MMM' 'yyyy' 'HH:mm:ss", Locale.US);

    public static final String getRfc822DateString(Date date) {
        return DateUtils.RFC_822_DATE_FORMAT.format(date);
    }

    public static final String getRfc822DateStringGMT(Date date) {

        String pattern = RFC_822_ZONE_LESS_FORMAT.toPattern();
        SimpleDateFormat format = new SimpleDateFormat(pattern, Locale.US);
        StringBuilder r = new StringBuilder();

        format.setTimeZone(TimeZone.getTimeZone("GMT"));

        r.append(format.format(date));
        r.append(" GMT");

        return r.toString();

    }

    public static final Date parseRfc822DateString(String date) {

        try {
            return RFC_822_DATE_FORMAT.parse(date);
        } catch (ParseException e) {
            e.printStackTrace();
            return null;
        }

    }

}

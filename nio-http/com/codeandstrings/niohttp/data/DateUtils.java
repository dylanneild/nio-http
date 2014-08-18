package com.codeandstrings.niohttp.data;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DateUtils {

    public static SimpleDateFormat RFC_822_DATE_FORMAT
            = new SimpleDateFormat("EEE', 'dd' 'MMM' 'yyyy' 'HH:mm:ss' 'Z", Locale.US);


    public static final String getRfc822DateString(Date date) {
        return DateUtils.RFC_822_DATE_FORMAT.format(date);
    }

}

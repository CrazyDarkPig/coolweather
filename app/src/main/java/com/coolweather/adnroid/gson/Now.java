package com.coolweather.adnroid.gson;

import com.google.gson.annotations.SerializedName;

/**
 * Created by dark on 2017/5/5.
 */

public class Now {

    @SerializedName("tmp")
    public  String temperature;

    @SerializedName("cond")
    public  More more;

    public  class More{
        @SerializedName("txt")
        public  String info;
    }
}

package com.xyoye.dandanplay.utils.net;

import android.text.TextUtils;

import com.xyoye.dandanplay.utils.net.gson.GsonFactory;
import com.xyoye.dandanplay.utils.net.okhttp.OkHttpEngine;
import com.xyoye.dandanplay.utils.net.service.PFBRetrofitService;

import java.util.concurrent.TimeUnit;

import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Created by xyoye on 2020/5/29.
 */

public class PFBRetroFactory {
    private final static String baseUrl = "http://127.0.0:8080/";
    private PFBRetrofitService retrofitService;

    private String pcIp;
    private String connectCode;

    private PFBRetroFactory() {
        retrofitService = new Retrofit.Builder()
                .baseUrl(baseUrl)
                .addConverterFactory(GsonConverterFactory.create(GsonFactory.buildGson()))
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .client(initOkHttp())
                .build()
                .create(PFBRetrofitService.class);
    }

    private static class Holder {
        static PFBRetroFactory factory = new PFBRetroFactory();
    }

    public static PFBRetrofitService getInstance() {
        return Holder.factory.retrofitService;
    }

    public static void setPcIp(String pcIp) {
        Holder.factory.pcIp = pcIp;
    }

    public static void setConnectCode(String connectCode) {
        Holder.factory.connectCode = connectCode;
    }

    private OkHttpClient initOkHttp() {
        return OkHttpEngine.getInstance()
                .getOkHttpClient()
                .newBuilder()
                .connectTimeout(5000, TimeUnit.MILLISECONDS)
                .readTimeout(5000, TimeUnit.MILLISECONDS)
                .addInterceptor(new GzipInterceptor())
                .addInterceptor(chain -> {
                    Request oldRequest = chain.request();
                    Request.Builder newRequest = oldRequest.newBuilder();

                    if (TextUtils.isEmpty(pcIp) || TextUtils.isEmpty(connectCode)) {
                        throw new NullPointerException("未初始化PC连接信息");
                    }

                    newRequest.header("Authorization", "Basic " + connectCode);
                    //构建新的链接
                    String baseUrl = "http://" + pcIp + ":8080/";
                    HttpUrl newBaseUrl = HttpUrl.parse(baseUrl);

                    if (newBaseUrl != null) {
                        HttpUrl newUrl = oldRequest.url()
                                .newBuilder()
                                .scheme(newBaseUrl.scheme())
                                .host(newBaseUrl.host())
                                .port(newBaseUrl.port())
                                .build();
                        return chain.proceed(newRequest.url(newUrl).build());
                    }
                    return chain.proceed(newRequest.build());
                })
                .addInterceptor(new HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY))
                .build();

    }
}

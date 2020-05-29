package com.xyoye.dandanplay.utils.net.service;

import com.xyoye.dandanplay.bean.PFBFileBean;
import com.xyoye.dandanplay.bean.PFBStatusBean;

import java.util.List;

import io.reactivex.Observable;
import okhttp3.Response;
import retrofit2.http.GET;
import retrofit2.http.Query;

/**
 * Created by xyoye on 2020/5/29.
 */

public interface PFBRetrofitService {

    @GET("/PCFileBrowser/api/file/root")
    Observable<PFBStatusBean<List<PFBFileBean>>> getRootFiles();

    @GET("/PCFileBrowser/api/file/directory")
    Observable<PFBStatusBean<List<PFBFileBean>>> openDirectory(@Query("path") String path);

    @GET("/PCFileBrowser/api/file/download")
    Observable<Response> downloadFile(@Query("path") String path);
}

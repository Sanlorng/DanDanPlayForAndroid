package com.xyoye.dandanplay.mvp.impl;

import android.os.Bundle;

import com.blankj.utilcode.util.FileUtils;
import com.blankj.utilcode.util.LogUtils;
import com.blankj.utilcode.util.ToastUtils;
import com.xyoye.dandanplay.app.IApplication;
import com.xyoye.dandanplay.base.BaseMvpPresenterImpl;
import com.xyoye.dandanplay.bean.AnimeTypeBean;
import com.xyoye.dandanplay.bean.SubGroupBean;
import com.xyoye.dandanplay.mvp.presenter.MainPresenter;
import com.xyoye.dandanplay.mvp.view.MainView;
import com.xyoye.dandanplay.utils.AppConfig;
import com.xyoye.dandanplay.utils.CloudFilterHandler;
import com.xyoye.dandanplay.utils.Constants;
import com.xyoye.dandanplay.utils.Lifeful;
import com.xyoye.dandanplay.utils.TrackerManager;
import com.xyoye.dandanplay.utils.database.DataBaseManager;
import com.xyoye.dandanplay.utils.net.CommOtherDataObserver;
import com.xyoye.dandanplay.utils.net.NetworkConsumer;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

/**
 * Created by xyoye on 2018/6/28 0028.
 */

public class MainPresenterImpl extends BaseMvpPresenterImpl<MainView> implements MainPresenter {

    public MainPresenterImpl(MainView view, Lifeful lifeful) {
        super(view, lifeful);
    }

    @Override
    public void init() {

    }

    @Override
    public void process(Bundle savedInstanceState) {
        initAnimeType();
        initSubGroup();
        long lastUpdateTime = AppConfig.getInstance().getUpdateFilterTime();
        long nowTime = System.currentTimeMillis();
        //七天更新一次云过滤列表
        if (nowTime - lastUpdateTime > 7 * 24 * 60 * 60 * 1000) {
            AppConfig.getInstance().setUpdateFilterTime(nowTime);
            initCloudFilter();
        } else {
            getCloudFilter();
        }
    }

    @Override
    public void resume() {

    }

    @Override
    public void pause() {

    }

    @Override
    public void destroy() {

    }

    @Override
    public void initScanFolder() {
        DataBaseManager.getInstance()
                .selectTable("scan_folder")
                .query()
                .execute(cursor -> {
                    if (!cursor.moveToNext()) {
                        //增加默认扫描文件夹
                        DataBaseManager.getInstance()
                                .selectTable("scan_folder")
                                .insert()
                                .param("folder_path", Constants.DefaultConfig.SYSTEM_VIDEO_PATH)
                                .param("folder_type", Constants.ScanType.SCAN)
                                .execute();
                    }
                });
    }

    //Tracker
    @Override
    public void initTracker() {
        IApplication.getExecutor().execute(() -> {
            //trackers数据
            File configFolder = new File(FileUtils.getDirName(Constants.DefaultConfig.configPath));
            if (configFolder.isFile())
                configFolder.delete();
            if (!configFolder.exists())
                configFolder.mkdirs();

            File trackerFile = new File(Constants.DefaultConfig.configPath);

            //文件不存在，读取asset中默认的trackers，并写入文件
            if (!trackerFile.exists()) {
                TrackerManager.resetTracker();
            }
            //文件存在，直接读取
            else {
                TrackerManager.queryTracker();
            }
        });
    }

    //番剧分类
    private void initAnimeType() {
        AnimeTypeBean.getAnimeType(new CommOtherDataObserver<AnimeTypeBean>(getLifeful()) {
            @Override
            public void onSuccess(AnimeTypeBean animeTypeBean) {
                if (animeTypeBean != null && animeTypeBean.getTypes() != null && animeTypeBean.getTypes().size() > 0) {
                    DataBaseManager.getInstance()
                            .selectTable("anime_type")
                            .delete()
                            .execute();
                    DataBaseManager.getInstance()
                            .selectTable("anime_type")
                            .insert()
                            .param("type_id", -1)
                            .param("type_name", "全部")
                            .execute();
                    for (AnimeTypeBean.TypesBean typesBean : animeTypeBean.getTypes()) {
                        DataBaseManager.getInstance()
                                .selectTable("anime_type")
                                .insert()
                                .param("type_id", typesBean.getId())
                                .param("type_name", typesBean.getName())
                                .execute();
                    }
                }
            }

            @Override
            public void onError(int errorCode, String message) {
                LogUtils.e(message);
                ToastUtils.showShort(message);
            }
        }, new NetworkConsumer());
    }

    //字幕组
    private void initSubGroup() {
        SubGroupBean.getSubGroup(new CommOtherDataObserver<SubGroupBean>(getLifeful()) {
            @Override
            public void onSuccess(SubGroupBean subGroupBean) {
                if (subGroupBean != null && subGroupBean.getSubgroups() != null && subGroupBean.getSubgroups().size() > 0) {

                    DataBaseManager.getInstance()
                            .selectTable("subgroup")
                            .delete()
                            .execute();

                    DataBaseManager.getInstance()
                            .selectTable("subgroup")
                            .insert()
                            .param("subgroup_id", -1)
                            .param("subgroup_name", "全部")
                            .execute();

                    for (SubGroupBean.SubgroupsBean subgroupsBean : subGroupBean.getSubgroups()) {
                        DataBaseManager.getInstance()
                                .selectTable("subgroup")
                                .insert()
                                .param("subgroup_id", subgroupsBean.getId())
                                .param("subgroup_name", subgroupsBean.getName())
                                .execute();
                    }
                }
            }

            @Override
            public void onError(int errorCode, String message) {
                LogUtils.e(message);
                ToastUtils.showShort(message);
            }
        }, new NetworkConsumer());
    }

    //弹幕云过滤
    private void initCloudFilter() {
        IApplication.getExecutor().execute(() -> {
            List<String> filters = getFilterString();
            IApplication.cloudFilterList.addAll(filters);

            DataBaseManager.getInstance()
                    .selectTable("cloud_filter")
                    .delete()
                    .execute();
            for (int i = 0; i < filters.size(); i++) {
                DataBaseManager.getInstance()
                        .selectTable("cloud_filter")
                        .insert()
                        .param("filter", filters.get(i))
                        .execute();
            }
        });
    }

    //获取保存的云过滤数据
    private void getCloudFilter() {
        //云屏蔽数据
        DataBaseManager.getInstance()
                .selectTable("cloud_filter")
                .query()
                .queryColumns("filter")
                .execute(cursor -> {
                    while (cursor.moveToNext()) {
                        String text = cursor.getString(0);
                        IApplication.cloudFilterList.add(text);
                    }
                });
    }

    /**
     * 下载xml
     */
    private List<String> getFilterString() {
        InputStream is = null;
        List<String> filter = new ArrayList<>();
        try {
            String xmlUrl = "https://api.acplay.net/config/filter.xml";
            URL url = new URL(xmlUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(10000);
            conn.setRequestMethod("GET");
            if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                is = conn.getInputStream();
                SAXParser saxParser = SAXParserFactory.newInstance().newSAXParser();
                saxParser.parse(is, new CloudFilterHandler(filter::addAll));
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (is != null)
                    is.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
        return filter;
    }
}

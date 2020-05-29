package com.xyoye.dandanplay.mvp.impl;

import android.os.Bundle;

import com.xyoye.dandanplay.base.BaseMvpPresenterImpl;
import com.xyoye.dandanplay.bean.PFBFileBean;
import com.xyoye.dandanplay.bean.PFBStatusBean;
import com.xyoye.dandanplay.mvp.presenter.PFBPresenter;
import com.xyoye.dandanplay.mvp.view.PFBView;
import com.xyoye.dandanplay.utils.Lifeful;
import com.xyoye.dandanplay.utils.net.CommOtherDataObserver;
import com.xyoye.dandanplay.utils.net.NetworkConsumer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Created by xyoye on 2020/5/29.
 */

public class PFBPresenterImpl extends BaseMvpPresenterImpl<PFBView> implements PFBPresenter {

    public PFBPresenterImpl(PFBView view, Lifeful lifeful) {
        super(view, lifeful);
    }

    @Override
    public void init() {

    }

    @Override
    public void process(Bundle savedInstanceState) {

    }

    @Override
    public void resume() {

    }

    @Override
    public void pause() {

    }

    @Override
    public void getRootFiles() {
        getView().showLoading();
        PFBFileBean.getRootFiles(new CommOtherDataObserver<PFBStatusBean<List<PFBFileBean>>>() {
            @Override
            public void onSuccess(PFBStatusBean<List<PFBFileBean>> resultBean) {
                getView().hideLoading();

                if (resultBean.isSuccess()){
                    List<PFBFileBean> fileList = resultBean.getData();
                    if (fileList == null || fileList.size() == 0){
                        getView().showError("空文件夹");
                    } else {
                        Collections.sort(fileList, (o1, o2) -> {
                            if (o1.isDirectory() && !o2.isDirectory())
                                return 1;
                            else if (!o1.isDirectory() && o2.isDirectory()){
                                return -1;
                            } else {
                                return o1.getFileName().compareTo(o2.getFileName());
                            }
                        });
                        getView().updateRootFiles(fileList);
                    }
                } else {
                    getView().showError(resultBean.getResultCode()+": "+resultBean.getMessage());
                }
            }

            @Override
            public void onError(int errorCode, String message) {
                getView().hideLoading();
                getView().showError(errorCode + ": " + message);
            }
        }, new NetworkConsumer());
    }

    @Override
    public void openDirectory(String path) {
        getView().showLoading();
        PFBFileBean.openDirectory(path, new CommOtherDataObserver<PFBStatusBean<List<PFBFileBean>>>() {
            @Override
            public void onSuccess(PFBStatusBean<List<PFBFileBean>> resultBean) {
                getView().hideLoading();

                if (resultBean.isSuccess()){
                    List<PFBFileBean> fileList = resultBean.getData();
                    if (fileList == null || fileList.size() == 0){
                        getView().showError("空文件夹");
                    } else {
                        Collections.sort(fileList, (o1, o2) -> {
                            if (o1.isDirectory() && !o2.isDirectory())
                                return -1;
                            else if (!o1.isDirectory() && o2.isDirectory()){
                                return 1;
                            } else {
                                return o1.getFileName().compareTo(o2.getFileName());
                            }
                        });
                        getView().updateDirectoryFiles(path, fileList);
                    }
                } else {
                    getView().showError(resultBean.getResultCode()+": "+resultBean.getMessage());
                }
            }

            @Override
            public void onError(int errorCode, String message) {
                getView().hideLoading();
                getView().showError(errorCode + ": " + message);
            }
        }, new NetworkConsumer());
    }
}

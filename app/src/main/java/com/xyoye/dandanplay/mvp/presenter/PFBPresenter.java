package com.xyoye.dandanplay.mvp.presenter;

import com.xyoye.dandanplay.utils.interf.presenter.BaseMvpPresenter;

/**
 * Created by xyoye on 2020/5/29.
 */

public interface PFBPresenter extends BaseMvpPresenter {
    void getRootFiles();

    void openDirectory(String path);
}

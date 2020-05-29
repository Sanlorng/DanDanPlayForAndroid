package com.xyoye.dandanplay.mvp.view;

import com.xyoye.dandanplay.bean.PFBFileBean;
import com.xyoye.dandanplay.utils.interf.view.BaseMvpView;
import com.xyoye.dandanplay.utils.interf.view.LoadDataView;

import java.util.List;

/**
 * Created by xyoye on 2020/5/29.
 */

public interface PFBView extends BaseMvpView, LoadDataView {
    void updateRootFiles(List<PFBFileBean> rootFiles);

    void updateDirectoryFiles(String pcPath, List<PFBFileBean> rootFiles);
}

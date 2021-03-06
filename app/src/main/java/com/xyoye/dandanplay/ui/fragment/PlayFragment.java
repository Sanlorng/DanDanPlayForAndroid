package com.xyoye.dandanplay.ui.fragment;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.blankj.utilcode.util.StringUtils;
import com.blankj.utilcode.util.ToastUtils;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.tbruyelle.rxpermissions2.RxPermissions;
import com.xyoye.dandanplay.R;
import com.xyoye.dandanplay.base.BaseMvpFragment;
import com.xyoye.dandanplay.base.BaseRvAdapter;
import com.xyoye.dandanplay.bean.FolderBean;
import com.xyoye.dandanplay.bean.event.OpenFolderEvent;
import com.xyoye.dandanplay.mvp.impl.PlayFragmentPresenterImpl;
import com.xyoye.dandanplay.mvp.presenter.PlayFragmentPresenter;
import com.xyoye.dandanplay.mvp.view.PlayFragmentView;
import com.xyoye.dandanplay.ui.activities.play.FolderActivity;
import com.xyoye.dandanplay.ui.weight.dialog.CommonDialog;
import com.xyoye.dandanplay.ui.weight.item.FolderItem;
import com.xyoye.dandanplay.utils.AppConfig;
import com.xyoye.dandanplay.utils.CommonUtils;
import com.xyoye.dandanplay.utils.FileNameComparator;
import com.xyoye.dandanplay.utils.interf.AdapterItem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import butterknife.BindView;
import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;

/**
 * Created by xyoye on 2018/6/29.
 */

public class PlayFragment extends BaseMvpFragment<PlayFragmentPresenter> implements PlayFragmentView {
    public static final int UPDATE_ADAPTER_DATA = 0;
    public static final int UPDATE_DATABASE_DATA = 1;
    public static final int UPDATE_SYSTEM_DATA = 2;

    @BindView(R.id.refresh_layout)
    SwipeRefreshLayout refresh;
    @BindView(R.id.rv)
    RecyclerView recyclerView;
    @BindView(R.id.fast_play_bt)
    FloatingActionButton fastPlayBt;

    private BaseRvAdapter<FolderBean> adapter;
    private Disposable permissionDis;
    private boolean updateVideoFlag = false;

    public static PlayFragment newInstance() {
        return new PlayFragment();
    }

    @NonNull
    @Override
    protected PlayFragmentPresenter initPresenter() {
        return new PlayFragmentPresenterImpl(this, this);
    }

    @Override
    protected int initPageLayoutId() {
        return R.layout.fragment_play;
    }

    @SuppressLint("CheckResult")
    @Override
    public void initView() {
        refresh.setColorSchemeResources(R.color.theme_color);

        FolderItem.PlayFolderListener itemListener = new FolderItem.PlayFolderListener() {
            @Override
            public void onClick(String folderPath) {
                Intent intent = new Intent(getContext(), FolderActivity.class);
                intent.putExtra(OpenFolderEvent.FOLDERPATH, folderPath);
                startActivity(intent);
            }

            @Override
            public void onDelete(String folderPath, String title) {
                new CommonDialog.Builder(getContext())
                        .setOkListener(dialog -> presenter.deleteFolderVideo(folderPath))
                        .setAutoDismiss()
                        .build()
                        .show("确认删除文件夹 [" + title + "] 内视频文件？");
            }

            @Override
            public void onShield(String folderPath, String title) {
                new CommonDialog.Builder(getContext())
                        .setOkListener(dialog -> {
                            presenter.filterFolder(folderPath);
                            refresh.setRefreshing(true);
                            refreshVideo(false);
                        })
                        .setAutoDismiss()
                        .build()
                        .show("确认屏蔽文件夹 [" + title + "]及其子文件夹？");
            }
        };

        adapter = new BaseRvAdapter<FolderBean>(new ArrayList<>()) {
            @NonNull
            @Override
            public AdapterItem<FolderBean> onCreateItem(int viewType) {
                return new FolderItem(itemListener);
            }
        };

        recyclerView.setLayoutManager(new LinearLayoutManager(this.getContext(), LinearLayoutManager.VERTICAL, false));
        recyclerView.setNestedScrollingEnabled(false);
        recyclerView.setItemViewCacheSize(10);
        recyclerView.setAdapter(adapter);

        fastPlayBt.setOnClickListener(v -> {
            String videoPath = AppConfig.getInstance().getLastPlayVideo();
            if (!StringUtils.isEmpty(videoPath)) {
                presenter.playLastVideo(getContext(), videoPath);
            }
        });

        if (updateVideoFlag) {
            refresh.setRefreshing(true);
            initVideoData();
        }
    }

    @Override
    public void initListener() {
        refresh.setOnRefreshListener(() -> refreshVideo(true));
    }

    @Override
    public void refreshAdapter(List<FolderBean> beans) {
        Collections.sort(beans, new FileNameComparator<FolderBean>() {
            @Override
            public String getCompareValue(FolderBean folderBean) {
                return CommonUtils.getFolderName(folderBean.getFolderPath());
            }
        });
        adapter.setData(beans);
        if (refresh != null)
            refresh.setRefreshing(false);
    }

    @Override
    public void deleteFolderSuccess() {
        refresh.setRefreshing(true);
        refreshVideo(false);
    }

    @Override
    public void showLoading() {

    }

    @Override
    public void hideLoading() {

    }

    @Override
    public void showError(String message) {
        ToastUtils.showShort(message);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (permissionDis != null)
            permissionDis.dispose();
    }

    public void refreshFolderData(int updateType) {
        if (updateType == UPDATE_ADAPTER_DATA) {
            adapter.notifyDataSetChanged();
        } else {
            //通知系统刷新目录
            Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
            intent.setData(Uri.fromFile(Environment.getExternalStorageDirectory()));
            if (getContext() != null)
                getContext().sendBroadcast(intent);
            presenter.refreshVideo(getContext(), updateType == UPDATE_SYSTEM_DATA);
        }
    }

    public void initVideoData() {
        if (presenter == null || refresh == null) {
            updateVideoFlag = true;
        } else {
            refresh.setRefreshing(true);
            presenter.refreshVideo(getContext(), true);
            updateVideoFlag = false;
        }
    }

    /**
     * 刷新文件列表
     *
     * @param reScan 是否重新扫描文件目录
     */
    @SuppressLint("CheckResult")
    private void refreshVideo(boolean reScan) {
        new RxPermissions(this).
                request(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                .subscribe(new Observer<Boolean>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        permissionDis = d;
                    }

                    @Override
                    public void onNext(Boolean granted) {
                        if (granted) {
                            presenter.refreshVideo(getContext(), reScan);
                        } else {
                            ToastUtils.showLong("未授予文件管理权限，无法扫描视频");
                            refresh.setRefreshing(false);
                        }
                    }

                    @Override
                    public void onError(Throwable e) {
                        e.printStackTrace();
                    }

                    @Override
                    public void onComplete() {

                    }
                });
    }
}

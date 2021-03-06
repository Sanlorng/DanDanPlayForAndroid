package com.xyoye.dandanplay.ui.activities.play;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.blankj.utilcode.util.FileUtils;
import com.blankj.utilcode.util.StringUtils;
import com.blankj.utilcode.util.ToastUtils;
import com.xyoye.dandanplay.R;
import com.xyoye.dandanplay.base.BaseMvpActivity;
import com.xyoye.dandanplay.base.BaseRvAdapter;
import com.xyoye.dandanplay.bean.BindResourceBean;
import com.xyoye.dandanplay.bean.DanmuMatchBean;
import com.xyoye.dandanplay.bean.VideoBean;
import com.xyoye.dandanplay.bean.event.OpenFolderEvent;
import com.xyoye.dandanplay.bean.event.SaveCurrentEvent;
import com.xyoye.dandanplay.bean.event.UpdateFolderDanmuEvent;
import com.xyoye.dandanplay.bean.event.UpdateFragmentEvent;
import com.xyoye.dandanplay.bean.params.BindResourceParam;
import com.xyoye.dandanplay.mvp.impl.FolderPresenterImpl;
import com.xyoye.dandanplay.mvp.presenter.FolderPresenter;
import com.xyoye.dandanplay.mvp.view.FolderView;
import com.xyoye.dandanplay.ui.activities.ShellActivity;
import com.xyoye.dandanplay.ui.activities.setting.PlayerSettingActivity;
import com.xyoye.dandanplay.ui.fragment.PlayFragment;
import com.xyoye.dandanplay.ui.fragment.settings.PlaySettingFragment;
import com.xyoye.dandanplay.ui.weight.dialog.CommonDialog;
import com.xyoye.dandanplay.ui.weight.dialog.DanmuDownloadDialog;
import com.xyoye.dandanplay.ui.weight.item.VideoItem;
import com.xyoye.dandanplay.utils.AppConfig;
import com.xyoye.dandanplay.utils.Constants;
import com.xyoye.dandanplay.utils.FileNameComparator;
import com.xyoye.dandanplay.utils.interf.AdapterItem;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import butterknife.BindView;

/**
 * Created by xyoye on 2018/6/30.
 */


public class FolderActivity extends BaseMvpActivity<FolderPresenter> implements FolderView {
    @BindView(R.id.rv)
    RecyclerView recyclerView;

    public final static int SELECT_NETWORK_DANMU = 104;
    public final static int SELECT_NETWORK_ZIMU = 105;

    private BaseRvAdapter<VideoBean> adapter;
    private List<VideoBean> videoList;
    private VideoBean selectVideoBean;
    private int selectPosition;
    private String folderPath;

    @Override
    public void initView() {
        EventBus.getDefault().register(this);
        videoList = new ArrayList<>();
        folderPath = getIntent().getStringExtra(OpenFolderEvent.FOLDERPATH);
        String folderTitle = FileUtils.getFileNameNoExtension(folderPath.substring(0, folderPath.length() - 1));
        setTitle(folderTitle);

        adapter = new BaseRvAdapter<VideoBean>(videoList) {
            @NonNull
            @Override
            public AdapterItem<VideoBean> onCreateItem(int viewType) {
                return new VideoItem(new VideoItem.VideoItemEventListener() {

                    @Override
                    public void onBindDanmu(int position) {
                        if (position >= videoList.size()) return;
                        selectVideoBean = videoList.get(position);
                        String videoPath = videoList.get(position).getVideoPath();
                        BindResourceParam param = new BindResourceParam(videoPath, position);
                        param.setCurrentResourcePath(selectVideoBean.getDanmuPath());
                        Intent intent = new Intent(FolderActivity.this, BindDanmuActivity.class);
                        intent.putExtra("bind_param", param);
                        startActivityForResult(intent, SELECT_NETWORK_DANMU);
                    }

                    @Override
                    public void onBindZimu(int position) {
                        if (position >= videoList.size()) return;
                        selectVideoBean = videoList.get(position);
                        String videoPath = videoList.get(position).getVideoPath();
                        BindResourceParam param = new BindResourceParam(videoPath, position);
                        param.setCurrentResourcePath(selectVideoBean.getZimuPath());
                        Intent intent = new Intent(FolderActivity.this, BindZimuActivity.class);
                        intent.putExtra("bind_param", param);
                        startActivityForResult(intent, SELECT_NETWORK_ZIMU);
                    }

                    @Override
                    public void onRemoveDanmu(int position) {
                        if (position >= videoList.size()) return;
                        VideoBean videoBean = videoList.get(position);
                        videoBean.setEpisodeId(-1);
                        videoBean.setDanmuPath("");
                        adapter.notifyItemChanged(position);
                        String folderPath = FileUtils.getDirName(videoBean.getVideoPath());
                        presenter.updateDanmu("", -1, new String[]{folderPath, videoBean.getVideoPath()});
                    }

                    @Override
                    public void onRemoveZimu(int position) {
                        if (position >= videoList.size()) return;
                        VideoBean videoBean = videoList.get(position);
                        videoBean.setZimuPath("");
                        adapter.notifyItemChanged(position);
                        String folderPath = FileUtils.getDirName(videoBean.getVideoPath());
                        presenter.updateZimu("", new String[]{folderPath, videoBean.getVideoPath()});
                    }

                    @Override
                    public void onVideoDelete(int position) {
                        if (position >= videoList.size()) return;
                        new CommonDialog.Builder(FolderActivity.this)
                                .setOkListener(dialog -> {
                                    String path = videoList.get(position).getVideoPath();
                                    if (FileUtils.delete(path)) {
                                        adapter.removeItem(position);
                                        EventBus.getDefault().post(UpdateFragmentEvent.updatePlay(PlayFragment.UPDATE_DATABASE_DATA));
                                    } else {
                                        ToastUtils.showShort("删除文件失败");
                                    }
                                })
                                .setAutoDismiss()
                                .build()
                                .show("确认删除文件？");
                    }

                    @Override
                    public void onOpenVideo(int position) {
                        if (position >= videoList.size()) return;
                        selectVideoBean = videoList.get(position);
                        selectPosition = position;
                        //未设置弹幕情况下，1、开启自动加载时自动加载，2、自动匹配相同目录下同名弹幕，3、匹配默认下载目录下同名弹幕
                        if (StringUtils.isEmpty(selectVideoBean.getDanmuPath())) {
                            String path = selectVideoBean.getVideoPath();
                            if (AppConfig.getInstance().isAutoLoadDanmu()) {
                                if (!StringUtils.isEmpty(path)) {
                                    presenter.getDanmu(path);
                                }
                            } else {
                                noMatchDanmu(path);
                            }
                        } else {
                            openIntentVideo(selectVideoBean);
                        }
                    }
                });
            }
        };

        recyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        recyclerView.setNestedScrollingEnabled(false);
        recyclerView.setItemViewCacheSize(10);
        recyclerView.setAdapter(adapter);

        presenter.getVideoList(folderPath);
    }

    @Override
    public void initListener() {
    }

    @Override
    public void refreshAdapter(List<VideoBean> beans) {
        videoList.clear();
        videoList.addAll(beans);
        sort(AppConfig.getInstance().getFolderSortType());
        adapter.notifyDataSetChanged();
    }

    @NonNull
    @Override
    protected FolderPresenter initPresenter() {
        return new FolderPresenterImpl(this, this);
    }

    @Override
    protected int initPageLayoutID() {
        return R.layout.activity_folder;
    }

    @Override
    public void showLoading() {
        showLoadingDialog("正在搜索网络弹幕");
    }

    @Override
    public void hideLoading() {
        dismissLoadingDialog();
    }

    @Override
    public void showError(String message) {
        ToastUtils.showShort(message);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.item_video_edit) {
            if (videoList != null && videoList.size() > 0) {
                showVideoEditDialog();
            } else {
                ToastUtils.showShort("视频列表为空");
            }
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_folder, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void saveCurrent(SaveCurrentEvent event) {
        adapter.getData().get(selectPosition).setCurrentPosition(event.getCurrentPosition());
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void saveCurrent(UpdateFolderDanmuEvent event) {
        if (!TextUtils.isEmpty(folderPath))
            presenter.getVideoList(folderPath);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            BindResourceBean bindResourceBean = data.getParcelableExtra("bind_data");
            if (bindResourceBean == null)
                return;

            int position = bindResourceBean.getItemPosition();
            if (position < 0 || position > videoList.size() || videoList.size() == 0)
                return;
            String videoPath = videoList.get(position).getVideoPath();

            if (requestCode == SELECT_NETWORK_DANMU) {
                String danmuPath = bindResourceBean.getDanmuPath();
                int episodeId = bindResourceBean.getEpisodeId();
                presenter.updateDanmu(danmuPath, episodeId, new String[]{folderPath, videoPath});

                videoList.get(position).setDanmuPath(danmuPath);
                videoList.get(position).setEpisodeId(episodeId);
                adapter.notifyItemChanged(position);
                showError("弹幕绑定完成");
            } else if (requestCode == SELECT_NETWORK_ZIMU) {
                String zimuPath = bindResourceBean.getZimuPath();
                presenter.updateZimu(zimuPath, new String[]{folderPath, videoPath});

                videoList.get(position).setZimuPath(zimuPath);
                adapter.notifyItemChanged(position);
                showError("字幕绑定完成");
            }
        }
    }

    @SuppressLint("CheckResult")
    @Override
    public void downloadDanmu(DanmuMatchBean.MatchesBean matchesBean) {
        new DanmuDownloadDialog(this, selectVideoBean.getVideoPath(), matchesBean, (danmuPath, episodeId) -> {
            selectVideoBean.setDanmuPath(danmuPath);
            selectVideoBean.setEpisodeId(episodeId);

            String folderPath = FileUtils.getDirName(selectVideoBean.getVideoPath());
            presenter.updateDanmu(danmuPath, episodeId, new String[]{folderPath, selectVideoBean.getVideoPath()});
            adapter.notifyItemChanged(selectPosition);

            openIntentVideo(selectVideoBean);
        }).show();
    }

    @Override
    public void noMatchDanmu(String videoPath) {
        if (videoPath.contains(".")){
            String danmuPath = videoPath.substring(0, videoPath.lastIndexOf(".")) + ".xml";
            File file = new File(danmuPath);
            if (file.exists()) {
                selectVideoBean.setDanmuPath(danmuPath);
                ToastUtils.showShort("匹配到相同目录下同名弹幕");
            } else {
                String name = FileUtils.getFileNameNoExtension(videoPath) + ".xml";
                danmuPath = AppConfig.getInstance().getDownloadFolder() + "/" + name;
                file = new File(danmuPath);
                if (file.exists()) {
                    selectVideoBean.setDanmuPath(danmuPath);
                    ToastUtils.showShort("匹配到下载目录下同名弹幕");
                }
            }
        }
        openIntentVideo(selectVideoBean);
    }

    @Override
    public void openIntentVideo(VideoBean videoBean) {
        boolean isNotExoPlayer = AppConfig.getInstance().getPlayerType() != com.xyoye.player.commom.utils.Constants.EXO_PLAYER;
        boolean isMkvVideo = FileUtils.getFileExtension(videoBean.getVideoPath()).toLowerCase().equals("mkv");
        boolean isNeverShowTips = AppConfig.getInstance().isShowMkvTips();
        if (isNotExoPlayer && isMkvVideo && isNeverShowTips) {
            new CommonDialog.Builder(this)
                    .setAutoDismiss()
                    .setOkListener(dialog -> launchPlay(videoBean))
                    .setCancelListener(dialog -> {
                        Bundle player = new Bundle();
                        player.putString("fragment", PlaySettingFragment.class.getName());
                        launchActivity(ShellActivity.class,player);
                    })
                    .setDismissListener(dialog -> AppConfig.getInstance().hideMkvTips())
                    .build()
                    .show(getResources().getString(R.string.mkv_tips), "关于MKV格式", "我知道了", "前往设置");
        } else {
            launchPlay(videoBean);
        }
    }

    /**
     * 启动播放器
     *
     * @param videoBean 数据
     */
    private void launchPlay(VideoBean videoBean) {
        //记录此次播放
        AppConfig.getInstance().setLastPlayVideo(videoBean.getVideoPath());
        EventBus.getDefault().post(UpdateFragmentEvent.updatePlay(PlayFragment.UPDATE_ADAPTER_DATA));

        PlayerManagerActivity.launchPlayerLocal(
                this,
                FileUtils.getFileNameNoExtension(videoBean.getVideoPath()),
                videoBean.getVideoPath(),
                videoBean.getDanmuPath(),
                videoBean.getZimuPath(),
                videoBean.getCurrentPosition(),
                videoBean.getEpisodeId());
    }

    private void showVideoEditDialog() {
        final String[] playTypes = {"弹幕管理", "字幕管理", "视频排序"};

        new AlertDialog.Builder(this)
                .setTitle("全局编辑")
                .setItems(playTypes, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            showDanmuEditDialog();
                            break;
                        case 1:
                            showSubtitleEditDialog();
                            break;
                        case 2:
                            showVideoSortDialog();
                            break;
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void showDanmuEditDialog() {
        final String[] playTypes = {"绑定 所有视频网络弹幕", "解绑 所有视频弹幕"};

        new AlertDialog.Builder(this)
                .setTitle("弹幕管理")
                .setItems(playTypes, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            presenter.bindAllDanmu(videoList, folderPath);
                            break;
                        case 1:
                            presenter.unbindAllDanmu(folderPath);
                            break;
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void showSubtitleEditDialog() {
        final String[] playTypes = {"绑定 所有视频同名字幕", "解绑 所有视频字幕"};

        new AlertDialog.Builder(this)
                .setTitle("字幕管理")
                .setItems(playTypes, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            presenter.bindAllZimu(videoList, folderPath);
                            break;
                        case 1:
                            presenter.unbindAllZimu(folderPath);
                            break;
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void showVideoSortDialog() {
        final String[] playTypes = {"按 文件名 排序", "按 视频时长 排序"};

        new AlertDialog.Builder(this)
                .setTitle("视频排序")
                .setItems(playTypes, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            int nameType = AppConfig.getInstance().getFolderSortType();
                            if (nameType == Constants.FolderSort.NAME_ASC)
                                sort(Constants.FolderSort.NAME_DESC);
                            else if (nameType == Constants.FolderSort.NAME_DESC)
                                sort(Constants.FolderSort.NAME_ASC);
                            else
                                sort(Constants.FolderSort.NAME_ASC);
                            adapter.notifyDataSetChanged();
                            break;
                        case 1:
                            int durationType = AppConfig.getInstance().getFolderSortType();
                            if (durationType == Constants.FolderSort.DURATION_ASC)
                                sort(Constants.FolderSort.DURATION_DESC);
                            else if (durationType == Constants.FolderSort.DURATION_DESC)
                                sort(Constants.FolderSort.DURATION_ASC);
                            else
                                sort(Constants.FolderSort.DURATION_ASC);
                            adapter.notifyDataSetChanged();
                            break;
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void sort(int type) {
        if (type == Constants.FolderSort.NAME_ASC) {
            Collections.sort(videoList, new FileNameComparator<VideoBean>() {
                @Override
                public String getCompareValue(VideoBean videoBean) {
                    return FileUtils.getFileNameNoExtension(videoBean.getVideoPath());
                }
            });
        } else if (type == Constants.FolderSort.NAME_DESC) {
            Collections.sort(videoList, new FileNameComparator<VideoBean>(true) {
                @Override
                public String getCompareValue(VideoBean videoBean) {
                    return FileUtils.getFileNameNoExtension(videoBean.getVideoPath());
                }
            });
        } else if (type == Constants.FolderSort.DURATION_ASC) {
            Collections.sort(videoList,
                    (o1, o2) -> Long.compare(o1.getVideoDuration(), o2.getVideoDuration()));
        } else if (type == Constants.FolderSort.DURATION_DESC) {
            Collections.sort(videoList,
                    (o1, o2) -> Long.compare(o2.getVideoDuration(), o1.getVideoDuration()));
        }
        AppConfig.getInstance().saveFolderSortType(type);
    }
}

package com.xyoye.dandanplay.ui.activities.anime;

import android.content.Intent;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.lifecycle.Lifecycle;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.blankj.utilcode.util.FileUtils;
import com.blankj.utilcode.util.KeyboardUtils;
import com.blankj.utilcode.util.StringUtils;
import com.blankj.utilcode.util.ToastUtils;
import com.frostwire.jlibtorrent.TorrentInfo;
import com.xunlei.downloadlib.XLDownloadManager;
import com.xunlei.downloadlib.XLTaskHelper;
import com.xunlei.downloadlib.parameter.BtIndexSet;
import com.xunlei.downloadlib.parameter.BtTaskParam;
import com.xunlei.downloadlib.parameter.XLTaskLocalUrl;
import com.xyoye.dandanplay.R;
import com.xyoye.dandanplay.app.IApplication;
import com.xyoye.dandanplay.base.BaseMvpActivity;
import com.xyoye.dandanplay.base.BaseRvAdapter;
import com.xyoye.dandanplay.bean.AnimeTypeBean;
import com.xyoye.dandanplay.bean.MagnetBean;
import com.xyoye.dandanplay.bean.SearchHistoryBean;
import com.xyoye.dandanplay.bean.SubGroupBean;
import com.xyoye.dandanplay.bean.event.DeleteHistoryEvent;
import com.xyoye.dandanplay.bean.event.SearchHistoryEvent;
import com.xyoye.dandanplay.bean.event.SelectInfoEvent;
import com.xyoye.dandanplay.mvp.impl.SearchPresenterImpl;
import com.xyoye.dandanplay.mvp.presenter.SearchPresenter;
import com.xyoye.dandanplay.mvp.view.SearchView;
import com.xyoye.dandanplay.ui.activities.personal.DownloadManagerActivity;
import com.xyoye.dandanplay.ui.activities.play.PlayerManagerActivity;
import com.xyoye.dandanplay.ui.weight.dialog.CommonEditTextDialog;
import com.xyoye.dandanplay.ui.weight.dialog.SelectInfoDialog;
import com.xyoye.dandanplay.ui.weight.dialog.TorrentCheckDownloadDialog;
import com.xyoye.dandanplay.ui.weight.dialog.TorrentCheckPlayDialog;
import com.xyoye.dandanplay.ui.weight.item.MagnetItem;
import com.xyoye.dandanplay.ui.weight.item.SearchHistoryItem;
import com.xyoye.dandanplay.utils.AppConfig;
import com.xyoye.dandanplay.utils.CommonUtils;
import com.xyoye.dandanplay.utils.Constants;
import com.xyoye.dandanplay.utils.interf.AdapterItem;
import com.xyoye.dandanplay.utils.jlibtorrent.Torrent;
import com.xyoye.player.commom.utils.AnimHelper;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import butterknife.BindView;
import butterknife.OnClick;

/**
 * Created by xyoye on 2019/1/8.
 */

public class SearchActivity extends BaseMvpActivity<SearchPresenter> implements SearchView {

    @BindView(R.id.search_et)
    EditText searchEt;
    @BindView(R.id.subgroup_tv)
    TextView subgroupTv;
    @BindView(R.id.type_tv)
    TextView typeTv;
    @BindView(R.id.history_rv)
    RecyclerView historyRv;
    @BindView(R.id.search_result_rv)
    RecyclerView resultRv;
    @BindView(R.id.history_rl)
    RelativeLayout historyRl;

    private boolean isSearch = false;
    private String animeTitle;
    private String searchWord;
    private String lastSearchWord;
    private int typeId = -1;
    private int subgroupsId = -1;
    private AtomicInteger atomicInteger;

    private List<SearchHistoryBean> historyList;
    private List<MagnetBean.ResourcesBean> resultList;
    private BaseRvAdapter<SearchHistoryBean> historyAdapter;
    private BaseRvAdapter<MagnetBean.ResourcesBean> resultAdapter;

    @NonNull
    @Override
    protected SearchPresenter initPresenter() {
        return new SearchPresenterImpl(this, this);
    }

    @Override
    protected int initPageLayoutID() {
        return R.layout.activity_search;
    }

    @Override
    public void initView() {
        historyList = new ArrayList<>();
        resultList = new ArrayList<>();
        animeTitle = getIntent().getStringExtra("anime_title");
        searchWord = getIntent().getStringExtra("search_word");
        boolean isAnime = getIntent().getBooleanExtra("is_anime", false);

        historyAdapter = new BaseRvAdapter<SearchHistoryBean>(historyList) {
            @NonNull
            @Override
            public AdapterItem<SearchHistoryBean> onCreateItem(int viewType) {
                return new SearchHistoryItem();
            }
        };
        resultAdapter = new BaseRvAdapter<MagnetBean.ResourcesBean>(resultList) {
            @NonNull
            @Override
            public AdapterItem<MagnetBean.ResourcesBean> onCreateItem(int viewType) {
                return new MagnetItem((position, onlyDownload, playResource) ->
                        onMagnetItemClick(position, onlyDownload, playResource));
            }
        };
        resultRv.setAdapter(resultAdapter);

        historyRv.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        historyRv.setNestedScrollingEnabled(false);
        historyRv.setItemViewCacheSize(10);
        historyRv.setAdapter(historyAdapter);

        resultRv.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        resultRv.setNestedScrollingEnabled(false);
        resultRv.setItemViewCacheSize(10);
        resultRv.setAdapter(resultAdapter);

        presenter.getSearchHistory(isAnime);
        if (!isAnime) {
            searchEt.postDelayed(() ->
                    KeyboardUtils.showSoftInput(searchEt), 200);
        }
    }

    @Override
    public void initListener() {
        searchEt.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus && historyRl.getVisibility() == View.GONE) {
                AnimHelper.doShowAnimator(historyRl);
            }
        });

        historyRl.setOnClickListener(v -> {
            AnimHelper.doHideAnimator(historyRl);
            searchEt.clearFocus();
            KeyboardUtils.hideSoftInput(searchEt);
        });

        searchEt.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                String searchText = searchEt.getText().toString().trim();
                if (StringUtils.isEmpty(searchText)) {
                    ToastUtils.showShort("请输入搜索条件");
                    return false;
                }
                search(searchText);
                return true;
            }
            return false;
        });
    }

    @OnClick({R.id.return_iv, R.id.change_source_tv, R.id.subgroup_tv, R.id.type_tv, R.id.search_iv})
    public void onViewClicked(View view) {
        KeyboardUtils.hideSoftInput(searchEt);
        switch (view.getId()) {
            case R.id.return_iv:
                if (historyRl.getVisibility() == View.GONE || !isSearch)
                    SearchActivity.this.finish();
                else {
                    AnimHelper.doHideAnimator(historyRl);
                    searchEt.clearFocus();
                }
                break;
            case R.id.change_source_tv:
                String searchDomain = AppConfig.getInstance().getSearchDomain();
                new CommonEditTextDialog(this, CommonEditTextDialog.SEARCH_DOMAIN, searchDomain, data -> {
                    AppConfig.getInstance().saveSearchDomain(data[0]);
                }).show();
                break;
            case R.id.subgroup_tv:
                presenter.querySubGroupList();
                break;
            case R.id.type_tv:
                presenter.queryTypeList();
                break;
            case R.id.search_iv:
                String searchText = searchEt.getText().toString().trim();
                if (StringUtils.isEmpty(searchText)) {
                    ToastUtils.showShort("请输入搜索条件");
                    return;
                }
                search(searchText);
                break;
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(SelectInfoEvent event) {
        if (event.getType() == SelectInfoEvent.TYPE) {
            if (event.getSelectId() == -1) {
                typeId = -1;
                typeTv.setText("选分类");
            } else {
                typeId = event.getSelectId();
                typeTv.setText(event.getSelectName());
            }
        } else if (event.getType() == SelectInfoEvent.SUBGROUP) {

            if (event.getSelectId() == -1) {
                subgroupsId = -1;
                subgroupTv.setText("字幕组");
            } else {
                subgroupsId = event.getSelectId();
                subgroupTv.setText(event.getSelectName());
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(DeleteHistoryEvent event) {
        if (event.isDeleteAll()) {
            presenter.deleteAllHistory();
            historyList.clear();
            historyAdapter.notifyDataSetChanged();
        } else {
            if (historyList != null &&
                    historyList.size() > 0 &&
                    historyList.size() > event.getDeletePosition()) {
                presenter.deleteHistory(historyList.get(event.getDeletePosition()).get_id());
                historyList.remove(event.getDeletePosition());
                if (historyList.size() == 1)
                    historyList.clear();
                historyAdapter.notifyDataSetChanged();
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(SearchHistoryEvent event) {
        if (StringUtils.isEmpty(event.getSearchText())) {
            ToastUtils.showShort("搜索条件不能为空");
            return;
        }
        search(event.getSearchText());
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!EventBus.getDefault().isRegistered(this))
            EventBus.getDefault().register(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (EventBus.getDefault().isRegistered(this))
            EventBus.getDefault().unregister(this);
    }

    @Override
    public void refreshHistory(List<SearchHistoryBean> historyBeanList, boolean doSearch) {
        if (historyBeanList != null) {
            historyList.clear();
            historyList.addAll(historyBeanList);
            historyAdapter.notifyDataSetChanged();
        }
        if (doSearch)
            search(searchWord);
    }

    @Override
    public void refreshSearch(List<MagnetBean.ResourcesBean> searchResult) {
        if (searchResult != null) {
            isSearch = true;
            resultList.clear();
            resultList.addAll(searchResult);
            resultAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void downloadTorrentOver(String torrentFilePath, int position, boolean onlyDownload, boolean playResource) {
        //更新视图
        if (position < resultList.size()) {
            resultList.get(position).setMagnetPath(torrentFilePath);
            resultAdapter.notifyItemChanged(position);
        }

        //仅更新种子资源
        if (onlyDownload) {
            ToastUtils.showShort("更新种子资源成功");
            return;
        }

        //播放 or 下载
        if (playResource) {
            showPlayTorrentInfoDialog(torrentFilePath);
        } else {
            showDownloadTorrentInfoDialog(torrentFilePath);
        }
    }

    @Override
    public String getDownloadFolder() {
        String downloadFolder = AppConfig.getInstance().getDownloadFolder();
        downloadFolder = StringUtils.isEmpty(animeTitle)
                ? downloadFolder
                : downloadFolder + "/" + animeTitle;
        return downloadFolder;
    }

    @Override
    public void showDownloadTorrentLoading() {
        showLoadingDialog();
    }

    @Override
    public void dismissDownloadTorrentLoading() {
        dismissLoadingDialog();
    }

    @Override
    public void showAnimeTypeDialog(List<AnimeTypeBean.TypesBean> typeList) {
        if (typeList.size() > 0) {
            new SelectInfoDialog(SearchActivity.this, typeList).show();
        }
    }

    @Override
    public void showSubGroupDialog(List<SubGroupBean.SubgroupsBean> subGroupList) {
        if (subGroupList.size() > 0) {
            new SelectInfoDialog(
                    SearchActivity.this,
                    subGroupList,
                    SelectInfoEvent.SUBGROUP).show();
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if ((keyCode == KeyEvent.KEYCODE_BACK)) {
            if (historyRl.getVisibility() == View.GONE || !isSearch)
                SearchActivity.this.finish();
            else {
                AnimHelper.doHideAnimator(historyRl);
                KeyboardUtils.hideSoftInput(searchEt);
                searchEt.clearFocus();
            }
        }
        return true;
    }

    private void search(String searchText) {
        if (getLifecycle().getCurrentState() == Lifecycle.State.DESTROYED)
            return;

        AnimHelper.doHideAnimator(historyRl);
        searchEt.setText(searchText);
        searchEt.clearFocus();
        KeyboardUtils.hideSoftInput(searchEt);

        boolean isExist = false;
        int existN = -1;
        for (int i = 0; i < historyList.size(); i++) {
            SearchHistoryBean historyBean = historyList.get(i);
            if (historyBean.getText().equals(searchText)) {
                isExist = true;
                existN = i;
                break;
            }
        }
        if (!isExist) {
            historyList.add(0, new SearchHistoryBean(historyList.size(), searchText, System.currentTimeMillis()));
            if (historyList.size() == 1) {
                historyList.add(new SearchHistoryBean(-1, "", -1));
            }
            historyAdapter.notifyDataSetChanged();
            presenter.addHistory(searchText);
        } else {
            SearchHistoryBean historyBean = historyList.get(existN);
            historyList.remove(existN);
            historyList.add(0, historyBean);
            historyAdapter.notifyDataSetChanged();
            presenter.updateHistory(historyBean.get_id());
        }
        lastSearchWord = searchText;
        presenter.search(animeTitle, searchText, typeId, subgroupsId);
    }

    private void onMagnetItemClick(int position, boolean onlyDownload, boolean playResource) {
        MagnetBean.ResourcesBean resourcesBean = resultList.get(position);
        if (onlyDownload) {
            presenter.downloadTorrent(resourcesBean.getMagnet(), position, true, false);
        } else {
            if (TextUtils.isEmpty(resourcesBean.getMagnetPath())) {
                presenter.downloadTorrent(resourcesBean.getMagnet(), position, false, playResource);
            } else {
                if (playResource) {
                    showPlayTorrentInfoDialog(resourcesBean.getMagnetPath());
                } else {
                    showDownloadTorrentInfoDialog(resourcesBean.getMagnetPath());
                }
            }
        }
    }

    private void showDownloadTorrentInfoDialog(String torrentFilePath) {
        try {
            TorrentInfo torrentInfo = new TorrentInfo(new File(torrentFilePath));
            //任务不存在则新增任务
            new TorrentCheckDownloadDialog(this, torrentInfo, priorityList -> {
                String saveDirPath = AppConfig.getInstance().getDownloadFolder();
                String taskName = torrentInfo.name();

                //单文件时会以文件名作为下载任务名称，去除后缀
                if (taskName.contains(".") && CommonUtils.isMediaFile(taskName)) {
                    taskName = taskName.substring(0, taskName.lastIndexOf("."));
                }

                //有番剧名则路径名为：下载目录/番剧名/任务名称/视频
                if (!TextUtils.isEmpty(animeTitle)) {
                    saveDirPath += "/" + animeTitle;
                }
                saveDirPath += "/" + taskName;

                Torrent torrent = new Torrent(
                        torrentFilePath,
                        saveDirPath,
                        priorityList);

                Intent intent = new Intent(SearchActivity.this, DownloadManagerActivity.class);
                intent.putExtra("download_data", torrent);
                startActivity(intent);
            }).show();
        } catch (Exception e) {
            e.printStackTrace();
            ToastUtils.showShort("获取下载任务详情失败");
        }
    }

    private void showPlayTorrentInfoDialog(String torrentFilePath) {
        com.xunlei.downloadlib.parameter.TorrentInfo thunderTorrentInfo =
                XLTaskHelper.getInstance().getTorrentInfo(torrentFilePath);

        if (thunderTorrentInfo == null || thunderTorrentInfo.mSubFileInfo == null) {
            ToastUtils.showShort("播放失败，无法解析播放内容");
            return;
        }

        new TorrentCheckPlayDialog(this, thunderTorrentInfo, (position, realIndex, fileSize) -> {
            atomicInteger = new AtomicInteger(0);
            playByThunder(realIndex, position, fileSize, torrentFilePath, thunderTorrentInfo);
        }).show();
    }

    /**
     * 启动播放
     */
    private void playByThunder(int realIndex,
                               int checkedPosition,
                               long checkedFileSize,
                               String torrentFilePath,
                               com.xunlei.downloadlib.parameter.TorrentInfo thunderTorrentInfo) {

        File cacheFolder = new File(Constants.DefaultConfig.cacheFolderPath);
        if (!cacheFolder.exists()) {
            if (!cacheFolder.mkdirs()) {
                ToastUtils.showShort("播放失败，创建缓存文件夹失败");
                return;
            }
        }
        FileUtils.deleteAllInDir(cacheFolder);

        if (cacheFolder.getFreeSpace() < checkedFileSize) {
            ToastUtils.showShort("播放失败，剩余缓存空间不足");
            return;
        }

        //构建参数
        BtTaskParam taskParam = new BtTaskParam();
        taskParam.setCreateMode(1);
        taskParam.setFilePath(cacheFolder.getAbsolutePath());
        taskParam.setMaxConcurrent(3);
        taskParam.setSeqId(atomicInteger.incrementAndGet());
        taskParam.setTorrentPath(torrentFilePath);

        BtIndexSet selectIndexSet = new BtIndexSet(1);
        selectIndexSet.mIndexSet[0] = realIndex;

        //选择的文件，与忽略的文件
        List<Integer> deselectIndexList = new ArrayList<>();
        for (int i = 0; i < thunderTorrentInfo.mSubFileInfo.length; i++) {
            if (thunderTorrentInfo.mSubFileInfo[i].mRealIndex != realIndex) {
                deselectIndexList.add(i);
            }
        }
        BtIndexSet deSelectIndexSet = new BtIndexSet(deselectIndexList.size());
        for (int i = 0; i < deselectIndexList.size(); i++) {
            deSelectIndexSet.mIndexSet[i] = deselectIndexList.get(i);
        }

        //开启任务
        long[] taskStatus = XLTaskHelper.getInstance().startTask(taskParam, selectIndexSet, deSelectIndexSet);

        long playTaskId = taskStatus[0];

        //任务出错重试
        if (playTaskId == -1) {
            XLTaskHelper.getInstance().stopTask(playTaskId);
            //重试两次
            if (atomicInteger.get() < 3) {
                XLDownloadManager.getInstance().uninit();
                XLTaskHelper.init(IApplication.get_context());
                playByThunder(realIndex, checkedPosition, checkedFileSize, torrentFilePath, thunderTorrentInfo);
            } else {
                FileUtils.deleteAllInDir(Constants.DefaultConfig.cacheFolderPath);
                ToastUtils.showShort("播放失败，无法开始播放任务");
            }
            return;
        }

        String fileName = thunderTorrentInfo.mSubFileInfo[checkedPosition].mFileName;
        String filePath = taskParam.mFilePath + "/" + fileName;
        XLTaskLocalUrl localUrl = new XLTaskLocalUrl();
        XLDownloadManager.getInstance().getLocalUrl(filePath, localUrl);

        //启动播放
        PlayerManagerActivity.launchPlayerOnline(
                this,
                fileName,
                localUrl.mStrUrl,
                "",
                0,
                0,
                playTaskId,
                lastSearchWord);
    }

    @Override
    public void showLoading() {
        showLoadingDialog();
    }

    @Override
    public void hideLoading() {
        dismissLoadingDialog();
    }

    @Override
    public void showError(String message) {
        ToastUtils.showShort(message);
    }
}

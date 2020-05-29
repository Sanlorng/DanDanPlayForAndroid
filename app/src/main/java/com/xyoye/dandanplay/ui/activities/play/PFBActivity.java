package com.xyoye.dandanplay.ui.activities.play;

import android.Manifest;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.blankj.utilcode.util.ToastUtils;
import com.tbruyelle.rxpermissions2.RxPermissions;
import com.xyoye.dandanplay.R;
import com.xyoye.dandanplay.base.BaseMvpActivity;
import com.xyoye.dandanplay.base.BaseRvAdapter;
import com.xyoye.dandanplay.bean.PFBFileBean;
import com.xyoye.dandanplay.bean.PFBScanBean;
import com.xyoye.dandanplay.mvp.impl.PFBPresenterImpl;
import com.xyoye.dandanplay.mvp.presenter.PFBPresenter;
import com.xyoye.dandanplay.mvp.view.PFBView;
import com.xyoye.dandanplay.ui.weight.item.PFBFileItem;
import com.xyoye.dandanplay.utils.JsonUtils;
import com.xyoye.dandanplay.utils.interf.AdapterItem;
import com.xyoye.dandanplay.utils.net.PFBRetroFactory;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.OnClick;
import io.reactivex.disposables.Disposable;

/**
 * Created by xyoye on 2020/5/29.
 */

public class PFBActivity extends BaseMvpActivity<PFBPresenter> implements PFBView {
    private final static int REQUEST_CODE_SCAN_PC = 10001;

    @BindView(R.id.scan_bt)
    Button scanBt;
    @BindView(R.id.file_ll)
    LinearLayout fileLl;
    @BindView(R.id.file_path_tv)
    TextView filePathTv;
    @BindView(R.id.file_rv)
    RecyclerView fileRv;

    private Disposable permissionDis;
    private List<PFBFileBean> fileList;
    private BaseRvAdapter<PFBFileBean> fileAdapter;

    private String path = "";

    @NonNull
    @Override
    protected PFBPresenter initPresenter() {
        return new PFBPresenterImpl(this, this);
    }

    @Override
    protected int initPageLayoutID() {
        return R.layout.activity_pfb;
    }

    @Override
    public void initView() {
        setTitle("PC文件浏览器");

        fileList = new ArrayList<>();

        fileAdapter = new BaseRvAdapter<PFBFileBean>(fileList) {
            @NonNull
            @Override
            public AdapterItem<PFBFileBean> onCreateItem(int viewType) {
                return new PFBFileItem(position -> onItemClick(position));
            }
        };

        fileRv.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        fileRv.setAdapter(fileAdapter);
    }

    @Override
    public void initListener() {

    }

    @OnClick(R.id.scan_bt)
    public void onViewClicked() {
        permissionDis = new RxPermissions(this).
                request(Manifest.permission.CAMERA)
                .subscribe(granted -> {
                    Intent intent = new Intent(this, RemoteScanActivity.class);
                    intent.putExtra("pfb_scan", true);
                    startActivityForResult(intent, REQUEST_CODE_SCAN_PC);
                });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (resultCode == RESULT_OK && data != null) {
            if (requestCode == REQUEST_CODE_SCAN_PC) {
                String connectionInfo = data.getStringExtra("scan_result_data");
                PFBScanBean scanBean = JsonUtils.fromJson(connectionInfo, PFBScanBean.class);

                if (scanBean == null) {
                    showError("错误的二维码");
                    return;
                }

                if (scanBean.getIpAddresses().size() == 0) {
                    showError("IP地址为空，无法连接");
                    return;
                }

                if (scanBean.getIpAddresses().size() > 1) {
                    String[] ways = scanBean.getIpAddresses().toArray(new String[0]);
                    new AlertDialog.Builder(this)
                            .setTitle("选择IP地址")
                            .setItems(ways, (dialog, which) -> {
                                PFBRetroFactory.setPcIp(ways[which]);
                                PFBRetroFactory.setConnectCode(scanBean.getSecurityCode());
                                presenter.getRootFiles();
                            })
                            .setNegativeButton("取消", null)
                            .show();
                } else {
                    PFBRetroFactory.setPcIp(scanBean.getIpAddresses().get(0));
                    PFBRetroFactory.setConnectCode(scanBean.getSecurityCode());
                    presenter.getRootFiles();
                }
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (!TextUtils.isEmpty(path)) {
                onItemClick(0);
                return true;
            } else {
                finish();
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onDestroy() {
        permissionDis.dispose();
        super.onDestroy();
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

    @Override
    public void updateRootFiles(List<PFBFileBean> rootFiles) {
        scanBt.setVisibility(View.GONE);
        fileLl.setVisibility(View.VISIBLE);

        path = "";
        updatePath();

        fileList.clear();
        fileList.addAll(rootFiles);
        fileAdapter.notifyDataSetChanged();
    }

    @Override
    public void updateDirectoryFiles(String dirPath, List<PFBFileBean> rootFiles) {
        rootFiles.add(0, new PFBFileBean(true, ".."));

        path = dirPath;
        updatePath();

        fileList.clear();
        fileList.addAll(rootFiles);
        fileAdapter.notifyDataSetChanged();
    }

    private void onItemClick(int position) {
        PFBFileBean fileBean = fileList.get(position);
        if (fileBean.isPreviousItem()) {
            boolean openRootDir = path.split("\\\\").length < 2;

            if (openRootDir) {
                presenter.getRootFiles();
            } else {
                int index = path.lastIndexOf("\\");
                if (index > 0) {
                    String newPath = path.substring(0, index);
                    if (newPath.endsWith(":"))
                        presenter.openDirectory(newPath + "\\");
                    else
                        presenter.openDirectory(newPath);
                }
            }
        } else if (fileBean.isDirectory()) {
            presenter.openDirectory(fileBean.getFilePath());
        } else {

        }
    }

    private void updatePath() {
        String showPath;
        if (TextUtils.isEmpty(path)) {
            showPath = "\\";
        } else {
            if (path.endsWith(":\\")) {
                showPath = "\\" + path.replace(":\\", ":");
            } else {
                showPath = "\\" + path;
            }
        }
        filePathTv.setText(showPath);
    }
}

package com.xyoye.dandanplay.ui.weight.dialog;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;

import com.blankj.utilcode.util.ToastUtils;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.xyoye.dandanplay.R;
import com.xyoye.dandanplay.app.IApplication;
import com.xyoye.dandanplay.bean.BiliBiliVideoInfo;
import com.xyoye.dandanplay.utils.AppConfig;
import com.xyoye.dandanplay.utils.Constants;
import com.xyoye.dandanplay.utils.DanmuUtils;
import com.xyoye.dandanplay.utils.JsonUtils;
import com.xyoye.dandanplay.utils.net.okhttp.OkHttpEngine;

import org.jsoup.Connection;
import org.jsoup.Jsoup;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

import butterknife.BindView;
import butterknife.ButterKnife;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;

/**
 * Created by xyoye on 2018/5/17.
 */

public class BiliBiliDownloadDialog extends Dialog {
    public final static int BILIBILI_URL = 1;
    public final static int BILIBILI_AV = 2;
    public final static int BILIBILI_BV = 3;

    private final int DOWNLOAD_ONE = 1;
    private final int DOWNLOAD_LIST = 2;

    @BindView(R.id.log_et)
    EditText logEt;
    @BindView(R.id.file_name_et)
    EditText fileNameEt;
    @BindView(R.id.download_start_bt)
    Button downloadStartBt;
    @BindView(R.id.download_over_bt)
    Button downloadOverBt;
    @BindView(R.id.change_file_ll)
    LinearLayout fileNameLayout;

    private String keyWord;
    private int type;
    private Context context;
    private String fileName;

    private String cid;
    private String videoTitle;

    private List<String> cidList;
    private String animeTitle;

    private int downloadType;

    private Handler handler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what) {
                //Log消息
                case 100:
                    logEt.append((String) msg.obj);
                    break;
                //获取cid成功
                case 101:
                    if (downloadType == DOWNLOAD_ONE) {
                        fileNameEt.setEnabled(true);
                        fileNameEt.setText(videoTitle);
                    }
                    downloadStartBt.setVisibility(View.VISIBLE);
                    downloadStartBt.setEnabled(true);
                    downloadStartBt.setBackground(ContextCompat.getDrawable(context, R.drawable.btn_corner_blue));
                    downloadStartBt.setText("开始下载");
                    break;
                //初始化完成
                case 102:
                    downloadStartBt.setVisibility(View.GONE);
                    downloadOverBt.setVisibility(View.VISIBLE);
                    break;
                //开始下载
                case 103:
                    fileNameLayout.setVisibility(View.GONE);
                    downloadStartBt.setEnabled(false);
                    downloadStartBt.setText("正在下载…");
                    break;
                //下载完成
                case 104:
                    downloadStartBt.setVisibility(View.GONE);
                    downloadOverBt.setVisibility(View.VISIBLE);
                    downloadOverBt.setText("关闭");
                    break;
            }
            return false;
        }
    });

    public BiliBiliDownloadDialog(@NonNull Context context, int themeResId, String keyWord, int type) {
        super(context, themeResId);
        this.context = context;
        this.keyWord = keyWord;
        this.type = type;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_bilibili_download);
        ButterKnife.bind(this, this);

        logEt.setFocusable(false);
        logEt.setFocusableInTouchMode(false);
        fileNameEt.setEnabled(false);
        downloadStartBt.setText("正在准备…");

        IApplication.getExecutor().execute(() -> {
            try {
                switch (type) {
                    case BILIBILI_URL:
                        downloadByUrl(keyWord);
                        break;
                    case BILIBILI_AV:
                        downloadByAv(keyWord);
                        break;
                    case BILIBILI_BV:
                        downloadByBv(keyWord);
                        break;
                }
            } catch (Exception e) {
                ToastUtils.showShort("无法获取视频信息，请检查链接或网络状态");
                sendLogMessage("无法获取视频信息，请检查链接或网络状态");
                handler.sendEmptyMessage(104);
                e.printStackTrace();
            }
        });

        initListener();
    }

    /**
     * 初始化接口
     */
    private void initListener() {
        downloadStartBt.setOnClickListener(v ->
                IApplication.getExecutor().execute(() -> {
                    handler.sendEmptyMessage(103);
                    if (downloadType == DOWNLOAD_ONE) {
                        downloadDanmuOne();
                    } else if (downloadType == DOWNLOAD_LIST) {
                        downloadDanmuList();
                    }
                })
        );

        downloadOverBt.setOnClickListener(v -> BiliBiliDownloadDialog.this.cancel());
    }

    /**
     * 根据av号获取相关信息
     */
    private void downloadByAv(String avNumber) throws Exception {
        if (avNumber.isEmpty()) {
            ToastUtils.showShort("请输入av号");
        } else {
            sendLogMessage("开始获取cid...\n");
            //尝试通过API获取cid
            if (getCidByApi(avNumber, "aid")) {
                return;
            }
            sendLogMessage("开始转换URL...\n");
            //API下载失败，通过网页获取
            String url = "https://www.bilibili.com/video/av" + avNumber;
            Connection.Response response = Jsoup.connect(url).timeout(10000).execute();
            //普通视频不会重定向，番剧会进行重定向
            if (!response.url().toString().equals(url)) {
                url = response.url().toString();
            }
            sendLogMessage("转换URL成功\n");
            downloadByUrl(url);
        }
    }

    /**
     * 根据bv号获取相关信息
     */
    private void downloadByBv(String bvCode) throws Exception {
        if (bvCode.isEmpty()) {
            ToastUtils.showShort("请输入bv号");
        } else {
            sendLogMessage("开始获取cid...\n");
            //尝试通过API获取cid
            if (getCidByApi(bvCode, "bvid")) {
                return;
            }
            //API下载失败，通过网页获取
            sendLogMessage("开始转换URL...\n");
            String url = "https://www.bilibili.com/video/" + bvCode;
            Connection.Response response = Jsoup.connect(url).timeout(10000).execute();
            //普通视频不会重定向，番剧会进行重定向
            if (!response.url().toString().equals(url)) {
                url = response.url().toString();
            }
            sendLogMessage("转换URL成功\n");
            downloadByUrl(url);
        }
    }

    /**
     * 根据url获取相关信息
     */
    private void downloadByUrl(String url) throws Exception {
        if (!url.isEmpty()) {
            sendLogMessage("开始连接URL...\n");
            String root = Jsoup.connect(url).timeout(10000).get().toString();
            sendLogMessage("连接URL成功\n");

            if (url.contains("www.bilibili.com/video") || url.contains("m.bilibili.com/video")) {
                getVideoCid(root);
            } else if (url.contains("www.bilibili.com/bangumi") || url.contains("m.bilibili.com/bangumi")) {
                getAnimeCid(root);
            } else {
                sendLogMessage("获取视频链接信息失败\n");
                ToastUtils.showShort("获取视频链接信息失败");
                handler.sendEmptyMessage(104);
            }
        } else {
            ToastUtils.showShort("请输入视频链接");
        }
    }

    /**
     * 通过API获取CID
     */
    private boolean getCidByApi(String code, String key) {
        String params = key + "=" + code;
        String apiUrl = "https://api.bilibili.com/x/web-interface/view?" + params;

        try {
            OkHttpClient client = OkHttpEngine.getInstance()
                    .getOkHttpClient()
                    .newBuilder()
                    .connectTimeout(5000, TimeUnit.MILLISECONDS)
                    .readTimeout(5000, TimeUnit.MILLISECONDS)
                    .addInterceptor(new HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY))
                    .build();

            Request request = new Request.Builder()
                    .url(apiUrl)
                    .get()
                    .build();

            Response response = client.newCall(request).execute();
            if (response.body() != null) {
                String resultJson = response.body().string();
                if (!TextUtils.isEmpty(resultJson)) {
                    BiliBiliVideoInfo info = JsonUtils.fromJson(resultJson, BiliBiliVideoInfo.class);
                    if (info != null && info.getCode() == 0 && info.getData() != null) {
                        cid = info.getData().getCid() + "";
                        videoTitle = info.getData().getTitle();
                        sendLogMessage("获取cid成功\n");
                        downloadType = DOWNLOAD_ONE;
                        handler.sendEmptyMessage(101);
                        return true;
                    }
                }
            }
        } catch (Exception ignore) {

        }
        return false;
    }

    /**
     * 下载视频弹幕
     */
    private void downloadDanmuOne() {
        fileName = fileNameEt.getText().toString();
        String path = AppConfig.getInstance().getDownloadFolder();

        sendLogMessage("开始下载弹幕文件...\n");
        String xmlContent = getXmlString(cid);
        if (xmlContent == null) {
            sendLogMessage("弹幕文件下载失败");
            return;
        }
        sendLogMessage("弹幕文件下载成功\n正在写入文件...\n");

        if (fileName.isEmpty())
            fileName = cid;
        DanmuUtils.saveDanmuSourceFormBiliBili(xmlContent, fileName, path);
        sendLogMessage("写入文件成功\n文件路径：\n" + path + "/" + fileName + ".xml");

        handler.sendEmptyMessage(102);
    }

    /**
     * 下载番剧弹幕集合
     */
    private void downloadDanmuList() {
        String path = AppConfig.getInstance().getDownloadFolder();
        path = path + "/" + animeTitle + Constants.DefaultConfig.danmuFolder;

        sendLogMessage("开始下载弹幕文件...\n");
        for (int i = 0; i < cidList.size(); i++) {
            int episode = i + 1;
            String cid = cidList.get(i);
            sendLogMessage("下载第" + episode + "集弹幕...\n");
            String xmlContent = getXmlString(cid);
            if (xmlContent == null) {
                sendLogMessage("第" + episode + "集弹幕文件下载失败\n");
                continue;
            }

            if (episode < 10)
                fileName = "0" + episode;
            else
                fileName = episode + "";
            DanmuUtils.saveDanmuSourceFormBiliBili(xmlContent, fileName, path);

            if (!BiliBiliDownloadDialog.this.isShowing()) {
                break;
            }
        }

        if (BiliBiliDownloadDialog.this.isShowing()) {
            sendLogMessage("弹幕下载完成\n文件路径：\n" + path);
            handler.sendEmptyMessage(102);
        }
    }

    /**
     * 发送Log消息
     */
    private void sendLogMessage(String msg) {
        Message message = new Message();
        message.what = 100;
        message.obj = msg;
        handler.sendMessage(message);
    }

    /**
     * 获取视频Cid
     */
    private void getVideoCid(String root) {
        try {
            int start = root.indexOf("INITIAL_STATE__=") + 16;
            int end = root.indexOf(";(function()");
            String jsonText = root.substring(start, end);
            //获取标题
            JsonObject jsonObject = new JsonParser().parse(jsonText).getAsJsonObject();
            JsonObject videoInfo = jsonObject.get("videoData").getAsJsonObject();
            videoTitle = videoInfo.get("title").getAsString();
            //获取cid
            JsonArray cidInfo = videoInfo.get("pages").getAsJsonArray();
            JsonObject cidObject = cidInfo.get(0).getAsJsonObject();
            cid = cidObject.get("cid").getAsString();

            sendLogMessage("获取cid成功\n");
            downloadType = DOWNLOAD_ONE;
            handler.sendEmptyMessage(101);
        } catch (Exception e) {
            sendLogMessage("cid解析错误");
        }
    }

    /**
     * 获取番剧Cid
     */
    private void getAnimeCid(String root) {
        try {
            sendLogMessage("开始获取番剧cid列表...\n");
            int start = root.indexOf("INITIAL_STATE__=") + 16;
            int end = root.indexOf(";(function()");
            String jsonText = root.substring(start, end);
            //获取标题
            JsonObject animeInfo = new JsonParser().parse(jsonText).getAsJsonObject();
            JsonObject animeTitleInfo = animeInfo.get("mediaInfo").getAsJsonObject();
            animeTitle = animeTitleInfo.get("title").getAsString();
            //获取cid集合
            JsonArray cidListArray = animeInfo.get("epList").getAsJsonArray();
            cidList = new ArrayList<>();
            for (int i = 0; i < cidListArray.size(); i++) {
                JsonObject cidInfo = cidListArray.get(i).getAsJsonObject();
                String cid = cidInfo.get("cid").getAsString();
                cidList.add(cid);
            }
            sendLogMessage("获取番剧【" + animeTitle + "】cid列表成功\n");
            downloadType = DOWNLOAD_LIST;
            handler.sendEmptyMessage(101);
        } catch (Exception e) {
            sendLogMessage("cid解析错误");
        }
    }

    /**
     * 下载xml
     */
    private String getXmlString(String cid) {
        InputStream in = null;
        InputStream flin = null;
        Scanner sc = null;
        try {
            String xmlUrl = "http://comment.bilibili.com/" + cid + ".xml";
            URL url = new URL(xmlUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestProperty("Accept-Encoding", "gzip,deflate");
            conn.setConnectTimeout(10000);
            conn.connect();

            in = conn.getInputStream();
            flin = new InflaterInputStream(in, new Inflater(true));

            sc = new Scanner(flin, "utf-8");

            StringBuilder stringBuffer = new StringBuilder();
            while (sc.hasNext())
                stringBuffer.append(sc.nextLine());
            return stringBuffer.toString();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (sc != null)
                    sc.close();
                if (flin != null)
                    flin.close();
                if (in != null)
                    in.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
        return null;
    }
}

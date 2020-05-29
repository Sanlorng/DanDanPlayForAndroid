package com.xyoye.dandanplay.bean;

import com.xyoye.dandanplay.utils.net.CommOtherDataObserver;
import com.xyoye.dandanplay.utils.net.NetworkConsumer;
import com.xyoye.dandanplay.utils.net.PFBRetroFactory;

import java.util.List;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

/**
 * Created by xyoye on 2020/5/27.
 */

public class PFBFileBean {
    private String fileName;
    private String filePath;
    private long fileLength;
    private boolean canRead;
    private boolean isDirectory;
    private long lastModifyDate;
    private long freeSpace;

    private boolean isPreviousItem;

    public PFBFileBean(boolean isPreviousItem, String fileName) {
        this.isPreviousItem = isPreviousItem;
        this.fileName = fileName;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public long getFileLength() {
        return fileLength;
    }

    public void setFileLength(long fileLength) {
        this.fileLength = fileLength;
    }

    public boolean isCanRead() {
        return canRead;
    }

    public void setCanRead(boolean canRead) {
        this.canRead = canRead;
    }

    public boolean isDirectory() {
        return isDirectory;
    }

    public void setDirectory(boolean directory) {
        isDirectory = directory;
    }

    public long getLastModifyDate() {
        return lastModifyDate;
    }

    public void setLastModifyDate(long lastModifyDate) {
        this.lastModifyDate = lastModifyDate;
    }

    public long isFreeSpace() {
        return freeSpace;
    }

    public void setFreeSpace(long freeSpace) {
        this.freeSpace = freeSpace;
    }

    public boolean isPreviousItem() {
        return isPreviousItem;
    }

    public void setPreviousItem(boolean previousItem) {
        isPreviousItem = previousItem;
    }

    public static void getRootFiles(CommOtherDataObserver<PFBStatusBean<List<PFBFileBean>>> observer, NetworkConsumer consumer) {
        PFBRetroFactory.getInstance().getRootFiles()
                .doOnSubscribe(consumer)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(observer);
    }

    public static void openDirectory(String path, CommOtherDataObserver<PFBStatusBean<List<PFBFileBean>>> observer, NetworkConsumer consumer) {
        PFBRetroFactory.getInstance().openDirectory(path)
                .doOnSubscribe(consumer)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(observer);
    }
}

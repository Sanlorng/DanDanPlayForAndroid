package com.xyoye.dandanplay.ui.weight.item;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.xyoye.dandanplay.R;
import com.xyoye.dandanplay.bean.PFBFileBean;
import com.xyoye.dandanplay.utils.interf.AdapterItem;

import butterknife.BindView;
import io.reactivex.annotations.NonNull;

/**
 * Created by xyoye on 2020/5/29.
 */

public class PFBFileItem implements AdapterItem<PFBFileBean> {
    @BindView(R.id.iv)
    ImageView iv;
    @BindView(R.id.tv)
    TextView tv;

    private View mView;
    private OnPFBItemClickListener listener;

    public PFBFileItem(@NonNull OnPFBItemClickListener listener) {
        this.listener = listener;
    }

    @Override
    public int getLayoutResId() {
        return R.layout.item_pfb_file;
    }

    @Override
    public void initItemViews(View itemView) {
        mView = itemView;
    }

    @Override
    public void onSetViews() {

    }

    @Override
    public void onUpdateViews(PFBFileBean model, int position) {
        if (model.isPreviousItem()) {
            iv.setImageResource(R.drawable.ic_chevron_left_dark);
        } else if (model.isDirectory()) {
            iv.setImageResource(R.drawable.ic_folder_dark);
        } else {
            iv.setImageResource(R.drawable.ic_xml_file);
        }

        tv.setText(model.getFileName());

        mView.setOnClickListener(v -> {
            listener.onClick(position);
        });
    }

    public interface OnPFBItemClickListener {
        void onClick(int position);
    }
}

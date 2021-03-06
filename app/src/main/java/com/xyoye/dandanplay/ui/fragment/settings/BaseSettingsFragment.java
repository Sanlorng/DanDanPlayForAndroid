package com.xyoye.dandanplay.ui.fragment.settings;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.fragment.app.FragmentActivity;
import androidx.preference.PreferenceFragmentCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.AppBarLayout;
import com.xyoye.dandanplay.R;
import com.xyoye.dandanplay.utils.view.WindowUtils;

public abstract class BaseSettingsFragment extends PreferenceFragmentCompat {
    private Toolbar mToolbar;

    public int getTitleIdRes() {
        return 0;
    }

    public String getTitle() {
        return "";
    }

    public Toolbar getToolbar() {
        return mToolbar;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);
        if (view != null) {
            //调整设置列表在父布局的参数，调整与顶部应用栏的间距
            CoordinatorLayout.LayoutParams layoutParams = new CoordinatorLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            CoordinatorLayout.Behavior<View> behavior = new AppBarLayout.ScrollingViewBehavior();
            layoutParams.setBehavior(behavior);

            RecyclerView listView = getListView();
            listView.setLayoutParams(layoutParams);

            //适配底部导航栏
            listView.setClipToPadding(false);
            WindowUtils.fitWindowInsetsBottom(listView);
            listView.addOnScrollListener(new RecyclerView.OnScrollListener() {
                @Override
                public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                    recyclerView.setClipToPadding(!recyclerView.canScrollVertically(1));
                    super.onScrolled(recyclerView, dx, dy);
                }
            });
            WindowUtils.requestApplyInsetsWhenAttached(listView);

            //初始化顶部工具栏
            Toolbar toolbar = view.findViewById(R.id.toolbar);
            mToolbar = toolbar;
            if (toolbar != null) {
                toolbar.getLayoutParams().height = ViewGroup.LayoutParams.WRAP_CONTENT;
                toolbar.setNavigationIcon(R.drawable.ic_baseline_arrow_back_24);
                toolbar.setNavigationOnClickListener(v -> {
                    FragmentActivity activity = getActivity();
                    if (activity != null) {
                        activity.onBackPressed();
                    }
                });

                WindowUtils.fitWindowInsetsTop(toolbar);
                WindowUtils.requestApplyInsetsWhenAttached(toolbar);

                if (getTitleIdRes() > 0) {
                    toolbar.setTitle(getTitleIdRes());
                } else if (!TextUtils.isEmpty(getTitle())) {
                    toolbar.setTitle(getTitle());
                }
            }
        }
        return view;
    }
}

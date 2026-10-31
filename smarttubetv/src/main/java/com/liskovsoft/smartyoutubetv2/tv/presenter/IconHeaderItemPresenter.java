package com.liskovsoft.smartyoutubetv2.tv.presenter;

import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.leanback.widget.HeaderItem;
import androidx.leanback.widget.ListRow;
import androidx.leanback.widget.PageRow;
import androidx.leanback.widget.Presenter;
import androidx.leanback.widget.RowHeaderPresenter;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.liskovsoft.sharedutils.mylogger.Log;
import com.liskovsoft.smartyoutubetv2.tv.R;
import com.liskovsoft.smartyoutubetv2.tv.util.ViewUtil;

public class IconHeaderItemPresenter extends RowHeaderPresenter {
    private static final String TAG = IconHeaderItemPresenter.class.getSimpleName();
    // Unselected look: 60% white (locked roottube_text_secondary), matching
    // the locked look. Selected look: full white + glass pill + bold label.
    private static final int UNSELECTED_ALPHA = 153; // 60% of 255
    private static final int LABEL_UNSELECTED = 0x99FFFFFF;
    private static final int LABEL_SELECTED = 0xFFFFFFFF;
    private final int mResId;
    private final String mIconUrl;
    private Drawable mDefaultIcon;
    private Typeface mRegularTypeface;
    private Typeface mBoldTypeface;
    private boolean mTypefacesLoaded;

    /** Per-holder views, cached once so the focus animation allocates nothing. */
    private static final class SidebarHolder {
        Drawable pill;
        ImageView icon;
        TextView label;
    }

    public IconHeaderItemPresenter(int resId, String iconUrl) {
        mResId = resId;
        mIconUrl = iconUrl;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup viewGroup) {
        LayoutInflater inflater = (LayoutInflater) viewGroup.getContext()
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mDefaultIcon = new ColorDrawable(ContextCompat.getColor(viewGroup.getContext(), R.color.lb_grey));

        View view = inflater.inflate(R.layout.icon_header_item, null);
        view.setFocusable(true);

        // Selected pill, one mutated instance per holder so fading one row
        // never fades another. Starts invisible (unselected).
        Drawable pill = ContextCompat.getDrawable(viewGroup.getContext(), R.drawable.sidebar_selected_pill);
        if (pill != null) {
            pill = pill.mutate();
            pill.setAlpha(0);
            view.setBackground(pill);
        }

        SidebarHolder holder = new SidebarHolder();
        holder.pill = pill;
        holder.icon = view.findViewById(R.id.header_icon);
        holder.label = view.findViewById(R.id.header_label);
        if (holder.icon != null) {
            holder.icon.setImageAlpha(UNSELECTED_ALPHA);
        }
        if (holder.label != null) {
            holder.label.setTextColor(LABEL_UNSELECTED);
        }
        view.setTag(holder);

        loadTypefaces(viewGroup.getContext());

        return new ViewHolder(view);
    }

    private void loadTypefaces(Context context) {
        if (mTypefacesLoaded) {
            return;
        }
        mTypefacesLoaded = true;
        try {
            Typeface regular = ResourcesCompat.getFont(context, R.font.roottube_sans);
            if (regular != null) {
                mRegularTypeface = regular;
                mBoldTypeface = Typeface.create(regular, Typeface.BOLD);
            }
        } catch (Exception e) {
            // Keep default typefaces when the font is unavailable.
            mRegularTypeface = null;
            mBoldTypeface = null;
        }
    }

    @Override
    public void onBindViewHolder(Presenter.ViewHolder viewHolder, Object item) {
        HeaderItem headerItem;

        if (item instanceof PageRow) {
            headerItem = ((PageRow) item).getHeaderItem();
        } else {
            headerItem = ((ListRow) item).getHeaderItem();
        }

        View rootView = viewHolder.view;
        rootView.setFocusable(true);

        ImageView iconView = rootView.findViewById(R.id.header_icon);
        if (iconView != null) {
            if (mIconUrl != null) {
                Glide.with(rootView.getContext())
                        .load(mIconUrl)
                        .apply(ViewUtil.glideOptions().error(mDefaultIcon))
                        .listener(mErrorListener)
                        .into(iconView);

                //ViewUtil.makeMonochrome(iconView);
            } else {
                Drawable icon = mResId > 0 ? ContextCompat.getDrawable(rootView.getContext(), mResId) : mDefaultIcon;
                iconView.setImageDrawable(icon);
            }
        }

        TextView label = rootView.findViewById(R.id.header_label);
        if (label != null) {
            label.setText(headerItem.getName());
        }

        // Recycled holders carry the previous row's visuals; re-sync to this
        // holder's current select level so a recycled row never shows a
        // stale pill, color, or weight.
        if (viewHolder instanceof RowHeaderPresenter.ViewHolder) {
            onSelectLevelChanged((RowHeaderPresenter.ViewHolder) viewHolder);
        }
    }

    @Override
    public void onUnbindViewHolder(Presenter.ViewHolder viewHolder) {
        // NOP: onBindViewHolder re-syncs visuals from the select level.
    }

    // RootTube v2.2: selection is a glass pill + white icon/label + bold
    // label, all driven by the Leanback select level. Leanback's header
    // focus animator calls this with smooth intermediate levels over its
    // 150ms glide, so the pill fades in and the label brightens instead
    // of snapping. No allocation here: cached views, cached typefaces,
    // manual color blend.
    @Override
    protected void onSelectLevelChanged(RowHeaderPresenter.ViewHolder holder) {
        float level = holder.getSelectLevel();
        if (level < 0f) {
            level = 0f;
        } else if (level > 1f) {
            level = 1f;
        }

        Object tag = holder.view.getTag();
        if (!(tag instanceof SidebarHolder)) {
            return;
        }
        SidebarHolder sidebar = (SidebarHolder) tag;

        if (sidebar.pill != null) {
            sidebar.pill.setAlpha((int) (level * 255));
        }
        if (sidebar.icon != null) {
            sidebar.icon.setImageAlpha(UNSELECTED_ALPHA + (int) (level * (255 - UNSELECTED_ALPHA)));
        }
        if (sidebar.label != null) {
            int alpha = 0x99 + (int) (level * (0xFF - 0x99));
            sidebar.label.setTextColor((alpha << 24) | 0x00FFFFFF);
            if (mRegularTypeface != null && mBoldTypeface != null) {
                Typeface target = level >= 0.5f ? mBoldTypeface : mRegularTypeface;
                if (sidebar.label.getTypeface() != target) {
                    sidebar.label.setTypeface(target);
                }
            }
        }
    }

    private final RequestListener<Drawable> mErrorListener = new RequestListener<Drawable>() {
        @Override
        public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
            Log.e(TAG, "Glide load failed: " + e);
            return false;
        }

        @Override
        public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
            return false;
        }
    };
}

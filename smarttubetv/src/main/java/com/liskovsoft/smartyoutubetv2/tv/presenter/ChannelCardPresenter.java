package com.liskovsoft.smartyoutubetv2.tv.presenter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Build.VERSION;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.liskovsoft.sharedutils.helpers.Helpers;
import com.liskovsoft.sharedutils.mylogger.Log;
import com.liskovsoft.smartyoutubetv2.common.app.models.data.Video;
import com.liskovsoft.smartyoutubetv2.common.prefs.MainUIData;
import com.liskovsoft.smartyoutubetv2.tv.R;
import com.liskovsoft.smartyoutubetv2.tv.presenter.base.LongClickPresenter;
import com.liskovsoft.smartyoutubetv2.tv.ui.browse.video.GridFragmentHelper;
import com.liskovsoft.smartyoutubetv2.tv.util.ViewUtil;

public class ChannelCardPresenter extends LongClickPresenter {
    private static final String TAG = VideoCardPresenter.class.getSimpleName();
    // RootTube v2.1 motion: one shared interpolator, no per-event allocation.
    private static final DecelerateInterpolator sFocusInterpolator = new DecelerateInterpolator();
    private static final float FOCUS_SCALE = 1.08f;
    private int mDefaultBackgroundColor;
    private int mDefaultTextColor;
    private int mNewContentBackgroundColor;
    private int mSelectedTextColor;
    private int mWidth;
    private int mHeight;

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent) {
        Context context = parent.getContext();

        mDefaultBackgroundColor =
                ContextCompat.getColor(context, Helpers.getThemeAttr(context, R.attr.cardDefaultBackground));
        mDefaultTextColor =
                ContextCompat.getColor(context, R.color.card_default_text);
        mNewContentBackgroundColor =
                ContextCompat.getColor(context, R.color.dark_red);
        // RootTube v2.1: focused channel cards use the glass focus panel
        // (see the focus listener), not the v1 red panel.
        mSelectedTextColor =
                ContextCompat.getColor(context, R.color.roottube_text_primary);

        updateDimensions(context);

        @SuppressLint("InflateParams")
        View container = LayoutInflater.from(context).inflate(R.layout.channel_card, null);
        container.setBackgroundColor(mDefaultBackgroundColor);
        //if (VERSION.SDK_INT >= 23 && MainUIData.instance(context).isUiTweakEnabled(MainUIData.UI_TWEAK_ROUNDED_CORNERS)) {
        //    container.setForeground(ContextCompat.getDrawable(context, R.drawable.lb_card_outline));
        //}

        TextView textView = container.findViewById(R.id.channel_title);
        textView.setBackgroundColor(mDefaultBackgroundColor);
        textView.setTextColor(mDefaultTextColor);
        applyRoottubeFont(textView, Typeface.BOLD);

        boolean autoScrollEnabled = isCardTextAutoScrollEnabled(context);
        if (autoScrollEnabled) {
            ViewUtil.setTextScrollSpeed(textView, getCardTextScrollSpeed(context));
        }

        container.setOnFocusChangeListener((v, hasFocus) -> {
            // RootTube v2.1 glass focus: the focused channel card gets the
            // glass focus panel and a 1.08 scale-up; the title text goes
            // transparent so the glass shows through. Unfocused cards keep
            // the solid dark background (new-content keeps its red panel).
            boolean isNew = textView.getTag(R.id.channel_new_content) != null;
            if (hasFocus) {
                v.setBackgroundResource(R.drawable.roottube_glass_focus);
                textView.setBackgroundColor(Color.TRANSPARENT);
                textView.setTextColor(mSelectedTextColor);
            } else {
                v.setBackgroundColor(mDefaultBackgroundColor);
                int backgroundColor = isNew ? mNewContentBackgroundColor : mDefaultBackgroundColor;
                textView.setBackgroundColor(backgroundColor);
                textView.setTextColor(mDefaultTextColor);
            }

            float scale = hasFocus ? FOCUS_SCALE : 1.0f;
            v.animate().scaleX(scale).scaleY(scale).setDuration(animFast(v.getContext()))
                    .setInterpolator(sFocusInterpolator).start();

            if (!autoScrollEnabled) {
                return;
            }

            if (hasFocus) {
                ViewUtil.enableMarquee(textView);
            } else {
                ViewUtil.disableMarquee(textView);
            }
        });

        // RootTube v2.1: entrance fade at medium speed on holder creation.
        container.setAlpha(0f);
        container.animate().alpha(1f).setDuration(animMedium(context))
                .setInterpolator(sFocusInterpolator).start();

        return new ViewHolder(container);
    }

    private static int animFast(Context context) {
        try {
            return context.getResources().getInteger(R.integer.roottube_anim_fast);
        } catch (Resources.NotFoundException e) {
            return 120;
        }
    }

    private static int animMedium(Context context) {
        try {
            return context.getResources().getInteger(R.integer.roottube_anim_medium);
        } catch (Resources.NotFoundException e) {
            return 250;
        }
    }

    private static void applyRoottubeFont(TextView textView, int style) {
        try {
            Typeface typeface = ResourcesCompat.getFont(textView.getContext(), R.font.roottube_sans);
            if (typeface != null) {
                textView.setTypeface(typeface, style);
            }
        } catch (Resources.NotFoundException e) {
            // Font not delivered yet; keep the theme default.
        }
    }

    @Override
    public void onBindViewHolder(ViewHolder viewHolder, Object item) {
        super.onBindViewHolder(viewHolder, item);

        Context context = viewHolder.view.getContext();
        Video video = (Video) item;

        ViewUtil.setDimensions(viewHolder.view.findViewById(R.id.channel_card_wrapper), mWidth, -1); // don't do auto height

        TextView textView = viewHolder.view.findViewById(R.id.channel_title);
        textView.setText(video.getTitle());

        // We should setup props each time because object may be reused by the underlying RecyclerView
        textView.setBackgroundColor(video.hasNewContent ? mNewContentBackgroundColor : mDefaultBackgroundColor);
        textView.setTag(R.id.channel_new_content, video.hasNewContent ? true : null);

        // RootTube v2.1: a rebound view that is already focused must show
        // the glass focus state, not the flat new/default background.
        if (viewHolder.view.hasFocus()) {
            viewHolder.view.setBackgroundResource(R.drawable.roottube_glass_focus);
            textView.setBackgroundColor(Color.TRANSPARENT);
            textView.setTextColor(mSelectedTextColor);
        }


        ImageView imageView = viewHolder.view.findViewById(R.id.channel_image);
        imageView.setVisibility(View.VISIBLE);

        Glide.with(context)
                .load(video.cardImageUrl)
                .apply(ViewUtil.glideOptions())
                .listener(mErrorListener)
                //.error(R.drawable.card_placeholder) // R.color.lb_grey
                .into(imageView);
    }

    @Override
    public void onUnbindViewHolder(ViewHolder viewHolder) {
        // RootTube v2.1: recycled views must never keep focus scale or a
        // half-finished entrance fade.
        viewHolder.view.animate().cancel();
        viewHolder.view.setScaleX(1.0f);
        viewHolder.view.setScaleY(1.0f);
        viewHolder.view.setAlpha(1.0f);

        // Remove references to images so that the garbage collector can free up memory.
        ImageView imageView = viewHolder.view.findViewById(R.id.channel_image);
        imageView.setImageDrawable(null);
    }

    private void updateDimensions(Context context) {
        Pair<Integer, Integer> dimens = getCardDimensPx(context);

        mWidth = dimens.first;
        mHeight = dimens.second;
    }

    protected Pair<Integer, Integer> getCardDimensPx(Context context) {
        return GridFragmentHelper.getCardDimensPx(
                context, R.dimen.channel_card_width,
                R.dimen.channel_card_height,
                MainUIData.instance(context).getVideoGridScale(),
                true);
    }

    protected boolean isCardTextAutoScrollEnabled(Context context) {
        return MainUIData.instance(context).isCardTextAutoScrollEnabled();
    }

    protected float getCardTextScrollSpeed(Context context) {
        return MainUIData.instance(context).getCardTextScrollSpeed();
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

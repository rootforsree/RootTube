package com.liskovsoft.smartyoutubetv2.tv.presenter;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Build.VERSION;
import android.util.Pair;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.leanback.widget.Presenter;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.liskovsoft.sharedutils.helpers.Helpers;
import com.liskovsoft.sharedutils.mylogger.Log;
import com.liskovsoft.smartyoutubetv2.common.app.models.data.Video;
import com.liskovsoft.smartyoutubetv2.common.prefs.MainUIData;
import com.liskovsoft.smartyoutubetv2.common.utils.ClickbaitRemover;
import com.liskovsoft.smartyoutubetv2.tv.R;
import com.liskovsoft.smartyoutubetv2.tv.presenter.base.LongClickPresenter;
import com.liskovsoft.smartyoutubetv2.tv.ui.browse.video.GridFragmentHelper;
import com.liskovsoft.smartyoutubetv2.tv.ui.widgets.complexcardview.ComplexImageCardView;
import com.liskovsoft.smartyoutubetv2.tv.util.ViewUtil;

/*
 * A CardPresenter is used to generate Views and bind Objects to them on demand.
 * It contains an Image CardView
 */
public class VideoCardPresenter extends LongClickPresenter {
    private static final String TAG = VideoCardPresenter.class.getSimpleName();
    // RootTube v2.1 motion: one shared interpolator, no per-event allocation.
    private static final DecelerateInterpolator sFocusInterpolator = new DecelerateInterpolator();
    private static final float FOCUS_SCALE = 1.08f;
    private int mDefaultBackgroundColor = -1;
    private int mDefaultTextColor = -1;
    private int mSelectedBackgroundColor = -1;
    private int mSecondaryTextColor = -1;
    private int mCardPreviewType;
    private int mThumbQuality;
    private int mWidth;
    private int mHeight;

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent) {
        Context context = parent.getContext();

        mDefaultBackgroundColor =
            ContextCompat.getColor(context, Helpers.getThemeAttr(context, R.attr.cardDefaultBackground));
        mDefaultTextColor =
                ContextCompat.getColor(context, R.color.card_default_text);
        mSelectedBackgroundColor =
                ContextCompat.getColor(context, Helpers.getThemeAttr(context, R.attr.cardSelectedBackground));
        mSecondaryTextColor =
                ContextCompat.getColor(context, R.color.roottube_text_secondary);

        mCardPreviewType = getCardPreviewType(context);
        mThumbQuality = getThumbQuality(context);

        boolean isCardMultilineTitleEnabled = isCardMultilineTitleEnabled(context);
        boolean isCardMultilineSubtitleEnabled = isCardMultilineSubtitleEnabled(context);
        boolean isCardTextAutoScrollEnabled = isCardTextAutoScrollEnabled(context);
        float cardTextScrollSpeed = getCardTextScrollSpeed(context);

        updateDimensions(context);

        ComplexImageCardView cardView = new ComplexImageCardView(context) {
            @Override
            public void setSelected(boolean selected) {
                updateCardBackgroundColor(this, selected);
                super.setSelected(selected);
                // RootTube v2.1: focused card scales up fast. Unselected
                // cards return to 1.0 so recycled views never keep scale.
                animateFocusScale(this, selected);
            }
        };

        cardView.setTitleLinesNum(isCardMultilineTitleEnabled ? 2 : 1);
        cardView.setContentLinesNum(isCardMultilineSubtitleEnabled ? 2 : 1);
        cardView.enableTextAutoScroll(isCardTextAutoScrollEnabled);
        cardView.setTextScrollSpeed(cardTextScrollSpeed);
        cardView.setFocusable(true);
        cardView.setFocusableInTouchMode(true);
        cardView.enableBadge(isBadgeEnabled());
        cardView.enableTitle(isTitleEnabled());
        cardView.enableContent(isContentEnabled());
        cardView.setBackgroundColor(mDefaultBackgroundColor); // background is temporarily visible during animations
        //if (VERSION.SDK_INT >= 23 && MainUIData.instance(context).isUiTweakEnabled(MainUIData.UI_TWEAK_ROUNDED_CORNERS)) {
        //    cardView.setForeground(ContextCompat.getDrawable(context, R.drawable.lb_card_outline));
        //}
        updateCardBackgroundColor(cardView, false);
        // RootTube v2.1: row/card entrance fade at medium speed. Runs only
        // when a new holder is created, never on scroll rebinds.
        cardView.setAlpha(0f);
        cardView.animate().alpha(1f).setDuration(animMedium(context))
                .setInterpolator(sFocusInterpolator).start();
        return new ViewHolder(cardView);
    }

    private void animateFocusScale(View view, boolean selected) {
        float scale = selected ? FOCUS_SCALE : 1.0f;
        view.animate().scaleX(scale).scaleY(scale).setDuration(animFast(view.getContext()))
                .setInterpolator(sFocusInterpolator).start();
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
        // Font file is delivered by the theme builder; if it is missing at
        // runtime the theme default applies and nothing crashes.
        try {
            Typeface typeface = ResourcesCompat.getFont(textView.getContext(), R.font.roottube_sans);
            if (typeface != null) {
                textView.setTypeface(typeface, style);
            }
        } catch (Resources.NotFoundException e) {
            // Font not delivered yet; keep the theme default.
        }
    }

    private void updateCardBackgroundColor(ComplexImageCardView view, boolean selected) {
        int backgroundColor = selected ? mSelectedBackgroundColor : mDefaultBackgroundColor;

        // Both background colors should be set because the view's
        // background is temporarily visible during animations.
        // NOTE: has visual bug with rounded corners
        //view.setBackgroundColor(backgroundColor);

        View infoField = view.findViewById(R.id.info_field);
        if (infoField != null) {
            infoField.setBackgroundColor(backgroundColor);
        }

        // RootTube v2.1 type: larger titles in the new font, subtitles one
        // step up, both keep their locked colors in both states.
        TextView titleText = view.findViewById(R.id.title_text);
        if (titleText != null) {
            titleText.setTextColor(mDefaultTextColor);
            titleText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
            applyRoottubeFont(titleText, Typeface.BOLD);
        }
        TextView contentText = view.findViewById(R.id.content_text);
        if (contentText != null) {
            contentText.setTextColor(mSecondaryTextColor);
            contentText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
            applyRoottubeFont(contentText, Typeface.NORMAL);
        }
    }

    @Override
    public void onBindViewHolder(Presenter.ViewHolder viewHolder, Object item) {
        super.onBindViewHolder(viewHolder, item);

        Video video = (Video) item;

        ComplexImageCardView cardView = (ComplexImageCardView) viewHolder.view;
        Context context = cardView.getContext();

        cardView.setTitleText(video.getTitle());
        cardView.setContentText(video.getSecondTitle());
        // Count progress that very close to zero. E.g. when user closed video immediately.
        cardView.setProgress(video.percentWatched > 0 && video.percentWatched < 1 ? 1 : Math.round(video.percentWatched));
        cardView.setBadgeText(
                video.hasNewContent ? context.getString(R.string.badge_new_content) :
                video.isLive ? context.getString(R.string.badge_live) :
                video.isShorts ? context.getString(R.string.header_shorts).toUpperCase() :
                video.badge
        );
        cardView.setBadgeColor(video.hasNewContent || video.isLive || video.isUpcoming ?
                ContextCompat.getColor(context, R.color.dark_red) : ContextCompat.getColor(context, R.color.black));

        if (mCardPreviewType != MainUIData.CARD_PREVIEW_DISABLED) {
            cardView.setPreview(video);
            cardView.setMute(mCardPreviewType == MainUIData.CARD_PREVIEW_MUTED);
        }

        cardView.setMainImageDimensions(mWidth, mHeight);

        if (context instanceof Activity && ((Activity) context).isDestroyed()) {
            // Glide.with(context): IllegalArgumentException: You cannot start a load for a destroyed activity
            return;
        }

        Glide.with(context)
                //.asBitmap() // disable animation (webp, gif)
                .load(ClickbaitRemover.updateThumbnail(video, mThumbQuality))
                //.placeholder(mDefaultCardImage)
                .apply(ViewUtil.glideOptions())
                // improve image compression on low end devices
                .override(mWidth, mHeight)
                // com.liskovsoft.smartyoutubetv2.tv.util.CacheGlideModule
                // Cache makes app crashing on old android versions
                .diskCacheStrategy(VERSION.SDK_INT > 21 ? DiskCacheStrategy.ALL : DiskCacheStrategy.NONE)
                .listener(mErrorListener)
                .error(
                    // Updated thumbnail url not found
                    Glide.with(context)
                        .load(video.cardImageUrl) // always working
                        //.placeholder(mDefaultCardImage)
                        .apply(ViewUtil.glideOptions())
                        .listener(mErrorListener)
                        .error(R.drawable.card_placeholder) // R.color.lb_grey
                )
                .into(cardView.getMainImageView());
    }

    @Override
    public void onUnbindViewHolder(Presenter.ViewHolder viewHolder) {
        super.onUnbindViewHolder(viewHolder);

        ComplexImageCardView cardView = (ComplexImageCardView) viewHolder.view;

        // RootTube v2.1: recycled views must never keep focus scale or a
        // half-finished entrance fade.
        cardView.animate().cancel();
        cardView.setScaleX(1.0f);
        cardView.setScaleY(1.0f);
        cardView.setAlpha(1.0f);

        // Remove references to images so that the garbage collector can free up memory.
        cardView.setBadgeImage(null);
        cardView.setMainImage(null);

        // Cleanup Glide resources. https://chatgpt.com/share/682120c5-e428-8010-b848-371b2dec0cd5
        Glide.with(cardView.getContext().getApplicationContext()).clear(cardView.getMainImageView());
    }

    private void updateDimensions(Context context) {
        Pair<Integer, Integer> dimens = getCardDimensPx(context);

        mWidth = dimens.first;
        mHeight = dimens.second;
    }
    
    protected Pair<Integer, Integer> getCardDimensPx(Context context) {
        return GridFragmentHelper.getCardDimensPx(context, R.dimen.card_width, R.dimen.card_height, MainUIData.instance(context).getVideoGridScale());
    }

    protected boolean isCardTextAutoScrollEnabled(Context context) {
        return MainUIData.instance(context).isCardTextAutoScrollEnabled();
    }

    protected int getCardPreviewType(Context context) {
        return MainUIData.instance(context).getCardPreviewType();
    }

    protected boolean isCardMultilineTitleEnabled(Context context) {
        return MainUIData.instance(context).isCardMultilineTitleEnabled();
    }

    protected boolean isCardMultilineSubtitleEnabled(Context context) {
        return MainUIData.instance(context).isCardMultilineSubtitleEnabled();
    }

    protected float getCardTextScrollSpeed(Context context) {
        return MainUIData.instance(context).getCardTextScrollSpeed();
    }

    protected int getThumbQuality(Context context) {
        return MainUIData.instance(context).getThumbQuality();
    }

    protected boolean isContentEnabled() {
        return true;
    }

    protected boolean isTitleEnabled() {
        return true;
    }

    protected boolean isBadgeEnabled() {
        return true;
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

    private final RequestListener<Bitmap> mErrorListener2 = new RequestListener<Bitmap>() {
        @Override
        public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Bitmap> target, boolean isFirstResource) {
            Log.e(TAG, "Glide load failed: " + e);
            return false;
        }

        @Override
        public boolean onResourceReady(Bitmap resource, Object model, Target<Bitmap> target, DataSource dataSource, boolean isFirstResource) {
            return false;
        }
    };
}

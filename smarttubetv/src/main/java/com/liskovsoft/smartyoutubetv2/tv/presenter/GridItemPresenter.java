package com.liskovsoft.smartyoutubetv2.tv.presenter;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.TextView;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.Fragment;
import androidx.leanback.widget.Presenter;
import com.liskovsoft.smartyoutubetv2.tv.R;

public class GridItemPresenter extends Presenter {
    private static final DecelerateInterpolator sFocusInterpolator = new DecelerateInterpolator();
    private static final float FOCUS_SCALE = 1.08f;
    private final Fragment mainFragment;

    public GridItemPresenter(Fragment mainFragment) {
        this.mainFragment = mainFragment;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent) {
        TextView view = new TextView(parent.getContext());

        Resources res = parent.getResources();
        int width = res.getDimensionPixelSize(R.dimen.settings_card_width);
        int height = res.getDimensionPixelSize(R.dimen.settings_card_height);

        view.setLayoutParams(new ViewGroup.LayoutParams(width, height));
        view.setFocusable(true);
        view.setFocusableInTouchMode(true);
        view.setBackgroundColor(ContextCompat.getColor(parent.getContext(),
                R.color.card_default_background));
        view.setTextColor(Color.WHITE);
        view.setGravity(Gravity.CENTER);
        // RootTube v2.1: new font (guarded) and the same focus scale as
        // every other browse card.
        try {
            Typeface typeface = ResourcesCompat.getFont(parent.getContext(), R.font.roottube_sans);
            if (typeface != null) {
                view.setTypeface(typeface, Typeface.BOLD);
            }
        } catch (Resources.NotFoundException e) {
            // Font not delivered yet; keep the theme default.
        }
        view.setOnFocusChangeListener((v, hasFocus) -> {
            float scale = hasFocus ? FOCUS_SCALE : 1.0f;
            int duration = 120;
            try {
                duration = v.getResources().getInteger(R.integer.roottube_anim_fast);
            } catch (Resources.NotFoundException e) {
                // Fall through with 120ms.
            }
            v.animate().scaleX(scale).scaleY(scale).setDuration(duration)
                    .setInterpolator(sFocusInterpolator).start();
        });
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder viewHolder, Object item) {
        ((TextView) viewHolder.view).setText((String) item);
    }

    @Override
    public void onUnbindViewHolder(ViewHolder viewHolder) {
        // Recycled views must never keep focus scale.
        viewHolder.view.animate().cancel();
        viewHolder.view.setScaleX(1.0f);
        viewHolder.view.setScaleY(1.0f);
    }
}

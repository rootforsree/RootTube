/*
 * Copyright (c) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.liskovsoft.smartyoutubetv2.tv.presenter;

import androidx.core.content.res.ResourcesCompat;
import androidx.leanback.widget.AbstractDetailsDescriptionPresenter;

import android.graphics.Typeface;

import com.liskovsoft.smartyoutubetv2.common.app.models.data.Video;
import com.liskovsoft.smartyoutubetv2.tv.R;

public class DetailsDescriptionPresenter extends AbstractDetailsDescriptionPresenter {

    @Override
    protected void onBindDescription(ViewHolder viewHolder, Object item) {
        Video video = (Video) item;

        if (video != null) {
            viewHolder.getTitle().setText(video.getTitle());
            viewHolder.getSubtitle().setText(video.getAuthor());
            viewHolder.getBody().setText(video.getSecondTitle());
        }

        // RootTube v2.1 merge fix: player title/subtitle/body in the app font.
        // Guarded so a missing font can never crash playback.
        try {
            Typeface font = ResourcesCompat.getFont(viewHolder.view.getContext(), R.font.roottube_sans);
            if (font != null) {
                viewHolder.getTitle().setTypeface(Typeface.create(font, Typeface.BOLD));
                viewHolder.getSubtitle().setTypeface(font);
                viewHolder.getBody().setTypeface(font);
            }
        } catch (Exception ignored) {
            // Keep the default typeface when the font is unavailable.
        }
    }
}

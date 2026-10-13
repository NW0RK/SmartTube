package com.liskovsoft.smartyoutubetv2.tv.ui.mobile;

import android.content.Context;
import android.graphics.Typeface;
import android.text.TextUtils;
import android.widget.*;
import com.bumptech.glide.Glide;
import com.liskovsoft.smartyoutubetv2.common.app.models.data.Video;
import com.liskovsoft.smartyoutubetv2.tv.R;

/** Profile fields must belong to the channel, rather than the video used to open it. */
final class MobileChannelHeader {
    static CharSequence name(Context c, Video channel, String id) {
        if (channel != null && !TextUtils.isEmpty(channel.author))
            return channel.author.startsWith("@") ? channel.author : channel.getAuthor();
        if (channel != null && channel.hasVideo() && !TextUtils.isEmpty(channel.getAuthor()))
            return channel.getAuthor();
        if (channel != null && channel.isChannel() && !TextUtils.isEmpty(channel.getTitle())) return channel.getTitle();
        return id != null && id.startsWith("@") ? id : c.getString(R.string.header_channels);
    }

    static LinearLayout create(Context c, Video channel, String id) {
        LinearLayout header = MobileUi.column(c);
        header.setPadding(0, MobileUi.dp(c, 16), 0, MobileUi.dp(c, 20));
        if (channel != null && channel.isChannel() && !TextUtils.isEmpty(channel.cardImageUrl)) {
            ImageView avatar = new ImageView(c);
            avatar.setImportantForAccessibility(android.view.View.IMPORTANT_FOR_ACCESSIBILITY_NO);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(MobileUi.dp(c, 80), MobileUi.dp(c, 80));
            params.setMargins(MobileUi.dp(c, 16), 0, 0, MobileUi.dp(c, 4));
            header.addView(avatar, params);
            Glide.with(avatar).load(channel.cardImageUrl).circleCrop().into(avatar);
        }
        TextView title = MobileUi.text(c, name(c, channel, id), 28);
        title.setTypeface(null, Typeface.BOLD);
        header.addView(title);
        if (id != null && id.startsWith("@") && !id.contentEquals(title.getText()))
            detail(header, id);
        if (channel != null && !TextUtils.isEmpty(channel.subscriberCount))
            detail(header, channel.subscriberCount);
        if (channel != null && channel.isChannel() && !TextUtils.isEmpty(channel.description))
            detail(header, channel.description);
        return header;
    }

    private static void detail(LinearLayout header, CharSequence value) {
        TextView text = MobileUi.text(header.getContext(), value, 14);
        text.setTextColor(header.getContext().getColor(R.color.mobile_text_secondary));
        text.setPaddingRelative(MobileUi.dp(header.getContext(), 16), 0,
                MobileUi.dp(header.getContext(), 16), MobileUi.dp(header.getContext(), 8));
        header.addView(text);
    }
}

package com.liskovsoft.smartyoutubetv2.tv.ui.mobile;

import com.liskovsoft.smartyoutubetv2.common.exoplayer.selector.FormatItem;
import java.util.List;

/** Keeps caption decoding separate from the user's visibility preference. */
final class MobileCaptions {
    private boolean enabled;
    private boolean prepared;

    boolean isEnabled() { return enabled; }

    void reset() {
        enabled = false;
        prepared = false;
    }

    void select(FormatItem format) {
        enabled = !format.isDefault();
        if (enabled) prepared = true;
    }

    FormatItem prepare(List<FormatItem> available, List<FormatItem> saved) {
        if (prepared) return null;
        FormatItem preferred = preferred(available, saved);
        if (preferred.isDefault()) return null;
        prepared = true;
        return preferred;
    }

    static FormatItem preferred(List<FormatItem> available, List<FormatItem> saved) {
        if (available != null) {
            if (saved != null) {
                for (FormatItem previous : saved) {
                    for (FormatItem format : available) {
                        if (!format.isDefault() && previous.equals(format)) return format;
                    }
                }
            }
            for (FormatItem format : available) {
                if (!format.isDefault()) return format;
            }
        }
        return FormatItem.SUBTITLE_NONE;
    }
}

package com.liskovsoft.smartyoutubetv2.tv.ui.mobile;

import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.RendererCapabilities;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection;
import com.google.android.exoplayer2.trackselection.TrackSelection.Definition;
import com.liskovsoft.smartyoutubetv2.common.exoplayer.selector.TrackSelectorUtil;
import com.liskovsoft.smartyoutubetv2.common.exoplayer.versions.selector.RestoreTrackSelector;

/** Per-item quality selection; never changes the saved TV quality preference. */
final class MobileTrackSelector extends RestoreTrackSelector {
    boolean manual;
    final java.util.Set<String> rejected = new java.util.HashSet<>();

    MobileTrackSelector() { super(new AdaptiveTrackSelection.Factory()); }

    @Override protected Definition selectPreferredVideoTrack(TrackGroupArray groups, int[][] support) {
        if (manual) return null;
        Format best = null;
        int bestGroup = -1, bestTrack = -1;
        for (int g = 0; g < groups.length; g++) {
            for (int t = 0; t < groups.get(g).length; t++) {
                if ((support[g][t] & RendererCapabilities.FORMAT_SUPPORT_MASK) != RendererCapabilities.FORMAT_HANDLED) continue;
                Format candidate = groups.get(g).getFormat(t);
                if (rejected.contains(key(candidate))) continue;
                if (candidate.width <= 0 || candidate.height <= 0) continue;
                if (best == null || compare(candidate, best) < 0) {
                    best = candidate; bestGroup = g; bestTrack = t;
                }
            }
        }
        return best == null ? null : new Definition(groups.get(bestGroup), bestTrack);
    }

    void reject(Format format) {
        if (format != null) { rejected.add(key(format)); invalidate(); }
    }

    private static String key(Format f) {
        return f.id + ":" + f.codecs + ":" + f.width + ":" + f.height + ":" + f.bitrate + ":" + f.frameRate;
    }

    static int compare(Format a, Format b) {
        int rank = Integer.compare(resolutionRank(a), resolutionRank(b));
        if (rank != 0) return rank;
        rank = Integer.compare(a.bitrate > 0 ? a.bitrate : Integer.MAX_VALUE,
                b.bitrate > 0 ? b.bitrate : Integer.MAX_VALUE);
        if (rank != 0) return rank;
        rank = Float.compare(a.frameRate > 0 ? a.frameRate : Float.MAX_VALUE,
                b.frameRate > 0 ? b.frameRate : Float.MAX_VALUE);
        if (rank != 0) return rank;
        rank = Boolean.compare(isHdr(a), isHdr(b));
        if (rank != 0) return rank;
        rank = Integer.compare(codecRank(a), codecRank(b));
        return rank != 0 ? rank : String.valueOf(a.id).compareTo(String.valueOf(b.id));
    }

    private static int resolutionRank(Format f) {
        int height = f.width < f.height ? Math.min(f.width, f.height) : TrackSelectorUtil.getRealHeight(f);
        return height == 1080 ? 0 : height < 1080 ? 1080 - height : height;
    }

    private static boolean isHdr(Format f) {
        return f.colorInfo != null && (f.colorInfo.colorTransfer == 6 || f.colorInfo.colorTransfer == 7);
    }

    private static int codecRank(Format f) {
        String codec = f.codecs == null ? "" : f.codecs;
        return codec.startsWith("av01") ? 0 : codec.startsWith("vp9") || codec.startsWith("vp09") ? 1 : codec.startsWith("avc") ? 2 : 3;
    }
}

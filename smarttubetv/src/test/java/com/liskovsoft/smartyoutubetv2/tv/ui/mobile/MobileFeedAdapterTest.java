package com.liskovsoft.smartyoutubetv2.tv.ui.mobile;

import static org.junit.Assert.*;

import com.liskovsoft.smartyoutubetv2.common.app.models.data.Video;
import com.liskovsoft.smartyoutubetv2.common.app.models.data.VideoGroup;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.*;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, sdk = 28)
public class MobileFeedAdapterTest {
    private MobileFeedAdapter adapter() {
        return new MobileFeedAdapter(v -> {}, v -> {}, v -> {});
    }

    private Video video(String id, String author) {
        Video v = new Video();
        v.videoId = id;
        v.author = author;
        return v;
    }

    private VideoGroup group(int id, Video... videos) {
        VideoGroup g = VideoGroup.from(new ArrayList<>(Arrays.asList(videos)));
        g.setId(id);
        g.setTitle("Group " + id);
        return g;
    }

    @Test
    public void cumulativePageDoesNotDuplicateExistingVideos() {
        MobileFeedAdapter adapter = adapter();
        Video first = video("first", "Channel");
        Video second = video("second", "Channel");
        VideoGroup group = group(1, first);
        adapter.update(group);
        group = group(1, first, second);
        adapter.update(group);
        adapter.update(group);
        assertEquals(3, adapter.getItemCount());
        assertEquals(1, adapter.indexOf(first));
        assertEquals(2, adapter.indexOf(second));
    }

    @Test
    public void replaceOnlyReplacesItsGroupAndPrependKeepsOrder() {
        MobileFeedAdapter adapter = adapter();
        Video first = video("first", "A"),
                other = video("other", "B"),
                replacement = video("replacement", "A"),
                prepended = video("prepended", "A");
        adapter.update(group(1, first));
        adapter.update(group(2, other));
        VideoGroup replace = group(1, replacement);
        replace.setAction(VideoGroup.ACTION_REPLACE);
        adapter.update(replace);
        VideoGroup prepend = group(1, prepended);
        prepend.setAction(VideoGroup.ACTION_PREPEND);
        adapter.update(prepend);
        assertEquals(-1, adapter.indexOf(first));
        assertEquals(1, adapter.indexOf(prepended));
        assertEquals(2, adapter.indexOf(replacement));
        assertTrue(adapter.indexOf(other) > adapter.indexOf(replacement));
    }

    @Test
    public void syncFromUnrelatedGroupUpdatesWatchedProgress() {
        MobileFeedAdapter adapter = adapter();
        Video original = video("video", "Channel");
        adapter.update(group(1, original));
        Video fresh = video("video", "Channel");
        fresh.percentWatched = 75;
        VideoGroup sync = group(99, fresh);
        sync.setAction(VideoGroup.ACTION_SYNC);
        adapter.update(sync);
        assertEquals(75, original.percentWatched, 0);
        assertEquals(2, adapter.getItemCount());
    }

    @Test
    public void removeAuthorAppliesAcrossGroups() {
        MobileFeedAdapter adapter = adapter();
        Video a = video("a", "Blocked"), b = video("b", "Blocked"), c = video("c", "Other");
        adapter.update(group(1, a));
        adapter.update(group(2, b, c));
        VideoGroup remove = group(99, video("unused", "Blocked"));
        remove.setAction(VideoGroup.ACTION_REMOVE_AUTHOR);
        adapter.update(remove);
        assertEquals(-1, adapter.indexOf(a));
        assertEquals(-1, adapter.indexOf(b));
        assertTrue(adapter.indexOf(c) >= 0);
    }
}

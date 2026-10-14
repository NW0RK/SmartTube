package com.liskovsoft.smartyoutubetv2.tv.ui.mobile;

import android.view.*;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.liskovsoft.smartyoutubetv2.common.app.models.data.*;
import com.liskovsoft.smartyoutubetv2.tv.R;

import java.util.*;
import java.util.function.Consumer;

/** Keeps groups strongly referenced: Video's pagination group is a weak reference. */
public final class MobileFeedAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private final List<VideoGroup> groups = new ArrayList<>();
    private final List<VideoGroup> retainedPages = new ArrayList<>();
    private final Map<Integer, Integer> requestedSizes = new HashMap<>();
    private final Map<Integer, List<Video>> videos = new LinkedHashMap<>();
    private View header;
    private boolean compactCards;

    void setCompactCards(boolean compact) {
        compactCards = compact;
    }
    private final List<Object> rows = new ArrayList<>();
    private final Consumer<Video> click, menu, more;

    MobileFeedAdapter(Consumer<Video> click, Consumer<Video> menu, Consumer<Video> more) {
        this.click = click;
        this.menu = menu;
        this.more = more;
    }

    public void update(VideoGroup group) {
        if (group == null || group.getVideos() == null) return;
        if (group.getAction() == VideoGroup.ACTION_SYNC
                || group.getAction() == VideoGroup.ACTION_REMOVE
                || group.getAction() == VideoGroup.ACTION_REMOVE_AUTHOR) {
            for (List<Video> list : videos.values()) {
                if (group.getAction() == VideoGroup.ACTION_SYNC) {
                    for (Video v : list)
                        for (Video fresh : group.getVideos()) if (v.equals(fresh)) v.sync(fresh);
                } else
                    list.removeIf(
                            v ->
                                    group.getVideos().stream()
                                            .anyMatch(
                                                    x ->
                                                            group.getAction()
                                                                            == VideoGroup
                                                                                    .ACTION_REMOVE
                                                                    ? v.equals(x)
                                                                    : Objects.equals(
                                                                            v.getAuthor(),
                                                                            x.getAuthor())));
            }
            rebuild();
            return;
        }
        if (!retainedPages.contains(group)) retainedPages.add(group);
        List<Video> items = videos.get(group.getId());
        int action = group.getAction();
        if (items == null) {
            int pos = group.getPosition();
            groups.add(pos >= 0 && pos <= groups.size() ? pos : groups.size(), group);
            items = new ArrayList<>();
            videos.put(group.getId(), items);
        }
        if (action == VideoGroup.ACTION_REPLACE) {
            items.clear();
            requestedSizes.remove(group.getId());
        }
        List<Video> added = new ArrayList<>();
        for (Video video : group.getVideos()) {
            if (!items.contains(video) && !added.contains(video)) added.add(video);
        }
        if (action == VideoGroup.ACTION_PREPEND) items.addAll(0, added);
        else items.addAll(added);
        rebuild();
    }

    private void rebuild() {
        rows.clear();
        if (header != null) rows.add(header);
        for (VideoGroup g : groups) {
            List<Video> list = videos.get(g.getId());
            if (list == null || list.isEmpty()) continue;
            if (g.getTitle() != null && !g.getTitle().isEmpty()) rows.add(g.getTitle());
            rows.addAll(list);
        }
        notifyDataSetChanged();
    }

    public void setHeader(View view) {
        header = view;
        rebuild();
    }

    public void clear() {
        groups.clear();
        retainedPages.clear();
        requestedSizes.clear();
        videos.clear();
        rebuild();
    }

    public int groupIndex(VideoGroup g) {
        for (int i = 0; i < groups.size(); i++) if (groups.get(i).getId() == g.getId()) return i;
        return -1;
    }

    public VideoGroup groupAt(int i) {
        return i >= 0 && i < groups.size() ? groups.get(i) : null;
    }

    public int indexOf(Video v) {
        return rows.indexOf(v);
    }

    public boolean isEmpty() {
        return videos.values().stream().allMatch(List::isEmpty);
    }

    public boolean isHeading(int p) {
        return !(rows.get(p) instanceof Video);
    }

    @Override
    public int getItemViewType(int p) {
        return rows.get(p) instanceof View ? 2 : isHeading(p) ? 0 : 1;
    }

    @Override
    public int getItemCount() {
        return rows.size();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int type) {
        if (type == 2) {
            FrameLayout container = new FrameLayout(parent.getContext());
            container.setLayoutParams(new RecyclerView.LayoutParams(-1, -2));
            return new RecyclerView.ViewHolder(container) {};
        }
        if (type == 0)
            return new RecyclerView.ViewHolder(MobileUi.text(parent.getContext(), "", 18)) {};
        Card card = new Card(
                LayoutInflater.from(parent.getContext())
                        .inflate(compactCards ? R.layout.mobile_watch_video_card : R.layout.mobile_video_card, parent, false));
        card.image.setClipToOutline(true);
        if (compactCards) {
            MobileUi.browseFeedback(card.itemView, false);
            MobileUi.pillTouchFeedback(card.menu);
        }
        return card;
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int p) {
        if (rows.get(p) instanceof View) {
            FrameLayout container = (FrameLayout) holder.itemView;
            if (header.getParent() != container) {
                if (header.getParent() != null) ((ViewGroup) header.getParent()).removeView(header);
                container.removeAllViews();
                container.addView(header, new FrameLayout.LayoutParams(-1, -2));
            }
            return;
        }
        if (isHeading(p)) {
            ((TextView) holder.itemView).setText((String) rows.get(p));
            return;
        }
        Card c = (Card) holder;
        Video v = (Video) rows.get(p);
        c.title.setText(v.getTitleFull());
        c.meta.setText(v.getSecondTitleFull());
        if (c.channel != null) bindRecommendationMetadata(c, v);
        c.badge.setText(v.badge);
        c.badge.setVisibility(v.badge == null ? View.GONE : View.VISIBLE);
        Glide.with(c.image).load(v.getCardImageUrl()).centerCrop().into(c.image);
        c.itemView.setOnClickListener(view -> click.accept(v));
        c.itemView.setOnLongClickListener(
                view -> {
                    menu.accept(v);
                    return true;
                });
        c.menu.setOnClickListener(view -> {
            MobileDialogActivity.requestPopup(view, v.getTitle());
            menu.accept(v);
        });
    }

    static void bindRecommendationMetadata(Card card, Video video) {
        String author = video.getAuthor();
        boolean hasAuthor = author != null && !author.trim().isEmpty();
        card.channel.setText(hasAuthor ? author : "");
        card.channel.setVisibility(hasAuthor ? View.VISIBLE : View.GONE);
        CharSequence subtitle = video.getSecondTitleFull();
        List<String> details = new ArrayList<>();
        if (subtitle != null) {
            for (String part : subtitle.toString().split("[•·]")) {
                String text = part.trim();
                if (!text.isEmpty() && (!hasAuthor || !text.equals(author.trim()))) details.add(text);
            }
        }
        card.meta.setText(android.text.TextUtils.join(" · ", details));
        card.meta.setVisibility(details.isEmpty() ? View.GONE : View.VISIBLE);
    }

    @Override
    public void onViewAttachedToWindow(@NonNull RecyclerView.ViewHolder holder) {
        int p = holder.getAdapterPosition();
        if (p < 0 || isHeading(p)) return;
        Video v = (Video) rows.get(p);
        VideoGroup g = v.getGroup();
        if (g != null) {
            List<Video> list = videos.get(g.getId());
            if (list != null
                    && list.indexOf(v) >= list.size() - 3
                    && !Objects.equals(requestedSizes.get(g.getId()), list.size())) {
                requestedSizes.put(g.getId(), list.size());
                holder.itemView.post(() -> more.accept(list.get(list.size() - 1)));
            }
        }
    }

    static final class Card extends RecyclerView.ViewHolder {
        final ImageView image;
        final TextView title, channel, meta, badge;
        final View menu;

        Card(View v) {
            super(v);
            image = v.findViewById(R.id.mobile_thumbnail);
            title = v.findViewById(R.id.mobile_card_title);
            channel = v.findViewById(R.id.mobile_card_channel);
            meta = v.findViewById(R.id.mobile_card_meta);
            badge = v.findViewById(R.id.mobile_duration);
            menu = v.findViewById(R.id.mobile_card_menu);
            if (menu instanceof Button) MobileUi.centerIcon((Button) menu, "more", 22);
        }
    }
}

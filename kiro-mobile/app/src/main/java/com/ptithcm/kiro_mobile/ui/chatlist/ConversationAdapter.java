package com.ptithcm.kiro_mobile.ui.chatlist;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.ptithcm.kiro_mobile.R;
import com.ptithcm.kiro_mobile.config.AppConfig;
import com.ptithcm.kiro_mobile.data.model.chat.Conversation;

import java.util.ArrayList;
import java.util.List;

public class ConversationAdapter extends ListAdapter<Conversation, ConversationAdapter.ViewHolder> {

    public interface OnItemClickListener {
        void onItemClick(Conversation conversation);
    }

    private OnItemClickListener listener;
    /** Full unfiltered list kept for filter operations */
    private List<Conversation> fullList = new ArrayList<>();

    public ConversationAdapter() {
        super(DIFF_CALLBACK);
    }

    public void setOnItemClickListener(OnItemClickListener l) {
        this.listener = l;
    }

    /** Submit a fresh full list and reset filters */
    @Override
    public void submitList(List<Conversation> list) {
        fullList = list != null ? new ArrayList<>(list) : new ArrayList<>();
        super.submitList(list);
    }

    /**
     * Filter the displayed list by search query and chip type.
     * @param query  text to match against conversation name (empty = all)
     * @param type   "all" | "unread" | "groups"
     */
    public void filter(String query, String type) {
        List<Conversation> filtered = new ArrayList<>();
        String q = query == null ? "" : query.trim().toLowerCase();
        for (Conversation c : fullList) {
            // Chip filter
            boolean matchType;
            switch (type) {
                case "unread": matchType = c.getUnreadCount() > 0; break;
                case "groups": matchType = c.isGroup(); break;
                default:       matchType = true; break;
            }
            // Text filter
            boolean matchQuery = q.isEmpty() ||
                    (c.getDisplayName() != null &&
                     c.getDisplayName().toLowerCase().contains(q));
            if (matchType && matchQuery) {
                filtered.add(c);
            }
        }
        super.submitList(filtered);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_conversation, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(getItem(position), listener);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final ImageView ivAvatar;
        private final TextView tvName;
        private final TextView tvLastMessage;
        private final TextView tvTime;
        private final TextView tvUnread;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivAvatar      = itemView.findViewById(R.id.iv_avatar);
            tvName        = itemView.findViewById(R.id.tv_name);
            tvLastMessage = itemView.findViewById(R.id.tv_last_message);
            tvTime        = itemView.findViewById(R.id.tv_time);
            tvUnread      = itemView.findViewById(R.id.tv_unread);
        }

        void bind(Conversation item, OnItemClickListener listener) {
            tvName.setText(item.getDisplayName());
            tvLastMessage.setText(item.getLastMessagePreview());

            // Unread badge
            long unread = item.getUnreadCount();
            if (unread > 0) {
                tvUnread.setVisibility(View.VISIBLE);
                tvUnread.setText(unread > 99 ? "99+" : String.valueOf(unread));
            } else {
                tvUnread.setVisibility(View.GONE);
            }

            // Timestamp
            if (item.getLastMessage() != null && item.getLastMessage().getTimestamp() != null) {
                tvTime.setText(formatTime(item.getLastMessage().getTimestamp()));
            } else {
                tvTime.setText("");
            }

            // Avatar
            String avatarUrl = normalizeUrl(item.getAvatarUrl());
            Glide.with(itemView.getContext())
                    .load(avatarUrl)
                    .circleCrop()
                    .placeholder(R.drawable.ic_avatar_placeholder)
                    .error(R.drawable.ic_avatar_placeholder)
                    .into(ivAvatar);

            itemView.setOnClickListener(v -> {
                if (listener != null) listener.onItemClick(item);
            });
        }

        private String formatTime(String isoTimestamp) {
            try {
                if (isoTimestamp.length() >= 16) {
                    return isoTimestamp.substring(11, 16);
                }
            } catch (Exception ignored) {}
            return "";
        }

        private String normalizeUrl(String url) {
            if (url == null) return null;
            return url.replace("http://localhost:9000", AppConfig.MINIO_PUBLIC_URL)
                      .replace("http://localhost:8080", "http://10.0.2.2:8080");
        }
    }

    private static final DiffUtil.ItemCallback<Conversation> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<Conversation>() {
                @Override
                public boolean areItemsTheSame(@NonNull Conversation a, @NonNull Conversation b) {
                    return a.getConversationId() != null &&
                           a.getConversationId().equals(b.getConversationId());
                }

                @Override
                public boolean areContentsTheSame(@NonNull Conversation a, @NonNull Conversation b) {
                    String aPreview = a.getLastMessagePreview();
                    String bPreview = b.getLastMessagePreview();
                    return aPreview.equals(bPreview) && a.getUnreadCount() == b.getUnreadCount();
                }
            };
}

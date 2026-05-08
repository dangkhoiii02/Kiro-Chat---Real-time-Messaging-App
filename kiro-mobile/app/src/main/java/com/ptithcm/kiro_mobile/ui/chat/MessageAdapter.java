package com.ptithcm.kiro_mobile.ui.chat;

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
import com.ptithcm.kiro_mobile.data.model.chat.LocalMessage;

public class MessageAdapter extends ListAdapter<LocalMessage, RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_MINE   = 1;
    private static final int VIEW_TYPE_THEIRS = 2;

    public interface RetryListener {
        void onRetry(String localId);
    }
    
    public interface ImageClickListener {
        void onImageClick(String url);
    }

    private RetryListener retryListener;
    private ImageClickListener imageClickListener;
    private boolean isGroup = false;

    public MessageAdapter() {
        super(DIFF_CALLBACK);
    }

    public void setRetryListener(RetryListener l) { this.retryListener = l; }
    public void setImageClickListener(ImageClickListener l) { this.imageClickListener = l; }

    public void setGroup(boolean group) {
        isGroup = group;
    }

    @Override
    public int getItemViewType(int position) {
        return getItem(position).isMine() ? VIEW_TYPE_MINE : VIEW_TYPE_THEIRS;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inf = LayoutInflater.from(parent.getContext());
        if (viewType == VIEW_TYPE_MINE) {
            View v = inf.inflate(R.layout.item_message_mine, parent, false);
            return new MineViewHolder(v);
        } else {
            View v = inf.inflate(R.layout.item_message_theirs, parent, false);
            return new TheirsViewHolder(v);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        LocalMessage msg = getItem(position);
        if (holder instanceof MineViewHolder) {
            ((MineViewHolder) holder).bind(msg, retryListener, imageClickListener);
        } else {
            ((TheirsViewHolder) holder).bind(msg, isGroup, imageClickListener);
        }
    }

    // ── Mine ViewHolder ───────────────────────────────────────────────────────

    static class MineViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvContent;
        private final ImageView ivAttachment;
        private final TextView tvTime;
        private final TextView tvStatus;
        private final View     retryBtn;

        MineViewHolder(@NonNull View v) {
            super(v);
            tvContent = v.findViewById(R.id.tv_content);
            ivAttachment = v.findViewById(R.id.iv_attachment);
            tvTime    = v.findViewById(R.id.tv_time);
            tvStatus  = v.findViewById(R.id.tv_status);
            retryBtn  = v.findViewById(R.id.btn_retry);
        }

        void bind(LocalMessage msg, RetryListener retryListener, ImageClickListener imageClickListener) {
            if (msg.getContent() != null && !msg.getContent().isEmpty()) {
                tvContent.setVisibility(View.VISIBLE);
                tvContent.setText(msg.getContent());
            } else {
                tvContent.setVisibility(View.GONE);
            }

            if ("image".equals(msg.getType()) && msg.getMediaUrl() != null && !msg.getMediaUrl().isEmpty()) {
                ivAttachment.setVisibility(View.VISIBLE);
                String rawUrl = msg.getMediaUrl();
                String url = rawUrl.startsWith("http") ? rawUrl : AppConfig.MINIO_PUBLIC_URL + rawUrl;
                url = url.replace("http://localhost:9000", AppConfig.MINIO_PUBLIC_URL)
                         .replace("http://localhost:8080", "http://10.0.2.2:8080");
                final String finalUrl = url;
                Glide.with(itemView.getContext())
                        .load(url)
                        .placeholder(android.R.color.darker_gray)
                        .into(ivAttachment);
                        
                ivAttachment.setOnClickListener(v -> {
                    if (imageClickListener != null) imageClickListener.onImageClick(finalUrl);
                });
            } else {
                ivAttachment.setVisibility(View.GONE);
                ivAttachment.setOnClickListener(null);
            }

            tvTime.setText(formatTime(msg.getTimestamp()));

            switch (msg.getStatus()) {
                case LOCAL_PENDING:
                    tvStatus.setText("⏳");
                    retryBtn.setVisibility(View.GONE);
                    break;
                case LOCAL_FAILED:
                    tvStatus.setText("❌");
                    retryBtn.setVisibility(View.VISIBLE);
                    retryBtn.setOnClickListener(v -> {
                        if (retryListener != null) retryListener.onRetry(msg.getLocalId());
                    });
                    break;
                case SENT:
                    tvStatus.setText("✓");
                    retryBtn.setVisibility(View.GONE);
                    break;
                case DELIVERED:
                    tvStatus.setText("✓✓");
                    retryBtn.setVisibility(View.GONE);
                    break;
                case SEEN:
                    tvStatus.setText("✓✓");
                    tvStatus.setTextColor(0xFF1A73E8);
                    retryBtn.setVisibility(View.GONE);
                    break;
            }
        }
    }

    // ── Theirs ViewHolder ─────────────────────────────────────────────────────

    static class TheirsViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvContent;
        private final ImageView ivAttachment;
        private final TextView tvTime;
        private final ImageView ivAvatar;
        private final TextView tvSenderName;

        TheirsViewHolder(@NonNull View v) {
            super(v);
            tvContent = v.findViewById(R.id.tv_content);
            ivAttachment = v.findViewById(R.id.iv_attachment);
            tvTime    = v.findViewById(R.id.tv_time);
            ivAvatar  = v.findViewById(R.id.iv_avatar);
            tvSenderName = v.findViewById(R.id.tv_sender_name);
        }

        void bind(LocalMessage msg, boolean isGroup, ImageClickListener imageClickListener) {
            if (msg.getContent() != null && !msg.getContent().isEmpty()) {
                tvContent.setVisibility(View.VISIBLE);
                tvContent.setText(msg.getContent());
            } else {
                tvContent.setVisibility(View.GONE);
            }

            if ("image".equals(msg.getType()) && msg.getMediaUrl() != null && !msg.getMediaUrl().isEmpty()) {
                ivAttachment.setVisibility(View.VISIBLE);
                String rawUrl = msg.getMediaUrl();
                String url = rawUrl.startsWith("http") ? rawUrl : AppConfig.MINIO_PUBLIC_URL + rawUrl;
                url = url.replace("http://localhost:9000", AppConfig.MINIO_PUBLIC_URL)
                         .replace("http://localhost:8080", "http://10.0.2.2:8080");
                final String finalUrl = url;
                Glide.with(itemView.getContext())
                        .load(url)
                        .placeholder(android.R.color.darker_gray)
                        .into(ivAttachment);
                        
                ivAttachment.setOnClickListener(v -> {
                    if (imageClickListener != null) imageClickListener.onImageClick(finalUrl);
                });
            } else {
                ivAttachment.setVisibility(View.GONE);
                ivAttachment.setOnClickListener(null);
            }

            tvTime.setText(formatTime(msg.getTimestamp()));

            if (isGroup) {
                ivAvatar.setVisibility(View.VISIBLE);
                tvSenderName.setVisibility(View.VISIBLE);
                
                // Set sender name
                String senderName = msg.getSenderId(); // Fallback to ID if no name
                // In a real app we'd get sender info from the message or a user cache
                tvSenderName.setText(senderName);

                // Placeholder avatar for group member
                Glide.with(itemView.getContext())
                        .load(R.drawable.ic_avatar_placeholder) // Would use actual avatar URL
                        .circleCrop()
                        .into(ivAvatar);
            } else {
                ivAvatar.setVisibility(View.GONE);
                tvSenderName.setVisibility(View.GONE);
            }
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    static String formatTime(String timestamp) {
        if (timestamp == null) return "";
        try {
            // ISO-8601 from server: "2026-05-04T13:45:00Z" → "13:45"
            if (timestamp.contains("T") && timestamp.length() >= 16) {
                return timestamp.substring(11, 16);
            }
            // Epoch millis from local pending
            long millis = Long.parseLong(timestamp);
            java.util.Calendar cal = java.util.Calendar.getInstance();
            cal.setTimeInMillis(millis);
            return String.format(java.util.Locale.getDefault(), "%02d:%02d",
                    cal.get(java.util.Calendar.HOUR_OF_DAY),
                    cal.get(java.util.Calendar.MINUTE));
        } catch (Exception e) {
            return "";
        }
    }

    private static final DiffUtil.ItemCallback<LocalMessage> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<LocalMessage>() {
                @Override
                public boolean areItemsTheSame(@NonNull LocalMessage a, @NonNull LocalMessage b) {
                    return a.getLocalId().equals(b.getLocalId());
                }

                @Override
                public boolean areContentsTheSame(@NonNull LocalMessage a, @NonNull LocalMessage b) {
                    return a.getStatus() == b.getStatus() &&
                           equals(a.getContent(), b.getContent()) &&
                           equals(a.getMessageId(), b.getMessageId());
                }

                private boolean equals(String x, String y) {
                    return x == null ? y == null : x.equals(y);
                }
            };
}

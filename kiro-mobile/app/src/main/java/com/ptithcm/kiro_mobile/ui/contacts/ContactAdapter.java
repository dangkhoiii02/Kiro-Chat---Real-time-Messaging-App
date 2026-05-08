package com.ptithcm.kiro_mobile.ui.contacts;

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
import com.google.android.material.button.MaterialButton;
import com.ptithcm.kiro_mobile.R;
import com.ptithcm.kiro_mobile.config.AppConfig;
import com.ptithcm.kiro_mobile.data.model.user.PublicProfile;
import com.ptithcm.kiro_mobile.util.ImageLoader;

public class ContactAdapter extends ListAdapter<PublicProfile, ContactAdapter.ViewHolder> {

    public enum Mode { FRIENDS, SEARCH, REQUESTS }

    public interface ActionListener {
        void onAddFriend(PublicProfile profile);
        void onUnsendRequest(PublicProfile profile);
        void onAcceptRequest(PublicProfile profile);
        void onRejectRequest(PublicProfile profile);
        void onRemoveFriend(PublicProfile profile);
        void onMessage(PublicProfile profile);
        void onItemClick(PublicProfile profile);
    }

    private Mode mode = Mode.FRIENDS;
    private ActionListener listener;

    public ContactAdapter() {
        super(DIFF_CALLBACK);
    }

    public void setMode(Mode mode) { this.mode = mode; }
    public void setActionListener(ActionListener l) { this.listener = l; }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_contact, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(getItem(position), mode, listener);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final ImageView ivAvatar;
        private final TextView tvName;
        private final TextView tvUsername;
        private final MaterialButton btnAction;
        private final MaterialButton btnSecondaryAction;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivAvatar   = itemView.findViewById(R.id.iv_avatar);
            tvName     = itemView.findViewById(R.id.tv_name);
            tvUsername = itemView.findViewById(R.id.tv_username);
            btnAction  = itemView.findViewById(R.id.btn_action);
            btnSecondaryAction = itemView.findViewById(R.id.btn_secondary_action);
        }

        void bind(PublicProfile profile, Mode mode, ActionListener listener) {
            tvName.setText(profile.getDisplayName());
            tvUsername.setText("@" + (profile.getUsername() != null ? profile.getUsername() : ""));

            String avatarUrl = normalizeUrl(profile.getProfilePictureUrl());
            ImageLoader.loadAvatar(itemView.getContext(), avatarUrl, ivAvatar);
            btnAction.setVisibility(View.GONE);
            btnSecondaryAction.setVisibility(View.GONE);

            switch (mode) {
                case SEARCH:
                    configureSearchActions(profile, listener);
                    break;
                case REQUESTS:
                    btnAction.setVisibility(View.VISIBLE);
                    btnAction.setText("Chấp nhận");
                    btnAction.setOnClickListener(v -> {
                        if (listener != null) listener.onAcceptRequest(profile);
                    });
                    btnSecondaryAction.setVisibility(View.VISIBLE);
                    btnSecondaryAction.setText("Từ chối");
                    btnSecondaryAction.setOnClickListener(v -> {
                        if (listener != null) listener.onRejectRequest(profile);
                    });
                    break;
                case FRIENDS:
                    btnAction.setVisibility(View.VISIBLE);
                    btnAction.setText("Nhắn");
                    btnAction.setOnClickListener(v -> {
                        if (listener != null) listener.onMessage(profile);
                    });
                    btnSecondaryAction.setVisibility(View.VISIBLE);
                    btnSecondaryAction.setText("Huỷ");
                    btnSecondaryAction.setOnClickListener(v -> {
                        if (listener != null) listener.onRemoveFriend(profile);
                    });
                    break;
            }

            itemView.setOnClickListener(v -> {
                if (listener != null) listener.onItemClick(profile);
            });
        }

        private String normalizeUrl(String url) {
            if (url == null) return null;
            return url.replace("http://localhost:9000", AppConfig.MINIO_PUBLIC_URL)
                      .replace("http://localhost:8080", "http://10.0.2.2:8080");
        }

        private void configureSearchActions(PublicProfile profile, ActionListener listener) {
            String status = profile.getFriendshipStatus();
            if ("FRIENDS".equalsIgnoreCase(status)) {
                btnAction.setVisibility(View.VISIBLE);
                btnAction.setText("Nhắn");
                btnAction.setOnClickListener(v -> {
                    if (listener != null) listener.onMessage(profile);
                });
            } else if ("FRIEND_REQUEST_SENT".equalsIgnoreCase(status)) {
                btnAction.setVisibility(View.VISIBLE);
                btnAction.setText("Huỷ lời mời");
                btnAction.setOnClickListener(v -> {
                    if (listener != null) listener.onUnsendRequest(profile);
                });
            } else if ("FRIEND_REQUEST_RECEIVED".equalsIgnoreCase(status)) {
                btnAction.setVisibility(View.VISIBLE);
                btnAction.setText("Chấp nhận");
                btnAction.setOnClickListener(v -> {
                    if (listener != null) listener.onAcceptRequest(profile);
                });
            } else if ("BLOCKED".equalsIgnoreCase(status) || "BLOCKED_BY".equalsIgnoreCase(status)) {
                btnAction.setVisibility(View.GONE);
            } else {
                btnAction.setVisibility(View.VISIBLE);
                btnAction.setText("Kết bạn");
                btnAction.setOnClickListener(v -> {
                    if (listener != null) listener.onAddFriend(profile);
                });
            }
        }
    }

    private static final DiffUtil.ItemCallback<PublicProfile> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<PublicProfile>() {
                @Override
                public boolean areItemsTheSame(@NonNull PublicProfile a, @NonNull PublicProfile b) {
                    return a.getUserId() != null && a.getUserId().equals(b.getUserId());
                }

                @Override
                public boolean areContentsTheSame(@NonNull PublicProfile a, @NonNull PublicProfile b) {
                    return a.getDisplayName().equals(b.getDisplayName());
                }
            };
}

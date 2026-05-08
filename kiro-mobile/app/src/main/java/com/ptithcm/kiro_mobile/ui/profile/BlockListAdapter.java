package com.ptithcm.kiro_mobile.ui.profile;

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
import com.ptithcm.kiro_mobile.data.model.contact.BlockedUser;
import com.ptithcm.kiro_mobile.util.ImageLoader;

public class BlockListAdapter extends ListAdapter<BlockedUser, BlockListAdapter.ViewHolder> {

    public interface OnUnblockClickListener {
        void onUnblockClick(BlockedUser user);
    }

    private final OnUnblockClickListener listener;

    public BlockListAdapter(OnUnblockClickListener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_blocked_user, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(getItem(position), listener);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final ImageView ivAvatar;
        private final TextView tvName;
        private final View btnUnblock;

        ViewHolder(@NonNull View v) {
            super(v);
            ivAvatar = v.findViewById(R.id.iv_avatar);
            tvName = v.findViewById(R.id.tv_name);
            btnUnblock = v.findViewById(R.id.btn_unblock);
        }

        void bind(BlockedUser user, OnUnblockClickListener listener) {
            // BlockedUser has: userId, username, avatarUrl, blockedAt
            String displayName = user.getUsername() != null ? user.getUsername() : "Unknown";
            tvName.setText(displayName);

            String avatarUrl = user.getAvatarUrl();
            if (avatarUrl != null) {
                avatarUrl = avatarUrl.replace("http://localhost:9000", AppConfig.MINIO_PUBLIC_URL)
                        .replace("http://localhost:8080", "http://10.0.2.2:8080");
            }
            Glide.with(itemView.getContext())
                    .load(avatarUrl)
                    .circleCrop()
                    .placeholder(R.drawable.ic_avatar_placeholder)
                    .into(ivAvatar);

            btnUnblock.setOnClickListener(v -> {
                if (listener != null) listener.onUnblockClick(user);
            });
        }
    }

    private static final DiffUtil.ItemCallback<BlockedUser> DIFF_CALLBACK = new DiffUtil.ItemCallback<BlockedUser>() {
        @Override
        public boolean areItemsTheSame(@NonNull BlockedUser oldItem, @NonNull BlockedUser newItem) {
            return oldItem.getUserId() != null && oldItem.getUserId().equals(newItem.getUserId());
        }

        @Override
        public boolean areContentsTheSame(@NonNull BlockedUser oldItem, @NonNull BlockedUser newItem) {
            return true;
        }
    };
}

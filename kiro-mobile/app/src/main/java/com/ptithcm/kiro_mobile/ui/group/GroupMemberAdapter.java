package com.ptithcm.kiro_mobile.ui.group;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.ptithcm.kiro_mobile.R;
import com.ptithcm.kiro_mobile.config.AppConfig;
import com.ptithcm.kiro_mobile.data.model.user.PublicProfile;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class GroupMemberAdapter extends ListAdapter<PublicProfile, GroupMemberAdapter.ViewHolder> {
    private final Set<String> selectedIds = new HashSet<>();

    public GroupMemberAdapter() {
        super(DIFF_CALLBACK);
    }

    public List<String> getSelectedIds() {
        return new ArrayList<>(selectedIds);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_group_member, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(getItem(position), selectedIds);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final ImageView ivAvatar;
        private final TextView tvName;
        private final TextView tvUsername;
        private final CheckBox cbSelect;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivAvatar = itemView.findViewById(R.id.iv_avatar);
            tvName = itemView.findViewById(R.id.tv_name);
            tvUsername = itemView.findViewById(R.id.tv_username);
            cbSelect = itemView.findViewById(R.id.cb_select);
        }

        void bind(PublicProfile profile, Set<String> selectedIds) {
            tvName.setText(profile.getDisplayName());
            tvUsername.setText("@" + (profile.getUsername() != null ? profile.getUsername() : ""));
            String userId = profile.getUserId();
            cbSelect.setOnCheckedChangeListener(null);
            cbSelect.setChecked(userId != null && selectedIds.contains(userId));

            String avatarUrl = normalizeUrl(profile.getProfilePictureUrl());
            Glide.with(itemView.getContext())
                    .load(avatarUrl)
                    .circleCrop()
                    .placeholder(R.drawable.ic_avatar_placeholder)
                    .error(R.drawable.ic_avatar_placeholder)
                    .into(ivAvatar);

            View.OnClickListener toggle = v -> {
                if (userId == null) return;
                if (selectedIds.contains(userId)) selectedIds.remove(userId);
                else selectedIds.add(userId);
                cbSelect.setChecked(selectedIds.contains(userId));
            };
            itemView.setOnClickListener(toggle);
            cbSelect.setOnClickListener(toggle);
        }

        private String normalizeUrl(String url) {
            if (url == null) return null;
            return url.replace("http://localhost:9000", AppConfig.MINIO_PUBLIC_URL);
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

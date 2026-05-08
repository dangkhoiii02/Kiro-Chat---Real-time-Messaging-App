package com.ptithcm.kiro_mobile.ui.group;

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
import com.ptithcm.kiro_mobile.data.model.group.GroupParticipant;
import com.ptithcm.kiro_mobile.util.ImageLoader;

public class GroupParticipantAdapter extends ListAdapter<GroupParticipant, GroupParticipantAdapter.ViewHolder> {

    public interface OnRemoveClickListener {
        void onRemoveClick(GroupParticipant participant);
    }

    private final OnRemoveClickListener listener;
    private final boolean isCurrentUserOwner;

    public GroupParticipantAdapter(boolean isCurrentUserOwner, OnRemoveClickListener listener) {
        super(DIFF_CALLBACK);
        this.isCurrentUserOwner = isCurrentUserOwner;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_group_participant, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(getItem(position), isCurrentUserOwner, listener);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final ImageView ivAvatar;
        private final TextView tvName;
        private final TextView tvRole;
        private final ImageView btnRemove;

        ViewHolder(@NonNull View v) {
            super(v);
            ivAvatar = v.findViewById(R.id.iv_avatar);
            tvName = v.findViewById(R.id.tv_name);
            tvRole = v.findViewById(R.id.tv_role);
            btnRemove = v.findViewById(R.id.btn_remove);
        }

        void bind(GroupParticipant item, boolean isCurrentUserOwner, OnRemoveClickListener listener) {
            if (item.getParticipantUser() != null) {
                tvName.setText(item.getParticipantUser().getDisplayName());
                ImageLoader.loadAvatar(itemView.getContext(),
                        item.getParticipantUser().getProfilePictureUrl(), ivAvatar);
            }
            
            boolean isOwner = "OWNER".equalsIgnoreCase(item.getRole());
            if (isOwner) {
                tvRole.setText("Trưởng nhóm");
                tvRole.setVisibility(View.VISIBLE);
                btnRemove.setVisibility(View.GONE);
            } else {
                tvRole.setVisibility(View.GONE);
                if (isCurrentUserOwner) {
                    btnRemove.setVisibility(View.VISIBLE);
                } else {
                    btnRemove.setVisibility(View.GONE);
                }
            }

            btnRemove.setOnClickListener(v -> {
                if (listener != null) listener.onRemoveClick(item);
            });
        }
    }

    private static final DiffUtil.ItemCallback<GroupParticipant> DIFF_CALLBACK = new DiffUtil.ItemCallback<GroupParticipant>() {
        @Override
        public boolean areItemsTheSame(@NonNull GroupParticipant oldItem, @NonNull GroupParticipant newItem) {
            if (oldItem.getParticipantUser() != null && newItem.getParticipantUser() != null) {
                return oldItem.getParticipantUser().getUserId().equals(newItem.getParticipantUser().getUserId());
            }
            return false;
        }

        @Override
        public boolean areContentsTheSame(@NonNull GroupParticipant oldItem, @NonNull GroupParticipant newItem) {
            return true;
        }
    };
}

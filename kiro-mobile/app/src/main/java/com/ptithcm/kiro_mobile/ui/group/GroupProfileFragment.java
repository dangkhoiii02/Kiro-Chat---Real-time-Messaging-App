package com.ptithcm.kiro_mobile.ui.group;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.appbar.MaterialToolbar;
import com.ptithcm.kiro_mobile.R;
import com.ptithcm.kiro_mobile.config.AppConfig;
import com.ptithcm.kiro_mobile.data.model.group.GroupProfile;
import com.ptithcm.kiro_mobile.ui.main.MainActivity;
import com.ptithcm.kiro_mobile.data.auth.TokenManager;
import com.ptithcm.kiro_mobile.ui.profile.ProfileViewModel;
import com.ptithcm.kiro_mobile.data.model.group.GroupParticipant;

import java.util.List;

public class GroupProfileFragment extends Fragment {

    public static final String ARG_GROUP_ID = "groupId";

    private GroupProfileViewModel viewModel;
    private GroupParticipantAdapter adapter;
    private String groupId;
    private String currentUserId;

    private ImageView ivAvatar;
    private TextView tvGroupName;
    private TextView tvMemberCount;
    private RecyclerView recyclerParticipants;
    private ProgressBar progress;

    public static GroupProfileFragment newInstance(String groupId) {
        GroupProfileFragment f = new GroupProfileFragment();
        Bundle args = new Bundle();
        args.putString(ARG_GROUP_ID, groupId);
        f.setArguments(args);
        return f;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_group_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getArguments() != null) {
            groupId = getArguments().getString(ARG_GROUP_ID);
        }

        currentUserId = null; // will be set from ProfileViewModel

        // Get current user ID from activity-scoped ProfileViewModel
        new ViewModelProvider(requireActivity())
                .get(ProfileViewModel.class)
                .getProfileResult()
                .observe(getViewLifecycleOwner(), result -> {
                    if (result != null && result.isSuccess() && result.getData() != null) {
                        currentUserId = result.getData().getUserId();
                    }
                });

        MaterialToolbar toolbar = view.findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> requireActivity().getSupportFragmentManager().popBackStack());

        ivAvatar = view.findViewById(R.id.iv_avatar);
        tvGroupName = view.findViewById(R.id.tv_group_name);
        tvMemberCount = view.findViewById(R.id.tv_member_count);
        recyclerParticipants = view.findViewById(R.id.recycler_participants);
        progress = view.findViewById(R.id.progress);

        viewModel = new ViewModelProvider(this).get(GroupProfileViewModel.class);

        // We will initialize the adapter once we know if current user is owner
        // For now, init with false. Will recreate if needed.
        adapter = new GroupParticipantAdapter(false, participant -> {
            Toast.makeText(requireContext(), "Tính năng xóa thành viên đang phát triển", Toast.LENGTH_SHORT).show();
        });
        recyclerParticipants.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerParticipants.setAdapter(adapter);

        observeViewModel();
        if (groupId != null) {
            viewModel.loadGroupInfo(groupId);
        }
    }

    private void observeViewModel() {
        viewModel.getGroupProfile().observe(getViewLifecycleOwner(), result -> {
            if (result.isLoading()) {
                progress.setVisibility(View.VISIBLE);
            } else if (result.isSuccess() && result.getData() != null) {
                progress.setVisibility(View.GONE);
                GroupProfile profile = result.getData();
                tvGroupName.setText(profile.getGroupName() != null ? profile.getGroupName() : "Nhóm");
                tvMemberCount.setText(profile.getMembersCount() + " Thành viên");

                String avatarUrl = profile.getProfileImage();
                if (avatarUrl != null) {
                    avatarUrl = avatarUrl.replace("http://localhost:9000", AppConfig.MINIO_PUBLIC_URL)
                            .replace("http://localhost:8080", "http://10.0.2.2:8080");
                }
                Glide.with(this)
                        .load(avatarUrl)
                        .circleCrop()
                        .placeholder(R.drawable.ic_avatar_placeholder)
                        .into(ivAvatar);
            } else if (result.isError()) {
                progress.setVisibility(View.GONE);
                Toast.makeText(requireContext(), result.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

        viewModel.getGroupParticipants().observe(getViewLifecycleOwner(), result -> {
            if (result.isSuccess() && result.getData() != null && result.getData().getParticipants() != null) {
                List<GroupParticipant> participants = result.getData().getParticipants();
                
                boolean isOwner = false;
                if (currentUserId != null) {
                    for (GroupParticipant p : participants) {
                        if (p.getParticipantUser() != null && currentUserId.equals(p.getParticipantUser().getUserId())) {
                            if ("OWNER".equalsIgnoreCase(p.getRole())) {
                                isOwner = true;
                            }
                            break;
                        }
                    }
                }

                // Recreate adapter with correct owner flag
                adapter = new GroupParticipantAdapter(isOwner, participant -> {
                    Toast.makeText(requireContext(), "Tính năng xóa thành viên đang phát triển", Toast.LENGTH_SHORT).show();
                });
                recyclerParticipants.setAdapter(adapter);
                adapter.submitList(participants);
            }
        });
    }
}

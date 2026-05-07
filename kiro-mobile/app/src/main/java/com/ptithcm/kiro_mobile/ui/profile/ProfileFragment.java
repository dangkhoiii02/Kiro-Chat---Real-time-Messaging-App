package com.ptithcm.kiro_mobile.ui.profile;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.ptithcm.kiro_mobile.R;
import com.ptithcm.kiro_mobile.config.AppConfig;
import com.ptithcm.kiro_mobile.data.model.user.UserProfile;
import com.ptithcm.kiro_mobile.databinding.FragmentProfileBinding;
import com.ptithcm.kiro_mobile.ui.main.MainActivity;

public class ProfileFragment extends Fragment {

    private FragmentProfileBinding binding;
    private ProfileViewModel viewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentProfileBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(requireActivity()).get(ProfileViewModel.class);

        binding.btnLogout.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).logout();
            }
        });

        binding.btnSave.setOnClickListener(v -> {
            // Placeholder cho API update profile (chưa implement endpoint backend trong app mobile)
            Toast.makeText(requireContext(), "Tính năng cập nhật hồ sơ đang phát triển", Toast.LENGTH_SHORT).show();
        });

        binding.switchOnline.setOnCheckedChangeListener((buttonView, isChecked) -> {
            // Placeholder cho API update privacy
            if (buttonView.isPressed()) { // Only respond to user clicks
                Toast.makeText(requireContext(), "Cập nhật trạng thái...", Toast.LENGTH_SHORT).show();
            }
        });

        observeViewModel();
        viewModel.loadProfile();
    }

    private void observeViewModel() {
        viewModel.getProfileResult().observe(getViewLifecycleOwner(), result -> {
            if (result == null) return;

            if (result.isLoading()) {
                binding.progress.setVisibility(View.VISIBLE);
                binding.tvError.setVisibility(View.GONE);
                return;
            }

            binding.progress.setVisibility(View.GONE);

            if (result.isError()) {
                binding.tvError.setText(result.getMessage());
                binding.tvError.setVisibility(View.VISIBLE);
                // If unauthorized, redirect to login
                if (result.getCode() == 401 && getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).logout();
                }
                return;
            }

            binding.tvError.setVisibility(View.GONE);
            bindProfile(result.getData());
        });
    }

    private void bindProfile(UserProfile profile) {
        binding.tvDisplayName.setText(profile.getDisplayName());
        binding.tvUsername.setText("@" + profile.getUsername());
        binding.tvEmail.setText(profile.getEmailAddress() != null ? profile.getEmailAddress() : "");

        binding.etFirstname.setText(profile.getFirstname() != null ? profile.getFirstname() : "");
        binding.etLastname.setText(profile.getLastname() != null ? profile.getLastname() : "");
        binding.switchOnline.setChecked(profile.isActivityStatus());

        String avatarUrl = normalizeUrl(profile.getProfilePictureUrl());
        if (avatarUrl != null && !avatarUrl.isEmpty()) {
            Glide.with(this)
                    .load(avatarUrl)
                    .circleCrop()
                    .placeholder(R.drawable.ic_avatar_placeholder)
                    .error(R.drawable.ic_avatar_placeholder)
                    .into(binding.ivAvatar);
        }
    }

    /** Replace localhost with 10.0.2.2 for emulator compatibility. */
    private String normalizeUrl(String url) {
        if (url == null) return null;
        return url.replace("http://localhost:9000", AppConfig.MINIO_PUBLIC_URL)
                  .replace("http://localhost:8080", "http://10.0.2.2:8080");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}

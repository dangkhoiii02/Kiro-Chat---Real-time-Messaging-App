package com.ptithcm.kiro_mobile.ui.profile;

import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
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
import com.ptithcm.kiro_mobile.util.ImageLoader;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public class ProfileFragment extends Fragment {

    private FragmentProfileBinding binding;
    private ProfileViewModel viewModel;

    private ActivityResultLauncher<PickVisualMediaRequest> pickAvatarLauncher;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentProfileBinding.inflate(inflater, container, false);

        // Register avatar picker
        pickAvatarLauncher = registerForActivityResult(
                new ActivityResultContracts.PickVisualMedia(),
                uri -> {
                    if (uri != null) {
                        File file = uriToFile(uri);
                        if (file != null) {
                            viewModel.uploadAvatar(file);
                        } else {
                            Toast.makeText(requireContext(), "Không thể đọc ảnh", Toast.LENGTH_SHORT).show();
                        }
                    }
                });

        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(requireActivity()).get(ProfileViewModel.class);

        // Click avatar to change
        binding.ivAvatar.setOnClickListener(v ->
                pickAvatarLauncher.launch(new PickVisualMediaRequest.Builder()
                        .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                        .build()));

        binding.btnLogout.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).logout();
            }
        });

        binding.btnSave.setOnClickListener(v ->
                Toast.makeText(requireContext(), "Tính năng cập nhật hồ sơ đang phát triển", Toast.LENGTH_SHORT).show());

        binding.switchOnline.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (buttonView.isPressed()) {
                Toast.makeText(requireContext(), "Cập nhật trạng thái...", Toast.LENGTH_SHORT).show();
            }
        });

        binding.tvBlockList.setOnClickListener(v ->
                requireActivity().getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, new BlockListFragment())
                        .addToBackStack(null)
                        .commit());

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
        ImageLoader.loadAvatar(requireContext(), avatarUrl, binding.ivAvatar);
    }

    private String normalizeUrl(String url) {
        if (url == null) return null;
        return url.replace("http://localhost:9000", AppConfig.MINIO_PUBLIC_URL)
                  .replace("http://localhost:8080", "http://10.0.2.2:8080");
    }

    private File uriToFile(Uri uri) {
        try {
            InputStream in = requireContext().getContentResolver().openInputStream(uri);
            if (in == null) return null;

            String fileName = "avatar_" + System.currentTimeMillis() + ".jpg";
            Cursor cursor = requireContext().getContentResolver().query(uri, null, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                int idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (idx != -1) fileName = cursor.getString(idx);
                cursor.close();
            }

            File tempFile = new File(requireContext().getCacheDir(), fileName);
            FileOutputStream out = new FileOutputStream(tempFile);
            byte[] buf = new byte[4096];
            int len;
            while ((len = in.read(buf)) > 0) out.write(buf, 0, len);
            out.close();
            in.close();
            return tempFile;
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}

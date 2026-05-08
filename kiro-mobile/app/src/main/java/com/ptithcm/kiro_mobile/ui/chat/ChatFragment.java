package com.ptithcm.kiro_mobile.ui.chat;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.ptithcm.kiro_mobile.R;
import com.ptithcm.kiro_mobile.databinding.FragmentChatBinding;
import com.ptithcm.kiro_mobile.data.socket.StompManager;
import com.ptithcm.kiro_mobile.data.socket.CallSignalingManager;
import com.ptithcm.kiro_mobile.data.model.call.CallSignalMessage;
import com.ptithcm.kiro_mobile.data.model.user.UserProfile;
import com.ptithcm.kiro_mobile.ui.main.MainActivity;
import com.ptithcm.kiro_mobile.ui.profile.ProfileViewModel;
import com.ptithcm.kiro_mobile.ui.call.VideoCallActivity;
import android.content.Intent;
import android.net.Uri;
import android.database.Cursor;
import android.provider.OpenableColumns;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.activity.result.PickVisualMediaRequest;
import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.Window;
import android.widget.ImageView;
import com.bumptech.glide.Glide;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public class ChatFragment extends Fragment {

    public static final String ARG_CONVERSATION_ID   = "conversationId";
    public static final String ARG_CONVERSATION_NAME = "conversationName";
    public static final String ARG_IS_GROUP          = "isGroup";
    public static final String ARG_IS_ONLINE         = "isOnline";
    public static final String ARG_REMOTE_USER_ID    = "remoteUserId";

    private FragmentChatBinding binding;
    private ChatViewModel viewModel;
    private MessageAdapter adapter;
    private LinearLayoutManager layoutManager;
    private String conversationId;
    private String remoteUserId;
    private boolean isGroup;
    private ActivityResultLauncher<PickVisualMediaRequest> pickMediaLauncher;

    public static ChatFragment newInstance(String conversationId, String conversationName) {
        return newInstance(conversationId, conversationName, false, false);
    }

    public static ChatFragment newInstance(String conversationId, String conversationName,
                                           boolean isGroup, boolean isOnline) {
        return newInstance(conversationId, conversationName, isGroup, isOnline, null);
    }

    public static ChatFragment newInstance(String conversationId, String conversationName,
                                           boolean isGroup, boolean isOnline, String remoteUserId) {
        ChatFragment f = new ChatFragment();
        Bundle args = new Bundle();
        args.putString(ARG_CONVERSATION_ID, conversationId);
        args.putString(ARG_CONVERSATION_NAME, conversationName);
        args.putBoolean(ARG_IS_GROUP, isGroup);
        args.putBoolean(ARG_IS_ONLINE, isOnline);
        if (remoteUserId != null) args.putString(ARG_REMOTE_USER_ID, remoteUserId);
        f.setArguments(args);
        return f;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentChatBinding.inflate(inflater, container, false);

        pickMediaLauncher = registerForActivityResult(new ActivityResultContracts.PickVisualMedia(), uri -> {            if (uri != null) {
                File file = uriToFile(uri);
                if (file != null) {
                    viewModel.uploadAndSendMedia(file);
                }
            }
        });

        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        conversationId   = requireArguments().getString(ARG_CONVERSATION_ID);
        String conversationName = requireArguments().getString(ARG_CONVERSATION_NAME, "Chat");
        isGroup         = requireArguments().getBoolean(ARG_IS_GROUP, false);
        boolean isOnline = requireArguments().getBoolean(ARG_IS_ONLINE, false);
        remoteUserId    = requireArguments().getString(ARG_REMOTE_USER_ID, null);

        // Toolbar
        binding.toolbar.setTitle(conversationName);
        if (!isGroup) {
            binding.toolbar.setSubtitle(isOnline ? "Đang hoạt động" : "Ngoại tuyến");
        }
        binding.toolbar.setNavigationOnClickListener(v -> {
            hideKeyboard();
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).showBottomNav();
            }
            requireActivity().getSupportFragmentManager().popBackStack();
        });

        binding.toolbar.inflateMenu(R.menu.chat_menu);
        binding.toolbar.getMenu().findItem(R.id.action_video_call).setVisible(!isGroup);
        binding.toolbar.getMenu().findItem(R.id.action_voice_call).setVisible(!isGroup);
        binding.toolbar.getMenu().findItem(R.id.action_group_info).setVisible(isGroup);
        binding.toolbar.getMenu().findItem(R.id.action_block_user).setVisible(!isGroup);

        binding.toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_video_call) {
                initiateCall(conversationId, true);
                return true;
            } else if (item.getItemId() == R.id.action_voice_call) {
                initiateCall(conversationId, false);
                return true;
            } else if (item.getItemId() == R.id.action_group_info) {
                requireActivity().getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, com.ptithcm.kiro_mobile.ui.group.GroupProfileFragment.newInstance(conversationId))
                        .addToBackStack(null)
                        .commit();
                return true;
            } else if (item.getItemId() == R.id.action_block_user) {
                // To block we need target user id, which is same as conversationId in 1-1 chat
                new Thread(() -> {
                    com.ptithcm.kiro_mobile.data.repository.BlockRepository repo = new com.ptithcm.kiro_mobile.data.repository.BlockRepository(requireContext());
                    com.ptithcm.kiro_mobile.util.Result<Void> result = repo.blockUser(conversationId);
                    requireActivity().runOnUiThread(() -> {
                        if (result.isSuccess()) {
                            Toast.makeText(requireContext(), "Đã chặn người dùng", Toast.LENGTH_SHORT).show();
                            requireActivity().getSupportFragmentManager().popBackStack();
                        } else {
                            Toast.makeText(requireContext(), "Lỗi: " + result.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                }).start();
                return true;
            }
            return false;
        });

        // ViewModel
        viewModel = new ViewModelProvider(this).get(ChatViewModel.class);

        // Inject current user id from Activity-scoped ProfileViewModel
        new ViewModelProvider(requireActivity())
                .get(ProfileViewModel.class)
                .getProfileResult()
                .observe(getViewLifecycleOwner(), result -> {
                    if (result != null && result.isSuccess() && result.getData() != null) {
                        viewModel.setCurrentUserId(result.getData().getUserId());
                    }
                });

        setupRecyclerView();
        setupInput();
        observeViewModel();

        viewModel.init(conversationId, isGroup);

        // Wire up realtime socket events
        viewModel.observeSocket(StompManager.getInstance(requireContext()));

        // Show keyboard automatically
        binding.etMessage.postDelayed(() -> {
            binding.etMessage.requestFocus();
            InputMethodManager imm = (InputMethodManager)
                    requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.showSoftInput(binding.etMessage, InputMethodManager.SHOW_IMPLICIT);
            }
        }, 200);
    }

    private void initiateCall(String targetConvId, boolean withVideo) {
        // Use remoteUserId if available, otherwise fall back to conversationId
        String targetUserId = (remoteUserId != null && !remoteUserId.isEmpty())
                ? remoteUserId : targetConvId;

        ProfileViewModel profileVm = new ViewModelProvider(requireActivity()).get(ProfileViewModel.class);
        UserProfile currentProfile = null;
        if (profileVm.getProfileResult().getValue() != null && profileVm.getProfileResult().getValue().getData() != null) {
            currentProfile = profileVm.getProfileResult().getValue().getData();
        }

        String callerName = currentProfile != null ? currentProfile.getDisplayName() : "User";
        String callerAvatar = currentProfile != null ? currentProfile.getProfilePictureUrl() : "";

        CallSignalMessage offer = new CallSignalMessage();
        offer.setType("OFFER");
        offer.setConversationId(conversationId);
        offer.setTargetUserId(targetUserId);
        offer.setCallerId(viewModel.getCurrentUserId());
        offer.setCallerName(callerName);
        offer.setCallerAvatar(callerAvatar);
        offer.setWithVideo(withVideo);

        CallSignalingManager.getInstance(requireContext()).sendSignal(offer);

        Intent intent = new Intent(requireContext(), VideoCallActivity.class);
        intent.putExtra(VideoCallActivity.EXTRA_CONVERSATION_ID, conversationId);
        intent.putExtra(VideoCallActivity.EXTRA_REMOTE_USER_ID, targetUserId);
        intent.putExtra(VideoCallActivity.EXTRA_WITH_VIDEO, withVideo);
        intent.putExtra(VideoCallActivity.EXTRA_CALLER_NAME, binding.toolbar.getTitle());
        intent.putExtra(VideoCallActivity.EXTRA_IS_CALLER, true);
        startActivity(intent);
    }

    private File uriToFile(Uri uri) {
        try {
            InputStream in = requireContext().getContentResolver().openInputStream(uri);
            if (in == null) return null;
            
            String fileName = "upload_" + System.currentTimeMillis();
            Cursor cursor = requireContext().getContentResolver().query(uri, null, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (nameIndex != -1) {
                    fileName = cursor.getString(nameIndex);
                }
                cursor.close();
            }
            
            File tempFile = new File(requireContext().getCacheDir(), fileName);
            FileOutputStream out = new FileOutputStream(tempFile);
            byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
            out.close();
            in.close();
            return tempFile;
        } catch (Exception e) {
            return null;
        }
    }

    // ── RecyclerView ──────────────────────────────────────────────────────────

    private void setupRecyclerView() {
        adapter = new MessageAdapter();
        adapter.setGroup(isGroup);
        adapter.setRetryListener(localId -> viewModel.retryMessage(localId));
        adapter.setImageClickListener(url -> showFullScreenImage(url));

        layoutManager = new LinearLayoutManager(requireContext());
        layoutManager.setStackFromEnd(true);

        binding.recyclerMessages.setLayoutManager(layoutManager);
        binding.recyclerMessages.setAdapter(adapter);

        // Load older when scrolled to top
        binding.recyclerMessages.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView rv, int dx, int dy) {
                if (!rv.canScrollVertically(-1)) {
                    viewModel.loadOlderMessages();
                }
            }
        });
    }

    private void showFullScreenImage(String url) {
        Dialog dialog = new Dialog(requireContext(), android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.BLACK));
        }
        
        ImageView imageView = new ImageView(requireContext());
        imageView.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 
                ViewGroup.LayoutParams.MATCH_PARENT));
        imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        
        Glide.with(this).load(url).into(imageView);
        
        imageView.setOnClickListener(v -> dialog.dismiss());
        
        dialog.setContentView(imageView);
        dialog.show();
    }

    // ── Input ─────────────────────────────────────────────────────────────────

    private void setupInput() {
        // Send on button click
        binding.btnSend.setOnClickListener(v -> sendMessage());

        // Send on keyboard "Send" action
        binding.etMessage.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                sendMessage();
                return true;
            }
            return false;
        });
        
        binding.btnAttachment.setOnClickListener(v -> {
            pickMediaLauncher.launch(new PickVisualMediaRequest.Builder()
                    .setMediaType(ActivityResultContracts.PickVisualMedia.ImageAndVideo.INSTANCE)
                    .build());
        });
    }

    private void sendMessage() {
        String text = binding.etMessage.getText() != null
                ? binding.etMessage.getText().toString() : "";
        if (!text.trim().isEmpty()) {
            viewModel.sendMessage(text);
            binding.etMessage.setText("");
        }
    }

    // ── Observe ───────────────────────────────────────────────────────────────

    private void observeViewModel() {
        viewModel.getMessages().observe(getViewLifecycleOwner(), messages -> {
            if (messages == null) return;
            int prevCount = adapter.getItemCount();
            adapter.submitList(messages, () -> {
                if (messages.size() > prevCount) {
                    binding.recyclerMessages.scrollToPosition(messages.size() - 1);
                }
            });
            Boolean loading = viewModel.getIsLoadingInitial().getValue();
            if (!Boolean.TRUE.equals(loading)) {
                binding.layoutEmpty.setVisibility(
                        messages.isEmpty() ? View.VISIBLE : View.GONE);
            }
        });

        viewModel.getIsLoadingInitial().observe(getViewLifecycleOwner(), loading -> {
            binding.progressInitial.setVisibility(
                    Boolean.TRUE.equals(loading) ? View.VISIBLE : View.GONE);
            binding.recyclerMessages.setVisibility(
                    Boolean.TRUE.equals(loading) ? View.GONE : View.VISIBLE);
            if (Boolean.TRUE.equals(loading)) binding.layoutEmpty.setVisibility(View.GONE);
        });

        viewModel.getIsLoadingOlder().observe(getViewLifecycleOwner(), loading ->
                binding.progressOlder.setVisibility(
                        Boolean.TRUE.equals(loading) ? View.VISIBLE : View.GONE));

        viewModel.getScrollToBottom().observe(getViewLifecycleOwner(), unused -> {
            int count = adapter.getItemCount();
            if (count > 0) binding.recyclerMessages.scrollToPosition(count - 1);
        });

        viewModel.getErrorEvent().observe(getViewLifecycleOwner(), msg ->
                Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show());
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager)
                requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null && binding != null) {
            imm.hideSoftInputFromWindow(binding.etMessage.getWindowToken(), 0);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}

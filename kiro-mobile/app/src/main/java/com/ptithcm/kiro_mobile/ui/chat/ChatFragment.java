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

import com.ptithcm.kiro_mobile.databinding.FragmentChatBinding;
import com.ptithcm.kiro_mobile.data.socket.StompManager;
import com.ptithcm.kiro_mobile.ui.main.MainActivity;
import com.ptithcm.kiro_mobile.ui.profile.ProfileViewModel;

public class ChatFragment extends Fragment {

    public static final String ARG_CONVERSATION_ID   = "conversationId";
    public static final String ARG_CONVERSATION_NAME = "conversationName";
    public static final String ARG_IS_GROUP          = "isGroup";

    private FragmentChatBinding binding;
    private ChatViewModel viewModel;
    private MessageAdapter adapter;
    private LinearLayoutManager layoutManager;

    public static ChatFragment newInstance(String conversationId, String conversationName) {
        return newInstance(conversationId, conversationName, false);
    }

    public static ChatFragment newInstance(String conversationId, String conversationName,
                                           boolean isGroup) {
        ChatFragment f = new ChatFragment();
        Bundle args = new Bundle();
        args.putString(ARG_CONVERSATION_ID, conversationId);
        args.putString(ARG_CONVERSATION_NAME, conversationName);
        args.putBoolean(ARG_IS_GROUP, isGroup);
        f.setArguments(args);
        return f;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentChatBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        String conversationId   = requireArguments().getString(ARG_CONVERSATION_ID);
        String conversationName = requireArguments().getString(ARG_CONVERSATION_NAME, "Chat");
        boolean isGroup         = requireArguments().getBoolean(ARG_IS_GROUP, false);

        // Toolbar
        binding.toolbar.setTitle(conversationName);
        binding.toolbar.setNavigationOnClickListener(v -> {
            hideKeyboard();
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).showBottomNav();
            }
            requireActivity().getSupportFragmentManager().popBackStack();
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

    // ── RecyclerView ──────────────────────────────────────────────────────────

    private void setupRecyclerView() {
        adapter = new MessageAdapter();
        adapter.setRetryListener(localId -> viewModel.retryMessage(localId));

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

package com.ptithcm.kiro_mobile.ui.chatlist;

import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.ptithcm.kiro_mobile.R;
import com.ptithcm.kiro_mobile.databinding.FragmentChatListBinding;
import com.ptithcm.kiro_mobile.ui.chat.ChatFragment;
import com.ptithcm.kiro_mobile.ui.group.CreateGroupFragment;
import com.ptithcm.kiro_mobile.ui.main.MainActivity;

public class ChatListFragment extends Fragment {

    private FragmentChatListBinding binding;
    private ChatListViewModel viewModel;
    private ConversationAdapter adapter;

    private String currentQuery = "";
    private String currentChipType = "all";
    private final Handler searchHandler = new Handler();
    private Runnable searchRunnable;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentChatListBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(ChatListViewModel.class);

        setupRecyclerView();
        setupSwipeRefresh();
        setupSearch();
        setupFilterChips();
        observeViewModel();

        viewModel.loadConversations();
    }

    private void setupRecyclerView() {
        adapter = new ConversationAdapter();
        adapter.setOnItemClickListener(conversation -> {
            ChatFragment chatFragment = ChatFragment.newInstance(
                    conversation.getConversationId(),
                    conversation.getDisplayName(),
                    conversation.isGroup()
            );
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).openChat(chatFragment);
            }
        });

        binding.recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerView.setAdapter(adapter);

        binding.fabCreateGroup.setOnClickListener(v ->
                requireActivity().getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragment_container, new CreateGroupFragment())
                        .addToBackStack(null)
                        .commit());
    }

    private void setupSwipeRefresh() {
        binding.swipeRefresh.setColorSchemeResources(R.color.brand_primary);
        binding.swipeRefresh.setOnRefreshListener(() -> {
            currentQuery = "";
            currentChipType = "all";
            binding.etSearch.setText("");
            binding.chipAll.setChecked(true);
            viewModel.loadConversations();
        });
    }

    private void setupSearch() {
        binding.etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (searchRunnable != null) searchHandler.removeCallbacks(searchRunnable);
            }
            @Override
            public void afterTextChanged(Editable s) {
                currentQuery = s.toString();
                searchRunnable = () -> adapter.filter(currentQuery, currentChipType);
                searchHandler.postDelayed(searchRunnable, 300);
            }
        });
    }

    private void setupFilterChips() {
        binding.chipGroupFilter.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) return;
            int id = checkedIds.get(0);
            if (id == R.id.chip_all)    currentChipType = "all";
            else if (id == R.id.chip_unread)  currentChipType = "unread";
            else if (id == R.id.chip_groups)  currentChipType = "groups";
            adapter.filter(currentQuery, currentChipType);
        });
    }

    private void observeViewModel() {
        viewModel.getConversations().observe(getViewLifecycleOwner(), result -> {
            binding.swipeRefresh.setRefreshing(false);

            if (result.isLoading()) {
                showState(State.LOADING);
                return;
            }

            if (result.isError()) {
                // Chỉ logout khi 401 từ profile, không logout khi conversations lỗi
                binding.tvError.setText(result.getMessage());
                showState(State.ERROR);
                return;
            }

            if (result.getData() == null || result.getData().isEmpty()) {
                showState(State.EMPTY);
            } else {
                adapter.submitList(result.getData());
                // Re-apply active filter after new data
                adapter.filter(currentQuery, currentChipType);
                showState(State.CONTENT);
            }
        });

        binding.btnRetry.setOnClickListener(v -> viewModel.loadConversations());
    }

    private enum State { LOADING, CONTENT, EMPTY, ERROR }

    private void showState(State state) {
        binding.progress.setVisibility(state == State.LOADING ? View.VISIBLE : View.GONE);
        binding.recyclerView.setVisibility(state == State.CONTENT ? View.VISIBLE : View.GONE);
        binding.layoutEmpty.setVisibility(state == State.EMPTY ? View.VISIBLE : View.GONE);
        binding.layoutError.setVisibility(state == State.ERROR ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        searchHandler.removeCallbacksAndMessages(null);
        binding = null;
    }
}

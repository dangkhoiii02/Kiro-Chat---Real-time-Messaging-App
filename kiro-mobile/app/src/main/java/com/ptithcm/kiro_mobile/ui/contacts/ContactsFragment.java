package com.ptithcm.kiro_mobile.ui.contacts;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.tabs.TabLayout;
import com.ptithcm.kiro_mobile.R;
import com.ptithcm.kiro_mobile.data.model.chat.ConversationReference;
import com.ptithcm.kiro_mobile.data.repository.ConversationRepository;
import com.ptithcm.kiro_mobile.databinding.FragmentContactsBinding;
import com.ptithcm.kiro_mobile.ui.chat.ChatFragment;
import com.ptithcm.kiro_mobile.ui.main.MainActivity;
import com.ptithcm.kiro_mobile.util.Result;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ContactsFragment extends Fragment {

    private FragmentContactsBinding binding;
    private ContactsViewModel viewModel;
    private ContactAdapter adapter;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private static final int TAB_FRIENDS  = 0;
    private static final int TAB_REQUESTS = 1;
    private static final int TAB_SEARCH   = 2;

    private int currentTab = TAB_FRIENDS;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentContactsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(ContactsViewModel.class);

        setupRecyclerView();
        setupSearch();
        setupTabs();
        observeViewModel();

        viewModel.loadFriends("");
    }

    private void setupRecyclerView() {
        adapter = new ContactAdapter();
        adapter.setMode(ContactAdapter.Mode.FRIENDS);
        adapter.setActionListener(new ContactAdapter.ActionListener() {
            @Override
            public void onAddFriend(com.ptithcm.kiro_mobile.data.model.user.PublicProfile profile) {
                viewModel.sendFriendRequest(profile.getUserId());
            }

            @Override
            public void onUnsendRequest(com.ptithcm.kiro_mobile.data.model.user.PublicProfile profile) {
                viewModel.unsendRequest(profile.getUserId());
            }

            @Override
            public void onAcceptRequest(com.ptithcm.kiro_mobile.data.model.user.PublicProfile profile) {
                viewModel.acceptRequest(profile.getUserId());
            }

            @Override
            public void onRejectRequest(com.ptithcm.kiro_mobile.data.model.user.PublicProfile profile) {
                viewModel.rejectRequest(profile.getUserId());
            }

            @Override
            public void onRemoveFriend(com.ptithcm.kiro_mobile.data.model.user.PublicProfile profile) {
                viewModel.removeFriend(profile.getUserId());
            }

            @Override
            public void onMessage(com.ptithcm.kiro_mobile.data.model.user.PublicProfile profile) {
                viewModel.openMessage(profile);
            }

            @Override
            public void onItemClick(com.ptithcm.kiro_mobile.data.model.user.PublicProfile profile) {
                viewModel.openMessage(profile);
            }
        });

        binding.recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerView.addItemDecoration(
                new DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL));
        binding.recyclerView.setAdapter(adapter);
    }

    private void setupSearch() {
        binding.etSearch.addTextChangedListener(new TextWatcher() {
            private android.os.Handler handler = new android.os.Handler();
            private Runnable runnable;

            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (runnable != null) handler.removeCallbacks(runnable);
            }

            @Override
            public void afterTextChanged(Editable s) {
                String query = s.toString().trim();
                runnable = () -> {
                    if (currentTab == TAB_SEARCH) {
                        if (!query.isEmpty()) {
                            viewModel.searchUsers(query);
                        } else {
                            adapter.submitList(null);
                            showState(State.EMPTY);
                        }
                    } else if (currentTab == TAB_FRIENDS) {
                        viewModel.loadFriends(query);
                    }
                };
                handler.postDelayed(runnable, 350);
            }
        });
    }

    private void setupTabs() {
        binding.tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                currentTab = tab.getPosition();
                binding.etSearch.setText("");
                
                if (currentTab == TAB_FRIENDS) {
                    adapter.setMode(ContactAdapter.Mode.FRIENDS);
                    viewModel.loadFriends("");
                    binding.tilSearch.setHint("Tìm kiếm bạn bè...");
                } else if (currentTab == TAB_REQUESTS) {
                    adapter.setMode(ContactAdapter.Mode.REQUESTS);
                    viewModel.loadPendingRequests();
                    binding.tilSearch.setHint("Tìm kiếm yêu cầu...");
                } else if (currentTab == TAB_SEARCH) {
                    adapter.setMode(ContactAdapter.Mode.SEARCH);
                    adapter.submitList(null);
                    showState(State.EMPTY);
                    binding.tilSearch.setHint("Tìm kiếm người dùng mới...");
                }
            }

            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void observeViewModel() {
        viewModel.getContacts().observe(getViewLifecycleOwner(), result -> {
            if (result.isLoading()) {
                showState(State.LOADING);
                return;
            }
            if (result.isError()) {
                if (result.getCode() == 401 && getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).logout();
                    return;
                }
                binding.tvError.setText(result.getMessage());
                showState(State.ERROR);
                return;
            }
            if (result.getData() == null || result.getData().isEmpty()) {
                showState(State.EMPTY);
            } else {
                adapter.submitList(result.getData());
                showState(State.CONTENT);
            }
        });

        viewModel.getToastMessage().observe(getViewLifecycleOwner(), msg ->
                Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show());

        viewModel.getOpenConversation().observe(getViewLifecycleOwner(), profile -> {
            if (profile != null && profile.getUserId() != null) {
                openConversation(profile);
            }
        });

        binding.btnRetry.setOnClickListener(v -> {
            if (currentTab == TAB_FRIENDS) viewModel.loadFriends(binding.etSearch.getText().toString());
            else if (currentTab == TAB_REQUESTS) viewModel.loadPendingRequests();
            else if (currentTab == TAB_SEARCH && !binding.etSearch.getText().toString().isEmpty()) {
                viewModel.searchUsers(binding.etSearch.getText().toString());
            }
        });
    }

    private void openConversation(com.ptithcm.kiro_mobile.data.model.user.PublicProfile profile) {
        executor.execute(() -> {
            ConversationRepository repo = new ConversationRepository(requireContext());
            Result<ConversationReference> result = repo.getConversationByUser(profile.getUserId());
            if (!result.isSuccess()) {
                result = repo.createDirectConversation(profile.getUserId());
            }
            Result<ConversationReference> finalResult = result;
            requireActivity().runOnUiThread(() -> {
                if (!finalResult.isSuccess() || finalResult.getData() == null) {
                    Toast.makeText(requireContext(), finalResult.getMessage(), Toast.LENGTH_SHORT).show();
                    return;
                }
                ChatFragment chatFragment = ChatFragment.newInstance(
                        finalResult.getData().getConversationId(),
                        profile.getDisplayName(),
                        false,
                        false);
                if (getActivity() instanceof com.ptithcm.kiro_mobile.ui.main.MainActivity) {
                    ((com.ptithcm.kiro_mobile.ui.main.MainActivity) getActivity())
                            .openChat(chatFragment);
                }
            });
        });
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
        binding = null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }
}

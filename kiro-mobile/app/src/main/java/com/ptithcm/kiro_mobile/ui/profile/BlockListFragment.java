package com.ptithcm.kiro_mobile.ui.profile;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.ptithcm.kiro_mobile.R;

public class BlockListFragment extends Fragment {

    private BlockListViewModel viewModel;
    private BlockListAdapter adapter;

    private RecyclerView recyclerBlocks;
    private ProgressBar progress;
    private TextView tvEmpty;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_block_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        MaterialToolbar toolbar = view.findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> requireActivity().getSupportFragmentManager().popBackStack());

        recyclerBlocks = view.findViewById(R.id.recycler_blocks);
        progress = view.findViewById(R.id.progress);
        tvEmpty = view.findViewById(R.id.tv_empty);

        viewModel = new ViewModelProvider(this).get(BlockListViewModel.class);

        adapter = new BlockListAdapter(user -> {
            if (user.getUserId() != null) {
                viewModel.unblockUser(user.getUserId());
            }
        });
        recyclerBlocks.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerBlocks.setAdapter(adapter);

        observeViewModel();
        viewModel.loadBlockedUsers();
    }

    private void observeViewModel() {
        viewModel.getBlockList().observe(getViewLifecycleOwner(), result -> {
            if (result.isLoading()) {
                progress.setVisibility(View.VISIBLE);
                recyclerBlocks.setVisibility(View.GONE);
                tvEmpty.setVisibility(View.GONE);
            } else if (result.isSuccess()) {
                progress.setVisibility(View.GONE);
                if (result.getData() != null && !result.getData().isEmpty()) {
                    recyclerBlocks.setVisibility(View.VISIBLE);
                    tvEmpty.setVisibility(View.GONE);
                    adapter.submitList(result.getData());
                } else {
                    recyclerBlocks.setVisibility(View.GONE);
                    tvEmpty.setVisibility(View.VISIBLE);
                }
            } else {
                progress.setVisibility(View.GONE);
                Toast.makeText(requireContext(), result.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

        viewModel.getUnblockEvent().observe(getViewLifecycleOwner(), result -> {
            if (result.isSuccess()) {
                Toast.makeText(requireContext(), "Đã bỏ chặn", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(requireContext(), result.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}

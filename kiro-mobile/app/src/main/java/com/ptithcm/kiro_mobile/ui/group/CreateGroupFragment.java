package com.ptithcm.kiro_mobile.ui.group;

import android.os.Bundle;
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

import com.ptithcm.kiro_mobile.R;
import com.ptithcm.kiro_mobile.databinding.FragmentCreateGroupBinding;
import com.ptithcm.kiro_mobile.ui.chat.ChatFragment;

public class CreateGroupFragment extends Fragment {
    private FragmentCreateGroupBinding binding;
    private CreateGroupViewModel viewModel;
    private GroupMemberAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentCreateGroupBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(CreateGroupViewModel.class);

        binding.toolbar.setNavigationOnClickListener(v ->
                requireActivity().getSupportFragmentManager().popBackStack());

        adapter = new GroupMemberAdapter();
        binding.recyclerMembers.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerMembers.addItemDecoration(
                new DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL));
        binding.recyclerMembers.setAdapter(adapter);

        binding.btnCreateGroup.setOnClickListener(v -> viewModel.createGroup(
                binding.etGroupName.getText() != null
                        ? binding.etGroupName.getText().toString()
                        : "",
                adapter.getSelectedIds()));

        observe();
        viewModel.loadFriends();
    }

    private void observe() {
        viewModel.getFriends().observe(getViewLifecycleOwner(), result -> {
            if (result.isLoading()) {
                binding.progress.setVisibility(View.VISIBLE);
                binding.tvEmpty.setVisibility(View.GONE);
                return;
            }
            binding.progress.setVisibility(View.GONE);
            if (result.isError()) {
                Toast.makeText(requireContext(), result.getMessage(), Toast.LENGTH_SHORT).show();
                binding.tvEmpty.setVisibility(View.VISIBLE);
                return;
            }
            adapter.submitList(result.getData());
            binding.tvEmpty.setVisibility(result.getData() == null || result.getData().isEmpty()
                    ? View.VISIBLE : View.GONE);
        });

        viewModel.getCreating().observe(getViewLifecycleOwner(), creating -> {
            boolean isCreating = Boolean.TRUE.equals(creating);
            binding.progress.setVisibility(isCreating ? View.VISIBLE : View.GONE);
            binding.btnCreateGroup.setEnabled(!isCreating);
        });

        viewModel.getToast().observe(getViewLifecycleOwner(), msg ->
                Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show());

        viewModel.getCreated().observe(getViewLifecycleOwner(), ref -> {
            String groupName = binding.etGroupName.getText() != null
                    ? binding.etGroupName.getText().toString().trim()
                    : "Nhóm mới";
            ChatFragment chatFragment = ChatFragment.newInstance(
                    ref.getConversationId(),
                    groupName.isEmpty() ? "Nhóm mới" : groupName,
                    true,
                    false);
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, chatFragment)
                    .addToBackStack(null)
                    .commit();
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}

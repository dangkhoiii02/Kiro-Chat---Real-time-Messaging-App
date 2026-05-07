package com.ptithcm.kiro_mobile.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.EditorInfo;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.ptithcm.kiro_mobile.databinding.ActivityLoginBinding;
import com.ptithcm.kiro_mobile.ui.main.MainActivity;
import com.ptithcm.kiro_mobile.util.Result;

public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding binding;
    private LoginViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        viewModel = new ViewModelProvider(this).get(LoginViewModel.class);

        setupListeners();
        observeViewModel();
    }

    private void setupListeners() {
        binding.btnLogin.setOnClickListener(v -> attemptLogin());

        binding.etPassword.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                attemptLogin();
                return true;
            }
            return false;
        });
    }

    private void attemptLogin() {
        String username = binding.etUsername.getText() != null
                ? binding.etUsername.getText().toString() : "";
        String password = binding.etPassword.getText() != null
                ? binding.etPassword.getText().toString() : "";
        viewModel.login(username, password);
    }

    private void observeViewModel() {
        viewModel.getLoginResult().observe(this, result -> {
            if (result == null) return;

            if (result.isLoading()) {
                showLoading(true);
                hideError();
                return;
            }

            showLoading(false);

            if (result.isError()) {
                showError(result.getMessage());
            }
        });

        viewModel.getNavigateToMain().observe(this, unused -> {
            startActivity(new Intent(this, MainActivity.class));
            finish();
        });
    }

    private void showLoading(boolean loading) {
        binding.progress.setVisibility(loading ? View.VISIBLE : View.GONE);
        binding.btnLogin.setEnabled(!loading);
    }

    private void showError(String message) {
        binding.tvError.setText(message);
        binding.tvError.setVisibility(View.VISIBLE);
    }

    private void hideError() {
        binding.tvError.setVisibility(View.GONE);
    }
}

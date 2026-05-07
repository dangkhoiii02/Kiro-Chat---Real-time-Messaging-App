package com.ptithcm.kiro_mobile.ui.main;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.ptithcm.kiro_mobile.R;
import com.ptithcm.kiro_mobile.data.auth.TokenManager;
import com.ptithcm.kiro_mobile.data.socket.StompManager;
import com.ptithcm.kiro_mobile.databinding.ActivityMainBinding;
import com.ptithcm.kiro_mobile.ui.auth.LoginActivity;
import com.ptithcm.kiro_mobile.ui.chat.ChatFragment;
import com.ptithcm.kiro_mobile.ui.chatlist.ChatListFragment;
import com.ptithcm.kiro_mobile.ui.contacts.ContactsFragment;
import com.ptithcm.kiro_mobile.ui.notifications.NotificationsFragment;
import com.ptithcm.kiro_mobile.ui.profile.ProfileFragment;
import com.ptithcm.kiro_mobile.ui.profile.ProfileViewModel;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    // package-visible so fragments can access bottomNav
    ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!TokenManager.getInstance(this).hasSession()) {
            Log.w(TAG, "No session, redirecting to Login");
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Connect STOMP WebSocket with current access token
        String token = TokenManager.getInstance(this).getAccessToken();
        StompManager.getInstance(this).connect(token);

        // Pre-load profile so ChatFragment can get currentUserId
        new ViewModelProvider(this).get(ProfileViewModel.class).loadProfile();

        setupBottomNav();

        if (savedInstanceState == null) {
            loadFragment(new ChatListFragment());
        }
    }

    private void setupBottomNav() {
        binding.bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_chats) {
                loadFragment(new ChatListFragment());
                return true;
            } else if (id == R.id.nav_contacts) {
                loadFragment(new ContactsFragment());
                return true;
            } else if (id == R.id.nav_notifications) {
                loadFragment(new NotificationsFragment());
                return true;
            } else if (id == R.id.nav_profile) {
                loadFragment(new ProfileFragment());
                return true;
            }
            return false;
        });
    }

    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
    }

    /** Open chat and hide bottom nav. */
    public void openChat(ChatFragment chatFragment) {
        binding.bottomNav.setVisibility(View.GONE);
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, chatFragment)
                .addToBackStack(null)
                .commit();
    }

    @Override
    public void onBackPressed() {
        if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
            getSupportFragmentManager().popBackStack();
            binding.bottomNav.setVisibility(View.VISIBLE);
        } else {
            super.onBackPressed();
        }
    }

    public void showBottomNav() {
        binding.bottomNav.setVisibility(View.VISIBLE);
    }

    public void logout() {
        StompManager.getInstance(this).disconnect();
        TokenManager.getInstance(this).clear();
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }
}

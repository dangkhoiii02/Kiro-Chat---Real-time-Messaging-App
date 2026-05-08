package com.ptithcm.kiro_mobile.data.repository;

import android.content.Context;
import com.ptithcm.kiro_mobile.data.api.ApiClient;
import com.ptithcm.kiro_mobile.data.model.group.GroupProfile;
import com.ptithcm.kiro_mobile.data.model.group.GroupParticipantList;
import com.ptithcm.kiro_mobile.util.Result;
import retrofit2.Response;

public class GroupRepository {
    private final ApiClient apiClient;

    public GroupRepository(Context context) {
        this.apiClient = ApiClient.getInstance(context);
    }

    public Result<GroupProfile> getGroupProfile(String groupId) {
        try {
            Response<GroupProfile> response = apiClient.getGroupApi().getGroupProfile(groupId).execute();
            if (response.isSuccessful() && response.body() != null) {
                return Result.success(response.body());
            }
            return Result.error("Failed to load group profile", response.code());
        } catch (Exception e) {
            return Result.networkError();
        }
    }

    public Result<GroupParticipantList> getGroupParticipants(String groupId) {
        try {
            Response<GroupParticipantList> response = apiClient.getGroupApi().getGroupParticipants(groupId).execute();
            if (response.isSuccessful() && response.body() != null) {
                return Result.success(response.body());
            }
            return Result.error("Failed to load group participants", response.code());
        } catch (Exception e) {
            return Result.networkError();
        }
    }
}

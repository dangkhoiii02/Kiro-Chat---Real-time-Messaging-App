package com.ptithcm.kiro_mobile.data.api;

import com.ptithcm.kiro_mobile.data.model.group.GroupProfile;
import com.ptithcm.kiro_mobile.data.model.group.GroupParticipantList;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

public interface GroupApi {
    @GET("groups/{groupId}")
    Call<GroupProfile> getGroupProfile(@Path("groupId") String groupId);

    @GET("groups/{groupId}/participants")
    Call<GroupParticipantList> getGroupParticipants(@Path("groupId") String groupId);
}

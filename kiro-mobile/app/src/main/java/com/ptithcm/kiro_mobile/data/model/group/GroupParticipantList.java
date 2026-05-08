package com.ptithcm.kiro_mobile.data.model.group;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class GroupParticipantList {
    @SerializedName("participants")
    private List<GroupParticipant> participants;

    public List<GroupParticipant> getParticipants() {
        return participants;
    }
}

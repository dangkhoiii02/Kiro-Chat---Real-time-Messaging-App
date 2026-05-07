package com.ptithcm.kiro_mobile.data.model.contact;

import com.google.gson.annotations.SerializedName;
import com.ptithcm.kiro_mobile.data.model.common.Page;

public class ContactRequestList {
    @SerializedName("requests")
    private Page<ContactRequest> requests;

    // Backend may return a flat list too
    @SerializedName("content")
    private java.util.List<ContactRequest> content;

    public Page<ContactRequest> getRequests() { return requests; }
    public java.util.List<ContactRequest> getContent() { return content; }
}

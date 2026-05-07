package com.ptithcm.kiro_mobile.data.model.common;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class Page<T> {
    @SerializedName("content")
    private List<T> content;

    @SerializedName("totalElements")
    private long totalElements;

    @SerializedName("totalPages")
    private int totalPages;

    @SerializedName("number")
    private int number;

    @SerializedName("size")
    private int size;

    @SerializedName("last")
    private boolean last;

    public List<T> getContent()    { return content; }
    public long getTotalElements() { return totalElements; }
    public int getTotalPages()     { return totalPages; }
    public int getNumber()         { return number; }
    public int getSize()           { return size; }
    public boolean isLast()        { return last; }
}

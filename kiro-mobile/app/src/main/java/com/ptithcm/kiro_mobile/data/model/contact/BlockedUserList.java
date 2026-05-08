package com.ptithcm.kiro_mobile.data.model.contact;

import java.util.List;

/**
 * Wrapper for the list of blocked users from GET /blocks.
 */
public class BlockedUserList {

    private List<BlockedUser> blockedUsers;

    public BlockedUserList() {}

    public List<BlockedUser> getBlockedUsers() { return blockedUsers; }
    public void setBlockedUsers(List<BlockedUser> blockedUsers) { this.blockedUsers = blockedUsers; }
}

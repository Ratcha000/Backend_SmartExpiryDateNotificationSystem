package com.app.expiry_system.restaurant.dto;

import jakarta.validation.constraints.NotBlank;

public class JoinRestaurantRequest {

    @NotBlank(message = "Invite code is required")
    private String inviteCode;

    public String getInviteCode() {
        return inviteCode;
    }

    public void setInviteCode(String inviteCode) {
        this.inviteCode = inviteCode;
    }
}

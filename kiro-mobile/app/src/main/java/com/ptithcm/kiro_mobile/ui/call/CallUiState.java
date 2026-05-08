package com.ptithcm.kiro_mobile.ui.call;

public class CallUiState {
    public boolean isConnecting = true;
    public boolean isVideoEnabled = false;
    public boolean isAudioEnabled = false;
    public boolean isSpeakerOn = false;
    public int durationSeconds = 0;
    public boolean remoteVideoActive = false;
    
    // Copy constructor or empty
    public CallUiState() {}
    
    public CallUiState copy() {
        CallUiState state = new CallUiState();
        state.isConnecting = this.isConnecting;
        state.isVideoEnabled = this.isVideoEnabled;
        state.isAudioEnabled = this.isAudioEnabled;
        state.isSpeakerOn = this.isSpeakerOn;
        state.durationSeconds = this.durationSeconds;
        state.remoteVideoActive = this.remoteVideoActive;
        return state;
    }
}

package com.ptithcm.kiro_mobile.ui.call

import android.content.Context
import android.util.Log
import io.livekit.android.LiveKit
import io.livekit.android.events.RoomEvent
import io.livekit.android.room.Room
import io.livekit.android.room.track.RemoteVideoTrack
import io.livekit.android.room.track.VideoTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Kotlin bridge that wraps LiveKit coroutine API for Java callers.
 */
class LiveKitBridge(private val context: Context) {

    interface Callback {
        fun onConnected()
        fun onDisconnected()
        fun onError(message: String)
        fun onRemoteVideoTrack(track: RemoteVideoTrack)
        fun onRemoteVideoTrackRemoved()
        fun onLocalVideoTrack(track: VideoTrack?)
    }

    private val TAG = "LiveKitBridge"
    private val scope = CoroutineScope(Dispatchers.Main + Job())

    val room: Room = LiveKit.create(context)

    fun connect(url: String, token: String, withVideo: Boolean, callback: Callback) {
        scope.launch {
            try {
                room.connect(url, token)
                room.localParticipant.setMicrophoneEnabled(true)
                if (withVideo) {
                    room.localParticipant.setCameraEnabled(true)
                    val videoTrack = room.localParticipant.videoTrackPublications
                        .firstOrNull()?.second as? VideoTrack
                    callback.onLocalVideoTrack(videoTrack)
                }
                callback.onConnected()
                collectEvents(callback)
            } catch (e: Exception) {
                Log.e(TAG, "connect error", e)
                callback.onError("Lỗi kết nối: ${e.message}")
            }
        }
    }

    private fun collectEvents(callback: Callback) {
        scope.launch {
            room.events.events.collect { event ->
                when (event) {
                    is RoomEvent.TrackSubscribed -> {
                        val track = event.track
                        if (track is RemoteVideoTrack) {
                            callback.onRemoteVideoTrack(track)
                        }
                    }
                    is RoomEvent.TrackUnsubscribed -> {
                        if (event.track is RemoteVideoTrack) {
                            callback.onRemoteVideoTrackRemoved()
                        }
                    }
                    is RoomEvent.Disconnected -> callback.onDisconnected()
                    else -> {}
                }
            }
        }
    }

    fun setCameraEnabled(enabled: Boolean) {
        scope.launch {
            try {
                room.localParticipant.setCameraEnabled(enabled)
            } catch (e: Exception) {
                Log.e(TAG, "setCameraEnabled error", e)
            }
        }
    }

    fun setMicrophoneEnabled(enabled: Boolean) {
        scope.launch {
            try {
                room.localParticipant.setMicrophoneEnabled(enabled)
            } catch (e: Exception) {
                Log.e(TAG, "setMicrophoneEnabled error", e)
            }
        }
    }

    fun disconnect() {
        scope.launch {
            try { room.disconnect() } catch (e: Exception) { /* ignore */ }
        }
    }

    fun release() {
        scope.cancel()
        room.release()
    }
}

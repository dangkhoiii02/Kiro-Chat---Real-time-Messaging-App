import {
  Component,
  Input,
  Output,
  EventEmitter,
  OnDestroy,
  OnInit,
  AfterViewInit,
  inject,
  signal,
  ViewChild,
  ElementRef,
  NgZone,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import {
  Room,
  RoomEvent,
  createLocalTracks,
  Track,
  RemoteParticipant,
  RemoteTrack,
} from 'livekit-client';
import { Subscription, interval } from 'rxjs';
import { filter, takeWhile } from 'rxjs/operators';

import { environment } from '../../../../../environments/environments';
import { CallService } from '../../service/call.service';
import { CallSignalingService } from '../../service/call-signaling.service';

@Component({
  selector: 'app-video-call',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './video-call.component.html',
  styleUrls: ['./video-call.component.css'],
})
export class VideoCallComponent implements OnInit, AfterViewInit, OnDestroy {
  @Input() conversationId!: string;
  @Input() remoteUserId = '';
  @Input() isOpen = false;
  /** true = video call (bật cam), false = voice only (không bật cam) */
  @Input() withVideo = true;
  @Input() remoteAvatarUrl = '';
  @Input() localAvatarUrl = '';

  @Output() close = new EventEmitter<void>();
  @Output() callEnded = new EventEmitter<number>();

  @ViewChild('localVideo') localVideoRef!: ElementRef<HTMLVideoElement>;

  // UI state
  isConnecting = signal(true);
  isVideoEnabled = signal(false); // sẽ được set đúng sau khi connect
  isAudioEnabled = signal(true);
  mediaError = signal<string | null>(null);
  callDurationSec = signal(0);

  // LiveKit
  room: Room | null = null;
  remoteParticipants = signal<RemoteParticipant[]>([]);
  /** Track theo dõi participant nào đang bật cam */
  remoteVideoActive = signal<Set<string>>(new Set());

  private readonly callService = inject(CallService);
  private readonly signalingService = inject(CallSignalingService);
  private readonly ngZone = inject(NgZone);

  private signalSub: Subscription | null = null;
  private timerSub: Subscription | null = null;
  private viewReady = false;
  private pendingToken: string | null = null;

  ngOnInit(): void {
    this.listenForRemoteSignals();
    this.fetchToken();
  }

  ngAfterViewInit(): void {
    this.viewReady = true;
    if (this.pendingToken) {
      const token = this.pendingToken;
      this.pendingToken = null;
      this.connectToRoom(token);
    }
  }

  ngOnDestroy(): void {
    this.signalSub?.unsubscribe();
    this.timerSub?.unsubscribe();
    this.disconnectRoom();
  }

  // ── Token ────────────────────────────────────────────────────────────────

  private fetchToken(): void {
    this.isConnecting.set(true);
    this.mediaError.set(null);
    this.callService.getToken(this.conversationId).subscribe({
      next: (token) => {
        if (this.viewReady) this.connectToRoom(token);
        else this.pendingToken = token;
      },
      error: () => {
        this.mediaError.set('Không thể kết nối cuộc gọi. Vui lòng thử lại.');
        this.isConnecting.set(false);
      },
    });
  }

  // ── Room ─────────────────────────────────────────────────────────────────

  private async connectToRoom(token: string): Promise<void> {
    this.room = new Room();

    this.room
      .on(RoomEvent.TrackSubscribed, (track, _pub, participant) => {
        this.ngZone.run(() => {
          this.updateParticipants();
          if (track.kind === Track.Kind.Video) {
            // Đánh dấu participant này đang có video
            const active = new Set(this.remoteVideoActive());
            active.add(participant.sid);
            this.remoteVideoActive.set(active);
          }
        });
        // Attach sau khi Angular render xong
        this.attachWhenReady(track, participant.sid);
      })
      .on(RoomEvent.TrackUnsubscribed, (track, _pub, participant) => {
        track.detach();
        this.ngZone.run(() => {
          if (track.kind === Track.Kind.Video) {
            const active = new Set(this.remoteVideoActive());
            active.delete(participant.sid);
            this.remoteVideoActive.set(active);
          }
          this.updateParticipants();
        });
      })
      .on(RoomEvent.TrackMuted, (pub, participant) => {
        if (pub.kind === Track.Kind.Video) {
          this.ngZone.run(() => {
            const active = new Set(this.remoteVideoActive());
            active.delete(participant.sid);
            this.remoteVideoActive.set(active);
          });
        }
      })
      .on(RoomEvent.TrackUnmuted, (pub, participant) => {
        if (pub.kind === Track.Kind.Video) {
          this.ngZone.run(() => {
            const active = new Set(this.remoteVideoActive());
            active.add(participant.sid);
            this.remoteVideoActive.set(active);
          });
        }
      })
      .on(RoomEvent.ParticipantConnected, () => this.ngZone.run(() => this.updateParticipants()))
      .on(RoomEvent.ParticipantDisconnected, () => this.ngZone.run(() => this.updateParticipants()))
      .on(RoomEvent.Disconnected, () => this.ngZone.run(() => {
        // Only trigger leaveRoom if we didn't initiate the disconnect ourselves
        if (this.room) this.leaveRoom();
      }));

    try {
      await this.room.connect(environment.livekitUrl, token);
    } catch {
      this.mediaError.set('Không thể kết nối phòng gọi.');
      this.isConnecting.set(false);
      return;
    }

    this.isConnecting.set(false);
    this.isVideoEnabled.set(this.withVideo);
    this.startTimer();

    // Publish local tracks theo loại cuộc gọi
    try {
      const tracks = await createLocalTracks({ audio: true, video: this.withVideo });
      for (const track of tracks) {
        await this.room.localParticipant.publishTrack(track);
        if (track.kind === Track.Kind.Video) {
          this.attachLocalVideo();
        }
      }
    } catch (err: unknown) {
      this.isVideoEnabled.set(false);
      this.handleMediaError(err);
    }
  }

  /** Attach local video vào #localVideo element với retry */
  private attachLocalVideo(attempt = 0): void {
    if (attempt > 20) return;
    const el = this.localVideoRef?.nativeElement;
    if (!el || !this.room) {
      setTimeout(() => this.attachLocalVideo(attempt + 1), 100);
      return;
    }
    const videoTrack = Array.from(this.room.localParticipant.videoTrackPublications.values())
      .find(p => p.track)?.track;
    if (videoTrack) {
      videoTrack.attach(el);
    } else {
      setTimeout(() => this.attachLocalVideo(attempt + 1), 100);
    }
  }

  /** Attach remote track với retry cho đến khi DOM element xuất hiện */
  private attachWhenReady(track: RemoteTrack, sid: string, attempt = 0): void {
    if (attempt > 30) return;
    const elId = track.kind === Track.Kind.Video ? `remote-video-${sid}` : `remote-audio-${sid}`;
    const el = document.getElementById(elId) as HTMLVideoElement | HTMLAudioElement | null;
    if (el) {
      track.attach(el);
    } else {
      setTimeout(() => this.attachWhenReady(track, sid, attempt + 1), 100);
    }
  }

  private handleMediaError(err: unknown): void {
    const name = (err as DOMException)?.name ?? '';
    const msg = (err as DOMException)?.message ?? String(err);
    if (name === 'NotAllowedError' || name === 'PermissionDeniedError') {
      this.mediaError.set('Trình duyệt từ chối quyền Micro/Camera. Vui lòng cấp quyền và thử lại.');
    } else if (name === 'NotReadableError' || name === 'TrackStartError') {
      this.mediaError.set('Không thể truy cập Micro/Camera. Thiết bị đang được ứng dụng khác sử dụng.');
    } else {
      this.mediaError.set('Lỗi thiết bị: ' + (msg || name));
    }
  }

  // ── Timer ────────────────────────────────────────────────────────────────

  private startTimer(): void {
    this.callDurationSec.set(0);
    this.timerSub = interval(1000)
      .pipe(takeWhile(() => this.room !== null))
      .subscribe(() => this.callDurationSec.update(s => s + 1));
  }

  formatDuration(sec: number): string {
    const m = Math.floor(sec / 60).toString().padStart(2, '0');
    const s = (sec % 60).toString().padStart(2, '0');
    return `${m}:${s}`;
  }

  // ── Signals ──────────────────────────────────────────────────────────────

  private listenForRemoteSignals(): void {
    this.signalSub = this.signalingService.onSignal$
      .pipe(filter(msg =>
        (msg.type === 'REJECT' || msg.type === 'END') &&
        msg.conversationId === this.conversationId
      ))
      .subscribe(() => this.leaveRoom());
  }

  // ── Controls ─────────────────────────────────────────────────────────────

  toggleVideo(): void {
    if (!this.room) return;
    const next = !this.isVideoEnabled();
    this.isVideoEnabled.set(next);
    this.room.localParticipant.setCameraEnabled(next);
    if (next) {
      // Bật lại cam → attach lại
      setTimeout(() => this.attachLocalVideo(), 200);
    }
  }

  toggleAudio(): void {
    if (!this.room) return;
    const next = !this.isAudioEnabled();
    this.isAudioEnabled.set(next);
    this.room.localParticipant.setMicrophoneEnabled(next);
  }

  leaveRoom(): void {
    const duration = this.callDurationSec();
    if (this.remoteUserId && this.conversationId) {
      this.signalingService.sendEnd({ conversationId: this.conversationId, targetUserId: this.remoteUserId });
    }
    this.timerSub?.unsubscribe();
    this.timerSub = null;
    this.disconnectRoom();
    this.callEnded.emit(duration);
    this.close.emit();
  }

  isRemoteVideoActive(sid: string): boolean {
    return this.remoteVideoActive().has(sid);
  }

  // ── Helpers ──────────────────────────────────────────────────────────────

  private disconnectRoom(): void {
    const room = this.room;
    this.room = null; // set null first to prevent re-entrant leaveRoom from RoomEvent.Disconnected
    if (room) { room.disconnect(); }
    this.isConnecting.set(false);
    this.remoteParticipants.set([]);
    this.remoteVideoActive.set(new Set());
  }

  private updateParticipants(): void {
    if (!this.room) return;
    this.remoteParticipants.set(Array.from(this.room.remoteParticipants.values()));
  }
}

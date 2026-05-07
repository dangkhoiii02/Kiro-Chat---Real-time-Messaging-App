/**
 * Copyright 2026 trung-kieen
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import { Injectable, inject, signal, OnDestroy } from '@angular/core';
import { Subject, Subscription } from 'rxjs';
import { filter, take } from 'rxjs/operators';
import { WebSocketService } from '../../../core/socket/websocket.service';

// ─── Types ────────────────────────────────────────────────────────────────────

export type SignalType = 'OFFER' | 'ANSWER' | 'REJECT' | 'END';

export interface CallSignalMessage {
  type: SignalType;
  conversationId: string;
  targetUserId: string;
  callerId: string;
  callerName: string;
  callerAvatar?: string;
  /** true = video call, false = voice only */
  withVideo: boolean;
}

export interface IncomingCallInfo {
  conversationId: string;
  callerId: string;
  callerName: string;
  callerAvatar?: string;
  /** Caller muốn video hay voice */
  withVideo: boolean;
}

// ─── WebSocket destination constants ──────────────────────────────────────────

/** The STOMP destination the backend listens on (prefixed with /app by the broker). */
const APP_CALL_SIGNAL = '/app/calls.signal';

/** The STOMP destination this user subscribes to for incoming signals. */
const USER_CALL_SIGNAL = '/user/queue/calls.signal';

// ─── Service ──────────────────────────────────────────────────────────────────

/**
 * CallSignalingService
 *
 * Manages the WebRTC-style call signaling flow over STOMP WebSocket:
 *  - OFFER  → caller notifies callee that a call is incoming
 *  - ANSWER → callee accepts the call
 *  - REJECT → callee declines the call
 *  - END    → either party ends/cancels the call
 *
 * Global state:
 *  - incomingCallInfo: non-null when there is an active incoming call waiting
 *    for the user to accept or reject.
 */
@Injectable()
export class CallSignalingService implements OnDestroy {
  private readonly ws = inject(WebSocketService);

  /** Reactive state: set when an OFFER arrives, cleared on ANSWER/REJECT/END. */
  readonly incomingCallInfo = signal<IncomingCallInfo | null>(null);

  /**
   * Set to true when callee accepts a call from the global modal.
   * DirectConversation reads this on init and opens video call immediately.
   */
  readonly pendingVideoOpen = signal(false);

  /** withVideo value from the OFFER — used when callee accepts from global modal */
  readonly pendingWithVideo = signal(true);

  /** Emits every raw signal received from the server. */
  private readonly signal$ = new Subject<CallSignalMessage>();

  /** Public observable for components that need to react to specific signal types. */
  readonly onSignal$ = this.signal$.asObservable();

  private subscription: Subscription | null = null;

  constructor() {
    this.startListening();
  }

  // ─── Subscription ───────────────────────────────────────────────────────────

  private startListening(): void {
    this.subscription = this.ws
      .subscribe<CallSignalMessage>(USER_CALL_SIGNAL)
      .subscribe(msg => this.handleIncoming(msg));
  }

  private handleIncoming(msg: CallSignalMessage): void {
    this.signal$.next(msg);

    switch (msg.type) {
      case 'OFFER':
        this.incomingCallInfo.set({
          conversationId: msg.conversationId,
          callerId: msg.callerId,
          callerName: msg.callerName,
          callerAvatar: msg.callerAvatar,
          // withVideo: null/undefined → mặc định true (video call)
          withVideo: msg.withVideo !== false,
        });
        break;

      case 'ANSWER':
      case 'REJECT':
      case 'END':
        // Clear any pending incoming call state
        this.incomingCallInfo.set(null);
        break;
    }
  }

  // ─── Send helpers ───────────────────────────────────────────────────────────

  /**
   * Caller sends OFFER to notify the remote user that a call is starting.
   */
  sendOffer(params: {
    conversationId: string;
    targetUserId: string;
    callerName: string;
    callerAvatar?: string;
    withVideo: boolean;
  }): void {
    this.publish({
      type: 'OFFER',
      conversationId: params.conversationId,
      targetUserId: params.targetUserId,
      callerId: '',
      callerName: params.callerName,
      callerAvatar: params.callerAvatar,
      withVideo: params.withVideo,
    });
  }

  /**
   * Callee sends ANSWER to accept the call.
   */
  sendAnswer(params: {
    conversationId: string;
    targetUserId: string;
    callerName: string;
  }): void {
    this.publish({
      type: 'ANSWER',
      conversationId: params.conversationId,
      targetUserId: params.targetUserId,
      callerId: '',
      callerName: params.callerName,
      withVideo: false,
    });
  }

  sendReject(params: {
    conversationId: string;
    targetUserId: string;
  }): void {
    this.publish({
      type: 'REJECT',
      conversationId: params.conversationId,
      targetUserId: params.targetUserId,
      callerId: '',
      callerName: '',
      withVideo: false,
    });
    this.incomingCallInfo.set(null);
  }

  /**
   * Either party sends END to terminate an ongoing call.
   */
  sendEnd(params: {
    conversationId: string;
    targetUserId: string;
  }): void {
    this.publish({
      type: 'END',
      conversationId: params.conversationId,
      targetUserId: params.targetUserId,
      callerId: '',
      callerName: '',
      withVideo: false,
    });
    this.incomingCallInfo.set(null);
  }

  /** Dismiss the incoming call modal without sending a signal (e.g. on navigation). */
  dismissIncomingCall(): void {
    this.incomingCallInfo.set(null);
  }

  // ─── Internal ───────────────────────────────────────────────────────────────

  private publish(msg: CallSignalMessage): void {
    this.ws.connected$
      .pipe(
        filter(Boolean),
        take(1),
      )
      .subscribe(() => this.ws.publish(APP_CALL_SIGNAL, msg));
  }

  ngOnDestroy(): void {
    this.subscription?.unsubscribe();
    this.signal$.complete();
  }
}

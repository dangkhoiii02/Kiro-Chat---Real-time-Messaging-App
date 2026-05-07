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

import {
  ChangeDetectionStrategy,
  Component,
  inject,
  input,
  output,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { IncomingCallInfo } from '../../service/call-signaling.service';

/**
 * IncomingCallModal
 *
 * A floating popup that appears when an OFFER signal is received.
 * It emits:
 *  - accept  → user clicked "Nghe" (Answer)
 *  - decline → user clicked "Từ chối" (Reject)
 */
@Component({
  selector: 'app-incoming-call-modal',
  standalone: true,
  imports: [CommonModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    @if (callInfo()) {
      <div
        role="dialog"
        aria-modal="true"
        aria-label="Cuộc gọi đến"
        class="fixed bottom-6 right-6 z-[9999] w-80 rounded-2xl bg-gray-900 text-white shadow-2xl
               border border-gray-700 p-5 flex flex-col gap-4 animate-slide-up"
      >
        <!-- Caller info -->
        <div class="flex items-center gap-3">
          <div class="relative shrink-0">
            <img
              [src]="callInfo()!.callerAvatar || '/assets/default-avatar.png'"
              alt="Avatar"
              class="w-14 h-14 rounded-full object-cover border-2 border-green-500"
            />
            <!-- Pulsing ring -->
            <span class="absolute inset-0 rounded-full border-2 border-green-400 animate-ping opacity-60"></span>
          </div>

          <div class="flex flex-col min-w-0">
            <span class="text-xs text-gray-400 uppercase tracking-wide">
              {{ callInfo()!.withVideo ? 'Cuộc gọi video đến' : 'Cuộc gọi thoại đến' }}
            </span>
            <span class="font-semibold text-base truncate">{{ callInfo()!.callerName }}</span>
          </div>
        </div>

        <!-- Action buttons -->
        <div class="flex gap-3">
          <!-- Decline -->
          <button
            (click)="onDecline()"
            class="flex-1 flex items-center justify-center gap-2 rounded-xl bg-red-600 hover:bg-red-700
                   active:scale-95 transition-all py-2.5 text-sm font-medium"
            aria-label="Từ chối cuộc gọi"
          >
            <svg xmlns="http://www.w3.org/2000/svg" class="w-5 h-5" viewBox="0 0 24 24" fill="none"
                 stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M10.68 13.31a16 16 0 0 0 3.41 2.6l1.27-1.27a2 2 0 0 1 2.11-.45
                       12.84 12.84 0 0 0 2.81.7 2 2 0 0 1 1.72 2v3a2 2 0 0 1-2.18 2
                       19.79 19.79 0 0 1-8.63-3.07 19.42 19.42 0 0 1-3.33-2.67
                       m-2.67-3.34a19.79 19.79 0 0 1-3.07-8.63A2 2 0 0 1 4.11 2h3
                       a2 2 0 0 1 2 1.72 12.84 12.84 0 0 0 .7 2.81 2 2 0 0 1-.45 2.11L8.09 9.91"/>
              <line x1="22" y1="2" x2="2" y2="22"/>
            </svg>
            Từ chối
          </button>

          <!-- Accept -->
          <button
            (click)="onAccept()"
            class="flex-1 flex items-center justify-center gap-2 rounded-xl bg-green-600 hover:bg-green-700
                   active:scale-95 transition-all py-2.5 text-sm font-medium"
            aria-label="Nghe cuộc gọi"
          >
            <svg xmlns="http://www.w3.org/2000/svg" class="w-5 h-5" viewBox="0 0 24 24" fill="none"
                 stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M22 16.92v3a2 2 0 0 1-2.18 2 19.79 19.79 0 0 1-8.63-3.07
                       19.13 19.13 0 0 1-6-6 19.79 19.79 0 0 1-3.07-8.67
                       A2 2 0 0 1 4.11 2h3a2 2 0 0 1 2 1.72
                       12.84 12.84 0 0 0 .7 2.81 2 2 0 0 1-.45 2.11L8.09 9.91
                       a16 16 0 0 0 6 6l1.27-1.27a2 2 0 0 1 2.11-.45
                       12.84 12.84 0 0 0 2.81.7A2 2 0 0 1 22 16.92z"/>
            </svg>
            Nghe
          </button>
        </div>
      </div>
    }
  `,
  styles: [`
    @keyframes slide-up {
      from { opacity: 0; transform: translateY(1.5rem); }
      to   { opacity: 1; transform: translateY(0); }
    }
    .animate-slide-up {
      animation: slide-up 0.25s ease-out both;
    }
  `],
})
export class IncomingCallModal {
  /** The incoming call data. When null the modal is hidden. */
  callInfo = input<IncomingCallInfo | null>(null);

  /** Emitted when the user clicks "Nghe" (Answer). */
  accept = output<void>();

  /** Emitted when the user clicks "Từ chối" (Reject). */
  decline = output<void>();

  onAccept(): void {
    this.accept.emit();
  }

  onDecline(): void {
    this.decline.emit();
  }
}

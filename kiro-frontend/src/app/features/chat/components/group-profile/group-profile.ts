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

import { Component, computed, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { GroupProfileService } from './group-profile.service';
import { Location, CommonModule } from '@angular/common';
import { IGroupParticipant } from '../../models/group-chat.models';
import { ActivatedRoute, Router } from '@angular/router';
import { ProfileApi } from '../../../user/services/profile.api';
import { forkJoin } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MessageTimePipe } from '../../pipe/message-time.pipe';
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'app-group-profile',
  imports: [MessageTimePipe, CommonModule, FormsModule],
  templateUrl: './group-profile.html',
  styleUrl: './group-profile.css',
})
export class GroupProfile implements OnInit {

  location = inject(Location);
  private readonly router = inject(Router);

  // ── State ──────────────────────────────────────────────────────────────────
  groupId = signal<string | null>(null);
  groupName = signal<string>('');
  profileImage = signal<string | null>(null);
  participantCount = signal<number>(0);
  createdAt = signal<string | null>(null);
  conversationId = signal<string | null>(null);

  participants = signal<IGroupParticipant[]>([]);
  currentUserId = signal<string | null>(null);
  loading = signal(true);
  error = signal<string | null>(null);
  actionError = signal<string | null>(null);
  actionSuccess = signal<string | null>(null);
  notificationsEnabled = signal(true);

  // Edit group name
  isEditingName = signal(false);
  editNameValue = signal('');
  isSavingName = signal(false);

  // Avatar upload
  isUploadingAvatar = signal(false);

  currentUserRole = computed(
    () =>
      this.participants().find((p) => p.userId === this.currentUserId())
        ?.role ?? 'member'
  );

  isAdmin = computed(() => this.currentUserRole() === 'admin');

  onlineCount = computed(
    () =>
      this.participants().filter((p) => p.presence?.status === 'online').length
  );

  private readonly svc = inject(GroupProfileService);
  private readonly route = inject(ActivatedRoute);
  private readonly profileApi = inject(ProfileApi);
  private readonly destroyRef = inject(DestroyRef);

  ngOnInit(): void {
    const cid = this.route.snapshot.paramMap.get('conversationId');

    if (!cid) {
      this.error.set('Conversation ID is missing from the route.');
      this.loading.set(false);
      return;
    }

    this.conversationId.set(cid);

    forkJoin({
      userId: this.profileApi.getCurrentUserId(),
      profile: this.svc.loadGroupProfile(cid),
    })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: ({ userId, profile }) => {
          this.currentUserId.set(userId);
          this.groupId.set(profile.meta.groupId);
          this.groupName.set(profile.meta.groupName);
          this.profileImage.set(profile.meta.profileImage);
          this.participantCount.set(profile.meta.participantCount);
          this.createdAt.set(profile.meta.createdAt);
          this.participants.set(profile.participants);
          this.loading.set(false);
        },
        error: () => {
          this.error.set('Failed to load group info. Please try again.');
          this.loading.set(false);
        },
      });
  }

  // ── Navigation ─────────────────────────────────────────────────────────────

  onBack(): void {
    this.location.back();
  }

  // ── Edit group name ────────────────────────────────────────────────────────

  startEditName(): void {
    this.editNameValue.set(this.groupName());
    this.isEditingName.set(true);
  }

  cancelEditName(): void {
    this.isEditingName.set(false);
    this.editNameValue.set('');
  }

  saveGroupName(): void {
    const gid = this.groupId();
    const name = this.editNameValue().trim();
    if (!gid || !name || name === this.groupName()) {
      this.isEditingName.set(false);
      return;
    }

    this.isSavingName.set(true);
    this.svc.updateGroupName(gid, name)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (updated) => {
          this.groupName.set(updated.groupName);
          this.isEditingName.set(false);
          this.isSavingName.set(false);
          this.showSuccess('Group name updated.');
        },
        error: () => {
          this.isSavingName.set(false);
          this.showActionError('Failed to update group name.');
        },
      });
  }

  // ── Avatar upload ──────────────────────────────────────────────────────────

  onAvatarFileSelected(event: Event): void {
    const file = (event.target as HTMLInputElement).files?.[0];
    const gid = this.groupId();
    if (!file || !gid) return;

    this.isUploadingAvatar.set(true);
    this.svc.updateGroupAvatar(gid, file)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (updated) => {
          this.profileImage.set(updated.profileImage);
          this.isUploadingAvatar.set(false);
          this.showSuccess('Group avatar updated.');
        },
        error: () => {
          this.isUploadingAvatar.set(false);
          this.showActionError('Failed to update group avatar.');
        },
      });
  }

  // ── Notifications ──────────────────────────────────────────────────────────

  toggleNotifications(): void {
    this.notificationsEnabled.update((v) => !v);
  }

  // ── Member actions (require backend endpoints not yet available) ───────────

  promoteToAdmin(participant: IGroupParticipant): void {
    // Backend endpoint not yet implemented — show informative message
    this.showActionError('Promote to admin is not yet supported by the server.');
  }

  removeMember(participant: IGroupParticipant): void {
    // Backend endpoint not yet implemented — show informative message
    this.showActionError('Remove member is not yet supported by the server.');
  }

  leaveGroup(): void {
    // Backend endpoint not yet implemented — show informative message
    this.showActionError('Leave group is not yet supported by the server.');
  }

  deleteGroup(): void {
    // Backend endpoint not yet implemented — show informative message
    this.showActionError('Delete group is not yet supported by the server.');
  }

  // ── Helpers ────────────────────────────────────────────────────────────────

  private showActionError(msg: string): void {
    this.actionError.set(msg);
    setTimeout(() => this.actionError.set(null), 4000);
  }

  private showSuccess(msg: string): void {
    this.actionSuccess.set(msg);
    setTimeout(() => this.actionSuccess.set(null), 3000);
  }
}

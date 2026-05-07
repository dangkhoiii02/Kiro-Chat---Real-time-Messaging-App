/*
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

package com.kiro.kiro_backend.core.identity.infrastructure.secondary.realtime;

import com.kiro.kiro_backend.common.ddd.infrastructure.stereotype.SecondaryPort;
import com.kiro.kiro_backend.core.groups.infrastructure.secondary.realtime.STOMPPresenceTrackingOperations;
import com.kiro.kiro_backend.core.identity.domain.aggregate.UserPresence;
import com.kiro.kiro_backend.core.identity.domain.repository.UserPresenceNotifier;
import com.kiro.kiro_backend.core.identity.infrastructure.secondary.entity.STOMPUserPresence;
import com.kiro.kiro_backend.core.identity.infrastructure.secondary.mapper.STOMPUserPresenceMapper;
import com.kiro.kiro_backend.core.messaging.domain.vo.UserSubcriberId;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@SecondaryPort
public class SpringWebSocketUserPresenceNotifier implements UserPresenceNotifier {
  private final STOMPPresenceTrackingOperations presenceOperations;
  private final STOMPUserPresenceMapper mapper;

  @Override
  public void notifyPresenceChange(UserSubcriberId forwardId, UserPresence userPresence) {
    STOMPUserPresence groupPresence = mapper.from(userPresence);
    presenceOperations.notifyUserPresenceChange(forwardId.value(), groupPresence);
  }

}

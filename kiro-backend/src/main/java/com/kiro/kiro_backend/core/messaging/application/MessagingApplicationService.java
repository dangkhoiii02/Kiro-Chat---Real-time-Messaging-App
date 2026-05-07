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

package com.kiro.kiro_backend.core.messaging.application;

import com.kiro.kiro_backend.common.ddd.infrastructure.stereotype.ApplicationService;
import com.kiro.kiro_backend.common.store.domain.aggregate.File;
import com.kiro.kiro_backend.core.messaging.domain.aggregate.Attachment;
import com.kiro.kiro_backend.core.messaging.domain.aggregate.ConversationMessageList;
import com.kiro.kiro_backend.core.messaging.domain.aggregate.MarkMessageSeenRequest;
import com.kiro.kiro_backend.core.messaging.domain.aggregate.Message;
import com.kiro.kiro_backend.core.messaging.domain.aggregate.MessageCursorQuery;
import com.kiro.kiro_backend.core.messaging.domain.aggregate.MessageReceiveAcknowledgement;
import com.kiro.kiro_backend.core.messaging.domain.aggregate.SendMessageRequest;
import com.kiro.kiro_backend.core.messaging.domain.event.MessageSentEvent;
import com.kiro.kiro_backend.core.messaging.domain.service.MessageManager;
import com.kiro.kiro_backend.core.messaging.domain.service.MessageSendingService;
import com.kiro.kiro_backend.core.messaging.domain.service.MessageTrackingStateService;

import lombok.RequiredArgsConstructor;

@ApplicationService
@RequiredArgsConstructor
public class MessagingApplicationService {
  private final MessageManager messageManager;
  private final MessageSendingService messageSendingService;
  private final MessageTrackingStateService messageTrackingStateService;

  public ConversationMessageList getConversationMessages(MessageCursorQuery query) {
    return messageManager.getDirectConversationMessages(query);
  }

  public Message sendDirectMessage(SendMessageRequest request) {
    return messageSendingService.prepareSendingMessage(request);
  }

  public Message sendGroupMessage(SendMessageRequest request) {
    return messageSendingService.prepareSendingMessage(request);
  }

  public Attachment uploadAttachment(File file) {
    return messageManager.uploadAttachment(file);
  }

  public void handleGroupMessageDelivery(MessageSentEvent event) {
    messageSendingService.sendGroupMessage(event);
  }

  public void handleDirectMessageDelivery(MessageSentEvent event) {
    messageSendingService.sendDirectMessage(event);
  }

  public Message markDirectMessageDelivered(MessageReceiveAcknowledgement messageReceiveRequest) {

    return messageTrackingStateService.markDirectMessageDelivered(messageReceiveRequest);
  }

  public Message markGroupMessageDelivered(MessageReceiveAcknowledgement messageReceiveRequest) {

    return messageTrackingStateService.markGroupMessageDelivered(messageReceiveRequest);
  }

  public void markDirectMessageSeen(MarkMessageSeenRequest request) {
    messageTrackingStateService.markDirectMessageSeen(request);
  }

  public void markGroupMessageSeen(MarkMessageSeenRequest request) {
    messageTrackingStateService.markGroupMessageSeen(request);
  }
}

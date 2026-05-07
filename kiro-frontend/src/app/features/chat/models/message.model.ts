export interface IAttachment {
  url: string;
  fileName: string;
  fileType: string;
  fileSize: number;
  messageType: MessageType;
}

export interface IConversationMessageList {
  messages: IConversationMessage[];
  hasMore: boolean;
}

export type MessageType = 'text' | 'file' | 'image' | 'video' | 'audio' | 'location' | 'call';

export interface IMessage {
  messageId: string;
  conversationId: string;
  senderId: string;
  content?: string;
  timestamp: Date;
  type: MessageType;
  messageState: MessageState;
  mediaName?: string;
  mediaUrl?: string;
  fileSize?: number;
  fileType?: string;
  callDuration?: number;
  isDeleted: boolean;
}

export interface IConversationMessage extends IMessage {
  owner: boolean;
}

export type ParticipantState = 'online' | 'offline' | 'away';

export type MessageState = 'created' | 'prepare' | 'sent' | 'delivered' | 'seen' | 'failed';

export interface ISendMessageRequest {
  conversationId: string;
  content?: string;
  type: MessageType;
  replyToMessageId?: string;
  attachment?: IAttachment;
  callDuration?: number;
}

export interface IMarkMessageSeenRequest {
  lastSeenMessageId: string;
  conversationId: string;
}

export interface IMarkMessageSeenResponse {
  seenAt: Date;
}

export interface IMessageSeenEvent {
  messageId: string;
  conversationId: string;
}

export interface IMessageStatusEvent {
  messageId: string;
  status: MessageState;
  conversationId: string;
}

export interface IFileUploadRequest {
  file: File;
  type: 'file';
}

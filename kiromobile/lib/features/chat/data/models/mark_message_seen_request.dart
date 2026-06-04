class MarkMessageSeenRequest {
  const MarkMessageSeenRequest({
    required this.conversationId,
    required this.lastSeenMessageId,
  });

  final String conversationId;
  final String lastSeenMessageId;

  Map<String, dynamic> toJson() {
    return {'lastSeenMessageId': lastSeenMessageId};
  }
}

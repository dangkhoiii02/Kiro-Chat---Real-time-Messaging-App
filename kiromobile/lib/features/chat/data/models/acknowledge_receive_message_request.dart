class AcknowledgeReceiveMessageRequest {
  const AcknowledgeReceiveMessageRequest({required this.messageId});

  final String messageId;

  Map<String, dynamic> toJson() {
    return {'messageId': messageId};
  }
}

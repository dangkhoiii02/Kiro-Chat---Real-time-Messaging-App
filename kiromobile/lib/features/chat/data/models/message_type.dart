enum MessageType {
  text('text'),
  file('file'),
  image('image'),
  video('video'),
  call('call'),
  unknown('unknown');

  const MessageType(this.value);

  final String value;

  static MessageType fromJson(String? value) {
    return MessageType.values.firstWhere(
      (type) => type.value == value,
      orElse: () => MessageType.unknown,
    );
  }
}

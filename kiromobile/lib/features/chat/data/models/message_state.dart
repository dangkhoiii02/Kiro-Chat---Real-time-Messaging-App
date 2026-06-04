enum MessageState {
  prepare('prepare'),
  sent('sent'),
  delivered('delivered'),
  seen('seen'),
  failed('failed'),
  unknown('unknown');

  const MessageState(this.value);

  final String value;

  static MessageState fromJson(String? value) {
    return MessageState.values.firstWhere(
      (state) => state.value == value,
      orElse: () => MessageState.unknown,
    );
  }
}

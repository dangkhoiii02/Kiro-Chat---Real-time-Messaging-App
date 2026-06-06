class PresenceEvent {
  const PresenceEvent({
    required this.userId,
    required this.isOnline,
    this.lastSeen,
    this.connectedAt,
  });

  final String userId;
  final bool isOnline;
  final DateTime? lastSeen;
  final DateTime? connectedAt;

  factory PresenceEvent.fromJson(Map<String, dynamic> json) {
    return PresenceEvent(
      userId: json['userId'] as String,
      isOnline: _onlineFromJson(json),
      lastSeen: _dateTimeFromJson(json['lastSeen']),
      connectedAt: _dateTimeFromJson(json['connectedAt']),
    );
  }

  static bool _onlineFromJson(Map<String, dynamic> json) {
    final isOnline = json['isOnline'];
    if (isOnline is bool) {
      return isOnline;
    }

    final online = json['online'];
    if (online is bool) {
      return online;
    }

    final status = json['status'] as String?;
    return status?.trim().toLowerCase() == 'online';
  }

  static DateTime? _dateTimeFromJson(Object? value) {
    if (value is! String || value.trim().isEmpty) {
      return null;
    }

    return DateTime.tryParse(value);
  }
}

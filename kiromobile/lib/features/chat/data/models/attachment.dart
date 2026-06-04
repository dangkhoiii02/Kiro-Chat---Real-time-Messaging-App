import 'package:kiromobile/features/chat/data/models/message_type.dart';

class Attachment {
  const Attachment({
    required this.url,
    required this.fileName,
    required this.fileType,
    required this.fileSize,
  });

  final String url;
  final String fileName;
  final String fileType;
  final int fileSize;

  MessageType get messageType {
    final normalized = fileType.toLowerCase();
    if (normalized.startsWith('image/')) {
      return MessageType.image;
    }
    if (normalized.startsWith('video/')) {
      return MessageType.video;
    }
    if (normalized.startsWith('text/')) {
      return MessageType.text;
    }
    return MessageType.file;
  }

  factory Attachment.fromJson(Map<String, dynamic> json) {
    return Attachment(
      url: json['url'] as String,
      fileName: json['fileName'] as String,
      fileType: json['fileType'] as String,
      fileSize: (json['fileSize'] as num).toInt(),
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'url': url,
      'fileName': fileName,
      'fileType': fileType,
      'fileSize': fileSize,
    };
  }
}

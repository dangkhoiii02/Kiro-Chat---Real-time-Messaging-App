import 'package:kiromobile/features/chat/data/models/conversation.dart';
import 'package:kiromobile/features/chat/data/models/page_response.dart';

class UserChatList {
  const UserChatList({required this.conversations});

  final PageResponse<Conversation> conversations;

  factory UserChatList.fromJson(Map<String, dynamic> json) {
    return UserChatList(
      conversations: PageResponse.fromJson(
        json['conversations'] as Map<String, dynamic>,
        Conversation.fromJson,
      ),
    );
  }
}

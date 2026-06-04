import 'package:flutter_test/flutter_test.dart';
import 'package:kiromobile/features/profile/data/models/current_user.dart';

void main() {
  test('fromJson maps /users/me profile response', () {
    final user = CurrentUser.fromJson({
      'userId': '20b7df33-a1d6-4c80-bc30-9f5a832d9439',
      'emailAddress': 'user@example.com',
      'firstname': 'Kiro',
      'lastname': 'User',
      'username': 'kiro_user',
      'profilePictureUrl': 'http://example.com/avatar.png',
    });

    expect(user.userId, '20b7df33-a1d6-4c80-bc30-9f5a832d9439');
    expect(user.emailAddress, 'user@example.com');
    expect(user.firstname, 'Kiro');
    expect(user.lastname, 'User');
    expect(user.username, 'kiro_user');
    expect(user.profilePictureUrl, 'http://example.com/avatar.png');
    expect(user.displayName, 'Kiro User');
  });
}

import 'package:dio/dio.dart';
import 'package:kiromobile/core/network/dio_client.dart';
import 'package:kiromobile/features/auth/data/repositories/auth_repository.dart';
import 'package:kiromobile/features/profile/data/models/current_user.dart';

class ProfileRepository {
  const ProfileRepository(this._dioClient, this._authRepository);

  final DioClient _dioClient;
  final AuthRepository _authRepository;

  Future<CurrentUser> getCurrentUser() async {
    final accessToken = await _authRepository.getAccessToken();

    if (accessToken == null || accessToken.isEmpty) {
      throw const ProfileException('Chưa có access token để tải profile.');
    }

    final response = await _dioClient.dio.get<Map<String, dynamic>>(
      'users/me',
      options: Options(headers: {'Authorization': 'Bearer $accessToken'}),
    );
    final data = response.data;

    if (data == null) {
      throw const ProfileException('Backend không trả về dữ liệu profile.');
    }

    return CurrentUser.fromJson(data);
  }
}

class ProfileException implements Exception {
  const ProfileException(this.message);

  final String message;

  @override
  String toString() => message;
}

import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:kiromobile/core/config/app_config.dart';
import 'package:kiromobile/features/auth/presentation/providers/auth_provider.dart';

final dioClientProvider = Provider<DioClient>((ref) {
  final authRepository = ref.watch(authRepositoryProvider);

  return DioClient(accessTokenReader: authRepository.getAccessToken);
});

class DioClient {
  DioClient({required Future<String?> Function() accessTokenReader}) {
    dio.interceptors.add(
      InterceptorsWrapper(
        onRequest: (options, handler) async {
          final accessToken = await accessTokenReader();

          if (accessToken != null && accessToken.isNotEmpty) {
            options.headers['Authorization'] = 'Bearer $accessToken';
          }

          handler.next(options);
        },
      ),
    );
  }

  final Dio dio = Dio(
    BaseOptions(
      baseUrl: AppConfig.apiBaseUrl,
      connectTimeout: const Duration(seconds: 30),
      receiveTimeout: const Duration(seconds: 30),
      headers: {'Content-Type': 'application/json'},
    ),
  );
}

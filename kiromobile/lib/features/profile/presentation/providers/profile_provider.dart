import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:kiromobile/core/network/dio_client.dart';
import 'package:kiromobile/features/auth/presentation/providers/auth_provider.dart';
import 'package:kiromobile/features/profile/data/models/current_user.dart';
import 'package:kiromobile/features/profile/data/repositories/profile_repository.dart';

final profileRepositoryProvider = Provider<ProfileRepository>((ref) {
  return ProfileRepository(
    ref.watch(dioClientProvider),
    ref.watch(authRepositoryProvider),
  );
});

final currentUserProvider = FutureProvider<CurrentUser>((ref) {
  return ref.watch(profileRepositoryProvider).getCurrentUser();
});

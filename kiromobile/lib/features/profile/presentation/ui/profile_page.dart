import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:kiromobile/core/route/route_name.dart';
import 'package:kiromobile/features/auth/presentation/providers/auth_provider.dart';
import 'package:kiromobile/features/profile/presentation/providers/profile_provider.dart';

class ProfilePage extends ConsumerWidget {
  const ProfilePage({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final currentUser = ref.watch(currentUserProvider);

    return Scaffold(
      appBar: AppBar(
        title: const Text('Profile'),
        actions: [
          IconButton(
            tooltip: 'Chats',
            onPressed: () => context.go(appChatsRoute),
            icon: const Icon(Icons.chat_bubble_outline),
          ),
          IconButton(
            tooltip: 'Đăng xuất',
            onPressed: () => ref.read(authControllerProvider.notifier).logout(),
            icon: const Icon(Icons.logout),
          ),
        ],
      ),
      body: currentUser.when(
        loading: () => const Center(child: CircularProgressIndicator()),
        error: (error, stackTrace) => Center(
          child: Padding(
            padding: const EdgeInsets.all(24),
            child: Text(
              'Không thể tải profile.\n$error',
              textAlign: TextAlign.center,
            ),
          ),
        ),
        data: (user) => ListView(
          padding: const EdgeInsets.all(24),
          children: [
            CircleAvatar(
              radius: 40,
              backgroundImage: user.profilePictureUrl == null
                  ? null
                  : NetworkImage(user.profilePictureUrl!),
              child: user.profilePictureUrl == null
                  ? const Icon(Icons.person_outline, size: 40)
                  : null,
            ),
            const SizedBox(height: 24),
            Text(
              user.displayName,
              textAlign: TextAlign.center,
              style: Theme.of(context).textTheme.headlineSmall,
            ),
            const SizedBox(height: 8),
            if (user.username != null)
              Text(
                '@${user.username}',
                textAlign: TextAlign.center,
                style: Theme.of(context).textTheme.bodyMedium,
              ),
            if (user.emailAddress != null) ...[
              const SizedBox(height: 8),
              Text(
                user.emailAddress!,
                textAlign: TextAlign.center,
                style: Theme.of(context).textTheme.bodyMedium,
              ),
            ],
          ],
        ),
      ),
    );
  }
}

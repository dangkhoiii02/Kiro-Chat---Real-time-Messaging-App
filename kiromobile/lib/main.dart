import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:kiromobile/core/route/go_router_provider.dart';
import 'package:kiromobile/features/auth/presentation/providers/auth_provider.dart';
import 'package:kiromobile/features/chat/presentation/providers/app_realtime_provider.dart';

void main() {
  runApp(const ProviderScope(child: KiroApp()));
}

class KiroApp extends ConsumerWidget {
  const KiroApp({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final router = ref.watch(goRouterProvider);
    final authState = ref.watch(authControllerProvider);

    Future.microtask(() {
      final realtimeController = ref.read(
        appRealtimeControllerProvider.notifier,
      );
      if (authState.status == AuthStatus.authenticated) {
        unawaited(realtimeController.start());
      } else {
        unawaited(realtimeController.stop());
      }
    });

    return MaterialApp.router(routerConfig: router, title: 'Kiro Chat');
  }
}

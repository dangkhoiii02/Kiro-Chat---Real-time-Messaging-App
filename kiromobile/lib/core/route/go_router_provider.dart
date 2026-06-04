import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:kiromobile/core/route/route_name.dart';
import 'package:kiromobile/features/auth/presentation/providers/auth_provider.dart';
import 'package:kiromobile/features/auth/presentation/ui/splash_page.dart';
import 'package:kiromobile/features/chat/data/models/conversation.dart';
import 'package:kiromobile/features/chat/presentation/ui/chat_detail_page.dart';
import 'package:kiromobile/features/chat/presentation/ui/chat_list_page.dart';
import 'package:kiromobile/features/contact/presentation/ui/contacts_page.dart';
import 'package:kiromobile/features/auth/presentation/ui/signup_page.dart';
import 'package:kiromobile/features/auth/presentation/ui/login_page.dart';
import 'package:kiromobile/features/profile/presentation/ui/profile_page.dart';

final goRouterProvider = Provider<GoRouter>((ref) {
  final authState = ref.watch(authControllerProvider);

  return GoRouter(
    initialLocation: splashRoute,
    redirect: (context, state) {
      final location = state.matchedLocation;
      final isAuthRoute = location == loginRoute || location == signUpRoute;

      if (authState.status == AuthStatus.unknown ||
          authState.status == AuthStatus.loading) {
        return location == splashRoute ? null : splashRoute;
      }

      if (authState.status == AuthStatus.unauthenticated) {
        return isAuthRoute ? null : loginRoute;
      }

      if (authState.status == AuthStatus.authenticated) {
        if (location == splashRoute || isAuthRoute) {
          return appChatsRoute;
        }
      }

      return null;
    },
    routes: [
      GoRoute(
        path: splashRoute,
        builder: (context, state) => const SplashPage(),
      ),
      GoRoute(
        path: loginRoute,
        builder: (context, state) => const LoginScreen(),
      ),
      GoRoute(
        path: signUpRoute,
        builder: (context, state) => const SignupPage(),
      ),
      GoRoute(
        path: appChatsRoute,
        builder: (context, state) => const ChatListPage(),
      ),
      GoRoute(
        path: appChatDetailRoute,
        builder: (context, state) {
          final conversation = state.extra;

          if (conversation is! Conversation) {
            return const ChatListPage();
          }

          return ChatDetailPage(conversation: conversation);
        },
      ),
      GoRoute(
        path: appContactsRoute,
        builder: (context, state) => const ContactsPage(),
      ),
      GoRoute(
        path: appProfileRoute,
        builder: (context, state) => const ProfilePage(),
      ),
    ],
  );
});

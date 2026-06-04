import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:kiromobile/core/route/route_name.dart';
import 'package:kiromobile/features/chat/data/models/conversation.dart';
import 'package:kiromobile/features/contact/data/models/contact_profile.dart';
import 'package:kiromobile/features/contact/data/models/contact_request.dart';
import 'package:kiromobile/features/contact/data/models/friend.dart';
import 'package:kiromobile/features/contact/data/models/friendship_status.dart';
import 'package:kiromobile/features/contact/presentation/providers/contacts_controller.dart';
import 'package:kiromobile/features/contact/presentation/providers/friend_requests_controller.dart';
import 'package:kiromobile/features/contact/presentation/providers/user_search_controller.dart';

class ContactsPage extends ConsumerStatefulWidget {
  const ContactsPage({super.key});

  @override
  ConsumerState<ContactsPage> createState() => _ContactsPageState();
}

class _ContactsPageState extends ConsumerState<ContactsPage> {
  final _friendSearchController = TextEditingController();
  final _userSearchController = TextEditingController();
  Timer? _searchDebounce;

  @override
  void initState() {
    super.initState();

    Future.microtask(() {
      ref.read(contactsControllerProvider.notifier).loadFriends();
      ref.read(friendRequestsControllerProvider.notifier).loadRequests();
    });
  }

  @override
  void dispose() {
    _searchDebounce?.cancel();
    _friendSearchController.dispose();
    _userSearchController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return DefaultTabController(
      length: 3,
      child: Scaffold(
        backgroundColor: const Color(0xFFF7F8FA),
        appBar: AppBar(
          title: const Text(
            'Contacts',
            style: TextStyle(fontWeight: FontWeight.w800, fontSize: 28),
          ),
          centerTitle: false,
          backgroundColor: Colors.white,
          foregroundColor: Colors.black,
          elevation: 0,
          bottom: const TabBar(
            labelColor: Colors.black,
            unselectedLabelColor: Color(0xFF7D7F87),
            indicatorColor: Colors.black,
            tabs: [
              Tab(text: 'Friends'),
              Tab(text: 'Requests'),
              Tab(text: 'Search'),
            ],
          ),
        ),
        body: TabBarView(
          children: [
            _FriendsTab(searchController: _friendSearchController),
            const _RequestsTab(),
            _SearchTab(
              controller: _userSearchController,
              onChanged: _onUserSearchChanged,
            ),
          ],
        ),
        bottomNavigationBar: NavigationBar(
          selectedIndex: 2,
          onDestinationSelected: (index) {
            if (index == 1) {
              context.go(appChatsRoute);
            } else if (index == 3) {
              context.go(appProfileRoute);
            }
          },
          destinations: const [
            NavigationDestination(
              icon: Icon(Icons.dashboard_outlined),
              label: 'Dashboard',
            ),
            NavigationDestination(
              icon: Icon(Icons.chat_bubble_outline),
              selectedIcon: Icon(Icons.chat_bubble),
              label: 'Chat',
            ),
            NavigationDestination(
              icon: Icon(Icons.groups_outlined),
              selectedIcon: Icon(Icons.groups),
              label: 'Contacts',
            ),
            NavigationDestination(
              icon: Icon(Icons.person_outline),
              label: 'Profile',
            ),
          ],
        ),
      ),
    );
  }

  void _onUserSearchChanged(String value) {
    _searchDebounce?.cancel();
    _searchDebounce = Timer(const Duration(milliseconds: 400), () {
      ref.read(userSearchControllerProvider.notifier).searchUsers(value);
    });
  }
}

class _FriendsTab extends ConsumerWidget {
  const _FriendsTab({required this.searchController});

  final TextEditingController searchController;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final state = ref.watch(contactsControllerProvider);

    return Column(
      children: [
        _SearchField(
          controller: searchController,
          hintText: 'Search friends...',
          onSubmitted: (value) {
            ref
                .read(contactsControllerProvider.notifier)
                .loadFriends(query: value);
          },
        ),
        Expanded(
          child: switch (state.status) {
            ContactStatus.initial || ContactStatus.loading => const Center(
              child: CircularProgressIndicator(),
            ),
            ContactStatus.failure => _ErrorState(
              message: state.errorMessage ?? 'Cannot load friends.',
              onRetry: () =>
                  ref.read(contactsControllerProvider.notifier).loadFriends(),
            ),
            ContactStatus.success =>
              state.friends.isEmpty
                  ? const _EmptyState(message: 'No friends yet')
                  : RefreshIndicator(
                      onRefresh: () => ref
                          .read(contactsControllerProvider.notifier)
                          .loadFriends(),
                      child: ListView.separated(
                        itemCount: state.friends.length,
                        separatorBuilder: (_, _) => const Divider(
                          height: 1,
                          thickness: 1,
                          color: Color(0xFFE1E4EA),
                        ),
                        itemBuilder: (context, index) {
                          return _FriendTile(friend: state.friends[index]);
                        },
                      ),
                    ),
          },
        ),
      ],
    );
  }
}

class _RequestsTab extends ConsumerWidget {
  const _RequestsTab();

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final state = ref.watch(friendRequestsControllerProvider);

    return switch (state.status) {
      FriendRequestsStatus.initial || FriendRequestsStatus.loading =>
        const Center(child: CircularProgressIndicator()),
      FriendRequestsStatus.failure => _ErrorState(
        message: state.errorMessage ?? 'Cannot load requests.',
        onRetry: () =>
            ref.read(friendRequestsControllerProvider.notifier).loadRequests(),
      ),
      FriendRequestsStatus.success =>
        state.requests.isEmpty
            ? const _EmptyState(message: 'No pending requests')
            : RefreshIndicator(
                onRefresh: () => ref
                    .read(friendRequestsControllerProvider.notifier)
                    .loadRequests(),
                child: ListView.separated(
                  itemCount: state.requests.length,
                  separatorBuilder: (_, _) => const Divider(
                    height: 1,
                    thickness: 1,
                    color: Color(0xFFE1E4EA),
                  ),
                  itemBuilder: (context, index) {
                    return _RequestTile(request: state.requests[index]);
                  },
                ),
              ),
    };
  }
}

class _SearchTab extends ConsumerWidget {
  const _SearchTab({required this.controller, required this.onChanged});

  final TextEditingController controller;
  final ValueChanged<String> onChanged;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final state = ref.watch(userSearchControllerProvider);

    return Column(
      children: [
        _SearchField(
          controller: controller,
          hintText: 'Search users...',
          onChanged: onChanged,
          onSubmitted: (value) => ref
              .read(userSearchControllerProvider.notifier)
              .searchUsers(value),
        ),
        Expanded(
          child: switch (state.status) {
            UserSearchStatus.initial => const _EmptyState(
              message: 'Search by name or username',
            ),
            UserSearchStatus.loading => const Center(
              child: CircularProgressIndicator(),
            ),
            UserSearchStatus.failure => _ErrorState(
              message: state.errorMessage ?? 'Cannot search users.',
              onRetry: () => ref
                  .read(userSearchControllerProvider.notifier)
                  .searchUsers(state.query),
            ),
            UserSearchStatus.success =>
              state.users.isEmpty
                  ? const _EmptyState(message: 'No users found')
                  : ListView.separated(
                      itemCount: state.users.length,
                      separatorBuilder: (_, _) => const Divider(
                        height: 1,
                        thickness: 1,
                        color: Color(0xFFE1E4EA),
                      ),
                      itemBuilder: (context, index) {
                        return _SearchUserTile(user: state.users[index]);
                      },
                    ),
          },
        ),
      ],
    );
  }
}

class _FriendTile extends ConsumerWidget {
  const _FriendTile({required this.friend});

  final Friend friend;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    return _ContactListTile(
      title: friend.displayName,
      subtitle: friend.username == null ? null : '@${friend.username}',
      avatarUrl: friend.profilePictureUrl,
      isOnline: friend.isOnline,
      trailing: IconButton(
        tooltip: 'Message',
        onPressed: () async {
          final conversation = await ref
              .read(contactsControllerProvider.notifier)
              .openChat(friend.userId);
          if (context.mounted) {
            _goToConversation(context, conversation);
          }
        },
        icon: const Icon(Icons.chat_bubble_outline),
      ),
      onTap: () async {
        final conversation = await ref
            .read(contactsControllerProvider.notifier)
            .openChat(friend.userId);
        if (context.mounted) {
          _goToConversation(context, conversation);
        }
      },
    );
  }
}

class _RequestTile extends ConsumerWidget {
  const _RequestTile({required this.request});

  final ContactRequest request;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    return _ContactListTile(
      title: request.displayName,
      subtitle: request.username == null ? null : '@${request.username}',
      avatarUrl: request.profilePictureUrl,
      trailing: Wrap(
        spacing: 8,
        children: [
          IconButton.filled(
            tooltip: 'Accept',
            onPressed: () async {
              await ref
                  .read(friendRequestsControllerProvider.notifier)
                  .acceptRequest(request.requestUserId);
              if (context.mounted) {
                _showActionError(
                  context,
                  ref.read(friendRequestsControllerProvider).actionErrorMessage,
                );
              }
            },
            icon: const Icon(Icons.check),
          ),
          IconButton.outlined(
            tooltip: 'Reject',
            onPressed: () async {
              await ref
                  .read(friendRequestsControllerProvider.notifier)
                  .rejectRequest(request.requestUserId);
              if (context.mounted) {
                _showActionError(
                  context,
                  ref.read(friendRequestsControllerProvider).actionErrorMessage,
                );
              }
            },
            icon: const Icon(Icons.close),
          ),
        ],
      ),
    );
  }
}

class _SearchUserTile extends ConsumerWidget {
  const _SearchUserTile({required this.user});

  final ContactProfile user;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    return _ContactListTile(
      title: user.displayName,
      subtitle: user.username == null ? null : '@${user.username}',
      avatarUrl: user.profilePictureUrl,
      trailing: _SearchActionButtons(user: user),
    );
  }
}

class _SearchActionButtons extends ConsumerWidget {
  const _SearchActionButtons({required this.user});

  final ContactProfile user;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final controller = ref.read(userSearchControllerProvider.notifier);

    return switch (user.friendshipStatus) {
      FriendshipStatus.notConnected ||
      FriendshipStatus.notDetermined => FilledButton.icon(
        onPressed: () async {
          await controller.sendRequest(user.userId);
          if (context.mounted) {
            _showActionError(
              context,
              ref.read(userSearchControllerProvider).actionErrorMessage,
            );
          }
        },
        icon: const Icon(Icons.person_add_alt_1),
        label: const Text('Add'),
      ),
      FriendshipStatus.friendRequestSent => OutlinedButton.icon(
        onPressed: () async {
          await controller.cancelRequest(user.userId);
          if (context.mounted) {
            _showActionError(
              context,
              ref.read(userSearchControllerProvider).actionErrorMessage,
            );
          }
        },
        icon: const Icon(Icons.hourglass_empty),
        label: const Text('Pending'),
      ),
      FriendshipStatus.friendRequestReceived => Wrap(
        spacing: 8,
        children: [
          IconButton.filled(
            tooltip: 'Accept',
            onPressed: () async {
              await controller.acceptRequest(user.userId);
              if (context.mounted) {
                _showActionError(
                  context,
                  ref.read(userSearchControllerProvider).actionErrorMessage,
                );
              }
            },
            icon: const Icon(Icons.check),
          ),
          IconButton.outlined(
            tooltip: 'Reject',
            onPressed: () async {
              await controller.rejectRequest(user.userId);
              if (context.mounted) {
                _showActionError(
                  context,
                  ref.read(userSearchControllerProvider).actionErrorMessage,
                );
              }
            },
            icon: const Icon(Icons.close),
          ),
        ],
      ),
      FriendshipStatus.friends => IconButton(
        tooltip: 'Message',
        onPressed: () async {
          final conversation = await controller.openChat(user.userId);
          if (context.mounted) {
            _goToConversation(context, conversation);
          }
        },
        icon: const Icon(Icons.chat_bubble_outline),
      ),
      FriendshipStatus.blocked ||
      FriendshipStatus.blockedBy ||
      FriendshipStatus.unknown => const IconButton(
        tooltip: 'Unavailable',
        onPressed: null,
        icon: Icon(Icons.block),
      ),
    };
  }
}

class _SearchField extends StatelessWidget {
  const _SearchField({
    required this.controller,
    required this.hintText,
    this.onChanged,
    this.onSubmitted,
  });

  final TextEditingController controller;
  final String hintText;
  final ValueChanged<String>? onChanged;
  final ValueChanged<String>? onSubmitted;

  @override
  Widget build(BuildContext context) {
    return Container(
      color: const Color(0xFFF7F8FA),
      padding: const EdgeInsets.fromLTRB(24, 18, 24, 18),
      child: TextField(
        controller: controller,
        textInputAction: TextInputAction.search,
        onChanged: onChanged,
        onSubmitted: onSubmitted,
        decoration: InputDecoration(
          hintText: hintText,
          prefixIcon: const Icon(Icons.search),
          filled: true,
          fillColor: Colors.white,
          contentPadding: const EdgeInsets.symmetric(vertical: 18),
          border: OutlineInputBorder(
            borderRadius: BorderRadius.circular(12),
            borderSide: const BorderSide(color: Color(0xFFE1E4EA)),
          ),
          enabledBorder: OutlineInputBorder(
            borderRadius: BorderRadius.circular(12),
            borderSide: const BorderSide(color: Color(0xFFE1E4EA)),
          ),
          focusedBorder: OutlineInputBorder(
            borderRadius: BorderRadius.circular(12),
            borderSide: const BorderSide(color: Colors.black, width: 1.2),
          ),
        ),
      ),
    );
  }
}

class _ContactListTile extends StatelessWidget {
  const _ContactListTile({
    required this.title,
    this.subtitle,
    this.avatarUrl,
    this.isOnline = false,
    this.trailing,
    this.onTap,
  });

  final String title;
  final String? subtitle;
  final String? avatarUrl;
  final bool isOnline;
  final Widget? trailing;
  final VoidCallback? onTap;

  @override
  Widget build(BuildContext context) {
    return Material(
      color: Colors.white,
      child: InkWell(
        onTap: onTap,
        child: Padding(
          padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 16),
          child: Row(
            children: [
              _ContactAvatar(
                name: title,
                avatarUrl: avatarUrl,
                isOnline: isOnline,
              ),
              const SizedBox(width: 18),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      title,
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                      style: const TextStyle(
                        fontSize: 18,
                        fontWeight: FontWeight.w800,
                      ),
                    ),
                    if (subtitle != null) ...[
                      const SizedBox(height: 4),
                      Text(
                        subtitle!,
                        maxLines: 1,
                        overflow: TextOverflow.ellipsis,
                        style: const TextStyle(
                          fontSize: 15,
                          color: Color(0xFF7D7F87),
                        ),
                      ),
                    ],
                  ],
                ),
              ),
              if (trailing != null) ...[const SizedBox(width: 12), trailing!],
            ],
          ),
        ),
      ),
    );
  }
}

class _ContactAvatar extends StatelessWidget {
  const _ContactAvatar({
    required this.name,
    this.avatarUrl,
    this.isOnline = false,
  });

  final String name;
  final String? avatarUrl;
  final bool isOnline;

  @override
  Widget build(BuildContext context) {
    final initial = name.trim().isEmpty ? '?' : name.trim()[0].toUpperCase();

    return Stack(
      clipBehavior: Clip.none,
      children: [
        CircleAvatar(
          radius: 28,
          backgroundColor: const Color(0xFFE9EAEE),
          foregroundImage: avatarUrl == null || avatarUrl!.isEmpty
              ? null
              : NetworkImage(avatarUrl!),
          child: Text(
            initial,
            style: const TextStyle(
              color: Color(0xFF5B5E68),
              fontSize: 20,
              fontWeight: FontWeight.w800,
            ),
          ),
        ),
        if (isOnline)
          Positioned(
            right: 1,
            bottom: 1,
            child: Container(
              width: 12,
              height: 12,
              decoration: BoxDecoration(
                color: const Color(0xFF9CA3AF),
                shape: BoxShape.circle,
                border: Border.all(color: Colors.white, width: 2),
              ),
            ),
          ),
      ],
    );
  }
}

class _ErrorState extends StatelessWidget {
  const _ErrorState({required this.message, required this.onRetry});

  final String message;
  final VoidCallback onRetry;

  @override
  Widget build(BuildContext context) {
    return Center(
      child: Padding(
        padding: const EdgeInsets.all(24),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            const Icon(
              Icons.wifi_off_outlined,
              size: 40,
              color: Color(0xFF747681),
            ),
            const SizedBox(height: 12),
            Text(
              message,
              textAlign: TextAlign.center,
              style: const TextStyle(color: Color(0xFF5B5E68)),
            ),
            const SizedBox(height: 16),
            FilledButton(onPressed: onRetry, child: const Text('Try again')),
          ],
        ),
      ),
    );
  }
}

class _EmptyState extends StatelessWidget {
  const _EmptyState({required this.message});

  final String message;

  @override
  Widget build(BuildContext context) {
    return Center(
      child: Text(
        message,
        style: const TextStyle(color: Color(0xFF7D7F87), fontSize: 16),
      ),
    );
  }
}

void _goToConversation(BuildContext context, Conversation? conversation) {
  if (conversation == null) {
    ScaffoldMessenger.of(
      context,
    ).showSnackBar(const SnackBar(content: Text('Cannot open conversation.')));
    return;
  }

  context.go('/app/chats/${conversation.conversationId}', extra: conversation);
}

void _showActionError(BuildContext context, String? message) {
  if (message == null || message.isEmpty) {
    return;
  }

  ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(message)));
}

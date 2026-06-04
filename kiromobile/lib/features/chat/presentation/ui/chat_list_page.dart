import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:kiromobile/core/route/route_name.dart';
import 'package:kiromobile/features/chat/data/models/conversation.dart';
import 'package:kiromobile/features/chat/presentation/providers/chat_list_provider.dart';

class ChatListPage extends ConsumerStatefulWidget {
  const ChatListPage({super.key});

  @override
  ConsumerState<ChatListPage> createState() => _ChatListPageState();
}

class _ChatListPageState extends ConsumerState<ChatListPage> {
  final _searchController = TextEditingController();

  @override
  void initState() {
    super.initState();

    Future.microtask(() {
      ref.read(chatListControllerProvider.notifier).loadConversations();
    });
  }

  @override
  void dispose() {
    _searchController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final chatListState = ref.watch(chatListControllerProvider);

    return Scaffold(
      backgroundColor: const Color(0xFFF7F8FA),
      appBar: AppBar(
        title: const Text(
          'Messages',
          style: TextStyle(fontWeight: FontWeight.w800, fontSize: 28),
        ),
        centerTitle: false,
        backgroundColor: Colors.white,
        foregroundColor: Colors.black,
        elevation: 0,
        actions: [
          IconButton(
            tooltip: 'Notifications',
            onPressed: () {},
            icon: const Icon(Icons.notifications_none),
          ),
          IconButton(
            tooltip: 'Profile',
            onPressed: () => context.go(appProfileRoute),
            icon: const Icon(Icons.settings_outlined),
          ),
        ],
      ),
      body: Column(
        children: [
          _ChatListHeader(searchController: _searchController),
          const _CreateGroupTile(),
          Expanded(child: _ChatListBody(state: chatListState)),
        ],
      ),
      bottomNavigationBar: NavigationBar(
        selectedIndex: 1,
        onDestinationSelected: (index) {
          if (index == 3) {
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
            label: 'CRM',
          ),
          NavigationDestination(
            icon: Icon(Icons.person_outline),
            label: 'Profile',
          ),
        ],
      ),
    );
  }
}

class _ChatListHeader extends ConsumerWidget {
  const _ChatListHeader({required this.searchController});

  final TextEditingController searchController;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    return Container(
      color: const Color(0xFFF7F8FA),
      padding: const EdgeInsets.fromLTRB(24, 24, 24, 18),
      child: Column(
        children: [
          TextField(
            controller: searchController,
            textInputAction: TextInputAction.search,
            onSubmitted: (value) {
              ref
                  .read(chatListControllerProvider.notifier)
                  .loadConversations(query: value.trim());
            },
            decoration: InputDecoration(
              hintText: 'Search conversations...',
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
          const SizedBox(height: 22),
          const SingleChildScrollView(
            scrollDirection: Axis.horizontal,
            child: Row(
              children: [
                _ChatFilterChip(label: 'Inbox', selected: true),
                SizedBox(width: 18),
                _ChatFilterChip(label: 'Unread'),
                SizedBox(width: 18),
                _ChatFilterChip(label: 'Follow-ups'),
                SizedBox(width: 18),
                _ChatFilterChip(label: 'Archived'),
              ],
            ),
          ),
        ],
      ),
    );
  }
}

class _ChatFilterChip extends StatelessWidget {
  const _ChatFilterChip({required this.label, this.selected = false});

  final String label;
  final bool selected;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 28, vertical: 12),
      decoration: BoxDecoration(
        color: selected ? const Color(0xFFE9EAEE) : Colors.transparent,
        borderRadius: BorderRadius.circular(8),
      ),
      child: Text(
        label,
        style: TextStyle(
          fontSize: 16,
          fontWeight: selected ? FontWeight.w600 : FontWeight.w500,
          color: selected ? Colors.black : const Color(0xFF7D7F87),
        ),
      ),
    );
  }
}

class _CreateGroupTile extends StatelessWidget {
  const _CreateGroupTile();

  @override
  Widget build(BuildContext context) {
    return Material(
      color: Colors.white,
      child: InkWell(
        onTap: () {},
        child: Container(
          padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 20),
          decoration: const BoxDecoration(
            border: Border(
              top: BorderSide(color: Color(0xFFE1E4EA)),
              bottom: BorderSide(color: Color(0xFFE1E4EA)),
            ),
          ),
          child: const Row(
            children: [
              CircleAvatar(
                radius: 32,
                backgroundColor: Color(0xFF747681),
                child: Icon(Icons.add, color: Colors.white, size: 30),
              ),
              SizedBox(width: 18),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      'Create New Group',
                      style: TextStyle(
                        fontSize: 18,
                        fontWeight: FontWeight.w800,
                      ),
                    ),
                    SizedBox(height: 4),
                    Text(
                      'Start a group conversation',
                      style: TextStyle(fontSize: 15, color: Color(0xFF7D7F87)),
                    ),
                  ],
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class _ChatListBody extends ConsumerWidget {
  const _ChatListBody({required this.state});

  final ChatListState state;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    switch (state.status) {
      case ChatListStatus.initial:
      case ChatListStatus.loading:
        return const Center(child: CircularProgressIndicator());
      case ChatListStatus.failure:
        return _ChatListError(
          message: state.errorMessage ?? 'Cannot load conversations.',
        );
      case ChatListStatus.success:
        if (state.conversations.isEmpty) {
          return const _ChatListEmpty();
        }

        return RefreshIndicator(
          onRefresh: () {
            return ref
                .read(chatListControllerProvider.notifier)
                .loadConversations();
          },
          child: ListView.separated(
            itemCount: state.conversations.length,
            separatorBuilder: (_, _) => const Divider(
              height: 1,
              thickness: 1,
              color: Color(0xFFE1E4EA),
            ),
            itemBuilder: (context, index) {
              return _ConversationTile(
                conversation: state.conversations[index],
              );
            },
          ),
        );
    }
  }
}

class _ConversationTile extends StatelessWidget {
  const _ConversationTile({required this.conversation});

  final Conversation conversation;

  @override
  Widget build(BuildContext context) {
    final lastMessage = conversation.lastMessage;
    final subtitle = _lastMessagePreview(conversation);
    final time = lastMessage == null ? '' : _formatTime(lastMessage.timestamp);

    return Material(
      color: Colors.white,
      child: InkWell(
        onTap: () {
          context.go(
            '/app/chats/${conversation.conversationId}',
            extra: conversation,
          );
        },
        child: Padding(
          padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 16),
          child: Row(
            children: [
              _ConversationAvatar(conversation: conversation),
              const SizedBox(width: 18),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Row(
                      children: [
                        Expanded(
                          child: Text(
                            conversation.conversationName,
                            maxLines: 1,
                            overflow: TextOverflow.ellipsis,
                            style: const TextStyle(
                              fontSize: 18,
                              fontWeight: FontWeight.w800,
                            ),
                          ),
                        ),
                        if (conversation.isGroup) ...[
                          const SizedBox(width: 6),
                          const Icon(
                            Icons.groups,
                            size: 16,
                            color: Color(0xFF747681),
                          ),
                        ],
                      ],
                    ),
                    const SizedBox(height: 6),
                    Text(
                      subtitle,
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                      style: TextStyle(
                        fontSize: 15,
                        color: lastMessage?.owner == true
                            ? const Color(0xFF5B5E68)
                            : const Color(0xFF7D7F87),
                      ),
                    ),
                  ],
                ),
              ),
              const SizedBox(width: 12),
              Column(
                crossAxisAlignment: CrossAxisAlignment.end,
                children: [
                  Text(
                    time,
                    style: const TextStyle(
                      color: Color(0xFF7D7F87),
                      fontSize: 14,
                    ),
                  ),
                  const SizedBox(height: 12),
                  if (conversation.unreadCount > 0)
                    _UnreadBadge(count: conversation.unreadCount)
                  else if (lastMessage?.owner == true)
                    const Icon(
                      Icons.done_all,
                      size: 18,
                      color: Color(0xFF7D7F87),
                    )
                  else
                    const SizedBox(height: 18),
                ],
              ),
            ],
          ),
        ),
      ),
    );
  }

  String _lastMessagePreview(Conversation conversation) {
    final lastMessage = conversation.lastMessage;

    if (lastMessage == null) {
      return 'No messages yet';
    }

    if (lastMessage.isDeleted) {
      return 'Message deleted';
    }

    final content = lastMessage.content;
    if (content != null && content.isNotEmpty) {
      return content;
    }

    final mediaName = lastMessage.mediaName;
    if (mediaName != null && mediaName.isNotEmpty) {
      return mediaName;
    }

    return lastMessage.type.value;
  }

  String _formatTime(DateTime value) {
    final hour = value.hour.toString().padLeft(2, '0');
    final minute = value.minute.toString().padLeft(2, '0');
    return '$hour:$minute';
  }
}

class _ConversationAvatar extends StatelessWidget {
  const _ConversationAvatar({required this.conversation});

  final Conversation conversation;

  @override
  Widget build(BuildContext context) {
    final avatarUrl = conversation.avatarUrl;
    final initial = conversation.conversationName.trim().isEmpty
        ? '?'
        : conversation.conversationName.trim()[0].toUpperCase();

    return Stack(
      clipBehavior: Clip.none,
      children: [
        CircleAvatar(
          radius: 28,
          backgroundColor: const Color(0xFFE9EAEE),
          foregroundImage: avatarUrl == null || avatarUrl.isEmpty
              ? null
              : NetworkImage(avatarUrl),
          child: Text(
            initial,
            style: const TextStyle(
              color: Color(0xFF5B5E68),
              fontSize: 20,
              fontWeight: FontWeight.w800,
            ),
          ),
        ),
        if (conversation.isOnline)
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

class _UnreadBadge extends StatelessWidget {
  const _UnreadBadge({required this.count});

  final int count;

  @override
  Widget build(BuildContext context) {
    return Container(
      constraints: const BoxConstraints(minWidth: 24, minHeight: 24),
      padding: const EdgeInsets.symmetric(horizontal: 7),
      alignment: Alignment.center,
      decoration: const BoxDecoration(
        color: Color(0xFF747681),
        shape: BoxShape.circle,
      ),
      child: Text(
        count > 99 ? '99+' : '$count',
        style: const TextStyle(
          color: Colors.white,
          fontSize: 12,
          fontWeight: FontWeight.w800,
        ),
      ),
    );
  }
}

class _ChatListError extends ConsumerWidget {
  const _ChatListError({required this.message});

  final String message;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
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
            FilledButton(
              onPressed: () {
                ref
                    .read(chatListControllerProvider.notifier)
                    .loadConversations();
              },
              child: const Text('Try again'),
            ),
          ],
        ),
      ),
    );
  }
}

class _ChatListEmpty extends StatelessWidget {
  const _ChatListEmpty();

  @override
  Widget build(BuildContext context) {
    return const Center(
      child: Text(
        'No conversations yet',
        style: TextStyle(color: Color(0xFF7D7F87), fontSize: 16),
      ),
    );
  }
}

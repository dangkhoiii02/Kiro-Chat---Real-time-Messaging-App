import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:kiromobile/core/route/route_name.dart';
import 'package:kiromobile/features/chat/data/models/chat_message.dart';
import 'package:kiromobile/features/chat/data/models/conversation.dart';
import 'package:kiromobile/features/chat/presentation/providers/chat_detail_provider.dart';

class ChatDetailPage extends ConsumerStatefulWidget {
  const ChatDetailPage({required this.conversation, super.key});

  final Conversation conversation;

  @override
  ConsumerState<ChatDetailPage> createState() => _ChatDetailPageState();
}

class _ChatDetailPageState extends ConsumerState<ChatDetailPage> {
  final _messageController = TextEditingController();

  @override
  void initState() {
    super.initState();

    Future.microtask(() {
      ref
          .read(chatDetailControllerProvider.notifier)
          .loadMessages(
            conversationId: widget.conversation.conversationId,
            markSeen: true,
          );
      ref
          .read(chatDetailControllerProvider.notifier)
          .startRealtime(widget.conversation);
    });
  }

  @override
  void dispose() {
    ref.read(chatDetailControllerProvider.notifier).stopRealtime();
    _messageController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final state = ref.watch(chatDetailControllerProvider);

    return Scaffold(
      backgroundColor: const Color(0xFFF7F8FA),
      appBar: AppBar(
        backgroundColor: Colors.white,
        foregroundColor: Colors.black,
        elevation: 0,
        titleSpacing: 0,
        leading: IconButton(
          tooltip: 'Back',
          onPressed: () => context.go(appChatsRoute),
          icon: const Icon(Icons.arrow_back),
        ),
        title: _ChatHeaderTitle(conversation: widget.conversation),
        actions: [
          IconButton(
            tooltip: 'Voice call',
            onPressed: () {},
            icon: const Icon(Icons.call_outlined),
          ),
          IconButton(
            tooltip: 'Video call',
            onPressed: () {},
            icon: const Icon(Icons.videocam_outlined),
          ),
        ],
      ),
      body: Column(
        children: [
          Expanded(
            child: _ChatDetailBody(
              conversation: widget.conversation,
              state: state,
            ),
          ),
          _MessageComposer(
            controller: _messageController,
            conversation: widget.conversation,
            state: state,
            onSend: _sendMessage,
          ),
        ],
      ),
    );
  }

  Future<void> _sendMessage() async {
    final content = _messageController.text;

    if (content.trim().isEmpty) {
      return;
    }

    await ref
        .read(chatDetailControllerProvider.notifier)
        .sendTextMessage(conversation: widget.conversation, content: content);

    if (!mounted) {
      return;
    }

    final sendErrorMessage = ref
        .read(chatDetailControllerProvider)
        .sendErrorMessage;
    if (sendErrorMessage == null) {
      _messageController.clear();
    }
  }
}

class _ChatHeaderTitle extends StatelessWidget {
  const _ChatHeaderTitle({required this.conversation});

  final Conversation conversation;

  @override
  Widget build(BuildContext context) {
    return Row(
      children: [
        _ConversationAvatar(conversation: conversation, radius: 20),
        const SizedBox(width: 12),
        Expanded(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            mainAxisSize: MainAxisSize.min,
            children: [
              Text(
                conversation.conversationName,
                maxLines: 1,
                overflow: TextOverflow.ellipsis,
                style: const TextStyle(
                  fontSize: 17,
                  fontWeight: FontWeight.w800,
                ),
              ),
              Text(
                conversation.isOnline ? 'Online' : 'Conversation',
                maxLines: 1,
                overflow: TextOverflow.ellipsis,
                style: const TextStyle(color: Color(0xFF7D7F87), fontSize: 12),
              ),
            ],
          ),
        ),
      ],
    );
  }
}

class _ChatDetailBody extends ConsumerWidget {
  const _ChatDetailBody({required this.conversation, required this.state});

  final Conversation conversation;
  final ChatDetailState state;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    switch (state.status) {
      case ChatDetailStatus.initial:
      case ChatDetailStatus.loading:
        return const Center(child: CircularProgressIndicator());
      case ChatDetailStatus.failure:
        return _ChatDetailError(
          message: state.errorMessage ?? 'Cannot load messages.',
          conversationId: conversation.conversationId,
        );
      case ChatDetailStatus.success:
        if (state.messages.isEmpty) {
          return const Center(
            child: Text(
              'No messages yet',
              style: TextStyle(color: Color(0xFF7D7F87), fontSize: 16),
            ),
          );
        }

        return RefreshIndicator(
          onRefresh: () {
            return ref
                .read(chatDetailControllerProvider.notifier)
                .loadMessages(conversationId: conversation.conversationId);
          },
          child: ListView.builder(
            reverse: true,
            padding: const EdgeInsets.fromLTRB(16, 16, 16, 24),
            itemCount: state.messages.length,
            itemBuilder: (context, index) {
              final reversedIndex = state.messages.length - 1 - index;
              return _MessageBubble(message: state.messages[reversedIndex]);
            },
          ),
        );
    }
  }
}

class _MessageBubble extends StatelessWidget {
  const _MessageBubble({required this.message});

  final ChatMessage message;

  @override
  Widget build(BuildContext context) {
    final isMine = message.owner == true;
    final content = _messageContent(message);

    return Align(
      alignment: isMine ? Alignment.centerRight : Alignment.centerLeft,
      child: Container(
        constraints: BoxConstraints(
          maxWidth: MediaQuery.sizeOf(context).width * 0.76,
        ),
        margin: const EdgeInsets.only(bottom: 10),
        padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 10),
        decoration: BoxDecoration(
          color: isMine ? const Color(0xFF111827) : Colors.white,
          borderRadius: BorderRadius.only(
            topLeft: const Radius.circular(16),
            topRight: const Radius.circular(16),
            bottomLeft: Radius.circular(isMine ? 16 : 4),
            bottomRight: Radius.circular(isMine ? 4 : 16),
          ),
          border: isMine ? null : Border.all(color: const Color(0xFFE1E4EA)),
        ),
        child: Column(
          crossAxisAlignment: isMine
              ? CrossAxisAlignment.end
              : CrossAxisAlignment.start,
          mainAxisSize: MainAxisSize.min,
          children: [
            Text(
              content,
              style: TextStyle(
                color: isMine ? Colors.white : Colors.black,
                fontSize: 15,
                height: 1.35,
              ),
            ),
            const SizedBox(height: 5),
            Text(
              _messageMeta(message),
              style: TextStyle(
                color: isMine ? Colors.white70 : const Color(0xFF7D7F87),
                fontSize: 11,
              ),
            ),
          ],
        ),
      ),
    );
  }

  String _messageContent(ChatMessage message) {
    if (message.isDeleted) {
      return 'Message deleted';
    }

    final content = message.content;
    if (content != null && content.isNotEmpty) {
      return content;
    }

    final mediaName = message.mediaName;
    if (mediaName != null && mediaName.isNotEmpty) {
      return mediaName;
    }

    return message.type.value;
  }

  String _formatTime(DateTime value) {
    final hour = value.hour.toString().padLeft(2, '0');
    final minute = value.minute.toString().padLeft(2, '0');
    return '$hour:$minute';
  }

  String _messageMeta(ChatMessage message) {
    final time = _formatTime(message.timestamp);
    if (message.owner != true) {
      return time;
    }

    return '$time · ${_stateLabel(message.messageState)}';
  }

  String _stateLabel(MessageState state) {
    return switch (state) {
      MessageState.prepare => 'sending',
      MessageState.sent => 'sent',
      MessageState.delivered => 'delivered',
      MessageState.seen => 'seen',
      MessageState.failed => 'failed',
      MessageState.unknown => 'sent',
    };
  }
}

class _ConversationAvatar extends StatelessWidget {
  const _ConversationAvatar({required this.conversation, required this.radius});

  final Conversation conversation;
  final double radius;

  @override
  Widget build(BuildContext context) {
    final avatarUrl = conversation.avatarUrl;
    final initial = conversation.conversationName.trim().isEmpty
        ? '?'
        : conversation.conversationName.trim()[0].toUpperCase();

    return CircleAvatar(
      radius: radius,
      backgroundColor: const Color(0xFFE9EAEE),
      foregroundImage: avatarUrl == null || avatarUrl.isEmpty
          ? null
          : NetworkImage(avatarUrl),
      child: Text(
        initial,
        style: const TextStyle(
          color: Color(0xFF5B5E68),
          fontWeight: FontWeight.w800,
        ),
      ),
    );
  }
}

class _ChatDetailError extends ConsumerWidget {
  const _ChatDetailError({required this.message, required this.conversationId});

  final String message;
  final String conversationId;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    return Center(
      child: Padding(
        padding: const EdgeInsets.all(24),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            const Icon(
              Icons.sms_failed_outlined,
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
                    .read(chatDetailControllerProvider.notifier)
                    .loadMessages(conversationId: conversationId);
              },
              child: const Text('Try again'),
            ),
          ],
        ),
      ),
    );
  }
}

class _MessageComposer extends StatelessWidget {
  const _MessageComposer({
    required this.controller,
    required this.conversation,
    required this.state,
    required this.onSend,
  });

  final TextEditingController controller;
  final Conversation conversation;
  final ChatDetailState state;
  final Future<void> Function() onSend;

  @override
  Widget build(BuildContext context) {
    return SafeArea(
      top: false,
      child: Container(
        padding: const EdgeInsets.fromLTRB(12, 10, 12, 10),
        decoration: const BoxDecoration(
          color: Colors.white,
          border: Border(top: BorderSide(color: Color(0xFFE1E4EA))),
        ),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            if (state.sendErrorMessage != null) ...[
              Align(
                alignment: Alignment.centerLeft,
                child: Text(
                  state.sendErrorMessage!,
                  maxLines: 2,
                  overflow: TextOverflow.ellipsis,
                  style: const TextStyle(
                    color: Color(0xFFB91C1C),
                    fontSize: 12,
                  ),
                ),
              ),
              const SizedBox(height: 8),
            ],
            Row(
              children: [
                IconButton(
                  tooltip: 'Attach file',
                  onPressed: () {},
                  icon: const Icon(Icons.attach_file),
                ),
                Expanded(
                  child: TextField(
                    controller: controller,
                    minLines: 1,
                    maxLines: 4,
                    textInputAction: TextInputAction.send,
                    onSubmitted: (_) => onSend(),
                    decoration: InputDecoration(
                      hintText: 'Message ${conversation.conversationName}',
                      filled: true,
                      fillColor: const Color(0xFFF7F8FA),
                      contentPadding: const EdgeInsets.symmetric(
                        horizontal: 14,
                        vertical: 12,
                      ),
                      border: OutlineInputBorder(
                        borderRadius: BorderRadius.circular(22),
                        borderSide: BorderSide.none,
                      ),
                    ),
                  ),
                ),
                const SizedBox(width: 8),
                IconButton.filled(
                  tooltip: 'Send message',
                  onPressed: state.isSending ? null : onSend,
                  icon: state.isSending
                      ? const SizedBox(
                          width: 18,
                          height: 18,
                          child: CircularProgressIndicator(strokeWidth: 2),
                        )
                      : const Icon(Icons.send),
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }
}

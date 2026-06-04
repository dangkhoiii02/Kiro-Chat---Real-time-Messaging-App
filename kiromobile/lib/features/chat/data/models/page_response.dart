class PageResponse<T> {
  const PageResponse({
    required this.content,
    required this.totalElements,
    required this.totalPages,
    required this.pageNumber,
    required this.pageSize,
    required this.last,
  });

  final List<T> content;
  final int totalElements;
  final int totalPages;
  final int pageNumber;
  final int pageSize;
  final bool last;

  factory PageResponse.fromJson(
    Map<String, dynamic> json,
    T Function(Map<String, dynamic>) itemFromJson,
  ) {
    final contentJson = (json['content'] as List<dynamic>? ?? [])
        .whereType<Map<String, dynamic>>();
    final pageable = json['pageable'] as Map<String, dynamic>?;

    return PageResponse(
      content: contentJson.map(itemFromJson).toList(),
      totalElements: (json['totalElements'] as num?)?.toInt() ?? 0,
      totalPages: (json['totalPages'] as num?)?.toInt() ?? 0,
      pageNumber:
          (json['number'] as num?)?.toInt() ??
          (pageable?['pageNumber'] as num?)?.toInt() ??
          0,
      pageSize:
          (json['size'] as num?)?.toInt() ??
          (pageable?['pageSize'] as num?)?.toInt() ??
          0,
      last: (json['last'] as bool?) ?? false,
    );
  }
}

package dev.yanianz.essentials.friends;

import java.util.UUID;

public record FriendRequest(UUID from, UUID to, long sentAt) {}
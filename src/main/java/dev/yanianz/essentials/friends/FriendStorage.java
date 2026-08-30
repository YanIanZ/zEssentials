package dev.yanianz.essentials.friends;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class FriendStorage {

    private final Map<UUID, Set<UUID>> friends = new ConcurrentHashMap<>();
    private final List<FriendRequest> pendingRequests = Collections.synchronizedList(new ArrayList<>());
    private final long expiryMillis;

    public FriendStorage() {
        this(7);
    }

    public FriendStorage(int expiryDays) {
        this.expiryDays = Math.max(0, expiryDays);
        this.expiryMillis = this.expiryDays * 86_400_000L;
    }

    private int expiryDays;

    public boolean sendRequest(UUID from, UUID to) {
        if (from.equals(to)) return false;
        if (isFriend(from, to)) return false;
        if (hasPendingRequest(from, to)) return false;
        pendingRequests.add(new FriendRequest(from, to, System.currentTimeMillis()));
        return true;
    }

    public boolean acceptRequest(UUID from, UUID to) {
        synchronized (pendingRequests) {
            boolean removed = pendingRequests.removeIf(r ->
                    r.from().equals(from) && r.to().equals(to));
            if (!removed) return false;
        }
        addFriend(from, to);
        addFriend(to, from);
        return true;
    }

    public boolean declineRequest(UUID from, UUID to) {
        synchronized (pendingRequests) {
            return pendingRequests.removeIf(r ->
                    r.from().equals(from) && r.to().equals(to));
        }
    }

    public boolean removeFriend(UUID player, UUID friend) {
        Set<UUID> playerFriends = friends.get(player);
        if (playerFriends == null) return false;
        boolean removed = playerFriends.remove(friend);
        if (removed) {
            friends.getOrDefault(friend, Set.of()).remove(player);
        }
        return removed;
    }

    public boolean isFriend(UUID player, UUID other) {
        return friends.getOrDefault(player, Set.of()).contains(other);
    }

    public boolean hasPendingRequest(UUID from, UUID to) {
        pruneExpired();
        synchronized (pendingRequests) {
            return pendingRequests.stream()
                    .anyMatch(r -> r.from().equals(from) && r.to().equals(to));
        }
    }

    public List<UUID> getPendingRequests(UUID to) {
        pruneExpired();
        synchronized (pendingRequests) {
            return pendingRequests.stream()
                    .filter(r -> r.to().equals(to))
                    .map(FriendRequest::from)
                    .collect(Collectors.toList());
        }
    }

    public List<UUID> getFriends(UUID player) {
        return new ArrayList<>(friends.getOrDefault(player, Set.of()));
    }

    public int getFriendCount(UUID player) {
        return friends.getOrDefault(player, Set.of()).size();
    }

    private void pruneExpired() {
        if (expiryMillis <= 0) {
            synchronized (pendingRequests) {
                pendingRequests.clear();
            }
            return;
        }
        long cutoff = System.currentTimeMillis() - expiryMillis;
        synchronized (pendingRequests) {
            pendingRequests.removeIf(r -> r.sentAt() < cutoff);
        }
    }

    private void addFriend(UUID a, UUID b) {
        friends.computeIfAbsent(a, k -> ConcurrentHashMap.newKeySet()).add(b);
    }
}
package dev.yanianz.essentials.friends;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class FriendStorageTest {

    private FriendStorage storage;
    private final UUID alice = UUID.randomUUID();
    private final UUID bob = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        storage = new FriendStorage(7);
    }

    @Test
    @DisplayName("sendRequest succeeds for two new players")
    void testSendRequest() {
        assertTrue(storage.sendRequest(alice, bob));
        assertTrue(storage.hasPendingRequest(alice, bob));
    }

    @Test
    @DisplayName("sendRequest rejects self-friend")
    void testSelfFriend() {
        assertFalse(storage.sendRequest(alice, alice));
    }

    @Test
    @DisplayName("sendRequest rejects duplicate pending request")
    void testDuplicatePending() {
        assertTrue(storage.sendRequest(alice, bob));
        assertFalse(storage.sendRequest(alice, bob));
    }

    @Test
    @DisplayName("acceptRequest creates mutual friendship")
    void testAccept() {
        storage.sendRequest(alice, bob);
        assertTrue(storage.acceptRequest(alice, bob));
        assertTrue(storage.isFriend(alice, bob));
        assertTrue(storage.isFriend(bob, alice));
        assertFalse(storage.hasPendingRequest(alice, bob));
    }

    @Test
    @DisplayName("acceptRequest fails when no pending request")
    void testAcceptNone() {
        assertFalse(storage.acceptRequest(alice, bob));
    }

    @Test
    @DisplayName("declineRequest removes the pending request")
    void testDecline() {
        storage.sendRequest(alice, bob);
        assertTrue(storage.declineRequest(alice, bob));
        assertFalse(storage.hasPendingRequest(alice, bob));
        assertFalse(storage.isFriend(alice, bob));
    }

    @Test
    @DisplayName("removeFriend drops the friendship both ways")
    void testRemove() {
        storage.sendRequest(alice, bob);
        storage.acceptRequest(alice, bob);
        assertTrue(storage.removeFriend(alice, bob));
        assertFalse(storage.isFriend(alice, bob));
        assertFalse(storage.isFriend(bob, alice));
    }

    @Test
    @DisplayName("removeFriend returns false when not friends")
    void testRemoveNone() {
        assertFalse(storage.removeFriend(alice, bob));
    }

    @Test
    @DisplayName("expired pending requests are pruned on check")
    void testExpiry() {
        FriendStorage expired = new FriendStorage(0);
        UUID carol = UUID.randomUUID();
        expired.sendRequest(alice, carol);
        assertFalse(expired.hasPendingRequest(alice, carol));
    }

    @Test
    @DisplayName("getPendingRequests lists incoming requests")
    void testListIncoming() {
        UUID carol = UUID.randomUUID();
        storage.sendRequest(carol, bob);
        storage.sendRequest(alice, bob);
        var incoming = storage.getPendingRequests(bob);
        assertEquals(2, incoming.size());
        assertTrue(incoming.contains(alice));
        assertTrue(incoming.contains(carol));
    }

    @Test
    @DisplayName("getFriends returns current friend UUIDs")
    void testGetFriends() {
        storage.sendRequest(alice, bob);
        storage.acceptRequest(alice, bob);
        var friends = storage.getFriends(alice);
        assertEquals(1, friends.size());
        assertTrue(friends.contains(bob));
    }
}
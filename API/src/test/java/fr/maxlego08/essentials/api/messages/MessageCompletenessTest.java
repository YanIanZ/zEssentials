package fr.maxlego08.essentials.api.messages;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class MessageCompletenessTest {

    @Test
    @DisplayName("All new Message enum entries exist and have non-null descriptions")
    void testNewMessageEntriesExist() throws Exception {
        Set<String> required = Set.of(
                "DESCRIPTION_ITEM_GLOW",
                "DESCRIPTION_ITEM_UNBREAKABLE",
                "DESCRIPTION_ITEM_MODEL_DATA",
                "DESCRIPTION_CONDENSE",
                "DESCRIPTION_HOMES_GUI",
                "DESCRIPTION_KITS_GUI",
                "DESCRIPTION_WARPS_GUI",
                "DESCRIPTION_BALTOP_GUI",
                "COMMAND_CONDENSE_SUCCESS",
                "COMMAND_CONDENSE_EMPTY",
                "COMMAND_TRASH_OPENED",
                "COMMAND_NEAR_INVALID_RADIUS"
        );

        Set<String> actual = new HashSet<>();
        for (Message msg : Message.values()) {
            actual.add(msg.name());
        }

        for (String name : required) {
            assertTrue(actual.contains(name), "Message enum must contain: " + name);
        }
    }

    @Test
    @DisplayName("No two Message enum entries share the same name")
    void testNoDuplicateNames() {
        Set<String> names = new HashSet<>();
        for (Message msg : Message.values()) {
            assertTrue(names.add(msg.name()), "Duplicate Message name: " + msg.name());
        }
    }

    @Test
    @DisplayName("Screen-related descriptions are distinct from item-name description")
    void testDescriptionsAreDistinct() {
        assertNotEquals(Message.DESCRIPTION_ITEM_NAME.getMessageAsString(),
                Message.DESCRIPTION_ITEM_GLOW.getMessageAsString(),
                "Item glow description must differ from item name description");
        assertNotEquals(Message.DESCRIPTION_ITEM_NAME.getMessageAsString(),
                Message.DESCRIPTION_ITEM_UNBREAKABLE.getMessageAsString(),
                "Item unbreakable description must differ from item name description");
        assertNotEquals(Message.DESCRIPTION_ITEM_NAME.getMessageAsString(),
                Message.DESCRIPTION_ITEM_MODEL_DATA.getMessageAsString(),
                "Item model data description must differ from item name description");
    }
}

package fr.maxlego08.essentials.api.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Date;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("DTO Records Tests")
class DTOTest {

    @Nested
    @DisplayName("HomeDTO Tests")
    class HomeDTOTests {

        @Test
        @DisplayName("Should construct HomeDTO and return correct values via accessors")
        void shouldConstructAndAccessFields() {
            String location = "world,100,64,-200,0,0";
            String name = "base";
            String material = "OAK_DOOR";
            Boolean isPublic = true;
            String category = "default";
            Boolean isFavorite = false;

            HomeDTO home = new HomeDTO(location, name, material, isPublic, category, isFavorite);

            assertEquals(location, home.location());
            assertEquals(name, home.name());
            assertEquals(material, home.material());
            assertTrue(home.is_public());
            assertEquals(category, home.category());
            assertFalse(home.is_favorite());
        }

        @Test
        @DisplayName("Should verify equals and hashCode consistency for HomeDTO")
        void shouldVerifyEqualsAndHashCode() {
            HomeDTO home1 = new HomeDTO("world,0,0,0,0,0", "mine", "STONE", false, "ores", true);
            HomeDTO home2 = new HomeDTO("world,0,0,0,0,0", "mine", "STONE", false, "ores", true);
            HomeDTO diffLocation = new HomeDTO("world,1,0,0,0,0", "mine", "STONE", false, "ores", true);
            HomeDTO diffName = new HomeDTO("world,0,0,0,0,0", "farm", "STONE", false, "ores", true);
            HomeDTO diffMaterial = new HomeDTO("world,0,0,0,0,0", "mine", "DIRT", false, "ores", true);
            HomeDTO diffPublic = new HomeDTO("world,0,0,0,0,0", "mine", "STONE", true, "ores", true);
            HomeDTO diffCategory = new HomeDTO("world,0,0,0,0,0", "mine", "STONE", false, "farming", true);
            HomeDTO diffFavorite = new HomeDTO("world,0,0,0,0,0", "mine", "STONE", false, "ores", false);

            assertEquals(home1, home2);
            assertEquals(home1.hashCode(), home2.hashCode());

            assertNotEquals(home1, diffLocation);
            assertNotEquals(home1, diffName);
            assertNotEquals(home1, diffMaterial);
            assertNotEquals(home1, diffPublic);
            assertNotEquals(home1, diffCategory);
            assertNotEquals(home1, diffFavorite);
            assertNotEquals(home1, null);
            assertNotEquals(home1, "other-type");
        }

        @Test
        @DisplayName("Should verify toString contains all field values for HomeDTO")
        void shouldVerifyToString() {
            HomeDTO home = new HomeDTO("loc_str", "spawn_home", "BED", true, "main", false);
            String str = home.toString();

            assertNotNull(str);
            assertTrue(str.contains("loc_str"));
            assertTrue(str.contains("spawn_home"));
            assertTrue(str.contains("BED"));
            assertTrue(str.contains("true"));
            assertTrue(str.contains("main"));
            assertTrue(str.contains("false"));
        }

        @Test
        @DisplayName("Should support null fields in HomeDTO")
        void shouldSupportNullFields() {
            HomeDTO home = new HomeDTO(null, null, null, null, null, null);
            assertNull(home.location());
            assertNull(home.name());
            assertNull(home.material());
            assertNull(home.is_public());
            assertNull(home.category());
            assertNull(home.is_favorite());
        }
    }

    @Nested
    @DisplayName("EconomyDTO Tests")
    class EconomyDTOTests {

        @Test
        @DisplayName("Should construct EconomyDTO and return correct values via accessors")
        void shouldConstructAndAccessFields() {
            String ecoName = "vault";
            BigDecimal amount = new BigDecimal("12345.67");

            EconomyDTO eco = new EconomyDTO(ecoName, amount);

            assertEquals(ecoName, eco.economy_name());
            assertEquals(amount, eco.amount());
        }

        @Test
        @DisplayName("Should verify equals and hashCode consistency for EconomyDTO")
        void shouldVerifyEqualsAndHashCode() {
            EconomyDTO eco1 = new EconomyDTO("coins", new BigDecimal("500"));
            EconomyDTO eco2 = new EconomyDTO("coins", new BigDecimal("500"));
            EconomyDTO diffName = new EconomyDTO("tokens", new BigDecimal("500"));
            EconomyDTO diffAmount = new EconomyDTO("coins", new BigDecimal("600"));

            assertEquals(eco1, eco2);
            assertEquals(eco1.hashCode(), eco2.hashCode());

            assertNotEquals(eco1, diffName);
            assertNotEquals(eco1, diffAmount);
            assertNotEquals(eco1, null);
            assertNotEquals(eco1, new Object());
        }

        @Test
        @DisplayName("Should verify toString contains all field values for EconomyDTO")
        void shouldVerifyToString() {
            EconomyDTO eco = new EconomyDTO("crystals", new BigDecimal("99.99"));
            String str = eco.toString();

            assertNotNull(str);
            assertTrue(str.contains("crystals"));
            assertTrue(str.contains("99.99"));
        }

        @Test
        @DisplayName("Should support null fields in EconomyDTO")
        void shouldSupportNullFields() {
            EconomyDTO eco = new EconomyDTO(null, null);
            assertNull(eco.economy_name());
            assertNull(eco.amount());
        }
    }

    @Nested
    @DisplayName("CooldownDTO Tests")
    class CooldownDTOTests {

        @Test
        @DisplayName("Should construct CooldownDTO and return correct values via accessors")
        void shouldConstructAndAccessFields() {
            String name = "teleport";
            long value = 30000L;
            Date createdAt = new Date(1700000000000L);

            CooldownDTO cooldown = new CooldownDTO(name, value, createdAt);

            assertEquals(name, cooldown.cooldown_name());
            assertEquals(value, cooldown.cooldown_value());
            assertEquals(createdAt, cooldown.created_at());
        }

        @Test
        @DisplayName("Should verify equals and hashCode consistency for CooldownDTO")
        void shouldVerifyEqualsAndHashCode() {
            Date date = new Date(1700000000000L);
            CooldownDTO c1 = new CooldownDTO("fly", 10000L, date);
            CooldownDTO c2 = new CooldownDTO("fly", 10000L, date);
            CooldownDTO diffName = new CooldownDTO("heal", 10000L, date);
            CooldownDTO diffVal = new CooldownDTO("fly", 20000L, date);
            CooldownDTO diffDate = new CooldownDTO("fly", 10000L, new Date(1800000000000L));

            assertEquals(c1, c2);
            assertEquals(c1.hashCode(), c2.hashCode());

            assertNotEquals(c1, diffName);
            assertNotEquals(c1, diffVal);
            assertNotEquals(c1, diffDate);
            assertNotEquals(c1, null);
            assertNotEquals(c1, "not-a-dto");
        }

        @Test
        @DisplayName("Should verify toString contains all field values for CooldownDTO")
        void shouldVerifyToString() {
            Date date = new Date(1700000000000L);
            CooldownDTO cooldown = new CooldownDTO("kit_daily", 86400000L, date);
            String str = cooldown.toString();

            assertNotNull(str);
            assertTrue(str.contains("kit_daily"));
            assertTrue(str.contains("86400000"));
            assertTrue(str.contains(date.toString()));
        }

        @Test
        @DisplayName("Should support null fields in CooldownDTO")
        void shouldSupportNullFields() {
            CooldownDTO cooldown = new CooldownDTO(null, 0L, null);
            assertNull(cooldown.cooldown_name());
            assertEquals(0L, cooldown.cooldown_value());
            assertNull(cooldown.created_at());
        }
    }

    @Nested
    @DisplayName("ChatMessageDTO Tests")
    class ChatMessageDTOTests {

        @Test
        @DisplayName("Should construct ChatMessageDTO and return correct values via accessors")
        void shouldConstructAndAccessFields() {
            UUID uuid = UUID.randomUUID();
            String content = "Hello world!";
            Date createdAt = new Date();

            ChatMessageDTO message = new ChatMessageDTO(uuid, content, createdAt);

            assertEquals(uuid, message.unique_id());
            assertEquals(content, message.content());
            assertEquals(createdAt, message.created_at());
        }

        @Test
        @DisplayName("Should verify equals and hashCode consistency for ChatMessageDTO")
        void shouldVerifyEqualsAndHashCode() {
            UUID uuid1 = UUID.randomUUID();
            UUID uuid2 = UUID.randomUUID();
            Date date = new Date(1700000000000L);

            ChatMessageDTO m1 = new ChatMessageDTO(uuid1, "msg", date);
            ChatMessageDTO m2 = new ChatMessageDTO(uuid1, "msg", date);
            ChatMessageDTO diffUuid = new ChatMessageDTO(uuid2, "msg", date);
            ChatMessageDTO diffContent = new ChatMessageDTO(uuid1, "diff", date);
            ChatMessageDTO diffDate = new ChatMessageDTO(uuid1, "msg", new Date(1800000000000L));

            assertEquals(m1, m2);
            assertEquals(m1.hashCode(), m2.hashCode());

            assertNotEquals(m1, diffUuid);
            assertNotEquals(m1, diffContent);
            assertNotEquals(m1, diffDate);
            assertNotEquals(m1, null);
            assertNotEquals(m1, "test");
        }

        @Test
        @DisplayName("Should verify toString contains all field values for ChatMessageDTO")
        void shouldVerifyToString() {
            UUID uuid = UUID.randomUUID();
            Date date = new Date(1700000000000L);
            ChatMessageDTO message = new ChatMessageDTO(uuid, "Test Chat Message", date);
            String str = message.toString();

            assertNotNull(str);
            assertTrue(str.contains(uuid.toString()));
            assertTrue(str.contains("Test Chat Message"));
            assertTrue(str.contains(date.toString()));
        }

        @Test
        @DisplayName("Should support null fields in ChatMessageDTO")
        void shouldSupportNullFields() {
            ChatMessageDTO message = new ChatMessageDTO(null, null, null);
            assertNull(message.unique_id());
            assertNull(message.content());
            assertNull(message.created_at());
        }
    }
}

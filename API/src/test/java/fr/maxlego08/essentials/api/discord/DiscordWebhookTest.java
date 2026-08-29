package fr.maxlego08.essentials.api.discord;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.awt.Color;

import static org.junit.jupiter.api.Assertions.*;

class DiscordWebhookTest {

    @Test
    @DisplayName("EmbedObject builder methods setTitle, setDescription, setUrl, setColor return this and set fields")
    void testEmbedObjectBasicSetters() {
        DiscordWebhook.EmbedObject embed = new DiscordWebhook.EmbedObject();

        DiscordWebhook.EmbedObject sameTitle = embed.setTitle("Test Title");
        assertSame(embed, sameTitle);
        assertEquals("Test Title", embed.getTitle());

        DiscordWebhook.EmbedObject sameDesc = embed.setDescription("Test Description");
        assertSame(embed, sameDesc);
        assertEquals("Test Description", embed.getDescription());

        DiscordWebhook.EmbedObject sameUrl = embed.setUrl("https://example.com");
        assertSame(embed, sameUrl);
        assertEquals("https://example.com", embed.getUrl());

        Color red = Color.RED;
        DiscordWebhook.EmbedObject sameColor = embed.setColor(red);
        assertSame(embed, sameColor);
        assertEquals(red, embed.getColor());
    }

    @Test
    @DisplayName("EmbedObject addField adds fields to the internal field list and returns this")
    void testEmbedObjectAddField() {
        DiscordWebhook.EmbedObject embed = new DiscordWebhook.EmbedObject();

        assertNotNull(embed.getFields());
        assertTrue(embed.getFields().isEmpty());

        DiscordWebhook.EmbedObject result = embed.addField("Field Name 1", "Field Value 1", true);
        assertSame(embed, result);
        assertEquals(1, embed.getFields().size());

        embed.addField("Field Name 2", "Field Value 2", false);
        assertEquals(2, embed.getFields().size());
    }

    @Test
    @DisplayName("EmbedObject setFooter creates footer and returns this")
    void testEmbedObjectSetFooter() {
        DiscordWebhook.EmbedObject embed = new DiscordWebhook.EmbedObject();
        assertNull(embed.getFooter());

        DiscordWebhook.EmbedObject result = embed.setFooter("Footer Text", "https://example.com/footer.png");
        assertSame(embed, result);
        assertNotNull(embed.getFooter());
    }

    @Test
    @DisplayName("EmbedObject setAuthor creates author and returns this")
    void testEmbedObjectSetAuthor() {
        DiscordWebhook.EmbedObject embed = new DiscordWebhook.EmbedObject();
        assertNull(embed.getAuthor());

        DiscordWebhook.EmbedObject result = embed.setAuthor("Author Name", "https://example.com/author", "https://example.com/author.png");
        assertSame(embed, result);
        assertNotNull(embed.getAuthor());
    }

    @Test
    @DisplayName("EmbedObject setImage creates image and returns this")
    void testEmbedObjectSetImage() {
        DiscordWebhook.EmbedObject embed = new DiscordWebhook.EmbedObject();
        assertNull(embed.getImage());

        DiscordWebhook.EmbedObject result = embed.setImage("https://example.com/image.png");
        assertSame(embed, result);
        assertNotNull(embed.getImage());
    }

    @Test
    @DisplayName("EmbedObject setThumbnail creates thumbnail and returns this")
    void testEmbedObjectSetThumbnail() {
        DiscordWebhook.EmbedObject embed = new DiscordWebhook.EmbedObject();
        assertNull(embed.getThumbnail());

        DiscordWebhook.EmbedObject result = embed.setThumbnail("https://example.com/thumbnail.png");
        assertSame(embed, result);
        assertNotNull(embed.getThumbnail());
    }

    @Test
    @DisplayName("DiscordWebhook configuration methods execute without exceptions")
    void testDiscordWebhookSetters() {
        DiscordWebhook webhook = new DiscordWebhook("https://discord.com/api/webhooks/dummy");

        assertDoesNotThrow(() -> {
            webhook.setContent("Message Content");
            webhook.setUsername("CustomBot");
            webhook.setAvatarUrl("https://example.com/bot_avatar.png");
            webhook.setTts(true);
            webhook.addEmbed(new DiscordWebhook.EmbedObject().setTitle("Embed Title"));
        });
    }

    @Test
    @DisplayName("execute() throws IllegalArgumentException when neither content nor embeds are provided")
    void testExecuteThrowsWhenEmpty() {
        DiscordWebhook webhook = new DiscordWebhook("https://discord.com/test");

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                webhook::execute,
                "Expected execute() to throw IllegalArgumentException when content is null and embeds are empty"
        );

        assertEquals("Set content or add at least one EmbedObject", exception.getMessage());
    }
}

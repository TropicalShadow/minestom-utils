package club.tesseract.minestom.utils.misc;

import club.tesseract.minestom.utils.block.BlockFinder;
import club.tesseract.minestom.utils.misc.lang.MiniMessageHelper;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.TextColor;
import net.minestom.server.instance.block.Block;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** Pins the hexes mirrored from ReleaseSplash's :theme Role, and checks the roles drive rarity and the legacy tags. */
class PaletteTest {

    @Test
    void hexesMatchTheThemeSourceOfTruth() {
        Map<String, String> expected = new LinkedHashMap<>();
        expected.put("TEXT", "#EEEEEE"); expected.put("MUTED", "#ABABAB"); expected.put("SUBTLE", "#808080");
        expected.put("BRAND", "#55DDFF"); expected.put("SUCCESS", "#6AE295"); expected.put("WARNING", "#FFBA43");
        expected.put("ERROR", "#F87164"); expected.put("INFO", "#78B9F3"); expected.put("REWARD", "#DECF5F");
        expected.put("PARTY", "#7BB0FF"); expected.put("FLEET", "#79D7CF"); expected.put("STAFF", "#E77DB7");
        expected.put("DM", "#CBA3EF");
        expected.put("TEAM_1", "#F84C4B"); expected.put("TEAM_2", "#1E84E7"); expected.put("TEAM_3", "#F6DD00");
        expected.put("TEAM_4", "#00D0DD");
        expected.put("ACCENT", "#FFB366"); expected.put("GEM", "#F097D8"); expected.put("SILVER", "#CACACA");
        expected.put("BRONZE", "#DD844A");

        assertEquals(expected.size(), Palette.values().length, "role set differs from the pinned list");
        expected.forEach((name, hex) -> {
            Palette role = Palette.valueOf(name);
            assertEquals(hex, role.hex(), name);
            assertEquals(TextColor.fromHexString(hex), role.color(), name);
            assertEquals(name.toLowerCase(), role.tag(), name);
        });
    }

    @Test
    void everyRoleIsAMiniMessageTag() {
        for (Palette role : Palette.values()) {
            Component c = MiniMessageHelper.MINI_MESSAGE.deserialize("<" + role.tag() + ">x");
            assertEquals(role.color(), c.color(), role.tag());
        }
    }

    @Test
    void legacyTagsAreRoleAliasesKeepingTheirGlyph() {
        assertLegacy("danger", Palette.ERROR, "×");
        assertLegacy("info", Palette.INFO, "•");
        assertLegacy("success", Palette.SUCCESS, "✓");
        assertLegacy("warning", Palette.WARNING, "!");
    }

    private static void assertLegacy(String tag, Palette role, String glyph) {
        Component c = MiniMessageHelper.MINI_MESSAGE.deserialize("<" + tag + ">hi");
        String plain = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(c);
        assertEquals(glyph + " hi", plain, tag);
        assertEquals(role.color(), firstColour(c), tag);
    }

    @Test
    void extraResolversAreAccepted() {
        var mm = MiniMessageHelper.withResolvers(net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.unparsed("who", "Bob"));
        assertEquals("Bob", net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText()
                .serialize(mm.deserialize("<who>")));
        Component c = MiniMessageHelper.toComponentWith("<brand><who>",
                net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.unparsed("who", "Bob"));
        assertEquals(Palette.BRAND.color(), firstColour(c));
    }

    private static TextColor firstColour(Component c) {
        if (c.color() != null) return c.color();
        for (Component child : c.children()) {
            TextColor found = firstColour(child);
            if (found != null) return found;
        }
        return null;
    }

    @Test
    void gaugeRunsFromSuccessThroughWarningToError() {
        assertEquals(Palette.SUCCESS.color(), Palette.gauge(0));
        assertEquals(Palette.WARNING.color(), Palette.gauge(0.5));
        assertEquals(Palette.ERROR.color(), Palette.gauge(1));
        assertEquals(Palette.ERROR.color(), Palette.gauge(7));
        assertEquals(Palette.SUCCESS.color(), Palette.gauge(-1));
        assertEquals(Palette.ERROR.color(), Palette.gauge(Double.NaN));
    }

    @Test
    void rarityUsesRoles() {
        assertEquals(Palette.TEXT.color(), Rarity.COMMON.getColor());
        assertEquals(Palette.SUCCESS.color(), Rarity.UNCOMMON.getColor());
        assertEquals(Palette.INFO.color(), Rarity.RARE.getColor());
        assertEquals(Palette.DM.color(), Rarity.EPIC.getColor());
        assertEquals(Palette.ACCENT.color(), Rarity.LEGENDARY.getColor());
        assertEquals(Palette.GEM.color(), Rarity.MYTHIC.getColor());
        assertEquals(Palette.ERROR.color(), Rarity.SPECIAL.getColor());
        assertEquals(Palette.ERROR.color(), Rarity.VERY_SPECIAL.getColor());
        assertEquals(Palette.STAFF.color(), Rarity.ADMIN.getColor());
        assertEquals(Palette.ACCENT.color(), ((TextComponent) Rarity.LEGENDARY.getDisplay()).color());
    }

    @Test
    void blockFinderRoleOverloadMatchesColourOverload() {
        for (Palette role : new Palette[]{Palette.ERROR, Palette.SUCCESS, Palette.TEAM_2}) {
            assertEquals(BlockFinder.fromColour(role.color(), Block.WHITE_WOOL),
                    BlockFinder.fromColour(role, Block.WHITE_WOOL), role.tag());
        }
    }
}

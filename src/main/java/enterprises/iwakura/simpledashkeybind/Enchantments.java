package enterprises.iwakura.simpledashkeybind;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;

import static enterprises.iwakura.simpledashkeybind.SimpleDashKeybindMod.MOD_ID;

public class Enchantments {

    public static final RegistryKey<Enchantment> DASH_ENCHANTMENT = RegistryKey.of(RegistryKeys.ENCHANTMENT, Identifier.of(MOD_ID, "dash"));

    public static void initialize() {
    }
}

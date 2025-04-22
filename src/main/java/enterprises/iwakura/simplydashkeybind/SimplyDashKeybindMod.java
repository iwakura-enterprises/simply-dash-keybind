package enterprises.iwakura.simplydashkeybind;

import net.fabricmc.api.ModInitializer;

public class SimplyDashKeybindMod implements ModInitializer {

    public static final String MOD_ID = "simplydashkeybind";

    @Override
    public void onInitialize() {
        Enchantments.initialize();
    }
}

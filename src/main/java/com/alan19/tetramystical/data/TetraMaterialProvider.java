package com.alan19.tetramystical.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.mojang.datafixers.util.Pair;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.DirectoryCache;
import net.minecraft.data.IDataProvider;
import net.minecraft.resources.IResource;
import net.minecraft.resources.ResourcePackType;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.generators.ExistingFileHelper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import se.mickelus.tetra.capabilities.Capability;
import se.mickelus.tetra.data.provider.ModuleBuilder;
import se.mickelus.tetra.module.ItemEffect;

import javax.annotation.Nonnull;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class TetraMaterialProvider implements IDataProvider {
    private static final Logger logger = LogManager.getLogger();

    public static final Gson gson = new GsonBuilder()
            .setPrettyPrinting()
            .create();

    private final DataGenerator generator;

    private final List<ModuleBuilder> builders;

    private final ExistingFileHelper existingFileHelper;

    private final String lang = "en_us";

    public TetraMaterialProvider(DataGenerator generator, ExistingFileHelper exFileHelper) {
        this.generator = generator;
        this.existingFileHelper = exFileHelper;

        builders = new LinkedList<>();

    }

    /**
     * This provider can be used to generate additional variants for existing modules, useful for mod devs or pack makers that want to make
     * tetra modules compatible with materials from other mods
     */
    @SuppressWarnings("unchecked")
    private void setup() {
        // the generator uses an existing module variant as a template when generating data, these are matched against the variant key to find a fitting template
        // it matches against them in decending order
        String[] metalReferences = new String[]{"iron"};
        String[] woodReferences = new String[]{"oak", "wooden"};
        String[] gemReferences = new String[]{"diamond"};

        // data shared between all module variants of the same materials are defined here, e.g. the localized
        ModuleBuilder.Material amethyst = new ModuleBuilder.Material("amethyst", "amethyst", 0xd9f3ccf, 0x9f3ccf,
                1, 70, "tag", "forge:gems/amethyst", 1, Capability.hammer, 2, gemReferences, Pair.of("enchantment/looting", 1), Pair.of("enchantment/fortune", 1));
        ModuleBuilder.Material tin = new ModuleBuilder.Material("tin", "tin", 0xd9f3ccf, 0xd9f3ccf, 0, 72, "tag", "forge:ingots/tin", 1, Capability.hammer, 1, metalReferences).addVariantData("reach", 0.25);
        ModuleBuilder.Material lead = new ModuleBuilder.Material("lead", "lead", 0xd9f3ccf, 0xd9f3ccf, 0, 66, "tag", "forge:ingots/lead", 1, Capability.hammer, 2, metalReferences).addItemEffect(ItemEffect.toughness, 1);
        ModuleBuilder.Material quicksilver = new ModuleBuilder.Material("quicksilver", "quicksilver", 0xB4C3C4, 0xB4C3C4, 1, 110, "tag", "forge:ingots/quicksilver", 1, Capability.hammer, 1, metalReferences).addItemEffect(ItemEffect.quickStrike, 1);
        ModuleBuilder.Material cactus = new ModuleBuilder.Material("cactus", "cactus", 0x649832, 0x649832, 0, 15, "item", "minecraft:cactus", 1, Capability.cut, 1, woodReferences);


        // Setup each module which should have additional variants generated here, using the materials defined above. The material is paired with an
        // an item below which is then used to grab additional data for the module, e.g. damage, mining speed or durability. There are several methods
        // for offsetting the values that are grabbed from both the item and the material
        setupModule(
                "double/basic_pickaxe", // this is resource location / path for the module, check src/main/resources/data/tetra/modules to see what's available
                "basic_pickaxe", // this will be used to prefix variant keys, variant keys typically begin with the module name e.g. basic_pickaxe/iron
                "%s pick", // %s will be replaced by the localization entry for each material to produce the names for all module variants
                "basic_pickaxe/iron", // the generator will fall back to using the variant with this key if none of the references from the material matches any variant key
                "double/basic_pickaxe/basic_pickaxe") // the path for the schema file, I've not been consistent in how I've structured this so double check that this is correct
                .offsetDurability(-20, 0.5f) // pickaxes have two heads and the default handle has 20 durability so the durability of the module should be = (itemDurability - 20) * 0.5
                .offsetSpeed(0, 0.5f) // same math goes for the speed, the flimsy handle has no impact on speed so the speed of the item should be split equally between the heads
                .addVariant(amethyst, getItemName("pickaxe", amethyst))
                .addVariant(quicksilver, getItemName("pickaxe", quicksilver))
                .addVariant(cactus, getItemName("pickaxe", cactus));

        setupModule("double/basic_axe", "basic_axe", "%s axe", "basic_axe/iron", "double/basic_axe/basic_axe")
                .offsetOutcome(2, 0) // offsets the amount of material required (defined per material above) by a multiplier of two
                .offsetDurability(-20, 0.75f)
                .offsetSpeed(-0.1f, 1)
                .addVariant(amethyst, getItemName("axe", amethyst))
                .addVariant(quicksilver, getItemName("axe", quicksilver))
                .addVariant(cactus, getItemName("axe", cactus));

        setupModule("double/hoe", "hoe", "%s hoe","hoe/iron", "double/hoe/hoe")
                .offsetOutcome(2, 0)
                .offsetDurability(-20, 0.75f)
                .offsetSpeed(-0.1f, 1)
                .addVariant(amethyst, getItemName("hoe", amethyst))
                .addVariant(quicksilver, getItemName("hoe", quicksilver))
                .addVariant(cactus, getItemName("hoe", cactus));

        setupModule("double/butt", "butt", "%s butt", "butt/iron", "double/butt/butt")
                .offsetOutcome(1, -1)
                .offsetDurability(0, .25f)
                .addVariant(amethyst, getItemName("axe", amethyst))
                .addVariant(quicksilver, getItemName("axe", quicksilver))
                .addVariant(cactus, getItemName("axe", cactus));

        setupModule("sword/basic_blade", "basic_blade", "%s blade", "basic_blade/iron", "sword/basic_blade")
                .offsetSpeed(-.45f, 1)
                .addVariant(amethyst, getItemName("sword", amethyst))
                .addVariant(quicksilver, getItemName("sword", quicksilver))
                .addVariant(cactus, getItemName("sword", cactus));

        setupModule("sword/heavy_blade", "heavy_blade", "Heavy %s blade", "heavy_blade/iron", "sword/heavy_blade")
                .offsetIntegrity(-1)
                .offsetSpeed(-.575f, .25f)
                .offsetDurability(-10, 1)
                .offsetOutcome(8, 0)
                .addVariant(amethyst, getItemName("sword", amethyst))
                .addVariant(quicksilver, getItemName("sword", quicksilver))
                .addVariant(cactus, getItemName("sword", cactus));

        setupModule("sword/machete", "machete", "%s machete", "machete/iron", "sword/machete")
                .offsetIntegrity(-1)
                .offsetSpeed(-1.0888888f, 1.5f)
                .offsetOutcome(2, 0)
                .addVariant(amethyst, getItemName("sword", amethyst))
                .addVariant(quicksilver, getItemName("sword", quicksilver))
                .addVariant(cactus, getItemName("sword", cactus));

        setupModule("sword/short_blade", "short_blade", "%s short_blade", "short_blade/iron", "sword/short_blade")
                .offsetOutcome(1, 1)
                .addVariant(amethyst, getItemName("knife", amethyst))
                .addVariant(quicksilver, getItemName("knife", quicksilver))
                .addVariant(cactus, getItemName("knife", cactus));

        setupModule("sword/socket", "sword_socket", "%s", "socket/diamond", "sword/socket")
                .addVariant(tin)
                .addVariant(lead)
                .addVariant(quicksilver)
                .addVariant(amethyst);

        setupModule("double/socket", "double_socket", "%s", "socket/diamond", "double/socket")
                .addVariant(tin)
                .addVariant(lead)
                .addVariant(quicksilver)
                .addVariant(amethyst);

        setupModule("single/socket", "single_socket", "%s", "socket/diamond", "single/socket")
                .addVariant(tin)
                .addVariant(lead)
                .addVariant(quicksilver)
                .addVariant(amethyst);


        setupModule("single/basic_shovel", "basic_shovel", "%s shovel", "basic_shovel/iron", "single/basic_shovel/basic_shovel")
                .offsetDurability(-20, 0.65f)
                .addVariant(amethyst, getItemName("shovel", amethyst))
                .addVariant(quicksilver, getItemName("shovel", quicksilver))
                .addVariant(cactus, getItemName("shovel", cactus));

        setupModule("single/spearhead", "spearhead", "%s spearhead", "spearhead/iron", "single/spearhead/spearhead")
                .offsetDurability(-20, 1)
                .offsetOutcome(2, 0)
                .addVariant(amethyst, getItemName("spear", amethyst))
                .addVariant(quicksilver, getItemName("spear", quicksilver))
                .addVariant(cactus, getItemName("spear", cactus));

    }

    private String getItemName(String tool, ModuleBuilder.Material material) {
        return String.format("mysticalworld:%s_%s", material.getKey(), tool);
    }

    @Override
    public void act(@Nonnull DirectoryCache cache) {
        setup();

        builders.forEach(builder -> saveModule(cache, builder.module, builder.getModuleJson()));
        builders.forEach(builder -> saveSchema(cache, builder.schemaPath, builder.getSchemaJson()));

        JsonObject localization = new JsonObject();
        builders.stream()
                .map(ModuleBuilder::getLocalizationEntries)
                .map(Map::entrySet)
                .flatMap(Set::stream)
                .forEach(entry -> localization.addProperty(entry.getKey(), entry.getValue()));
        saveLocalization(cache, localization);
    }

    private ModuleBuilder setupModule(String module, String prefix, String localization, String fallbackReference, String schemaPath) {
        JsonObject referenceModule = null;
        try {
            IResource resource = existingFileHelper.getResource(new ResourceLocation("tetra", module), ResourcePackType.SERVER_DATA, ".json", "modules");
            BufferedReader reader = new BufferedReader(new InputStreamReader(resource.getInputStream()));
            referenceModule = gson.fromJson(reader, JsonObject.class);

        } catch (IOException e) {
            e.printStackTrace();
        }


        ModuleBuilder builder = new ModuleBuilder(module, prefix, localization, referenceModule, fallbackReference, schemaPath);
        builders.add(builder);
        return builder;
    }

    private void saveModule(DirectoryCache cache, String moduleKey, JsonObject moduleData) {
        Path outputPath = generator.getOutputFolder().resolve("data/tetra/modules/" + moduleKey + ".json");
        try {
            IDataProvider.save(gson, cache, moduleData, outputPath);
        } catch (IOException e) {
            logger.error("Couldn't save module data to {}", outputPath, e);
        }
    }

    private void saveSchema(DirectoryCache cache, String schemaPath, JsonObject schemaData) {
        Path outputPath = generator.getOutputFolder().resolve("data/tetra/schemas/" + schemaPath + ".json");
        try {
            IDataProvider.save(gson, cache, schemaData, outputPath);
        } catch (IOException e) {
            logger.error("Couldn't save schema data to {}", outputPath, e);
        }
    }

    private void saveLocalization(DirectoryCache cache, JsonObject localizationEntries) {
        Path outputPath = generator.getOutputFolder().resolve("temp/modules_" + lang + ".json");
        try {
            IDataProvider.save(gson, cache, localizationEntries, outputPath);
        } catch (IOException e) {
            logger.error("Couldn't save localization to {}", outputPath, e);
        }
    }

    @Override
    public String getName() {
        return "tetramystical module data provider";
    }
}
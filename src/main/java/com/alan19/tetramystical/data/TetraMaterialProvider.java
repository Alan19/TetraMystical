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
                3, 70, "tag", "forge:gems/amethyst", 1, Capability.hammer, 2, gemReferences, Pair.of("enchantment/looting", 1), Pair.of("enchantment/fortune", 1));
        ModuleBuilder.Material tin = new ModuleBuilder.Material("tin", "tin", 0xd9f3ccf, 0xd9f3ccf, 0, 72, "tag", "forge:ingots/tin", 1, Capability.hammer, 1, metalReferences);

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
                .addVariant(amethyst, "mysticalworld:amethyst_pickaxe");

        setupModule("double/basic_axe", "basic_axe", "%s axe", "basic_axe/iron", "double/basic_axe/basic_axe")
                .offsetOutcome(2, 0) // offsets the amount of material required (defined per material above) by a multiplier of two
                .offsetDurability(-20, 0.7f)
                .offsetSpeed(-0.1f, 1)
                .addVariant(amethyst, "mysticalworld:amethyst_axe")
                .addVariant(tin);

        setupModule("double/butt", "butt", "%s butt", "butt/iron", "double/butt/butt")
                .offsetOutcome(1, -1)
                .addVariant(amethyst)
                .addVariant(tin);

        setupModule("sword/basic_blade", "basic_blade", "%s blade", "basic_blade/iron", "sword/basic_blade")
                .offsetDurability(-10, 1)
                .addVariant(amethyst, "mysticalworld:amethyst_sword")
                .addVariant(tin);

        setupModule("sword/heavy_blade", "heavy_blade", "Heavy %s blade", "heavy_blade/iron", "sword/heavy_blade")
                .offsetIntegrity(-1)
                .offsetSpeed(-.575f, .25f)
                .offsetDurability(-10, 1)
                .offsetOutcome(8, 0)
                .addVariant(amethyst)
                .addVariant(tin);

        setupModule("sword/machete", "machete", "%s machete", "machete/iron", "sword/machete")
                .offsetIntegrity(-1)
                .offsetSpeed((float) (-.2 - 8 / 9f), 1.5f)
                .offsetOutcome(2, 0)
                .addVariant(amethyst)
                .addVariant(tin);

        setupModule("sword/short_blade", "short_blade", "%s short_blade", "short_blade/iron", "sword/short_blade")
                .offsetSpeed(1 + 1 / 30f, 7 / 3f)
                .offsetOutcome(1, 1)
                .addVariant(amethyst, "mysticalworld:amethyst_knife")
                .addVariant(tin);

        setupModule("sword/socket", "sword_socket", "%s", "socket/diamond", "sword/socket")
                .addVariant(tin);
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
            IResource resource = existingFileHelper.getResource(new ResourceLocation("reference", module), ResourcePackType.SERVER_DATA, ".json", "modules");
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
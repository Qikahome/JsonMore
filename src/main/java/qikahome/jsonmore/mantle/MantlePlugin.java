package qikahome.jsonmore.mantle;

import static qikahome.jsonmore.JsonMore.*;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import dev.gigaherz.jsonthings.things.parsers.ThingParseException;
import dev.gigaherz.jsonthings.things.serializers.FlexItemType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.font.FontManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.registries.ForgeRegistries;
import qikahome.jsonmore.mantle.ingredient.FluidItemIngredient;
import qikahome.jsonmore.tconstruct.FlexFluidTankItem;
import slimeknights.mantle.client.book.BookLoader;
import slimeknights.mantle.client.book.data.BookData;
import slimeknights.mantle.client.book.repository.FileRepository;
import slimeknights.mantle.client.book.transformer.BookTransformer;
import slimeknights.tconstruct.shared.block.PlaceBlockDispenserBehavior;

public class MantlePlugin {
    private static final Map<ResourceLocation, BiConsumer<JsonObject, BookData>> BOOK_DATA_TRANSFORMERS = new HashMap<>();
    private static final Map<BookData, JsonArray> BOOKS = new HashMap<>();

    public static void load() {
        LOGGER.info("Loading JsonMore MantlePlugin");

        registerTransformer(new ResourceLocation("mantle:index"), (jsonObject, bookData) -> {
            bookData.addTransformer(BookTransformer.indexTranformer());
        });
        registerTransformer(new ResourceLocation("mantle:padding"), (jsonObject, bookData) -> {
            bookData.addTransformer(BookTransformer.paddingTransformer());
        });
        registerTransformer(new ResourceLocation("mantle:content_table"), (jsonObject, bookData) -> {
            bookData.addTransformer(BookTransformer.contentTableTransformer());
        });
        registerTransformer(new ResourceLocation("mantle:repository"), (jsonObject, bookData) -> {
            if (jsonObject == null || !jsonObject.has("id"))
                throw new ThingParseException("mantle:repository requires a JSON object with 'id' field");
            bookData.addRepository(new FileRepository(new ResourceLocation(jsonObject.get("id").getAsString())));
        });
        registerTransformer(new ResourceLocation("mantle:set_unicode"), (jsonObject, bookData) -> {
            bookData.fontRenderer = unicodeFontRender();
        });

        FlexItemType.register("jsonmore:book", data -> {
            ResourceLocation bookId = new ResourceLocation(GsonHelper.getAsString(data, "book_id"));
            BookData bookData = BookLoader.registerBook(bookId, false, false);
            JsonArray jsonArray = GsonHelper.getAsJsonArray(data, "book_data");
            BOOKS.put(bookData, jsonArray);
            return (props, builder) -> {
                return new FlexBookItem(props, builder, bookData);
            };
        });
    }

    public static void onClientSetup() {
        for (var entry : BOOKS.entrySet()) {
            JsonArray jsonArray = entry.getValue();
            for (JsonElement element : jsonArray) {
                if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isString()) {
                    ResourceLocation stringLocation = new ResourceLocation(element.getAsString());
                    getTransformer(stringLocation).accept(null, entry.getKey());
                } else if (element.isJsonObject()) {
                    JsonObject jsonObject = element.getAsJsonObject();
                    ResourceLocation id = new ResourceLocation(GsonHelper.getAsString(jsonObject, "type"));
                    getTransformer(id).accept(jsonObject, entry.getKey());
                } else
                    throw new ThingParseException("Book Data must be a string or object");
            }
        }
    }


    public static void onCommonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            FluidItemIngredient.register();
        });
    }

    /**
     * Registers a book data transformer.
     * @param actionId The action ID to use for the transformer.
     * @param transformer The transformer to register.
     */
    public static void registerTransformer(ResourceLocation actionId, BiConsumer<JsonObject, BookData> transformer) {
        BOOK_DATA_TRANSFORMERS.put(actionId, transformer);
    }

    /**
     * Gets a book data transformer.
     * @param actionId The action ID to use for the transformer.
     * @return The transformer.
     * @throws ThingParseException If the transformer is not found.
     */
    public static BiConsumer<JsonObject, BookData> getTransformer(ResourceLocation actionId)
            throws ThingParseException {
        if (!BOOK_DATA_TRANSFORMERS.containsKey(actionId)) {
            throw new ThingParseException("Book Data Transformer not found: " + actionId);
        }
        return BOOK_DATA_TRANSFORMERS.get(actionId);
    }

    // region SlimeKnights
    private static Font unicodeRenderer;

    /** Gets the unicode font renderer */
    public static Font unicodeFontRender() {
        if (unicodeRenderer == null)
            unicodeRenderer = new Font(rl -> {
                FontManager resourceManager = Minecraft.getInstance().fontManager;
                return resourceManager.fontSets.get(Minecraft.UNIFORM_FONT);
            }, false);

        return unicodeRenderer;
    }
    // endregion
}

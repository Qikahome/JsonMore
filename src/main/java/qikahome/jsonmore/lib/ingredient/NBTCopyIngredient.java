package qikahome.jsonmore.lib.ingredient;

import static qikahome.jsonmore.JsonMore.LOGGER;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nullable;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.common.crafting.CraftingHelper;
import net.minecraftforge.common.crafting.IIngredientSerializer;

public class NBTCopyIngredient extends SelfConsumingIngredient {
    public static final ResourceLocation ID = new ResourceLocation("jsonmore:nbt_copy");

    private static final Pattern PATH_PATTERN = Pattern.compile("(\\w+)(?:\\[(\\d+)\\])?");

    public enum Mode {
        REPLACE,
        MERGE_TARGET_FIRST,
        MERGE_SOURCE_FIRST
    }

    private final Mode mode;
    @Nullable
    private final List<String> tags;
    @Nullable
    private final ItemStack remainder_override;
    @Nullable
    private final transient Set<PathSpec> parsedPaths;

    private static class PathSpec {
        final List<String> keys;
        final List<Integer> indices;

        PathSpec(List<String> keys, List<Integer> indices) {
            this.keys = keys;
            this.indices = indices;
        }
    }

    public NBTCopyIngredient(Ingredient ingredient, Mode mode) {
        this(ingredient, mode, null, null);
    }

    public NBTCopyIngredient(Ingredient ingredient, Mode mode, @Nullable List<String> tags) {
        this(ingredient, mode, tags, null);
    }

    public NBTCopyIngredient(Ingredient ingredient, Mode mode, @Nullable ItemStack remainder_override) {
        this(ingredient, mode, null, remainder_override);
    }

    public NBTCopyIngredient(Ingredient ingredient, Mode mode, @Nullable List<String> tags, @Nullable ItemStack remainder_override) {
        super(ingredient);
        this.mode = mode;
        this.tags = tags;
        this.remainder_override = remainder_override == null ? null : remainder_override.copy();
        this.parsedPaths = (tags != null && !tags.isEmpty()) ? parsePaths(tags) : null;
    }

    private Set<PathSpec> parsePaths(List<String> paths) {
        Set<PathSpec> specs = new HashSet<>();
        for (String path : paths) {
            List<String> keys = new ArrayList<>();
            List<Integer> indices = new ArrayList<>();
            
            Matcher matcher = PATH_PATTERN.matcher(path);
            int pos = 0;
            boolean valid = true;
            
            while (matcher.find(pos)) {
                String key = matcher.group(1);
                String indexStr = matcher.group(2);
                
                keys.add(key);
                indices.add(indexStr != null ? Integer.parseInt(indexStr) : -1);
                pos = matcher.end();
                
                if (pos < path.length() && path.charAt(pos) != '.') {
                    valid = false;
                    break;
                }
                pos++;
            }
            
            if (valid && !keys.isEmpty()) {
                specs.add(new PathSpec(keys, indices));
            }
        }
        return specs;
    }

    @Override
    public ItemStack consume(ItemStack stack) {
        if (stack.isEmpty())
            return stack;
        if (stack.getCount() > 1)
            throw new IllegalArgumentException("NBTCopyIngredient only consumes single items");
        return stack;
    }

    public void copyNBT(ItemStack target, ItemStack source) {
        if (target.isEmpty() || source.isEmpty())
            return;

        CompoundTag targetNBT = target.getTag();
        CompoundTag sourceNBT = source.getTag();

        if (sourceNBT == null)
            return;

        switch (mode) {
            case REPLACE:
                if (targetNBT != null) {
                    target.setTag(sourceNBT.copy());
                } else {
                    target.setTag(sourceNBT.copy());
                }
                break;

            case MERGE_TARGET_FIRST:
                if (targetNBT == null) {
                    target.setTag(sourceNBT.copy());
                } else {
                    CompoundTag merged = targetNBT.copy();
                    mergeNBT(merged, sourceNBT, false);
                    target.setTag(merged);
                }
                break;

            case MERGE_SOURCE_FIRST:
                if (targetNBT == null) {
                    target.setTag(sourceNBT.copy());
                } else {
                    CompoundTag merged = sourceNBT.copy();
                    mergeNBT(merged, targetNBT, false);
                    target.setTag(merged);
                }
                break;
        }
    }

    private void mergeNBT(CompoundTag target, CompoundTag source, boolean sourceWins) {
        mergeNBT(target, source, sourceWins, new ArrayList<>(), 0);
    }

    private void mergeNBT(CompoundTag target, CompoundTag source, boolean sourceWins, 
                          List<String> currentPath, int depth) {
        for (String key : source.getAllKeys()) {
            List<String> newPath = new ArrayList<>(currentPath);
            newPath.add(key);
            
            if (parsedPaths != null && !parsedPaths.isEmpty()) {
                if (!matchesAnyPath(newPath, depth)) {
                    continue;
                }
            }

            Tag sourceTag = source.get(key);
            if (sourceTag == null)
                continue;

            if (target.contains(key)) {
                Tag targetTag = target.get(key);
                if (targetTag instanceof CompoundTag && sourceTag instanceof CompoundTag) {
                    mergeNBT((CompoundTag) targetTag, (CompoundTag) sourceTag, sourceWins, newPath, depth + 1);
                } else if (targetTag instanceof ListTag && sourceTag instanceof ListTag) {
                    if (sourceWins) {
                        target.put(key, sourceTag.copy());
                    }
                } else {
                    if (sourceWins) {
                        target.put(key, sourceTag.copy());
                    }
                }
            } else {
                target.put(key, sourceTag.copy());
            }
        }
    }

    private boolean matchesAnyPath(List<String> currentPath, int depth) {
        for (PathSpec spec : parsedPaths) {
            if (currentPath.size() > spec.keys.size()) {
                continue;
            }
            
            boolean matches = true;
            for (int i = 0; i < currentPath.size() && i < spec.keys.size(); i++) {
                if (!spec.keys.get(i).equals(currentPath.get(i))) {
                    matches = false;
                    break;
                }
                int index = spec.indices.get(i);
                if (index >= 0) {
                    matches = false;
                    break;
                }
            }
            
            if (matches) {
                if (currentPath.size() == spec.keys.size()) {
                    int lastIndex = spec.indices.get(spec.keys.size() - 1);
                    if (lastIndex >= 0) {
                        return true;
                    }
                }
                return true;
            }
        }
        return false;
    }

    @Override
    public IIngredientSerializer<? extends Ingredient> getSerializer() {
        return Serializer.INSTANCE;
    }

    @Override
    public JsonElement toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("type", ID.toString());
        json.add("ingredient", ingredient.toJson());
        json.addProperty("mode", mode.name());
        if (tags != null && !tags.isEmpty()) {
            if (tags.size() == 1) {
                json.addProperty("tags", tags.get(0));
            } else {
                JsonArray tagsArray = new JsonArray();
                for (String tag : tags) {
                    tagsArray.add(tag);
                }
                json.add("tags", tagsArray);
            }
        }
        if (remainder_override != null) {
            DataResult<JsonElement> result = ItemStack.CODEC.encodeStart(JsonOps.INSTANCE, remainder_override);
            JsonElement element = result.getOrThrow(false, exception -> LOGGER.error(exception));
            json.add("remainder_override", element);
        }
        return json;
    }

    public static class Serializer implements IIngredientSerializer<NBTCopyIngredient> {
        public static final Serializer INSTANCE = new Serializer();

        @Override
        public NBTCopyIngredient parse(FriendlyByteBuf buffer) {
            Ingredient ingredient = Ingredient.fromNetwork(buffer);
            int modeOrdinal = buffer.readVarInt();
            Mode mode = Mode.values()[modeOrdinal];
            boolean hasTags = buffer.readBoolean();
            List<String> tags = null;
            if (hasTags) {
                int tagCount = buffer.readVarInt();
                tags = new java.util.ArrayList<>();
                for (int i = 0; i < tagCount; i++) {
                    tags.add(buffer.readUtf());
                }
            }
            boolean hasRemainderOverride = buffer.readBoolean();
            ItemStack remainder_override = null;
            if (hasRemainderOverride) {
                remainder_override = buffer.readItem();
            }
            return new NBTCopyIngredient(ingredient, mode, tags, remainder_override);
        }

        @Override
        public NBTCopyIngredient parse(JsonObject json) {
            if (!json.has("ingredient")) {
                throw new JsonParseException("NBTCopyIngredient must have 'ingredient' field");
            }

            Mode mode = Mode.REPLACE;
            if (json.has("mode")) {
                String modeStr = GsonHelper.getAsString(json, "mode");
                try {
                    mode = Mode.valueOf(modeStr);
                } catch (IllegalArgumentException e) {
                    throw new JsonParseException("Invalid mode: " + modeStr);
                }
            }

            List<String> tags = null;
            if (json.has("tags")) {
                JsonElement tagsElement = json.get("tags");
                if (tagsElement.isJsonArray()) {
                    tags = new java.util.ArrayList<>();
                    for (JsonElement element : tagsElement.getAsJsonArray()) {
                        tags.add(element.getAsString());
                    }
                } else {
                    tags = Arrays.asList(GsonHelper.getAsString(json, "tags"));
                }
            }

            ItemStack remainder_override = null;
            if (json.has("remainder_override")) {
                DataResult<ItemStack> result = ItemStack.CODEC.parse(JsonOps.INSTANCE, json.get("remainder_override"));
                remainder_override = result.getOrThrow(false,
                        exception -> new JsonParseException("Invalid remainder_override: " + exception));
            }

            JsonElement ingredientJson = json.get("ingredient");
            Ingredient ingredient = Ingredient.fromJson(ingredientJson);

            return new NBTCopyIngredient(ingredient, mode, tags, remainder_override);
        }

        @Override
        public void write(FriendlyByteBuf buffer, NBTCopyIngredient ingredient) {
            ingredient.ingredient.toNetwork(buffer);
            buffer.writeVarInt(ingredient.mode.ordinal());
            buffer.writeBoolean(ingredient.tags != null);
            if (ingredient.tags != null) {
                buffer.writeVarInt(ingredient.tags.size());
                for (String tag : ingredient.tags) {
                    buffer.writeUtf(tag);
                }
            }
            buffer.writeBoolean(ingredient.remainder_override != null);
            if (ingredient.remainder_override != null) {
                buffer.writeItemStack(ingredient.remainder_override, false);
            }
        }
    }

    public static void register() {
        CraftingHelper.register(ID, Serializer.INSTANCE);
    }
}

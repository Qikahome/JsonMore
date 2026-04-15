package qikahome.jsonmore.lib;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Predicate;

import javax.annotation.Nullable;

import com.google.gson.JsonElement;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

public class ItemFilter implements Predicate<ItemStack> {
    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger("JsonMore/ItemFilter");
    
    private Ingredient ingredient;
    private JsonElement pendingJson;
    private boolean resolved = false;
    
    private final Map<ItemStackKey, Boolean> cache = new LinkedHashMap<ItemStackKey, Boolean>(16, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<ItemStackKey, Boolean> eldest) {
            return size() > 5;
        }
    };

    public static final ItemFilter EMPTY = new ItemFilter((Ingredient) null);

    public ItemFilter(@Nullable Ingredient ingredient) {
        this.ingredient = ingredient;
        this.resolved = true;
    }

    public ItemFilter(JsonElement json) {
        this.pendingJson = json;
        this.resolved = false;
        LOGGER.debug("Created ItemFilter with pending JSON: {}", json);
    }

    public static ItemFilter parse(JsonElement json) {
        if (json == null || json.isJsonNull()) {
            return EMPTY;
        }
        return new ItemFilter(json);
    }

    private void resolve() {
        if (resolved) {
            return;
        }
        if (pendingJson != null) {
            LOGGER.debug("Resolving ItemFilter from JSON: {}", pendingJson);
            try {
                ingredient = Ingredient.fromJson(pendingJson);
                LOGGER.debug("Resolved ingredient: {}", ingredient);
            } catch (Exception e) {
                LOGGER.error("Failed to resolve ingredient from JSON", e);
            }
        }
        resolved = true;
        pendingJson = null;
    }

    @Override
    public boolean test(ItemStack stack) {
        resolve();
        if (ingredient == null) {
            return true;
        }
        
        ItemStackKey key = new ItemStackKey(stack);
        synchronized (cache) {
            Boolean cached = cache.get(key);
            if (cached != null) {
                return cached;
            }
        }
        
        boolean result = ingredient.test(stack);
        
        synchronized (cache) {
            cache.put(key, result);
        }
        
        return result;
    }

    public boolean isEmpty() {
        resolve();
        return ingredient == null;
    }

    @Nullable
    public Ingredient getIngredient() {
        resolve();
        return ingredient;
    }
    
    public void clearCache() {
        synchronized (cache) {
            cache.clear();
        }
    }
    
    private static class ItemStackKey {
        private final int itemId;
        private final int damage;
        @Nullable
        private final net.minecraft.nbt.CompoundTag tag;
        private final int hashCode;
        
        ItemStackKey(ItemStack stack) {
            this.itemId = net.minecraft.core.registries.BuiltInRegistries.ITEM.getId(stack.getItem());
            this.damage = stack.getDamageValue();
            this.tag = stack.getTag();
            this.hashCode = computeHashCode();
        }
        
        private int computeHashCode() {
            int result = itemId;
            result = 31 * result + damage;
            result = 31 * result + (tag != null ? tag.hashCode() : 0);
            return result;
        }
        
        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (!(obj instanceof ItemStackKey other)) return false;
            if (itemId != other.itemId || damage != other.damage) return false;
            return java.util.Objects.equals(tag, other.tag);
        }
        
        @Override
        public int hashCode() {
            return hashCode;
        }
    }
}

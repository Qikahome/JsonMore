package qikahome.jsonmore.lib.registration;

import java.util.function.Supplier;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;

/**
 * 注册句柄：等价 Forge 侧 {@code RegistryObject} / Porting Lib 的 {@code DeferredHolder}。
 * 注册前 {@link #get()} 会抛异常（防止误在注册前使用）。
 */
public class DeferredHolder<T, R extends T> implements Supplier<R> {
    private final ResourceLocation id;
    private Supplier<R> supplier;
    private R value;

    DeferredHolder(ResourceLocation id, Supplier<R> supplier) {
        this.id = id;
        this.supplier = supplier;
    }

    void register(Registry<T> registry) {
        if (value != null)
            return;
        value = supplier.get();
        Registry.register(registry, id, value);
        supplier = null;
    }

    public ResourceLocation getId() {
        return id;
    }

    @Override
    public R get() {
        if (value == null)
            throw new IllegalStateException("Tried to access value of " + id + " before it was registered");
        return value;
    }

    /** 是否已完成注册（等价 Porting Lib 的 {@code isPresent()}）。 */
    public boolean isPresent() {
        return value != null;
    }
}

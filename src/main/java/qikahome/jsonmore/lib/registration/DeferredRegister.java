package qikahome.jsonmore.lib.registration;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;

/**
 * Forge {@code DeferredRegister} 的极简 Fabric 替代。
 *
 * <p>Porting Lib 2.3.x（1.20.1）只提供 {@code porting_lib.util.RegistryObject}，没有 DeferredRegister，
 * 且 Fabric 也没有"注册事件"可以回放。这里保留 Forge 侧的写法（先登记、稍后统一注册），
 * 由入口点在 {@code onInitialize} 里调用 {@link #register()} 完成真正的注册。
 */
public class DeferredRegister<T> {
    private final Registry<T> registry;
    private final String modid;
    private final List<DeferredHolder<T, ?>> entries = new ArrayList<>();

    private DeferredRegister(Registry<T> registry, String modid) {
        this.registry = registry;
        this.modid = modid;
    }

    public static <T> DeferredRegister<T> create(Registry<T> registry, String modid) {
        return new DeferredRegister<>(registry, modid);
    }

    /** 登记一个条目（此时不注册），返回可供其它代码引用的句柄。 */
    public <R extends T> DeferredHolder<T, R> register(String name, Supplier<R> supplier) {
        DeferredHolder<T, R> holder = new DeferredHolder<>(new ResourceLocation(modid, name), supplier);
        entries.add(holder);
        return holder;
    }

    /** 把登记过的条目全部注册进游戏注册表（Fabric 侧在入口点里调用一次）。 */
    public void register() {
        for (DeferredHolder<T, ?> holder : entries) {
            holder.register(registry);
        }
    }
}

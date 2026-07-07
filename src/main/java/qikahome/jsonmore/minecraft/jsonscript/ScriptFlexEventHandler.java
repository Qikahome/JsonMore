package qikahome.jsonmore.minecraft.jsonscript;

import dev.gigaherz.jsonthings.things.events.ContextValue;
import dev.gigaherz.jsonthings.things.events.FlexEventContext;
import dev.gigaherz.jsonthings.things.events.FlexEventResult;
import dev.gigaherz.jsonthings.things.scripting.ThingScript;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import qikahome.jsonmore.minecraft.gamerule.FlexGameRuleType;
import qikahome.jsonmore.minecraft.jsonscript.variable.BlockPosVariable;
import qikahome.jsonmore.minecraft.jsonscript.variable.BlockStateVariable;
import qikahome.jsonmore.minecraft.jsonscript.variable.EntityVariable;
import qikahome.jsonmore.minecraft.jsonscript.variable.ItemStackVariable;
import qikahome.jsonmore.minecraft.jsonscript.variable.NumberVariable;
import qikahome.jsonmore.minecraft.jsonscript.variable.ServerVariable;
import qikahome.jsonmore.minecraft.jsonscript.variable.StringVariable;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;

/**
 * 将 {@link ScriptNode} 节点链桥接到 JsonThings 的事件系统。
 * <p>
 * 作为 {@link ThingScript} 的子类，可以通过 {@link dev.gigaherz.jsonthings.things.scripting.ScriptParser#getEvent(ResourceLocation)}
 * 查找，从而被 JsonThings 的事件机制引用。
 */
public class ScriptFlexEventHandler extends ThingScript {

    private final ResourceLocation id;
    private final List<ScriptNode> nodes;

    public ScriptFlexEventHandler(ResourceLocation id, List<ScriptNode> nodes) {
        this.id = id;
        this.nodes = nodes;
    }

    public ResourceLocation getId() {
        return id;
    }

    @Override
    public FlexEventResult apply(String eventName, FlexEventContext flexCtx) {
        if (nodes.isEmpty()) {
            return FlexEventResult.pass();
        }

        // 1. 创建脚本状态，注入 FlexEventContext 中的变量
        ScriptState state = new ScriptState();
        fillVariables(state, flexCtx);

        // 2. 顺序执行所有节点，遇到 return/fail 就终止
        for (ScriptNode node : nodes) {
            node.process(state);
            if (!state.shouldContinue()) {
                break;
            }
        }

        // 3. 返回结果
        FlexEventResult result = state.returnValue();
        if (state.flow() == ScriptState.FlowControl.FAIL) {
            return result != null ? result : FlexEventResult.fail();
        }
        if (state.flow() == ScriptState.FlowControl.RETURN) {
            return result != null ? result : FlexEventResult.success();
        }
        // 没有 return 也没 fail → pass
        return FlexEventResult.pass();
    }

    /**
     * 将 {@link FlexEventContext} 中的已知变量注入 {@link ScriptState}。
     */
    private void fillVariables(ScriptState state, FlexEventContext ctx) {
        // -- 基础上下文 --
        putState(state, "level", ctx, FlexEventContext.WORLD, Level.class, ScriptFlexEventHandler::scriptLevel);
        state.setVar("server", new ServerVariable(
                ctx.has(FlexEventContext.WORLD) ? ctx.get(FlexEventContext.WORLD).getServer() : null));
        putState(state, "stack", ctx, FlexEventContext.STACK, ItemStack.class,
                v -> new ItemStackVariable(v));
        putState(state, "user", ctx, FlexEventContext.USER, LivingEntity.class,
                ScriptFlexEventHandler::scriptEntity);

        // -- 方块相关 --
        putState(state, "block_pos", ctx, FlexEventContext.BLOCK_POS, BlockPos.class,
                v -> new BlockPosVariable(v));
        putState(state, "block_state", ctx, FlexEventContext.BLOCK_STATE, BlockState.class,
                v -> new BlockStateVariable(v));

        // -- 交互相关 --
        if (ctx.has(FlexEventContext.HIT_POS)) {
            putState(state, "hit_pos", ctx, FlexEventContext.HIT_POS, BlockPos.class,
                    v -> new BlockPosVariable(v));
        }
        // hand（mcfunction 命令槽位名：weapon.mainhand / weapon.offhand）
        if (ctx.has(FlexEventContext.HAND)) {
            String handName = switch (ctx.get(FlexEventContext.HAND)) {
                case MAIN_HAND -> "weapon.mainhand";
                case OFF_HAND -> "weapon.offhand";
            };
            state.setVar("hand", new StringVariable(handName));
        }

        if (ctx.has(FlexEventContext.ATTACKER)) {
            putState(state, "attacker", ctx, FlexEventContext.ATTACKER, Entity.class,
                    ScriptFlexEventHandler::scriptEntity);
        }
        if (ctx.has(FlexEventContext.TARGET)) {
            putState(state, "target", ctx, FlexEventContext.TARGET, Entity.class,
                    ScriptFlexEventHandler::scriptEntity);
        }

        // -- 数字 --
        if (ctx.has(FlexEventContext.SLOT)) {
            state.setVar("slot", new NumberVariable(ctx.get(FlexEventContext.SLOT)));
        }

        // -- 游戏规则相关 --
        if (ctx.has(FlexGameRuleType.GAMERULE)) {
            GameRules.Value<?> rule = ctx.get(FlexGameRuleType.GAMERULE);
            state.setVar("gamerule", new StringVariable(rule.toString()));
        }
    }

    /**
     * 从 FlexEventContext 中按 ContextValue 取出值，包装为 ScriptVariable 放入 ScriptState。
     */
    private static <T> void putState(ScriptState state, String varName,
                                     FlexEventContext ctx, ContextValue<T> ctxKey,
                                     Class<T> type,
                                     java.util.function.Function<T, ScriptVariable<?>> wrapper) {
        if (ctx.has(ctxKey)) {
            T value = ctx.get(ctxKey);
            if (value != null && type.isInstance(value)) {
                state.setVar(varName, wrapper.apply(type.cast(value)));
            }
        }
    }

    private static ScriptVariable<?> scriptLevel(Level level) {
        ResourceLocation dim = level.dimension().location();
        return new StringVariable(dim.toString());
    }

    private static ScriptVariable<?> scriptEntity(Entity entity) {
        return new EntityVariable(entity);
    }
}

package qikahome.jsonmore.minecraft.jsonscript.node;

import com.google.gson.JsonObject;

import dev.gigaherz.jsonthings.things.events.FlexEventResult;
import qikahome.jsonmore.minecraft.jsonscript.*;
import qikahome.jsonmore.minecraft.jsonscript.variable.*;

import static qikahome.jsonmore.JsonMore.LOGGER;

import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * 数学运算节点容器。所有数学运算作为内部 record 注册。
 * <p>
 * JSON 示例：
 * <pre>
 * {"type":"plus","a":1,"b":2}
 * {"type":"sqrt","a":9}
 * {"type":"multiply","a":{"type":"plus","a":"$count","b":3},"b":"$offset"}
 * </pre>
 */
public interface CalcNode extends ScriptNode {

    // -- 注册所有数学运算 --

    static void load() {
        ScriptNode.register("plus", getParser(PlusNode::new));
        ScriptNode.register("minus", getParser(MinusNode::new));
        ScriptNode.register("multiply", getParser(MultiplyNode::new));
        ScriptNode.register("divide", getParser(DivideNode::new));
        ScriptNode.register("power", getParser(PowerNode::new));
        ScriptNode.register("sqrt", getParser(SqrtNode::new));
        ScriptNode.register("round", getParser(RoundNode::new));
        ScriptNode.register("ceil", getParser(CeilNode::new));
        ScriptNode.register("floor", getParser(FloorNode::new));
        ScriptNode.register("negate", getParser(NegateNode::new));
    }

    // -- 解析器工厂 --

    static Function<JsonObject, ScriptNode> getParser(Function<ScriptNode, ScriptNode> constructor) {
        return json -> {
            ScriptNode a = ScriptNode.parse(json.get("a"));
            return constructor.apply(a);
        };
    }

    static Function<JsonObject, ScriptNode> getParser(
            BiFunction<ScriptNode, ScriptNode, ScriptNode> constructor) {
        return json -> {
            ScriptNode a = ScriptNode.parse(json.get("a"));
            ScriptNode b = ScriptNode.parse(json.get("b"));
            return constructor.apply(a, b);
        };
    }

    // -- 错误处理辅助 --

    private static ScriptVariable<?> error(String msg, Object... args) {
        LOGGER.error(msg, args);
        return NullVariable.INSTANCE;
    }

    private static void fail(ScriptState state) {
        state.fail();
        state.setReturnValue(FlexEventResult.fail());
    }

    // ================================================================
    //  加法
    // ================================================================

    record PlusNode(ScriptNode a, ScriptNode b) implements CalcNode {
        @Override
        public ScriptVariable<?> process(ScriptState state) {
            var aVar = a.process(state);
            var bVar = b.process(state);
            if (aVar instanceof NullVariable || bVar instanceof NullVariable) {
                fail(state);
                return error("Cannot Plus with values that not exists");
            }
            if (aVar instanceof StringVariable || bVar instanceof StringVariable) {
                return new StringVariable(aVar.toScriptString() + bVar.toScriptString());
            }
            if (aVar instanceof NumberVariable aNum && bVar instanceof NumberVariable bNum) {
                return new NumberVariable(aNum.doubleValue() + bNum.doubleValue());
            }
            if (aVar instanceof BlockPosVariable aPos && bVar instanceof BlockPosVariable bPos) {
                return new BlockPosVariable(aPos.x() + bPos.x(), aPos.y() + bPos.y(), aPos.z() + bPos.z());
            }
            fail(state);
            return error("Cannot Plus {} and {}", aVar.type(), bVar.type());
        }
    }

    // ================================================================
    //  减法（二元）
    // ================================================================

    record MinusNode(ScriptNode a, ScriptNode b) implements CalcNode {
        @Override
        public ScriptVariable<?> process(ScriptState state) {
            var aVar = a.process(state);
            var bVar = b.process(state);
            if (aVar instanceof NullVariable || bVar instanceof NullVariable) {
                fail(state);
                return error("Cannot Minus with values that not exists");
            }
            if (aVar instanceof NumberVariable aNum && bVar instanceof NumberVariable bNum) {
                return new NumberVariable(aNum.doubleValue() - bNum.doubleValue());
            }
            if (aVar instanceof BlockPosVariable aPos && bVar instanceof BlockPosVariable bPos) {
                return new BlockPosVariable(aPos.x() - bPos.x(), aPos.y() - bPos.y(), aPos.z() - bPos.z());
            }
            fail(state);
            return error("Cannot Minus {} and {}", aVar.type(), bVar.type());
        }
    }

    // ================================================================
    //  乘法
    // ================================================================

    record MultiplyNode(ScriptNode a, ScriptNode b) implements CalcNode {
        static { ScriptNode.register("multiply", getParser(MultiplyNode::new)); }

        @Override
        public ScriptVariable<?> process(ScriptState state) {
            var aVar = a.process(state);
            var bVar = b.process(state);
            if (aVar instanceof NullVariable || bVar instanceof NullVariable) {
                fail(state);
                return error("Cannot Multiply with values that not exists");
            }
            if (aVar instanceof NumberVariable aNum && bVar instanceof NumberVariable bNum) {
                return new NumberVariable(aNum.doubleValue() * bNum.doubleValue());
            }
            if (aVar instanceof BlockPosVariable aPos && bVar instanceof NumberVariable bNum) {
                double m = bNum.doubleValue();
                return new BlockPosVariable((int)(aPos.x() * m), (int)(aPos.y() * m), (int)(aPos.z() * m));
            }
            fail(state);
            return error("Cannot Multiply {} and {}", aVar.type(), bVar.type());
        }
    }

    // ================================================================
    //  除法
    // ================================================================

    record DivideNode(ScriptNode a, ScriptNode b) implements CalcNode {
        static { ScriptNode.register("divide", getParser(DivideNode::new)); }

        @Override
        public ScriptVariable<?> process(ScriptState state) {
            var aVar = a.process(state);
            var bVar = b.process(state);
            if (aVar instanceof NullVariable || bVar instanceof NullVariable) {
                fail(state);
                return error("Cannot Divide with values that not exists");
            }
            if (aVar instanceof NumberVariable aNum && bVar instanceof NumberVariable bNum) {
                if (bNum.doubleValue() == 0) {
                    fail(state);
                    return error("Cannot Divide by zero");
                }
                return new NumberVariable(aNum.doubleValue() / bNum.doubleValue());
            }
            if (aVar instanceof BlockPosVariable aPos && bVar instanceof NumberVariable bNum) {
                double m = bNum.doubleValue();
                if (m == 0) {
                    fail(state);
                    return error("Cannot Divide by zero");
                }
                return new BlockPosVariable((int)(aPos.x() / m), (int)(aPos.y() / m), (int)(aPos.z() / m));
            }
            fail(state);
            return error("Cannot Divide {} and {}", aVar.type(), bVar.type());
        }
    }

    // ================================================================
    //  幂运算
    // ================================================================

    record PowerNode(ScriptNode a, ScriptNode b) implements CalcNode {
        static { ScriptNode.register("power", getParser(PowerNode::new)); }

        @Override
        public ScriptVariable<?> process(ScriptState state) {
            var aVar = a.process(state);
            var bVar = b.process(state);
            if (aVar instanceof NullVariable || bVar instanceof NullVariable) {
                fail(state);
                return error("Cannot Power with values that not exists");
            }
            if (aVar instanceof NumberVariable aNum && bVar instanceof NumberVariable bNum) {
                return new NumberVariable(Math.pow(aNum.doubleValue(), bNum.doubleValue()));
            }
            fail(state);
            return error("Cannot Power {} and {}", aVar.type(), bVar.type());
        }
    }

    // ================================================================
    //  平方根（一元）
    // ================================================================

    record SqrtNode(ScriptNode a) implements CalcNode {
        static { ScriptNode.register("sqrt", getParser(SqrtNode::new)); }

        @Override
        public ScriptVariable<?> process(ScriptState state) {
            var aVar = a.process(state);
            if (aVar instanceof NullVariable) {
                fail(state);
                return error("Cannot Sqrt with value that not exists");
            }
            if (aVar instanceof NumberVariable aNum) {
                double v = aNum.doubleValue();
                if (v < 0) {
                    fail(state);
                    return error("Cannot sqrt negative number: {}", v);
                }
                return new NumberVariable(Math.sqrt(v));
            }
            fail(state);
            return error("Cannot sqrt {}", aVar.type());
        }
    }

    // ================================================================
    //  四舍五入
    // ================================================================

    record RoundNode(ScriptNode a) implements CalcNode {
        static { ScriptNode.register("round", getParser(RoundNode::new)); }

        @Override
        public ScriptVariable<?> process(ScriptState state) {
            var aVar = a.process(state);
            if (aVar instanceof NullVariable) {
                fail(state);
                return error("Cannot Round with value that not exists");
            }
            if (aVar instanceof NumberVariable aNum) {
                return new NumberVariable((int) Math.round(aNum.doubleValue()));
            }
            fail(state);
            return error("Cannot Round {}", aVar.type());
        }
    }

    // ================================================================
    //  向上取整
    // ================================================================

    record CeilNode(ScriptNode a) implements CalcNode {
        static { ScriptNode.register("ceil", getParser(CeilNode::new)); }

        @Override
        public ScriptVariable<?> process(ScriptState state) {
            var aVar = a.process(state);
            if (aVar instanceof NullVariable) {
                fail(state);
                return error("Cannot Ceil with value that not exists");
            }
            if (aVar instanceof NumberVariable aNum) {
                return new NumberVariable((int) Math.ceil(aNum.doubleValue()));
            }
            fail(state);
            return error("Cannot Ceil {}", aVar.type());
        }
    }

    // ================================================================
    //  向下取整
    // ================================================================

    record FloorNode(ScriptNode a) implements CalcNode {
        static { ScriptNode.register("floor", getParser(FloorNode::new)); }

        @Override
        public ScriptVariable<?> process(ScriptState state) {
            var aVar = a.process(state);
            if (aVar instanceof NullVariable) {
                fail(state);
                return error("Cannot Floor with value that not exists");
            }
            if (aVar instanceof NumberVariable aNum) {
                return new NumberVariable((int) Math.floor(aNum.doubleValue()));
            }
            fail(state);
            return error("Cannot Floor {}", aVar.type());
        }
    }

    // ================================================================
    //  取负（一元）
    // ================================================================

    record NegateNode(ScriptNode a) implements CalcNode {
        static { ScriptNode.register("negate", getParser(NegateNode::new)); }

        @Override
        public ScriptVariable<?> process(ScriptState state) {
            var aVar = a.process(state);
            if (aVar instanceof NullVariable) {
                fail(state);
                return error("Cannot Negate with value that not exists");
            }
            if (aVar instanceof NumberVariable aNum) {
                return new NumberVariable(-aNum.doubleValue());
            }
            if (aVar instanceof BooleanVariable b) {
                return b.negate();
            }
            fail(state);
            return error("Cannot Negate {}", aVar.type());
        }
    }
}

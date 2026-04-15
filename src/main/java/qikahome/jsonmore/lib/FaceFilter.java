package qikahome.jsonmore.lib;

import net.minecraft.core.Direction;

import javax.annotation.Nullable;

import com.google.common.base.Supplier;

import java.util.EnumSet;
import java.util.Set;

/**
 * 面过滤器枚举，用于判断给定方向是否符合过滤条件
 * 
 * 支持的过滤器：
 * - ALL: 所有 6 个面
 * - UP, DOWN, NORTH, SOUTH, EAST, WEST: 单个具体方向
 * - FRONT: 方块朝向的面（facing 为 null 时返回空）
 * - BACK: 方块朝向的反方向（facing 为 null 时返回空）
 * - HORIZONTAL: 4 个水平面 (north, south, east, west)
 * - VERTICAL: 2 个垂直面 (up, down)
 * - SIDES: 除了 front 的 5 个面（facing 为 null 时返回空）
 * - AROUND: 除了 front 和 back 的 4 个面（facing 为 null 时返回空）
 * - SIDE_HORIZONTAL: 除了 front 的水平面（facing 为垂直方向时等价于 HORIZONTAL）
 * - AROUND_HORIZONTAL: 除了 front 和 back 的水平面（facing 为垂直方向时等价于 HORIZONTAL）
 * - NONE: 无方向（用于 GUI 操作，永远返回 false）
 */
public enum FaceFilter {
    ALL {
        @Override
        public boolean test(@Nullable Direction facing, @Nullable Direction direction) {
            return direction != null;
        }
        
        @Override
        public Set<Direction> getDirections(@Nullable Direction facing) {
            return EnumSet.allOf(Direction.class);
        }
    },
    UP {
        @Override
        public boolean test(@Nullable Direction facing, @Nullable Direction direction) {
            return direction == Direction.UP;
        }
        
        @Override
        public Set<Direction> getDirections(@Nullable Direction facing) {
            return EnumSet.of(Direction.UP);
        }
    },
    DOWN {
        @Override
        public boolean test(@Nullable Direction facing, @Nullable Direction direction) {
            return direction == Direction.DOWN;
        }
        
        @Override
        public Set<Direction> getDirections(@Nullable Direction facing) {
            return EnumSet.of(Direction.DOWN);
        }
    },
    NORTH {
        @Override
        public boolean test(@Nullable Direction facing, @Nullable Direction direction) {
            return direction == Direction.NORTH;
        }
        
        @Override
        public Set<Direction> getDirections(@Nullable Direction facing) {
            return EnumSet.of(Direction.NORTH);
        }
    },
    SOUTH {
        @Override
        public boolean test(@Nullable Direction facing, @Nullable Direction direction) {
            return direction == Direction.SOUTH;
        }
        
        @Override
        public Set<Direction> getDirections(@Nullable Direction facing) {
            return EnumSet.of(Direction.SOUTH);
        }
    },
    EAST {
        @Override
        public boolean test(@Nullable Direction facing, @Nullable Direction direction) {
            return direction == Direction.EAST;
        }
        
        @Override
        public Set<Direction> getDirections(@Nullable Direction facing) {
            return EnumSet.of(Direction.EAST);
        }
    },
    WEST {
        @Override
        public boolean test(@Nullable Direction facing, @Nullable Direction direction) {
            return direction == Direction.WEST;
        }
        
        @Override
        public Set<Direction> getDirections(@Nullable Direction facing) {
            return EnumSet.of(Direction.WEST);
        }
    },
    FRONT {
        @Override
        public boolean test(@Nullable Direction facing, @Nullable Direction direction) {
            return facing != null && direction == facing;
        }
        
        @Override
        public Set<Direction> getDirections(@Nullable Direction facing) {
            return facing != null ? EnumSet.of(facing) : EnumSet.noneOf(Direction.class);
        }
    },
    BACK {
        @Override
        public boolean test(@Nullable Direction facing, @Nullable Direction direction) {
            return facing != null && direction == facing.getOpposite();
        }
        
        @Override
        public Set<Direction> getDirections(@Nullable Direction facing) {
            return facing != null ? EnumSet.of(facing.getOpposite()) : EnumSet.noneOf(Direction.class);
        }
    },
    HORIZONTAL {
        @Override
        public boolean test(@Nullable Direction facing, @Nullable Direction direction) {
            return direction != null && direction.getAxis().isHorizontal();
        }
        
        @Override
        public Set<Direction> getDirections(@Nullable Direction facing) {
            return EnumSet.of(Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST);
        }
    },
    VERTICAL {
        @Override
        public boolean test(@Nullable Direction facing, @Nullable Direction direction) {
            return direction != null && direction.getAxis().isVertical();
        }
        
        @Override
        public Set<Direction> getDirections(@Nullable Direction facing) {
            return EnumSet.of(Direction.UP, Direction.DOWN);
        }
    },
    SIDES {
        @Override
        public boolean test(@Nullable Direction facing, @Nullable Direction direction) {
            if (direction == null || facing == null) return false;
            return direction != facing;
        }
        
        @Override
        public Set<Direction> getDirections(@Nullable Direction facing) {
            if (facing == null) {
                return EnumSet.noneOf(Direction.class);
            }
            Set<Direction> dirs = EnumSet.allOf(Direction.class);
            dirs.remove(facing);
            return dirs;
        }
    },
    AROUND {
        @Override
        public boolean test(@Nullable Direction facing, @Nullable Direction direction) {
            if (direction == null || facing == null) return false;
            return direction != facing && direction != facing.getOpposite();
        }
        
        @Override
        public Set<Direction> getDirections(@Nullable Direction facing) {
            if (facing == null) {
                return EnumSet.noneOf(Direction.class);
            }
            Set<Direction> dirs = EnumSet.allOf(Direction.class);
            dirs.remove(facing);
            dirs.remove(facing.getOpposite());
            return dirs;
        }
    },
    SIDE_HORIZONTAL {
        @Override
        public boolean test(@Nullable Direction facing, @Nullable Direction direction) {
            if (direction == null || !direction.getAxis().isHorizontal()) return false;
            return facing == null || direction != facing;
        }
        
        @Override
        public Set<Direction> getDirections(@Nullable Direction facing) {
            Set<Direction> dirs = EnumSet.of(Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST);
            if (facing != null && facing.getAxis().isHorizontal()) {
                dirs.remove(facing);
            }
            return dirs;
        }
    },
    AROUND_HORIZONTAL {
        @Override
        public boolean test(@Nullable Direction facing, @Nullable Direction direction) {
            if (direction == null || !direction.getAxis().isHorizontal()) return false;
            if (facing == null) return true;
            return direction != facing && direction != facing.getOpposite();
        }
        
        @Override
        public Set<Direction> getDirections(@Nullable Direction facing) {
            Set<Direction> dirs = EnumSet.of(Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST);
            if (facing != null && facing.getAxis().isHorizontal()) {
                dirs.remove(facing);
                dirs.remove(facing.getOpposite());
            }
            return dirs;
        }
    },
    NONE {
        @Override
        public boolean test(@Nullable Direction facing, @Nullable Direction direction) {
            return false;
        }
        
        @Override
        public Set<Direction> getDirections(@Nullable Direction facing) {
            return EnumSet.noneOf(Direction.class);
        }
    },
    ANY {
        @Override
        public boolean test(@Nullable Direction facing, @Nullable Direction direction) {
            return true;
        }
        
        @Override
        public Set<Direction> getDirections(@Nullable Direction facing) {
            return EnumSet.allOf(Direction.class);
        }
    };
    
    /**
     * 测试给定方向是否符合此过滤器
     * @param facing 方块的朝向（用于 FRONT/BACK 等相对方向），可为 null
     * @param direction 要测试的方向，可为 null（GUI 操作时）
     * @return 是否符合
     */
    public abstract boolean test(@Nullable Direction facing, @Nullable Direction direction);
    
    /**
     * 获取此过滤器包含的所有方向
     * @param facing 方块的朝向
     * @return 方向集合
     */
    public abstract Set<Direction> getDirections(@Nullable Direction facing);
    
    /**
     * 如果方向匹配此过滤器，则执行 Supplier 并返回其结果，否则返回默认值
     * @param facing 方块的朝向
     * @param direction 要测试的方向
     * @param sup 匹配时执行的 Supplier
     * @param defaultValue 不匹配时返回的默认值
     * @return 匹配时返回 Supplier 的结果，否则返回默认值
     * @param <T> 返回值类型
     */
    public <T> T map(@Nullable Direction facing, @Nullable Direction direction, Supplier<T> sup, @Nullable T defaultValue) {
        if (test(facing, direction)) {
            return sup.get();
        }
        return defaultValue;
    }
    
    /**
     * 如果方向匹配此过滤器，则执行 Supplier 并返回其结果，否则返回 null
     * @param facing 方块的朝向
     * @param direction 要测试的方向
     * @param sup 匹配时执行的 Supplier
     * @return 匹配时返回 Supplier 的结果，否则返回 null
     * @param <T> 返回值类型
     */
    public <T> T map(@Nullable Direction facing, @Nullable Direction direction, Supplier<T> sup) {
        return map(facing, direction, sup, null);
    }
    
    /**
     * 从字符串解析过滤器
     * @param name 过滤器名称（支持大小写，如 "front", "FRONT", "side_horizontal"）
     * @return 对应的过滤器，若无效则返回 null
     */
    @Nullable
    public static FaceFilter fromString(String name) {
        if (name == null) return null;
        try {
            return valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
    
    /**
     * 测试方向是否匹配任意一个过滤器
     * @param filters 过滤器集合
     * @param facing 方块朝向
     * @param direction 要测试的方向
     * @return 是否匹配任意一个
     */
    public static boolean testAny(Set<FaceFilter> filters, @Nullable Direction facing, @Nullable Direction direction) {
        for (FaceFilter filter : filters) {
            if (filter.test(facing, direction)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * 合并多个过滤器的方向集合
     * @param filters 过滤器集合
     * @param facing 方块朝向
     * @return 合并后的方向集合
     */
    public static Set<Direction> unionDirections(Set<FaceFilter> filters, @Nullable Direction facing) {
        Set<Direction> result = EnumSet.noneOf(Direction.class);
        for (FaceFilter filter : filters) {
            result.addAll(filter.getDirections(facing));
        }
        return result;
    }
}

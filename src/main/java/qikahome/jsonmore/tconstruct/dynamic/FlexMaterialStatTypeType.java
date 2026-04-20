package qikahome.jsonmore.tconstruct.dynamic;

import java.util.HashMap;

import com.google.gson.JsonObject;

import qikahome.jsonmore.tconstruct.dynamic.MaterialStatTypeParser.MaterialStatTypeBuilder;
import slimeknights.tconstruct.library.materials.stats.MaterialStatType;

public class FlexMaterialStatTypeType<T extends MaterialStatType<?>> {
        public static interface IMaterialStatsSerializer<T extends MaterialStatType<?>> {
                IMaterialStatsFactory<T> createFactory(JsonObject data);
        }

        public static interface IMaterialStatsFactory<T extends MaterialStatType<?>> {
                T construct(MaterialStatTypeBuilder builder);
        }

        public static <T extends MaterialStatType<?>> FlexMaterialStatTypeType<T> register(String name,
                        IMaterialStatsSerializer<T> factory) {
                var newType = new FlexMaterialStatTypeType<T>(factory);
                REGISTRY.put(name, newType);
                return newType;
        };

        public static FlexMaterialStatTypeType<MaterialStatType<?>> PLAIN;

        public static void load() {
                // stat type fields
                DynamicStatField.REGISTRY.register(TierDynamicStatField.TYPE, TierDynamicStatField.LOADER);
                DynamicStatField.REGISTRY.register(FloatDynamicStatField.TYPE, FloatDynamicStatField.LOADER);
                PLAIN = register("jsonmore:plain", data -> builder -> builder.build());
        }

        private FlexMaterialStatTypeType(IMaterialStatsSerializer<T> factory) {
                this.factory = factory;
        }

        private final IMaterialStatsSerializer<T> factory;

        public IMaterialStatsFactory<T> getFactory(JsonObject data) {
                return factory.createFactory(data);
        }

        public static HashMap<String, FlexMaterialStatTypeType<? extends MaterialStatType<?>>> REGISTRY = new HashMap<>();

}
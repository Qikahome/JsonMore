package qikahome.jsonmore.tconstruct.parts;

import java.util.HashMap;

import com.google.gson.JsonObject;

import slimeknights.tconstruct.library.materials.stats.MaterialStatsId;

public class FlexMaterialStatTypeType<T extends FlexMaterialStatType> {
        public static interface IMaterialStatsSerializer<T extends FlexMaterialStatType> {
                IMaterialStatsFactory<T> createFactory(JsonObject data);
        }

        public static interface IMaterialStatsFactory<T extends FlexMaterialStatType> {
                T construct(MaterialStatTypeBuilder builder);
        }

        public static <T extends FlexMaterialStatType> FlexMaterialStatTypeType<T> register(String name, IMaterialStatsSerializer<T> factory) { 
                var newType = new FlexMaterialStatTypeType<T>(factory);
                REGISTRY.put(name, newType);
                return newType;
        };

        public static FlexMaterialStatTypeType<FlexMaterialStatType> PLAIN;

        public static void load() {
                PLAIN = register("jsonmore:plain", data->builder->
                new FlexMaterialStatType(
                        new MaterialStatsId(builder.getRegistryName()),
                        builder.getCanRepair(),
                        builder.getStats()
                ));
        }

        private FlexMaterialStatTypeType(IMaterialStatsSerializer<T> factory) {
                this.factory = factory;
        }
        private final IMaterialStatsSerializer<T> factory;
        public IMaterialStatsFactory<T> getFactory(JsonObject data) {
                return factory.createFactory(data);
        }

        public static HashMap<String, FlexMaterialStatTypeType<? extends FlexMaterialStatType>> REGISTRY = new HashMap<>();

}
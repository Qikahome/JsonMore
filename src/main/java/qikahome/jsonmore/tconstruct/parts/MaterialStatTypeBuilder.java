package qikahome.jsonmore.tconstruct.parts;

import java.util.LinkedHashMap;

import javax.annotation.Nullable;

import org.apache.commons.lang3.text.StrBuilder;

import com.google.common.base.Supplier;
import com.google.gson.JsonObject;

import dev.gigaherz.jsonthings.things.builders.BaseBuilder;
import dev.gigaherz.jsonthings.things.parsers.ThingParseException;
import dev.gigaherz.jsonthings.things.parsers.ThingParser;
import dev.gigaherz.jsonthings.util.Utils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.Tiers;
import qikahome.jsonmore.tconstruct.parts.FlexMaterialStatTypeType.IMaterialStatsFactory;
import qikahome.jsonmore.tconstruct.parts.StatTypes.Stat;

public class MaterialStatTypeBuilder extends BaseBuilder<FlexMaterialStatType, MaterialStatTypeBuilder> {

    protected MaterialStatTypeBuilder(ThingParser<MaterialStatTypeBuilder> ownerParser, ResourceLocation registryName) {
        super(ownerParser, registryName);
    }

    @Override
    protected String getThingTypeDisplayName() {
        return "Material Stat";
    }

    public static MaterialStatTypeBuilder begin(ThingParser<MaterialStatTypeBuilder> ownerParser,
            ResourceLocation registryName) {
        return new MaterialStatTypeBuilder(ownerParser, registryName);
    }

    private FlexMaterialStatTypeType<? extends FlexMaterialStatType> type;
    private IMaterialStatsFactory<? extends FlexMaterialStatType> factory;
    private LinkedHashMap<String, Supplier<Stat<?, ?>>> stats = new LinkedHashMap<>();
    private boolean canRepair = false;

    public LinkedHashMap<String, Supplier<Stat<?, ?>>> getStats() {
        return stats;
    }

    public void setCanRepair(boolean canRepair) {
        this.canRepair = canRepair;
    }

    public boolean getCanRepair() {
        return canRepair;
    }

    public void setType(String type) {
        FlexMaterialStatTypeType<? extends FlexMaterialStatType> partType = FlexMaterialStatTypeType.REGISTRY.get(type);
        if (partType == null)
            throw new IllegalStateException("No known part type with name " + type);
        this.type = partType;
    }

    public void setFactory(IMaterialStatsFactory<? extends FlexMaterialStatType> factory) {
        this.factory = factory;
    }

    @Nullable
    public FlexMaterialStatTypeType<? extends FlexMaterialStatType> getMaterialStatTypeTypeRaw() {
        return getValue(type, MaterialStatTypeBuilder::getMaterialStatTypeTypeRaw);
    }

    public FlexMaterialStatTypeType<? extends FlexMaterialStatType> getMaterialStatTypeType() {
        return Utils.orElseGet(getMaterialStatTypeTypeRaw(), () -> FlexMaterialStatTypeType.PLAIN);
    }

    public void setStats(JsonObject rawStats) {
        try {
            rawStats.asMap().forEach((str, val) -> {
                var jo = val.getAsJsonObject();
                var type = jo.get("type").getAsString();
                Supplier<Stat<?, ?>> stat = null;
                var regName = this.getRegistryName();
                var descToolTip = GsonHelper.getAsString(jo, "descToolTip", new StringBuilder().append(regName.getPath()).append('.')
                            .append(regName.getNamespace()).append('.').append(str).append(".description").toString());
                var infoToolTip = GsonHelper.getAsString(jo, "infoToolTip", net.minecraft.Util.makeDescriptionId("tool_stat",regName));
                
                switch (type) {
                    case "float":
                        var value = jo.get("value").getAsFloat();
                        var operator = StatTypes.Operator.valueOf(jo.get("operator").getAsString().toUpperCase());
                        stat = () -> new StatTypes.FloatStat(value, operator, descToolTip,infoToolTip);
                        break;
                    case "tier":
                        var tier = Tiers.valueOf(jo.get("value").getAsString().toUpperCase());
                        stat = () -> new StatTypes.TierStat(tier, descToolTip,infoToolTip);
                        break;
                }
                stats.put(str, stat);
            });
        } catch (Exception e) {
            throw new ThingParseException("Parse Material Stat Type Fail, ", e);
        }
    }

    @Override
    protected FlexMaterialStatType buildInternal() {
        return factory.construct(this);
    }

}

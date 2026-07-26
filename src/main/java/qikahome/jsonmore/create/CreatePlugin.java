package qikahome.jsonmore.create;

import com.simibubi.create.api.contraption.BlockMovementChecks;
import com.simibubi.create.api.contraption.BlockMovementChecks.CheckResult;

import qikahome.jsonmore.minecraft.FlexBarrelBlock;

public class CreatePlugin {
    public static void load(){
        BlockMovementChecks.registerMovementAllowedCheck(
                (s, w, p) -> s.getBlock() instanceof FlexBarrelBlock && !s.getValue(FlexBarrelBlock.CONNECTED)
                        ? CheckResult.SUCCESS : CheckResult.PASS);
    }
}

package qikahome.jsonmore.tconstruct.parts;

import slimeknights.tconstruct.library.tools.stat.ToolStatId;

class NoSuchToolStatException extends RuntimeException {
    public NoSuchToolStatException(ToolStatId stat) {
        super("No such tool stat: " + stat.toString());
    }
}
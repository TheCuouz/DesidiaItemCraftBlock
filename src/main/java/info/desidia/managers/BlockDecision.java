package info.desidia.managers;

import info.desidia.api.events.BlockReason;

public class BlockDecision {

    private final boolean blocked;
    private final BlockReason reason;
    private final String context;
    private final String customMessage;

    private BlockDecision(boolean blocked, BlockReason reason, String context, String customMessage) {
        this.blocked = blocked;
        this.reason = reason;
        this.context = context;
        this.customMessage = customMessage;
    }

    public static BlockDecision allow() {
        return new BlockDecision(false, null, null, null);
    }

    public static BlockDecision block(BlockReason reason, String context, String customMessage) {
        return new BlockDecision(true, reason, context, customMessage);
    }

    public boolean isBlocked() { return blocked; }
    public BlockReason getReason() { return reason; }
    public String getContext() { return context; }
    public String getCustomMessage() { return customMessage; }
}

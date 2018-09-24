package nats.truducer.exceptions;

public class BlockedInteractionException extends RuntimeException {
    public BlockedInteractionException (String errormsg) {
        super(errormsg);
    }
}

package jigsaw.server.ormmodels;

/**
 *
 */
public class InvalidGameStatException extends Exception {
    public InvalidGameStatException() {
        super("GameStatModel has invalid fields.");
    }

    public InvalidGameStatException(String errorMessage) {
        super(errorMessage);
    }
}

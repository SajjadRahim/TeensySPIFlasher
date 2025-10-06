package spiflasher;

public class ReportableException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public ReportableException(String message) {
        super(message);
    }

    public ReportableException(Throwable throwable) {
        super(throwable);
    }

    public ReportableException(String message, Throwable throwable) {
        super(message, throwable);
    }
}

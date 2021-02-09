package systems.cauldron.service.superresolution.web;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.Instant;

@Getter
@AllArgsConstructor
public class UpscaleJob {

    public enum Status {
        IN_PROGRESS,
        CANCELLED,
        FAILED,
        COMPLETED
    }

    private final Status status;
    private final String statusMessage;

    private final Instant startTime;
    private final Instant endTime;

    @Getter
    @AllArgsConstructor
    public static class ResultLink {
        private final String id;
        private final Instant expirationTime;
    }

    private final ResultLink result;
}

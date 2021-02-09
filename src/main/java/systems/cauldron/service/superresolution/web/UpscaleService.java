package systems.cauldron.service.superresolution.web;

import io.helidon.common.http.Http;
import io.helidon.config.Config;
import io.helidon.webserver.Routing;
import io.helidon.webserver.ServerRequest;
import io.helidon.webserver.ServerResponse;
import io.helidon.webserver.Service;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import systems.cauldron.service.superresolution.core.InferenceService;

import javax.json.Json;
import javax.json.JsonBuilderFactory;
import javax.json.JsonException;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;

public class UpscaleService implements Service {

    private static final Logger LOG = LogManager.getLogger(UpscaleService.class.getName());

    private static final JsonBuilderFactory JSON = Json.createBuilderFactory(Collections.emptyMap());

    private final ConcurrentHashMap<String, UpscaleJob> jobs = new ConcurrentHashMap<>();

    private final ConcurrentHashMap<String, byte[]> results = new ConcurrentHashMap<>();

    private final InferenceService inferenceService;

    public UpscaleService(Config config) {
        Config modelConfig = config.get("app").get("model");
        Path modelPath = Paths.get("models").resolve(modelConfig.get("filename").asString().get());
        int scalingFactor = modelConfig.get("scaling-factor").asInt().get();
        this.inferenceService = new InferenceService(modelPath, scalingFactor);
    }

    @Override
    public void update(Routing.Rules rules) {
        rules
                .get("/", this::handleRoot)
                .post("/jobs", this::submitJob)
                .get("/jobs/{id}", this::checkJob)
                .get("/results/{id}", this::serveResult);
    }


    private void handleRoot(ServerRequest request,
                            ServerResponse response) {
        response.status(404);
    }

    private void submitJob(ServerRequest request,
                           ServerResponse response) {
        UpscalePipeline pipeline = new UpscalePipeline(inferenceService);
        request.content().subscribe(pipeline);
        response.send(pipeline);
    }


    private void submitJob(JsonObject jsonObject, ServerResponse response) {
        response.status(Http.Status.NO_CONTENT_204).send();
    }

    private void checkJob(ServerRequest request,
                          ServerResponse response) {
        String jobId = request.path().param("id");
        UpscaleJob upscaleJob = jobs.get(jobId);
        if (upscaleJob == null) {
            response.status(404).send();
        } else {
            JsonObjectBuilder objectBuilder = JSON.createObjectBuilder()
                    .add("status", upscaleJob.getStatus().toString());
            switch (upscaleJob.getStatus()) {
                case IN_PROGRESS:
                    break;
                case CANCELLED:
                    objectBuilder.add("statusMessage", "processing cancelled by user");
                    break;
                case FAILED:
                    objectBuilder.add("statusMessage", "processing failed due to internal system error");
                    break;
                case COMPLETED:
                    UpscaleJob.ResultLink resultLink = upscaleJob.getResult();
                    objectBuilder.add("result", Json.createObjectBuilder()
                            .add("location", String.format("/upscale/results/%s", resultLink.getId())))
                            .add("expirationTime", resultLink.getExpirationTime().toString());
                    break;
            }
            response.send(objectBuilder.build());
        }
    }

    private void serveResult(ServerRequest request,
                             ServerResponse response) {
        String id = request.path().param("id");
        byte[] content = results.get(id);
        if (content == null) {
            response.status(404).send();
        } else {
            // TODO: send binary here
            response.send(content);
        }
    }

    private static <T> T processErrors(Throwable ex, ServerRequest request, ServerResponse response) {
        if (ex.getCause() instanceof JsonException) {
            response.status(Http.Status.BAD_REQUEST_400).send();
        } else {
            response.status(Http.Status.INTERNAL_SERVER_ERROR_500).send();
        }
        return null;
    }
}

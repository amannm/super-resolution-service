package systems.cauldron.service.superresolution;

import io.helidon.common.LogConfig;
import io.helidon.config.Config;
import io.helidon.health.HealthSupport;
import io.helidon.health.checks.HealthChecks;
import io.helidon.media.jsonp.JsonpSupport;
import io.helidon.metrics.MetricsSupport;
import io.helidon.webserver.Routing;
import io.helidon.webserver.Service;
import io.helidon.webserver.WebServer;
import io.helidon.webserver.cors.CorsSupport;
import io.helidon.webserver.cors.CrossOriginConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import systems.cauldron.service.superresolution.inference.InferenceServer;
import systems.cauldron.service.superresolution.web.UpscaleService;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

public class Server {

    private final static Logger LOG = LogManager.getLogger(Server.class);

    private Server() {
    }

    public static void main(String[] args) {
        start();
    }

    static WebServer start() {
        LogConfig.configureRuntime();

        Config config = Config.create();
        Config appConfig = config.get("app");
        Config modelConfig = appConfig.get("model");
        Path modelPath = Paths.get("models").resolve(modelConfig.get("filename").asString().get());
        int scalingFactor = modelConfig.get("scaling-factor").asInt().get();

        InferenceServer inferenceServer = new InferenceServer(modelPath, scalingFactor);

        UpscaleService upscaleService = new UpscaleService(inferenceServer);

        Map<String, Service> serviceMap = Map.of("/api/v1", upscaleService);

        WebServer server = WebServer.builder(getRouting(serviceMap))
                .config(config.get("server"))
                .addMediaSupport(JsonpSupport.create())
                .build();
        server.start().thenAccept(s -> {
            LOG.info("server started @ http://localhost:" + s.port());
            s.whenShutdown().thenRun(() -> {
                try {
                    inferenceServer.close();
                } catch (Exception ex) {
                    LOG.error("inference server was unable to shutdown cleanly", ex);
                }
                LOG.info("server stopped");
            });
        }).exceptionally(ex -> {
            LOG.error("startup failed", ex);
            return null;
        });
        return server;
    }

    private static Routing getRouting(Map<String, Service> serviceMap) {
        MetricsSupport metrics = MetricsSupport.create();
        HealthSupport health = HealthSupport.builder()
                .addLiveness(HealthChecks.healthChecks())
                .build();
        Routing.Builder routing = Routing.builder()
                .register(health)
                .register(metrics);
        CorsSupport corsSupport = CorsSupport.builder()
                .addCrossOrigin("/upscale", CrossOriginConfig.builder()
                        .allowOrigins("*")
                        .allowMethods("*")
                        .allowHeaders("*")
                        .allowCredentials(true)
                        .enabled(true)
                        .build())
                .build();
        serviceMap.forEach((path, service) -> routing.register(path, corsSupport, service));
        return routing.build();
    }
}
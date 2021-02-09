package systems.cauldron.service.superresolution;

import io.helidon.common.LogConfig;
import io.helidon.config.Config;
import io.helidon.health.HealthSupport;
import io.helidon.health.checks.HealthChecks;
import io.helidon.media.jsonp.JsonpSupport;
import io.helidon.metrics.MetricsSupport;
import io.helidon.webserver.Routing;
import io.helidon.webserver.WebServer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import systems.cauldron.service.superresolution.web.UpscaleService;

public class Server {

    private final static Logger LOG = LogManager.getLogger(Server.class);

    private Server() {
    }

    public static void main(String[] args) {
        start();
    }

    static WebServer start() {
        LogConfig.configureRuntime();
        Config config = getConfig();
        WebServer server = WebServer.builder(getRouting(config))
                .config(getConfig())
                .addMediaSupport(JsonpSupport.create())
                .build();
        server.start().thenAccept(s -> {
            LOG.info("server started @ http://localhost:" + s.port());
            s.whenShutdown().thenRun(() -> LOG.info("server stopped"));
        }).exceptionally(ex -> {
            LOG.error("startup failed", ex);
            return null;
        });

        return server;
    }

    private static Config getConfig() {
        Config config = Config.create();
        return config.get("server");
    }

    private static Routing getRouting(Config config) {
        MetricsSupport metrics = MetricsSupport.create();
        HealthSupport health = HealthSupport.builder()
                .addLiveness(HealthChecks.healthChecks())
                .build();
        UpscaleService upscaleService = new UpscaleService(config);
        return Routing.builder()
                .register(health)
                .register(metrics)
                .register("/upscale", upscaleService)
                .build();
    }
}
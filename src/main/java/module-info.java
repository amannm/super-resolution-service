module systems.cauldron.service.superresolution {
    requires static lombok;
    requires com.microsoft.onnxruntime;
    requires io.helidon.webserver;
    requires io.helidon.metrics;
    requires io.helidon.health.checks;
    requires io.helidon.health;
    requires io.helidon.media.jsonp;
    requires org.apache.logging.log4j;
    exports systems.cauldron.service.superresolution;
}
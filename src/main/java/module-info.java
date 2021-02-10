module systems.cauldron.service.superresolution {
    requires com.microsoft.onnxruntime;
    requires java.desktop;
    requires org.apache.logging.log4j;
    requires static lombok;
    requires io.helidon.webserver;
    requires io.helidon.metrics;
    requires io.helidon.health.checks;
    requires io.helidon.health;
    requires io.helidon.media.jsonp;
    exports systems.cauldron.service.superresolution;
}
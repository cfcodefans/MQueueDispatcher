import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.junit.Test;

import java.util.Date;

/**
 * Created by fan on 2016/11/23.
 */
public class Log4j2Tests {
    @Test
    public void testLogger() {
        Logger log = LogManager.getLogger(Log4j2Tests.class);

        log.info("info");
        log.info("info: {}", new Date());
    }

    @Test
    public void testLoggerContext() {
        LoggerContext context = (LoggerContext) LogManager.getContext();
        Configuration cfg = context.getConfiguration();
        ConfigurationSource cfgSrc = cfg.getConfigurationSource();

        System.out.println(cfgSrc);
        System.out.println(cfg.getAppenders());
    }
}

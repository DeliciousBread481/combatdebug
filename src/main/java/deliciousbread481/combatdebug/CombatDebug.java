package deliciousbread481.combatdebug;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.FileAppender;
import org.apache.logging.log4j.core.config.AppenderRef;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.layout.PatternLayout;

@Mod(modid = CombatDebug.MODID, name = CombatDebug.NAME, version = CombatDebug.VERSION, acceptableRemoteVersions = "*")
public class CombatDebug {

    public static final String MODID   = "combatdebug";
    public static final String NAME    = "CombatDebug";
    public static final String VERSION = "1.1.0";

    public static Logger logger;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        logger = setupFileLogger();
        logger.info("[CombatDebug] loaded. Logging to logs/combatdebug.log");
    }

    private static Logger setupFileLogger() {
        final LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
        final Configuration config = ctx.getConfiguration();

        PatternLayout layout = PatternLayout.newBuilder()
                .withConfiguration(config)
                .withPattern("[%d{HH:mm:ss}] [%t/%level]: %msg%n")
                .build();

        FileAppender appender = FileAppender.newBuilder()
                .withName("CombatDebugFile")
                .withFileName("logs/combatdebug.log")
                .withAppend(false)
                .withLayout(layout)
                .setConfiguration(config)
                .build();
        appender.start();
        config.addAppender(appender);

        AppenderRef ref = AppenderRef.createAppenderRef("CombatDebugFile", null, null);
        AppenderRef[] refs = new AppenderRef[] { ref };

        LoggerConfig loggerConfig = LoggerConfig.createLogger(
                false, Level.ALL, "combatdebug", "true", refs, null, config, null);
        loggerConfig.addAppender(appender, null, null);
        config.addLogger("combatdebug", loggerConfig);

        ctx.updateLoggers();
        return LogManager.getLogger("combatdebug");
    }

    @Mod.EventHandler
    public void serverStarting(FMLServerStartingEvent event) {
        event.registerServerCommand(new PipelineDumpCommand());
        logger.info("[CombatDebug] registered command: /combatdebug pipeline");
    }
}
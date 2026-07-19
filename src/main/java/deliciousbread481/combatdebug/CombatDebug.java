package deliciousbread481.combatdebug;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.RollingFileAppender;
import org.apache.logging.log4j.core.appender.rolling.DefaultRolloverStrategy;
import org.apache.logging.log4j.core.appender.rolling.SizeBasedTriggeringPolicy;
import org.apache.logging.log4j.core.config.AppenderRef;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.layout.PatternLayout;

@Mod(modid = "combatdebug", name = "CombatDebug", version = "1.2.0", acceptableRemoteVersions = "*")
public class CombatDebug {

    public static Logger logger;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        logger = setupFileLogger();
        logger.warn("CombatDebug 启动，日志写入 logs/combatdebug-1.log（单文件 50MB，超出后新建 combatdebug-X.log）");
    }

    @Mod.EventHandler
    public void serverStarting(FMLServerStartingEvent event) {
        event.registerServerCommand(new PipelineDumpCommand());
    }

    private static Logger setupFileLogger() {
        LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
        Configuration config = ctx.getConfiguration();

        PatternLayout layout = PatternLayout.newBuilder()
                .withPattern("[%d{HH:mm:ss}] [%t/%level]: %msg%n")
                .withConfiguration(config)
                .build();

        SizeBasedTriggeringPolicy policy = SizeBasedTriggeringPolicy.createPolicy("50 MB");
        DefaultRolloverStrategy strategy = DefaultRolloverStrategy.createStrategy(
                "2147483647",
                "2",
                "nomax",
                null,
                null,
                false,
                config);

        RollingFileAppender appender = RollingFileAppender.newBuilder()
                .withName("CombatDebugFile")
                .withFileName("logs/combatdebug-1.log")
                .withFilePattern("logs/combatdebug-%i.log")
                .withPolicy(policy)
                .withStrategy(strategy)
                .withLayout(layout)
                .withConfiguration(config)
                .build();
        appender.start();
        config.addAppender(appender);

        AppenderRef ref = AppenderRef.createAppenderRef("CombatDebugFile", null, null);
        AppenderRef[] refs = new AppenderRef[]{ref};
        LoggerConfig loggerConfig = LoggerConfig.createLogger(
                false, Level.ALL, "combatdebug", "true", refs, null, config, null);
        loggerConfig.addAppender(appender, null, null);
        config.addLogger("combatdebug", loggerConfig);

        ctx.updateLoggers();
        return LogManager.getLogger("combatdebug");
    }
}
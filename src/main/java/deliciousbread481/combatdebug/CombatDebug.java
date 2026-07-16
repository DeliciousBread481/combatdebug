package deliciousbread481.combatdebug;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import org.apache.logging.log4j.Logger;

@Mod(
        modid = CombatDebug.MODID,
        name = CombatDebug.NAME,
        version = CombatDebug.VERSION,
        acceptableRemoteVersions = "*"
)
public class CombatDebug {

    public static final String MODID   = "combatdebug";
    public static final String NAME    = "CombatDebug";
    public static final String VERSION = "1.0.0";

    public static Logger logger;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        logger = event.getModLog();
        logger.info("[CombatDebug] loaded. Listening to attack/damage events.");
    }

    @Mod.EventHandler
    public void serverStarting(FMLServerStartingEvent event) {
        event.registerServerCommand(new PipelineDumpCommand());
        logger.info("[CombatDebug] registered command: /combatdebug pipeline");
    }
}

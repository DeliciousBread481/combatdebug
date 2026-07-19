package deliciousbread481.combatdebug;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelPipeline;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.network.NetworkManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentString;

import java.lang.reflect.Method;
import java.util.Map;

public class PipelineDumpCommand extends CommandBase {

    @Override
    public String getName() {
        return "combatdebug";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/combatdebug pipeline";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 2;
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (args.length < 1 || !"pipeline".equalsIgnoreCase(args[0])) {
            throw new CommandException("用法: /combatdebug pipeline");
        }

        EntityPlayerMP player = getCommandSenderAsPlayer(sender);

        try {
            NetHandlerPlayServer connection = player.connection;
            if (connection == null) {
                throw new CommandException("该玩家没有可用的网络连接(connection == null)");
            }

            NetworkManager netManager = connection.netManager;
            if (netManager == null) {
                throw new CommandException("NetworkManager 为 null");
            }

            Channel channel;
            try {
                Method channelMethod = NetworkManager.class.getMethod("channel");
                channel = (Channel) channelMethod.invoke(netManager);
            } catch (NoSuchMethodException e) {
                throw new CommandException("当前 NetworkManager 没有 channel() 方法(可能不是 Cleanroom 运行环境)");
            }

            if (channel == null) {
                throw new CombatDebugException("netty Channel 为 null");
            }

            ChannelPipeline pipeline = channel.pipeline();

            CombatDebug.logger.warn("========== [CombatDebug] netty pipeline dump for player {} ==========",
                    player.getName());
            sender.sendMessage(new TextComponentString(
                    "[CombatDebug] pipeline handlers (详见服务端日志):"));

            int i = 0;
            for (Map.Entry<String, ChannelHandler> entry : pipeline.toMap().entrySet()) {
                String name = entry.getKey();
                ChannelHandler handler = entry.getValue();
                String clazz = handler == null ? "null" : handler.getClass().getName();

                CombatDebug.logger.warn("[CombatDebug] pipeline[{}] name={} class={}", i, name, clazz);
                sender.sendMessage(new TextComponentString(
                        String.format("  [%d] %s -> %s", i, name, clazz)));
                i++;
            }

            CombatDebug.logger.warn("========== [CombatDebug] pipeline dump end ({} handlers) ==========", i);

        } catch (CommandException ce) {
            throw ce;
        } catch (Exception e) {
            CombatDebug.logger.error("[CombatDebug] dump pipeline 失败", e);
            throw new CommandException("dump pipeline 失败: " + e);
        }
    }

    private static class CombatDebugException extends CommandException {
        CombatDebugException(String msg) { super(msg); }
    }
}
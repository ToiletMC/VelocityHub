package de.fruxz.vhub.command

import com.mojang.brigadier.Command
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.builder.RequiredArgumentBuilder
import com.velocitypowered.api.command.BrigadierCommand
import com.velocitypowered.api.command.CommandSource
import com.velocitypowered.api.proxy.Player
import com.velocitypowered.api.proxy.ProxyServer
import de.fruxz.vhub.VelocityHub
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import kotlin.jvm.optionals.getOrNull

object HubCommand {

    private fun sendToHub(target: Player) {
        val hubServer = VelocityHub.lobby

        if (hubServer != null) {

            if (target.currentServer.getOrNull()?.server != hubServer) {

                target.sendMessage(
                    Component.text("正在连接中心服务器...")
                        .color(NamedTextColor.GRAY)
                )
                target.createConnectionRequest(hubServer).fireAndForget()

            } else {
                target.sendMessage(
                    Component.text("你已连接到中心服务器。")
                        .color(NamedTextColor.RED)
                )
            }

        } else {
            target.sendMessage(
                Component.text("中心服务器目前不可用。")
                    .color(NamedTextColor.RED)
                    .hoverEvent(
                        Component.text("当前配置的所有服务器均无法访问！")
                            .color(NamedTextColor.GRAY)
                    )
            )
        }


    }

    fun create(proxy: ProxyServer): BrigadierCommand {
        val node = LiteralArgumentBuilder
            .literal<CommandSource>("hub")
            .requires { it.hasPermission("vhub.command.hub") }
            .executes { context ->
                val source = context.source

                if (source is Player) {
                    sendToHub(source)
                } else {
                    source.sendMessage(
                        Component.text("你必须是一名玩家才能使用此命令。")
                            .color(NamedTextColor.RED)
                    )
                }

                return@executes Command.SINGLE_SUCCESS
            }
            .then(
                RequiredArgumentBuilder.argument<CommandSource?, String>("player", StringArgumentType.word())
                    .suggests { _, builder ->
                        proxy.allPlayers.forEach { player ->
                            builder.suggest(
                                player.username,
                            )
                        }
                        return@suggests builder.buildFuture()
                    }
                    .requires { it.hasPermission("vhub.command.hub.others") }
                    .executes { context ->
                        val source = context.source
                        val target = context.getArgument("player", String::class.java)
                        val targetPlayer = proxy.getPlayer(target).getOrNull()

                        if (targetPlayer != null) {
                            sendToHub(targetPlayer)
                            source.sendMessage(
                                Component.text("你将 ")
                                    .color(NamedTextColor.GRAY)
                                    .append(
                                        Component.text(targetPlayer.username)
                                            .color(NamedTextColor.YELLOW)
                                    )
                                    .append(
                                        Component.text(" 发送到了中心服务器。")
                                            .color(NamedTextColor.GRAY)
                                    )
                            )
                        } else {
                            source.sendMessage(
                                Component.text("玩家 ")
                                    .color(NamedTextColor.RED)
                                    .append(
                                        Component.text(target)
                                            .color(NamedTextColor.YELLOW)
                                    )
                                    .append(
                                        Component.text(" 当前不在线。")
                                            .color(NamedTextColor.RED)
                                    )
                            )
                        }

                        return@executes Command.SINGLE_SUCCESS
                    }
            ).build()

        return BrigadierCommand(node)
    }
}
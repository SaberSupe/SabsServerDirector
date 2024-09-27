package saber.sabsServerDirector;

import com.google.inject.Inject;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.player.PlayerChooseInitialServerEvent;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.plugin.Dependency;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.cacheddata.CachedMetaData;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.NodeType;
import net.luckperms.api.node.types.MetaNode;
import org.slf4j.Logger;

import java.util.Optional;

@Plugin(
        id = "sabsserverdirector",
        name = "SabsServerDirector",
        version = "1.0",
        description = "relogs players back onto the server they left from",
        authors = {"Saber"},
        dependencies = {
                @Dependency(id = "luckperms")
        }
)
public class SabsServerDirector {

    @Inject
    private Logger logger;

    @Inject
    private ProxyServer server;

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
    }

    @Subscribe
    public void onPlayerServerConnect(ServerConnectedEvent event){
        if (server.getPluginManager().getPlugin("luckperms").isEmpty()) return;
        LuckPerms luckPerms = LuckPermsProvider.get();
        // obtain a User instance (by any means! see above for other ways)
        User user = luckPerms.getPlayerAdapter(Player.class).getUser(event.getPlayer());

        // create a new MetaNode holding the level value
        // of course, this can have context/expiry/etc too!
        MetaNode node = MetaNode.builder("lastserver", event.getServer().getServerInfo().getName()).build();

        // clear any existing meta nodes with the same key - we want to override
        user.data().clear(NodeType.META.predicate(mn -> mn.getMetaKey().equals("lastserver")));
        // add the new node
        user.data().add(node);

        // save!
        luckPerms.getUserManager().saveUser(user);
    }

    @Subscribe
    public void onPlayerConnect(PlayerChooseInitialServerEvent event) {
        if (server.getPluginManager().getPlugin("luckperms").isEmpty()) return;
        LuckPerms luckPerms = LuckPermsProvider.get();

        CachedMetaData metaData = luckPerms.getPlayerAdapter(Player.class).getMetaData(event.getPlayer());

        String serverName = metaData.getMetaValue("lastserver");
        if (serverName == null) return;
        Optional<RegisteredServer> optServer = server.getServer(serverName);
        if (optServer.isEmpty()) return;

        event.setInitialServer(optServer.get());
    }
}

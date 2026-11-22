package xyz.jvkeh.chatbinds;

import com.google.gson.*;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;

public class ChatBinds implements ModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("chat-binds");
    protected static KeyMapping.Category USERBINDS = KeyMapping.Category.register(Identifier.fromNamespaceAndPath("chatbinds", "user"));
    static ArrayList<ChatBind> Binds = new ArrayList<>();
    static Path configDir = FabricLoader.getInstance().getConfigDir().resolve("chatbinds");
    static Path configFileBinds = configDir.resolve("binds.json");

    public static void sendMessage(String msg) {
        assert Minecraft.getInstance().player != null;
        if (msg.startsWith("/")) {
            Minecraft.getInstance().player.connection.sendChat(msg.substring(1));
        } else
            Minecraft.getInstance().player.connection.sendChat(msg);
    }

    public static void registerCommand(String cmd, String title) {
        ChatBind b = new ChatBind();
        b.cmd = cmd;
        b.title = title;
        b.bind = new KeyMapping(title,
                InputConstants.Type.KEYBOARD, InputConstants.UNKNOWN.getValue(), USERBINDS);


        Minecraft mc = Minecraft.getInstance();
        ArrayList<KeyMapping> keys = new ArrayList<>(Arrays.asList(mc.options.keyMappings));
        keys.add(b.bind);
        mc.options.keyMappings = keys.toArray(new KeyMapping[0]);

        Binds.add(b);
    }

    public static void removeCommand(ChatBind bind) {
        Binds.remove(bind);

        Minecraft mc = Minecraft.getInstance();
        ArrayList<KeyMapping> keys = new ArrayList<>(Arrays.asList(mc.options.keyMappings));
        keys.remove(bind.bind);
        mc.options.keyMappings = keys.toArray(new KeyMapping[0]);
    }

    static void loadConfig() {
        Binds = new ArrayList<>();

        try {
            Files.createDirectories(configDir);
            if (!Files.exists(configFileBinds))
                return;

            String str = Files.readString(configFileBinds);
            JsonArray ja = (JsonArray) JsonParser.parseString(str);

            for (JsonElement je : ja)
                if (je instanceof JsonObject jo)
                    registerCommand(jo.get("cmd").getAsString(), jo.get("title").getAsString());
        } catch (Exception e) {
            LOGGER.error("Failed to load config", e);
        }
    }

    static void saveConfig() {
        JsonArray ja = new JsonArray();

        for (ChatBind b : Binds) {
            JsonObject jo = new JsonObject();
            jo.add("cmd", new JsonPrimitive(b.cmd));
            jo.add("title", new JsonPrimitive(b.title));

            ja.add(jo);
        }

        String json = new Gson().toJson(ja);

        try {
            Files.createDirectories(configDir);
            Files.writeString(configFileBinds, json);
        } catch (Exception e) {
            LOGGER.error("Failed to save config", e);
        }
    }

    @Override
    public void onInitialize() {
        final KeyMapping.Category CHATBINDS = KeyMapping.Category.register(Identifier.fromNamespaceAndPath("chatbinds", "chatbinds"));
        KeyMapping menuBind = new KeyMapping("chatbinds.key.menu",
                InputConstants.Type.KEYBOARD,
                InputConstants.UNKNOWN.getValue(),
                CHATBINDS);
        KeyMappingHelper.registerKeyMapping(menuBind);
        KeyMapping addChatBind = new KeyMapping("chatbinds.key.add_chat",
                InputConstants.Type.KEYBOARD,
                InputConstants.UNKNOWN.getValue(),
                CHATBINDS);
        KeyMappingHelper.registerKeyMapping(addChatBind);

        ClientTickEvents.START_CLIENT_TICK.register(client -> {
            if (menuBind.consumeClick())
                client.gui.setScreen(new BindMenuScreen(null));
            if (addChatBind.consumeClick())
                client.gui.setScreen(new AddChatScreen("", null));

            for (ChatBind b : Binds)
                if (b.bind.consumeClick())
                    sendMessage(b.cmd);
        });
        loadConfig();
    }

    static class ChatBind {
        public KeyMapping bind;
        public String cmd;
        public String title;
    }
}
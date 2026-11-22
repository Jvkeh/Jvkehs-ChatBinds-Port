package xyz.jvkeh.chatbinds;

import com.mojang.blaze3d.platform.InputConstants;
import com.terraformersmc.modmenu.api.ModMenuApi;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.NonNull;
import xyz.jvkeh.chatbinds.ChatBinds.ChatBind;

public class BindMenuScreen extends Screen implements ModMenuApi {
    public BindList.Entry selectedEntry;
    Screen parent;
    BindList list;
    Button doneButton;
    Button reloadButton;
    Button addButton;
    Button removeButton;

    public BindMenuScreen(Screen parent) {
        super(Component.empty());
        this.parent = parent;
    }

    @Override
    protected void init() {
        this.clearWidgets();
        int sideBarW = 128;
        int sideBarX = width - 128;
        int padding = 8;
        int buttonHeight = 32;

        list = new BindList(this, width - sideBarW, height);
        list.updateEntries();
        addRenderableWidget(list);

        doneButton = Button.builder(Component.translatable("chatbinds.menuScreen.done"), this::onClose)
                .bounds(sideBarX + padding, height - buttonHeight - padding, sideBarW - (padding * 2), buttonHeight)
                .build();

        addRenderableWidget(doneButton);

        reloadButton = Button.builder(Component.translatable("chatbinds.menuScreen.reload"), this::onReload)
                .bounds(doneButton.getX(), doneButton.getY() - buttonHeight - padding, doneButton.getWidth(), buttonHeight)
                .build();
        addRenderableWidget(reloadButton);

        addButton = Button.builder(Component.translatable("chatbinds.menuScreen.new"), this::onAdd)
                .bounds(reloadButton.getX(), reloadButton.getY() - buttonHeight, reloadButton.getWidth(), buttonHeight)
                .build();
        addRenderableWidget(addButton);

        removeButton = Button.builder(Component.translatable("chatbinds.menuScreen.remove"), this::onRemove)
                .bounds(addButton.getX(), addButton.getY() - buttonHeight - padding, addButton.getWidth(), buttonHeight)
                .build();
        addRenderableWidget(removeButton);
    }

    void onAdd(Button w) {
        this.minecraft.gui.setScreen(new AddChatScreen("", this));
    }

    void onRemove(Button w) {
        BindList.Entry e = list.getSelected();
        if (e != null) {
            ChatBinds.removeCommand(e.bind);
            ChatBinds.saveConfig();
            list.updateEntries();
        }
    }

    void onReload(Button w) {
        ChatBinds.loadConfig();
        init();
    }

    void onClose(Button w) {
        onClose();
    }

    @Override
    public void onClose() {
        this.minecraft.gui.setScreen(parent);
    }

    @Override
    public boolean keyPressed(@NonNull KeyEvent event) {
        if (selectedEntry != null) {
            if (event.isEscape()) {
                selectedEntry.bind.bind.setKey(InputConstants.UNKNOWN);
            } else {
                selectedEntry.bind.bind.setKey(InputConstants.getKey(event));
            }
            KeyMapping.resetMapping();
            selectedEntry.update();
            selectedEntry = null;
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean mouseClicked(@NonNull MouseButtonEvent event, boolean doubleClick) {
        if (selectedEntry != null) {
            selectedEntry.bind.bind.setKey(InputConstants.Type.MOUSE.getOrCreate(event.button()));
            KeyMapping.resetMapping();
            selectedEntry.update();
            selectedEntry = null;
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    static class BindList extends ObjectSelectionList<BindList.Entry> {
        public BindMenuScreen parent;

        public BindList(BindMenuScreen parent, int w, int h) {
            super(parent.minecraft, w, h, 0, 32);
            this.parent = parent;
            updateEntries();
        }

        public void updateEntries() {
            this.clearEntries();
            for (ChatBind b : ChatBinds.Binds) {
                Entry e = new Entry(minecraft, b, this);
                addEntry(e);
                setSelected(e);
            }
        }

        static class Entry extends ObjectSelectionList.Entry<Entry> implements GuiEventListener {
            public ChatBind bind;
            Minecraft client;
            BindList parent;
            Button keyButton;

            Entry(Minecraft client, ChatBind b, BindList parent) {
                super();
                this.client = client;
                this.bind = b;
                this.parent = parent;
                keyButton = Button.builder(Component.empty(), this::onButton).build();
                update();
            }

            public void update() {
                keyButton.setMessage(bind.bind.getTranslatedKeyMessage());
            }

            void onButton(Button b) {
                parent.parent.selectedEntry = this;
                keyButton.setMessage(Component.literal("..."));
            }

            @Override
            public @NonNull Component getNarration() {
                return Component.empty();
            }

            @Override
            public boolean mouseClicked(@NonNull MouseButtonEvent event, boolean doubleClick) {
                parent.setSelected(this);
                keyButton.mouseClicked(event, doubleClick);
                return true;
            }

            @Override
            public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float a) {
                graphics.text(client.font, Component.literal(bind.title), getX() + 8, getY() + 6, 0xFFFFFFFF);
                graphics.text(client.font, Component.literal(bind.cmd), getX() + 8, getY() + 18, 0xFF888888);

                keyButton.setX(getX() + this.getContentWidth() - 64 - 4);
                keyButton.setY(getY() + 2);
                keyButton.setWidth(64);
                keyButton.setHeight(this.getContentHeight());
                keyButton.extractRenderState(graphics, mouseX, mouseY, a);
            }
        }
    }
}

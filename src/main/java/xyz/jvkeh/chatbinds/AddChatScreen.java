package xyz.jvkeh.chatbinds;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.NonNull;

public class AddChatScreen extends Screen {
    EditBox titleBox;
    EditBox commandBox;

    Button keyButton;
    Button addButton;
    Button cancelButton;

    Screen parent;

    String cmd;
    InputConstants.Key key = InputConstants.UNKNOWN;
    boolean listening;

    public AddChatScreen(String cmd_, Screen parent) {
        super(Component.empty());
        cmd = cmd_;
        this.parent = parent;
    }

    @Override
    protected void init() {
        StringWidget titleText = new StringWidget(Component.translatable("chatbinds.addScreen.title"), minecraft.font);
        titleText.setX(width / 4);
        titleText.setY((height / 2) - 64);
        addRenderableOnly(titleText);

        titleBox = new EditBox(minecraft.font, width / 2, 24, Component.empty());
        titleBox.setPosition(width / 2 - (titleBox.getWidth() / 2), titleText.getY() + titleText.getHeight());
        addRenderableWidget(titleBox);

        StringWidget commandText = new StringWidget(Component.translatable("chatbinds.addScreen.command"), minecraft.font);
        commandText.setX(width / 4);
        commandText.setY(titleBox.getY() + titleBox.getHeight() + 4);
        addRenderableOnly(commandText);

        commandBox = new EditBox(minecraft.font, width / 2, 24, Component.empty());
        titleBox.setResponder(this::onCommandChanged);
        commandBox.setPosition(width / 2 - (commandBox.getWidth() / 2), commandText.getY() + commandText.getHeight());
        addRenderableWidget(commandBox);
        this.setInitialFocus(commandBox);

        int buttonWidth = 64;
        int buttonHeight = 16;

        keyButton = Button.builder(Component.empty(), this::onListenButton)
                .bounds(commandBox.getX() + commandBox.getWidth() - buttonWidth,
                        commandBox.getY() + commandBox.getHeight() + 6,
                        buttonWidth,
                        buttonHeight)
                .build();
        addRenderableWidget(keyButton);
        updateKeyText();

        StringWidget keyText = new StringWidget(Component.translatable("chatbinds.addScreen.keybind"), minecraft.font);
        keyText.setX(keyButton.getX() - keyText.getWidth() - 2);
        keyText.setY(keyButton.getY() + (keyButton.getHeight() / 2) - (keyText.getHeight() / 2));
        addRenderableOnly(keyText);

        addButton = Button.builder(Component.translatable("chatbinds.addScreen.add"), this::onAdd)
                .bounds(keyButton.getX(),
                        keyButton.getY() + keyButton.getHeight() + 6,
                        buttonWidth,
                        buttonHeight)
                .build();
        addRenderableWidget(addButton);

        cancelButton = Button.builder(Component.translatable("chatbinds.addScreen.cancel"), this::onCancel)
                .bounds(addButton.getX() - buttonWidth - 2,
                        addButton.getY(),
                        buttonWidth,
                        buttonHeight)
                .build();
        addRenderableWidget(cancelButton);
    }

    void onListenButton(Button b) {
        listening = true;
        updateKeyText();
    }

    public boolean keyPressed(@NonNull KeyEvent event) {
        if (listening) {
            if (event.isEscape()) {
                key = InputConstants.UNKNOWN;
            } else {
                key = InputConstants.getKey(event);
            }
            listening = false;
            updateKeyText();
            return true;
        }

        if (event.key() == InputConstants.KEY_RETURN || event.key() == InputConstants.KEY_NUMPADENTER) {
            add();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean mouseClicked(@NonNull MouseButtonEvent event, boolean doubleClick) {
        if (listening) {
            key = InputConstants.Type.MOUSE.getOrCreate(event.button());
            listening = false;
            updateKeyText();
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    void updateKeyText() {
        if (listening)
            keyButton.setMessage(Component.literal("..."));
        else
            keyButton.setMessage(key.getDisplayName());
    }

    void onCommandChanged(String c) {
        cmd = c;
    }

    void onAdd(Button b) {
        add();
    }

    void onCancel(Button b) {
        onClose();
    }

    void add() {
        String cmd = commandBox.getValue().strip();
        String title = titleBox.getValue().strip();

        if (cmd.isEmpty())
            return;
        if (title.isEmpty())
            title = cmd;

        ChatBinds.registerCommand(cmd, title);
        ChatBinds.saveConfig();
        onClose();
    }

    @Override
    public void onClose() {
        minecraft.gui.setScreen(parent);
    }
}

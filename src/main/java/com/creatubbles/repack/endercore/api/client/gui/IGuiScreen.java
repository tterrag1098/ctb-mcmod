package com.creatubbles.repack.endercore.api.client.gui;

import java.io.IOException;
import java.util.List;

import net.minecraft.client.gui.GuiButton;

import com.creatubbles.repack.endercore.client.gui.widget.GhostSlot;
import com.creatubbles.repack.endercore.client.gui.widget.GuiToolTip;

public interface IGuiScreen {

    void addToolTip(GuiToolTip toolTip);

    boolean removeToolTip(GuiToolTip toolTip);

    void clearToolTips();

    int getGuiLeft();

    int getGuiTop();

    int getXSize();

    int getYSize();

    void addButton(GuiButton button);

    void removeButton(GuiButton button);

    int getOverlayOffsetX();

    void doActionPerformed(GuiButton but) throws IOException;

    List<GhostSlot> getGhostSlots();

}

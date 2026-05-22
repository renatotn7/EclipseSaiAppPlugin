package com.mcp.sailibrary.plugin.chat.blocks.views;

import org.eclipse.swt.SWT;
import org.eclipse.ui.part.ViewPart;

public class NamedBlocksView extends ViewPart {

    public static final String ID = "com.mcp.sailibrary.plugin.chat.blocks.views.NamedBlocksView";

    private NamedBlocksPanel panel;

    @Override
    public void createPartControl(org.eclipse.swt.widgets.Composite parent) {
        panel = new NamedBlocksPanel(parent, SWT.NONE);
    }

    @Override
    public void setFocus() {
        if (panel != null && !panel.isDisposed()) {
            panel.setFocus();
        }
    }
}
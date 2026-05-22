package com.mcp.sailibrary.plugin.chat.context.decorators;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;

import com.mcp.sailibrary.plugin.Activator;

/* yaml_header: version: "1.1" purpose: "Escalar e cachear overlays do explorer a partir dos assets @2x." libraries: - org.eclipse.jface.resource.ImageDescriptor: runtime - org.eclipse.swt.graphics.Image: runtime - org.eclipse.swt.graphics.ImageData: runtime */
public class ExplorerOverlayIconService {

    private static final ExplorerOverlayIconService INSTANCE = new ExplorerOverlayIconService();

    private static final int OVERLAY_ICON_SIZE = 12;

    private final Map<String, ImageDescriptor> descriptorCache = new HashMap<String, ImageDescriptor>();

    private ExplorerOverlayIconService() {
    }

    public static ExplorerOverlayIconService getInstance() {
        return INSTANCE;
    }

    /* * Feature * Data: 2026-05-20 00:00:00 * Caller: NamedStructuralContextDecorator * Callee: ImageDescriptor.createFromImageData * Objetivo: Devolver ImageDescriptor escalado e cacheado para overlays do explorer. */
    public synchronized ImageDescriptor getOverlayDescriptor(String pluginRelativePath) {
        if (pluginRelativePath == null || pluginRelativePath.trim().length() == 0) {
            return null;
        }

        String cacheKey = pluginRelativePath + "|" + OVERLAY_ICON_SIZE;
        if (descriptorCache.containsKey(cacheKey)) {
            return descriptorCache.get(cacheKey);
        }

        Image baseImage = null;
        try {
            ImageDescriptor baseDescriptor = org.eclipse.ui.plugin.AbstractUIPlugin.imageDescriptorFromPlugin(
                    Activator.PLUGIN_ID,
                    pluginRelativePath
            );

            if (baseDescriptor == null) {
                return null;
            }

            baseImage = baseDescriptor.createImage();
            if (baseImage == null) {
                return null;
            }

            ImageData baseData = baseImage.getImageData();
            ImageData scaledData = baseData.scaledTo(OVERLAY_ICON_SIZE, OVERLAY_ICON_SIZE);

            ImageDescriptor scaledDescriptor = ImageDescriptor.createFromImageData(scaledData);
            descriptorCache.put(cacheKey, scaledDescriptor);

            return scaledDescriptor;
        } catch (Exception e) {
            return null;
        } finally {
            if (baseImage != null && !baseImage.isDisposed()) {
                baseImage.dispose();
            }
        }
    }

    public synchronized void clearCache() {
        descriptorCache.clear();
    }
}
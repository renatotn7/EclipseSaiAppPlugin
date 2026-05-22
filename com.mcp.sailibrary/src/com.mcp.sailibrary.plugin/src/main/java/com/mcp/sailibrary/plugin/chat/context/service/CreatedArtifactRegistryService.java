package com.mcp.sailibrary.plugin.chat.context.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Properties;

/* yaml_header: version: "1.0" purpose: "Registrar arquivos criados pela propria ferramenta para permitir edicao e exclusao segura." libraries: - java.io.File: runtime - java.util.Properties: runtime */
public class CreatedArtifactRegistryService {

    private final File rootDirectory;
    private final File registryFile;

    public CreatedArtifactRegistryService(File rootDirectory) {
        this.rootDirectory = rootDirectory;
        File saiDirectory = new File(rootDirectory, ".sai");
        this.registryFile = new File(saiDirectory, "created-artifacts.properties");
    }

    public synchronized void registerCreatedFile(File file) {
        if (file == null) {
            return;
        }

        Properties properties = loadProperties();
        properties.setProperty(normalizePath(file), String.valueOf(System.currentTimeMillis()));
        saveProperties(properties);
    }

    public synchronized boolean isCreatedFile(File file) {
        if (file == null) {
            return false;
        }

        Properties properties = loadProperties();
        return properties.containsKey(normalizePath(file));
    }

    public synchronized void unregisterCreatedFile(File file) {
        if (file == null) {
            return;
        }

        Properties properties = loadProperties();
        properties.remove(normalizePath(file));
        saveProperties(properties);
    }

    private Properties loadProperties() {
        Properties properties = new Properties();

        if (!registryFile.exists()) {
            return properties;
        }

        FileInputStream input = null;
        try {
            input = new FileInputStream(registryFile);
            properties.load(input);
        } catch (Exception e) {
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (Exception e) {
                }
            }
        }

        return properties;
    }

    private void saveProperties(Properties properties) {
        File parent = registryFile.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }

        FileOutputStream output = null;
        try {
            output = new FileOutputStream(registryFile);
            properties.store(output, "created by sai library plugin");
        } catch (Exception e) {
        } finally {
            if (output != null) {
                try {
                    output.close();
                } catch (Exception e) {
                }
            }
        }
    }

    private String normalizePath(File file) {
        try {
            return file.getCanonicalPath().replace("\\", "/");
        } catch (Exception e) {
            return file.getAbsolutePath().replace("\\", "/");
        }
    }
}
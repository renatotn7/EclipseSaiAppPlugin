package com.mcp.sailibrary.plugin.chat.blocks.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.mcp.sailibrary.plugin.chat.blocks.model.NamedBlockKind;
import com.mcp.sailibrary.plugin.chat.blocks.model.NamedCodeBlock;

/* yaml_header: version: "1.1" purpose: "Gerenciar a sessao viva dos blocos nomeados textuais, incluindo foco principal, escopo editavel e escopo referencial." libraries: - java.util.List: runtime - java.util.Collections: runtime - java.util.Comparator: runtime */
public class NamedBlockSessionService {

    private static final NamedBlockSessionService INSTANCE = new NamedBlockSessionService();

    private static final int MAX_BLOCKS_PER_SESSION = 20;
    private static final int MAX_PREVIEW_LENGTH = 120;

    private final List<NamedCodeBlock> blocks = new ArrayList<NamedCodeBlock>();

    private NamedBlockSessionService() {
    }

    public static NamedBlockSessionService getInstance() {
        return INSTANCE;
    }

    /* * Feature * Data: 2026-05-20 00:00:00 * Caller: NamedBlocksController * Callee: validateBlockInput, clearPrimaryBlock, removeExactMatch, sortInternal * Objetivo: Registrar um novo bloco textual na sessao preservando integridade e unicidade. */
    public synchronized NamedCodeBlock addBlock(String name, String selectedText, NamedBlockKind kind, String filePath, String fileName, int offset, int length, int startLine, int endLine) {

        validateBlockInput(name, kind, filePath, fileName, offset, length, startLine, endLine);

        if (blocks.size() >= MAX_BLOCKS_PER_SESSION) {
            throw new IllegalStateException("Limite maximo de blocos da sessao atingido.");
        }

        if (existsName(name)) {
            throw new IllegalArgumentException("Ja existe um bloco com este nome: " + name);
        }

        if (kind == NamedBlockKind.PRIMARY) {
            clearPrimaryBlock();
        }

        removeExactMatch(filePath, offset, length);

        if (hasOverlappingBlock(filePath, offset, length)) {
            throw new IllegalArgumentException("A selecao informada se sobrepoe a um bloco existente no mesmo arquivo.");
        }

        NamedCodeBlock block = new NamedCodeBlock();
        block.setName(sanitizeName(name));
        block.setKind(kind);
        block.setFilePath(normalizePath(filePath));
        block.setFileName(safeTrim(fileName));
        block.setOffset(offset);
        block.setLength(length);
        block.setStartLine(startLine);
        block.setEndLine(endLine);
        block.setPreview(buildPreview(selectedText));
        block.setOriginalTextSnapshot(selectedText);
        block.setCreatedAt(System.currentTimeMillis());

        blocks.add(block);
        sortInternal();

        return block;
    }

    public synchronized boolean removeByName(String name) {
        if (isBlank(name)) {
            return false;
        }

        Iterator<NamedCodeBlock> it = blocks.iterator();
        while (it.hasNext()) {
            NamedCodeBlock block = it.next();
            if (safeEquals(block.getName(), name)) {
                it.remove();
                return true;
            }
        }

        return false;
    }

    public synchronized int removeByFilePath(String filePath) {
        if (isBlank(filePath)) {
            return 0;
        }

        String normalized = normalizePath(filePath);
        int removed = 0;

        Iterator<NamedCodeBlock> it = blocks.iterator();
        while (it.hasNext()) {
            NamedCodeBlock block = it.next();
            if (safeEquals(block.getFilePath(), normalized)) {
                it.remove();
                removed++;
            }
        }

        return removed;
    }

    public synchronized void clearAll() {
        blocks.clear();
    }

    public synchronized List<NamedCodeBlock> getAll() {
        return new ArrayList<NamedCodeBlock>(blocks);
    }

    public synchronized List<NamedCodeBlock> getByKind(NamedBlockKind kind) {
        List<NamedCodeBlock> result = new ArrayList<NamedCodeBlock>();
        if (kind == null) {
            return result;
        }

        for (int i = 0; i < blocks.size(); i++) {
            if (blocks.get(i).getKind() == kind) {
                result.add(blocks.get(i));
            }
        }

        return result;
    }

    public synchronized NamedCodeBlock findPrimary() {
        for (int i = 0; i < blocks.size(); i++) {
            if (blocks.get(i).getKind() == NamedBlockKind.PRIMARY) {
                return blocks.get(i);
            }
        }
        return null;
    }

    public synchronized List<NamedCodeBlock> getByFilePath(String filePath) {
        List<NamedCodeBlock> result = new ArrayList<NamedCodeBlock>();
        if (isBlank(filePath)) {
            return result;
        }

        String normalized = normalizePath(filePath);

        for (int i = 0; i < blocks.size(); i++) {
            if (safeEquals(blocks.get(i).getFilePath(), normalized)) {
                result.add(blocks.get(i));
            }
        }

        return result;
    }

    public synchronized NamedCodeBlock findByName(String name) {
        if (isBlank(name)) {
            return null;
        }

        for (int i = 0; i < blocks.size(); i++) {
            if (safeEquals(blocks.get(i).getName(), name)) {
                return blocks.get(i);
            }
        }

        return null;
    }

    public synchronized boolean existsName(String name) {
        return findByName(name) != null;
    }

    public synchronized Set<String> collectNames() {
        Set<String> names = new HashSet<String>();
        for (int i = 0; i < blocks.size(); i++) {
            if (!isBlank(blocks.get(i).getName())) {
                names.add(blocks.get(i).getName());
            }
        }
        return names;
    }

    public synchronized int countByKind(NamedBlockKind kind) {
        int total = 0;
        if (kind == null) {
            return total;
        }

        for (int i = 0; i < blocks.size(); i++) {
            if (blocks.get(i).getKind() == kind) {
                total++;
            }
        }

        return total;
    }

    public synchronized boolean hasOverlappingBlock(String filePath, int offset, int length) {
        if (isBlank(filePath) || offset < 0 || length <= 0) {
            return false;
        }

        String normalized = normalizePath(filePath);
        int newStart = offset;
        int newEnd = offset + length;

        for (int i = 0; i < blocks.size(); i++) {
            NamedCodeBlock block = blocks.get(i);
            if (!safeEquals(block.getFilePath(), normalized)) {
                continue;
            }

            int existingStart = block.getEffectiveOffset();
            int existingEnd = block.getEffectiveEndOffset();

            boolean overlaps = newStart < existingEnd && newEnd > existingStart;
            if (overlaps) {
                return true;
            }
        }

        return false;
    }

    public synchronized boolean renameBlock(String currentName, String newName) {
        if (isBlank(currentName) || isBlank(newName)) {
            return false;
        }

        NamedCodeBlock block = findByName(currentName);
        if (block == null) {
            return false;
        }

        String sanitized = sanitizeName(newName);
        if (isBlank(sanitized)) {
            return false;
        }

        if (!safeEquals(currentName, sanitized) && existsName(sanitized)) {
            return false;
        }

        block.setName(sanitized);
        sortInternal();
        return true;
    }

    public synchronized boolean changeKind(String name, NamedBlockKind newKind) {
        if (isBlank(name) || newKind == null) {
            return false;
        }

        NamedCodeBlock block = findByName(name);
        if (block == null) {
            return false;
        }

        if (newKind == NamedBlockKind.PRIMARY) {
            clearPrimaryBlock();
        }

        block.setKind(newKind);
        sortInternal();
        return true;
    }

    private void clearPrimaryBlock() {
        Iterator<NamedCodeBlock> it = blocks.iterator();
        while (it.hasNext()) {
            NamedCodeBlock block = it.next();
            if (block.getKind() == NamedBlockKind.PRIMARY) {
                it.remove();
            }
        }
    }

    private void removeExactMatch(String filePath, int offset, int length) {
        Iterator<NamedCodeBlock> it = blocks.iterator();
        String normalized = normalizePath(filePath);

        while (it.hasNext()) {
            NamedCodeBlock block = it.next();
            if (safeEquals(block.getFilePath(), normalized)
                    && block.getOffset() == offset
                    && block.getLength() == length) {
                it.remove();
            }
        }
    }

    private void validateBlockInput(String name, NamedBlockKind kind, String filePath, String fileName, int offset, int length, int startLine, int endLine) {

        if (isBlank(name)) {
            throw new IllegalArgumentException("Nome do bloco nao pode ser vazio.");
        }

        if (kind == null) {
            throw new IllegalArgumentException("Tipo do bloco nao pode ser nulo.");
        }

        if (isBlank(filePath)) {
            throw new IllegalArgumentException("Caminho do arquivo nao pode ser vazio.");
        }

        if (isBlank(fileName)) {
            throw new IllegalArgumentException("Nome do arquivo nao pode ser vazio.");
        }

        if (offset < 0) {
            throw new IllegalArgumentException("Offset invalido para o bloco.");
        }

        if (length <= 0) {
            throw new IllegalArgumentException("Comprimento invalido para o bloco.");
        }

        if (startLine <= 0 || endLine <= 0) {
            throw new IllegalArgumentException("Linhas do bloco sao invalidas.");
        }

        if (endLine < startLine) {
            throw new IllegalArgumentException("Faixa de linhas do bloco esta inconsistente.");
        }
    }

    private String buildPreview(String text) {
        if (text == null) {
            return "";
        }

        String value = text.trim();
        value = value.replace("\r", " ");
        value = value.replace("\n", " ");
        value = value.replace("\t", " ");
        value = value.replaceAll("\\s+", " ");
        value = value.replace("\"", "'");
        value = value.trim();

        if (value.length() > MAX_PREVIEW_LENGTH) {
            value = value.substring(0, MAX_PREVIEW_LENGTH) + "...";
        }

        return value;
    }

    private void sortInternal() {
        Collections.sort(blocks, new Comparator<NamedCodeBlock>() {
            @Override
            public int compare(NamedCodeBlock a, NamedCodeBlock b) {
                int kindCompare = Integer.compare(kindWeight(a.getKind()), kindWeight(b.getKind()));
                if (kindCompare != 0) {
                    return kindCompare;
                }

                int fileCompare = safeTrim(a.getFileName()).compareToIgnoreCase(safeTrim(b.getFileName()));
                if (fileCompare != 0) {
                    return fileCompare;
                }

                if (a.getStartLine() != b.getStartLine()) {
                    return Integer.compare(a.getStartLine(), b.getStartLine());
                }

                return safeTrim(a.getName()).compareToIgnoreCase(safeTrim(b.getName()));
            }
        });
    }

    private int kindWeight(NamedBlockKind kind) {
        if (kind == NamedBlockKind.PRIMARY) {
            return 0;
        }
        if (kind == NamedBlockKind.EDITABLE) {
            return 1;
        }
        return 2;
    }

    private String sanitizeName(String name) {
        if (name == null) {
            return "";
        }

        String value = name.trim().toLowerCase();
        value = value.replaceAll("[^a-z0-9_]", "");
        if (value.length() > 12) {
            value = value.substring(0, 12);
        }
        return value;
    }

    private String normalizePath(String path) {
        if (path == null) {
            return "";
        }
        return path.replace("\\", "/").trim();
    }

    private String safeTrim(String value) {
        return value == null ? "" : value.trim();
    }

    private boolean safeEquals(String a, String b) {
        if (a == null) {
            return b == null;
        }
        return a.equals(b);
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().length() == 0;
    }
}
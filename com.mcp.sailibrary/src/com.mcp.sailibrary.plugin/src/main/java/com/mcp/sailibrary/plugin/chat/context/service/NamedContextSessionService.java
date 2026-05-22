package com.mcp.sailibrary.plugin.chat.context.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import com.mcp.sailibrary.plugin.chat.context.model.NamedContextTarget;
import com.mcp.sailibrary.plugin.chat.context.model.NamedContextTargetRole;
import com.mcp.sailibrary.plugin.chat.context.model.NamedContextTargetType;

/* yaml_header: version: "1.0" purpose: "Gerenciar a sessao viva dos alvos contextuais nomeados para a conversa e para o editor." libraries: - java.util.List: runtime - java.util.Collections: runtime */
public class NamedContextSessionService {

    private static final NamedContextSessionService INSTANCE = new NamedContextSessionService();

    private static final int MAX_TARGETS_PER_SESSION = 24;

    private final List<NamedContextTarget> targets = new ArrayList<NamedContextTarget>();

    private NamedContextSessionService() {
    }

    public static NamedContextSessionService getInstance() {
        return INSTANCE;
    }

    /* * Feature * Data: 2026-05-20 00:00:00 * Caller: Controllers de editor e explorer * Callee: validateTargetInput, removeExactMatch, sortInternal * Objetivo: Registrar um novo alvo contextual na sessao preservando integridade e evitando sobreposicao parcial. */
    public synchronized NamedContextTarget addTarget(NamedContextTarget target) {
        validateTargetInput(target);

        if (targets.size() >= MAX_TARGETS_PER_SESSION) {
            throw new IllegalStateException("Limite maximo de alvos da sessao atingido.");
        }

        if (existsName(target.getName())) {
            throw new IllegalArgumentException("Ja existe um alvo com este nome: " + target.getName());
        }

        if (target.getRole() == NamedContextTargetRole.PRIMARY) {
            clearPrimaryRole();
        }

        if (target.getType() == NamedContextTargetType.CODE_BLOCK) {
            removeExactMatch(target.getFilePath(), target.getOffset(), target.getLength(), target.getType());

            if (hasOverlappingCodeTarget(target.getFilePath(), target.getOffset(), target.getLength())) {
                throw new IllegalArgumentException("A selecao informada se sobrepoe a um bloco existente no mesmo arquivo.");
            }
        }

        targets.add(target);
        sortInternal();
        return target;
    }

    /* * Feature * Data: 2026-05-20 00:00:00 * Caller: Controllers da view de contexto * Callee: nenhum * Objetivo: Remover alvo pelo nome de forma segura. */
    public synchronized boolean removeByName(String name) {
        if (isBlank(name)) {
            return false;
        }

        Iterator<NamedContextTarget> it = targets.iterator();
        while (it.hasNext()) {
            NamedContextTarget target = it.next();
            if (safeEquals(target.getName(), name)) {
                it.remove();
                return true;
            }
        }

        return false;
    }

    public synchronized void clearAll() {
        targets.clear();
    }

    public synchronized List<NamedContextTarget> getAll() {
        return new ArrayList<NamedContextTarget>(targets);
    }

    public synchronized List<NamedContextTarget> getByRole(NamedContextTargetRole role) {
        List<NamedContextTarget> result = new ArrayList<NamedContextTarget>();
        if (role == null) {
            return result;
        }

        for (int i = 0; i < targets.size(); i++) {
            if (targets.get(i).getRole() == role) {
                result.add(targets.get(i));
            }
        }

        return result;
    }

    public synchronized NamedContextTarget findPrimary() {
        for (int i = 0; i < targets.size(); i++) {
            if (targets.get(i).getRole() == NamedContextTargetRole.PRIMARY) {
                return targets.get(i);
            }
        }
        return null;
    }

    public synchronized NamedContextTarget findByName(String name) {
        if (isBlank(name)) {
            return null;
        }

        for (int i = 0; i < targets.size(); i++) {
            if (safeEquals(targets.get(i).getName(), name)) {
                return targets.get(i);
            }
        }

        return null;
    }

    public synchronized boolean existsName(String name) {
        return findByName(name) != null;
    }

    public synchronized int countByRole(NamedContextTargetRole role) {
        int total = 0;
        if (role == null) {
            return total;
        }

        for (int i = 0; i < targets.size(); i++) {
            if (targets.get(i).getRole() == role) {
                total++;
            }
        }

        return total;
    }

    /* * Feature * Data: 2026-05-20 00:00:00 * Caller: addTarget * Callee: nenhum * Objetivo: Detectar sobreposicao parcial entre blocos de codigo no mesmo arquivo. */
    public synchronized boolean hasOverlappingCodeTarget(String filePath, int offset, int length) {
        if (isBlank(filePath) || offset < 0 || length <= 0) {
            return false;
        }

        for (int i = 0; i < targets.size(); i++) {
            NamedContextTarget target = targets.get(i);
            if (target == null || target.getType() != NamedContextTargetType.CODE_BLOCK) {
                continue;
            }

            if (target.overlaps(filePath, offset, length)) {
                return true;
            }
        }

        return false;
    }

    public synchronized boolean renameTarget(String currentName, String newName) {
        if (isBlank(currentName) || isBlank(newName)) {
            return false;
        }

        NamedContextTarget target = findByName(currentName);
        if (target == null) {
            return false;
        }

        if (!safeEquals(currentName, newName) && existsName(newName)) {
            return false;
        }

        target.setName(newName);
        sortInternal();
        return true;
    }

    public synchronized boolean changeRole(String name, NamedContextTargetRole newRole) {
        if (isBlank(name) || newRole == null) {
            return false;
        }

        NamedContextTarget target = findByName(name);
        if (target == null) {
            return false;
        }

        if (newRole == NamedContextTargetRole.PRIMARY) {
            if (target.getType() == NamedContextTargetType.PACKAGE && isPackagePlaceholder(target)) {
                return false;
            }
            clearPrimaryRole();
        }

        target.setRole(newRole);
        sortInternal();
        return true;
    }

    public synchronized boolean changeType(String name, NamedContextTargetType newType) {
        if (isBlank(name) || newType == null) {
            return false;
        }

        NamedContextTarget target = findByName(name);
        if (target == null) {
            return false;
        }

        if (target.getRole() == NamedContextTargetRole.PRIMARY
                && newType == NamedContextTargetType.PACKAGE
                && isPackagePlaceholder(target)) {
            return false;
        }

        target.setType(newType);
        sortInternal();
        return true;
    }

    private void clearPrimaryRole() {
        for (int i = 0; i < targets.size(); i++) {
            NamedContextTarget target = targets.get(i);
            if (target.getRole() == NamedContextTargetRole.PRIMARY) {
                target.setRole(NamedContextTargetRole.EDITABLE);
            }
        }
    }

    private void removeExactMatch(String filePath, int offset, int length, NamedContextTargetType type) {
        Iterator<NamedContextTarget> it = targets.iterator();

        while (it.hasNext()) {
            NamedContextTarget target = it.next();
            if (target.getType() != type) {
                continue;
            }

            if (safeEquals(target.getFilePath(), filePath)
                    && target.getOffset() == offset
                    && target.getLength() == length) {
                it.remove();
            }
        }
    }

    private void validateTargetInput(NamedContextTarget target) {
        if (target == null) {
            throw new IllegalArgumentException("Alvo contextual nao pode ser nulo.");
        }

        if (isBlank(target.getName())) {
            throw new IllegalArgumentException("Nome do alvo nao pode ser vazio.");
        }

        if (target.getRole() == null) {
            throw new IllegalArgumentException("Papel do alvo nao pode ser nulo.");
        }

        if (target.getType() == null) {
            throw new IllegalArgumentException("Tipo do alvo nao pode ser nulo.");
        }

        if (target.getType() == NamedContextTargetType.CODE_BLOCK) {
            if (isBlank(target.getFilePath()) || isBlank(target.getFileName())) {
                throw new IllegalArgumentException("Bloco de codigo exige arquivo valido.");
            }
            if (target.getOffset() < 0 || target.getLength() <= 0) {
                throw new IllegalArgumentException("Range invalido para bloco de codigo.");
            }
            if (target.getStartLine() <= 0 || target.getEndLine() <= 0 || target.getEndLine() < target.getStartLine()) {
                throw new IllegalArgumentException("Linhas invalidas para bloco de codigo.");
            }
        }

        if (target.getType() == NamedContextTargetType.FILE) {
            if (isBlank(target.getFilePath()) || isBlank(target.getFileName())) {
                throw new IllegalArgumentException("Arquivo contextual exige caminho e nome validos.");
            }
        }

        if (target.getType() == NamedContextTargetType.PACKAGE) {
            if (isBlank(target.getPackageName()) && isBlank(target.getFilePath()) && isBlank(target.getRelativeFilePath())) {
                throw new IllegalArgumentException("Package contextual exige identificacao minima.");
            }
        }

        if (target.getRole() == NamedContextTargetRole.PRIMARY
                && target.getType() == NamedContextTargetType.PACKAGE
                && isPackagePlaceholder(target)) {
            throw new IllegalArgumentException("Package vazio nao pode ser PRIMARY por padrao.");
        }
    }

    private boolean isPackagePlaceholder(NamedContextTarget target) {
        if (target == null) {
            return false;
        }

        String packageName = target.getPackageName();
        String fileName = target.getFileName();

        return !isBlank(packageName) && isBlank(fileName);
    }

    private void sortInternal() {
        Collections.sort(targets, new Comparator<NamedContextTarget>() {
            @Override
            public int compare(NamedContextTarget a, NamedContextTarget b) {
                int roleCompare = Integer.compare(roleWeight(a.getRole()), roleWeight(b.getRole()));
                if (roleCompare != 0) {
                    return roleCompare;
                }

                int typeCompare = Integer.compare(typeWeight(a.getType()), typeWeight(b.getType()));
                if (typeCompare != 0) {
                    return typeCompare;
                }

                int fileCompare = safe(a.getFileName()).compareToIgnoreCase(safe(b.getFileName()));
                if (fileCompare != 0) {
                    return fileCompare;
                }

                if (a.getStartLine() != b.getStartLine()) {
                    return Integer.compare(a.getStartLine(), b.getStartLine());
                }

                return safe(a.getName()).compareToIgnoreCase(safe(b.getName()));
            }
        });
    }

    private int roleWeight(NamedContextTargetRole role) {
        if (role == NamedContextTargetRole.PRIMARY) return 0;
        if (role == NamedContextTargetRole.EDITABLE) return 1;
        return 2;
    }

    private int typeWeight(NamedContextTargetType type) {
        if (type == NamedContextTargetType.CODE_BLOCK) return 0;
        if (type == NamedContextTargetType.FILE) return 1;
        return 2;
    }

    private String safe(String value) {
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
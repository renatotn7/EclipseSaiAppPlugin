package com.mcp.sailibrary.plugin.chat.context.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.HashSet;

import com.mcp.sailibrary.plugin.chat.context.model.NamedContextTargetRole;
import com.mcp.sailibrary.plugin.chat.context.model.NamedStructuralContext;
import com.mcp.sailibrary.plugin.chat.context.model.NamedStructuralContextType;

/* yaml_header: version: "1.0" purpose: "Gerenciar a sessao viva dos alvos estruturais nomeados, como arquivos, packages e pastas." libraries: - java.util.List: runtime - java.util.Collections: runtime - java.util.Comparator: runtime - java.util.Set: runtime */
public class NamedStructuralContextSessionService {

    private static final NamedStructuralContextSessionService INSTANCE = new NamedStructuralContextSessionService();

    private static final int MAX_STRUCTURAL_CONTEXTS_PER_SESSION = 24;

    private final List<NamedStructuralContext> contexts = new ArrayList<NamedStructuralContext>();

    private NamedStructuralContextSessionService() {
    }

    public static NamedStructuralContextSessionService getInstance() {
        return INSTANCE;
    }

    /* * Feature * Data: 2026-05-20 00:00:00 * Caller: controllers do explorer e package explorer * Callee: validateContextInput, clearPrimaryRole, sortInternal * Objetivo: Registrar um novo alvo estrutural na sessao preservando integridade e unicidade basica. */
    public synchronized NamedStructuralContext addContext(NamedStructuralContext context) {
        validateContextInput(context);

        if (contexts.size() >= MAX_STRUCTURAL_CONTEXTS_PER_SESSION) {
            throw new IllegalStateException("Limite maximo de alvos estruturais da sessao atingido.");
        }

        if (existsName(context.getName())) {
            throw new IllegalArgumentException("Ja existe um alvo estrutural com este nome: " + context.getName());
        }

        if (context.getRole() == NamedContextTargetRole.PRIMARY) {
            clearPrimaryRole();
        }

        removeExactMatch(context);

        contexts.add(context);
        sortInternal();

        return context;
    }

    /* * Feature * Data: 2026-05-20 00:00:00 * Caller: views e controllers * Callee: nenhum * Objetivo: Remover alvo estrutural pelo nome. */
    public synchronized boolean removeByName(String name) {
        if (isBlank(name)) {
            return false;
        }

        Iterator<NamedStructuralContext> it = contexts.iterator();
        while (it.hasNext()) {
            NamedStructuralContext context = it.next();
            if (safeEquals(context.getName(), name)) {
                it.remove();
                return true;
            }
        }

        return false;
    }

    /* * Feature * Data: 2026-05-20 00:00:00 * Caller: views e controllers * Callee: nenhum * Objetivo: Limpar toda a sessao estrutural. */
    public synchronized void clearAll() {
        contexts.clear();
    }

    public synchronized List<NamedStructuralContext> getAll() {
        return new ArrayList<NamedStructuralContext>(contexts);
    }

    public synchronized List<NamedStructuralContext> getByRole(NamedContextTargetRole role) {
        List<NamedStructuralContext> result = new ArrayList<NamedStructuralContext>();
        if (role == null) {
            return result;
        }

        for (int i = 0; i < contexts.size(); i++) {
            if (contexts.get(i).getRole() == role) {
                result.add(contexts.get(i));
            }
        }

        return result;
    }

    public synchronized List<NamedStructuralContext> getByType(NamedStructuralContextType type) {
        List<NamedStructuralContext> result = new ArrayList<NamedStructuralContext>();
        if (type == null) {
            return result;
        }

        for (int i = 0; i < contexts.size(); i++) {
            if (contexts.get(i).getType() == type) {
                result.add(contexts.get(i));
            }
        }

        return result;
    }

    public synchronized NamedStructuralContext findPrimary() {
        for (int i = 0; i < contexts.size(); i++) {
            if (contexts.get(i).getRole() == NamedContextTargetRole.PRIMARY) {
                return contexts.get(i);
            }
        }
        return null;
    }

    public synchronized NamedStructuralContext findByName(String name) {
        if (isBlank(name)) {
            return null;
        }

        for (int i = 0; i < contexts.size(); i++) {
            if (safeEquals(contexts.get(i).getName(), name)) {
                return contexts.get(i);
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

        for (int i = 0; i < contexts.size(); i++) {
            if (contexts.get(i).getRole() == role) {
                total++;
            }
        }

        return total;
    }

    public synchronized int countByType(NamedStructuralContextType type) {
        int total = 0;
        if (type == null) {
            return total;
        }

        for (int i = 0; i < contexts.size(); i++) {
            if (contexts.get(i).getType() == type) {
                total++;
            }
        }

        return total;
    }

    public synchronized boolean renameContext(String currentName, String newName) {
        if (isBlank(currentName) || isBlank(newName)) {
            return false;
        }

        NamedStructuralContext context = findByName(currentName);
        if (context == null) {
            return false;
        }

        if (!safeEquals(currentName, newName) && existsName(newName)) {
            return false;
        }

        context.setName(newName);
        sortInternal();
        return true;
    }

    public synchronized boolean changeRole(String name, NamedContextTargetRole newRole) {
        if (isBlank(name) || newRole == null) {
            return false;
        }

        NamedStructuralContext context = findByName(name);
        if (context == null) {
            return false;
        }

        if (newRole == NamedContextTargetRole.PRIMARY) {
            if (context.getType() == NamedStructuralContextType.PACKAGE && isPackagePlaceholder(context)) {
                return false;
            }
            if (context.getType() == NamedStructuralContextType.FOLDER) {
                return false;
            }
            clearPrimaryRole();
        }

        context.setRole(newRole);
        sortInternal();
        return true;
    }

    public synchronized boolean changeType(String name, NamedStructuralContextType newType) {
        if (isBlank(name) || newType == null) {
            return false;
        }

        NamedStructuralContext context = findByName(name);
        if (context == null) {
            return false;
        }

        if (context.getRole() == NamedContextTargetRole.PRIMARY) {
            if (newType == NamedStructuralContextType.PACKAGE && isPackagePlaceholder(context)) {
                return false;
            }
            if (newType == NamedStructuralContextType.FOLDER) {
                return false;
            }
        }

        context.setType(newType);
        sortInternal();
        return true;
    }

    public synchronized Set<String> collectNames() {
        Set<String> names = new HashSet<String>();
        for (int i = 0; i < contexts.size(); i++) {
            if (!isBlank(contexts.get(i).getName())) {
                names.add(contexts.get(i).getName());
            }
        }
        return names;
    }

    private void clearPrimaryRole() {
        for (int i = 0; i < contexts.size(); i++) {
            NamedStructuralContext context = contexts.get(i);
            if (context.getRole() == NamedContextTargetRole.PRIMARY) {
                context.setRole(NamedContextTargetRole.EDITABLE);
            }
        }
    }

    private void removeExactMatch(NamedStructuralContext contextToAdd) {
        if (contextToAdd == null) {
            return;
        }

        Iterator<NamedStructuralContext> it = contexts.iterator();
        while (it.hasNext()) {
            NamedStructuralContext existing = it.next();

            if (existing.getType() != contextToAdd.getType()) {
                continue;
            }

            if (contextToAdd.getType() == NamedStructuralContextType.FILE) {
                if (safeEquals(existing.getFilePath(), contextToAdd.getFilePath())) {
                    it.remove();
                }
                continue;
            }

            if (contextToAdd.getType() == NamedStructuralContextType.PACKAGE) {
                if (safeEquals(existing.getPackageName(), contextToAdd.getPackageName())
                        && safeEquals(existing.getRelativePath(), contextToAdd.getRelativePath())) {
                    it.remove();
                }
                continue;
            }

            if (contextToAdd.getType() == NamedStructuralContextType.FOLDER) {
                if (safeEquals(existing.getFilePath(), contextToAdd.getFilePath())
                        && safeEquals(existing.getRelativePath(), contextToAdd.getRelativePath())) {
                    it.remove();
                }
            }
        }
    }

    private void validateContextInput(NamedStructuralContext context) {
        if (context == null) {
            throw new IllegalArgumentException("Alvo estrutural nao pode ser nulo.");
        }

        if (isBlank(context.getName())) {
            throw new IllegalArgumentException("Nome do alvo estrutural nao pode ser vazio.");
        }

        if (context.getRole() == null) {
            throw new IllegalArgumentException("Papel do alvo estrutural nao pode ser nulo.");
        }

        if (context.getType() == null) {
            throw new IllegalArgumentException("Tipo do alvo estrutural nao pode ser nulo.");
        }

        if (context.getType() == NamedStructuralContextType.FILE) {
            if (isBlank(context.getFilePath()) && isBlank(context.getRelativePath()) && isBlank(context.getFileName())) {
                throw new IllegalArgumentException("Arquivo contextual exige identificacao minima.");
            }
        }

        if (context.getType() == NamedStructuralContextType.PACKAGE) {
            if (isBlank(context.getPackageName()) && isBlank(context.getRelativePath()) && isBlank(context.getFilePath())) {
                throw new IllegalArgumentException("Package contextual exige identificacao minima.");
            }
        }

        if (context.getType() == NamedStructuralContextType.FOLDER) {
            if (isBlank(context.getFilePath()) && isBlank(context.getRelativePath()) && isBlank(context.getFileName())) {
                throw new IllegalArgumentException("Pasta contextual exige identificacao minima.");
            }
        }

        if (context.getRole() == NamedContextTargetRole.PRIMARY
                && context.getType() == NamedStructuralContextType.PACKAGE
                && isPackagePlaceholder(context)) {
            throw new IllegalArgumentException("Package vazio nao pode ser PRIMARY por padrao.");
        }

        if (context.getRole() == NamedContextTargetRole.PRIMARY
                && context.getType() == NamedStructuralContextType.FOLDER) {
            throw new IllegalArgumentException("Pasta nao pode ser PRIMARY por padrao.");
        }
    }

    private boolean isPackagePlaceholder(NamedStructuralContext context) {
        if (context == null) {
            return false;
        }

        String packageName = context.getPackageName();
        String fileName = context.getFileName();

        return !isBlank(packageName) && isBlank(fileName);
    }

    private void sortInternal() {
        Collections.sort(contexts, new Comparator<NamedStructuralContext>() {
            @Override
            public int compare(NamedStructuralContext a, NamedStructuralContext b) {
                int roleCompare = Integer.compare(roleWeight(a.getRole()), roleWeight(b.getRole()));
                if (roleCompare != 0) {
                    return roleCompare;
                }

                int typeCompare = Integer.compare(typeWeight(a.getType()), typeWeight(b.getType()));
                if (typeCompare != 0) {
                    return typeCompare;
                }

                int packageCompare = safe(a.getPackageName()).compareToIgnoreCase(safe(b.getPackageName()));
                if (packageCompare != 0) {
                    return packageCompare;
                }

                int fileCompare = safe(a.getFileName()).compareToIgnoreCase(safe(b.getFileName()));
                if (fileCompare != 0) {
                    return fileCompare;
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

    private int typeWeight(NamedStructuralContextType type) {
        if (type == NamedStructuralContextType.FILE) return 0;
        if (type == NamedStructuralContextType.PACKAGE) return 1;
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
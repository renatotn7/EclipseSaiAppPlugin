package com.mcp.sailibrary.plugin.agent.context;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;

import com.mcp.sailibrary.plugin.agent.context.ContextOrchestrator;

/**  * dependencies: * - org.eclipse.core.resources * - org.eclipse.jdt.core * - com.mcp.sailibrary.plugin.agent.context.ContextOrchestrator * purpose: "Resolver classe e metodo por nome no workspace e acionar o motor de contexto baseado em unit e offset." * design_pattern: "Adapter / Resolver" * --- */
public class JdtContextByNameResolver {

    private ContextOrchestrator contextOrchestrator;

    /**
 * Inicializa o resolver que converte nomes em coordenadas JDT.
 *
 * @author Renato Tomaz Nati
 */
    public JdtContextByNameResolver() {
        this.contextOrchestrator = new ContextOrchestrator();
    }

    /**
 * Resolve classe e metodo por nome e dispara o enraizamento pelo motor principal.
 *
 * @author Renato Tomaz Nati
 */
    public String enraizarChamadasPorNome(String nomeClasse, String nomeMetodo, int profundidadeMaxima) {
        if (nomeClasse == null || nomeClasse.trim().length() == 0) {
            return "Erro Operacional: O parametro 'classe' e obrigatorio.";
        }

        if (nomeMetodo == null || nomeMetodo.trim().length() == 0) {
            return "Erro Operacional: O parametro 'metodo' e obrigatorio.";
        }

        if (profundidadeMaxima <= 0) {
            profundidadeMaxima = 1;
        }

        if (profundidadeMaxima > 5) {
            profundidadeMaxima = 5;
        }

        try {
            IType tipoEncontrado = localizarTipoNoWorkspace(nomeClasse);
            if (tipoEncontrado == null || !tipoEncontrado.exists()) {
                return "Erro Tatico: A classe [" + nomeClasse + "] nao foi encontrada no workspace ativo.";
            }

            IMethod metodoEncontrado = localizarMetodoNoTipo(tipoEncontrado, nomeMetodo);
            if (metodoEncontrado == null || !metodoEncontrado.exists()) {
                return "Erro Tatico: O metodo [" + nomeMetodo + "] nao foi encontrado na classe [" + nomeClasse + "].";
            }

            ICompilationUnit compilationUnit = tipoEncontrado.getCompilationUnit();
            if (compilationUnit == null) {
                return "Erro Tatico: A classe [" + nomeClasse + "] foi encontrada, mas nao possui unidade de compilacao acessivel.";
            }

            int offsetMetodo = metodoEncontrado.getNameRange().getOffset();
            return contextOrchestrator.enraizarChamadas(compilationUnit, offsetMetodo, profundidadeMaxima);
        } catch (Exception e) {
            return "Erro Tatico: Falha ao resolver coordenadas para [" + nomeClasse + "." + nomeMetodo + "]: " + e.getMessage();
        }
    }

    /**
 * Procura a classe primeiro por FQCN e depois por nome simples em todos os projetos Java abertos.
 *
 * @author Renato Tomaz Nati
 * @since 2026-05-18
 */
    private IType localizarTipoNoWorkspace(String nomeClasse) throws Exception {
        IProject[] projetos = ResourcesPlugin.getWorkspace().getRoot().getProjects();

        for (int indiceProjeto = 0; indiceProjeto < projetos.length; indiceProjeto++) {
            IProject projetoAtual = projetos[indiceProjeto];
            if (!projetoAtual.isOpen() || !projetoAtual.hasNature(JavaCore.NATURE_ID)) {
                continue;
            }

            IJavaProject javaProject = JavaCore.create(projetoAtual);

            IType tipoPorNomeCanonico = javaProject.findType(nomeClasse);
            if (tipoPorNomeCanonico != null && tipoPorNomeCanonico.exists()) {
                return tipoPorNomeCanonico;
            }

            IType tipoPorNomeSimples = localizarTipoPorNomeSimples(javaProject, nomeClasse);
            if (tipoPorNomeSimples != null && tipoPorNomeSimples.exists()) {
                return tipoPorNomeSimples;
            }
        }

        return null;
    }

    /**
 * Faz fallback por nome simples percorrendo pacotes fonte do projeto.
 *
 * @author Renato Tomaz Nati
 */
    private IType localizarTipoPorNomeSimples(IJavaProject javaProject, String nomeClasse) throws Exception {
        IPackageFragment[] pacotes = javaProject.getPackageFragments();

        for (int indicePacote = 0; indicePacote < pacotes.length; indicePacote++) {
            IPackageFragment pacoteAtual = pacotes[indicePacote];
            if (pacoteAtual.getKind() != IPackageFragmentRoot.K_SOURCE) {
                continue;
            }

            ICompilationUnit[] unidades = pacoteAtual.getCompilationUnits();
            for (int indiceUnidade = 0; indiceUnidade < unidades.length; indiceUnidade++) {
                ICompilationUnit unidadeAtual = unidades[indiceUnidade];
                IType[] tipos = unidadeAtual.getAllTypes();

                for (int indiceTipo = 0; indiceTipo < tipos.length; indiceTipo++) {
                    IType tipoAtual = tipos[indiceTipo];
                    if (nomeClasse.equals(tipoAtual.getElementName())) {
                        return tipoAtual;
                    }
                }
            }
        }

        return null;
    }

    /**
 * Localiza o metodo pelo nome dentro do tipo encontrado.
 *
 * @author Renato Tomaz Nati
 */
    private IMethod localizarMetodoNoTipo(IType tipo, String nomeMetodo) throws Exception {
        IMethod[] metodos = tipo.getMethods();

        for (int indiceMetodo = 0; indiceMetodo < metodos.length; indiceMetodo++) {
            IMethod metodoAtual = metodos[indiceMetodo];
            if (nomeMetodo.equals(metodoAtual.getElementName())) {
                return metodoAtual;
            }
        }

        return null;
    }
}
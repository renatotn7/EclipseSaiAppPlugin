package com.mcp.sailibrary.plugin.agent.context;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;

/** * Resolve classe e metodo por nome no workspace e aciona o motor de contexto * baseado em compilation unit e offset. * * <p>Esta implementacao foi reforcada para reduzir ambiguidades em workspaces * com varios projetos e em cenarios Maven multimodulo, priorizando resolucao * por FQCN e apenas depois usando fallback por nome simples.</p> * * @author Renato Tomaz Nati * @since 2026-05-20 */
public class JdtContextByNameResolver {

    private ContextOrchestrator contextOrchestrator;

    /** * Inicializa o resolver que converte nomes em coordenadas JDT. * * @author Renato Tomaz Nati * @since 2026-05-20 */
    public JdtContextByNameResolver() {
        this.contextOrchestrator = new ContextOrchestrator();
    }

    /** * Resolve classe e metodo por nome e dispara o enraizamento pelo motor * principal. * * @param nomeClasse nome simples ou FQCN da classe * @param nomeMetodo nome do metodo * @param profundidadeMaxima profundidade maxima do rastreamento * @return breadcrumb estrutural produzido pelo orquestrador * * @author Renato Tomaz Nati * @since 2026-05-20 */
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

    /** * Procura a classe primeiro por FQCN e depois, se necessario, por nome * simples com criterio deterministico. * * @param nomeClasse nome simples ou FQCN * @return tipo resolvido ou null * * @throws Exception quando houver falha grave de leitura JDT * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private IType localizarTipoNoWorkspace(String nomeClasse) throws Exception {
        IProject[] projetos = ResourcesPlugin.getWorkspace().getRoot().getProjects();

        List<IJavaProject> projetosJava = new ArrayList<IJavaProject>();
        for (int i = 0; i < projetos.length; i++) {
            IProject projetoAtual = projetos[i];
            if (!projetoAtual.isOpen() || !projetoAtual.hasNature(JavaCore.NATURE_ID)) {
                continue;
            }
            projetosJava.add(JavaCore.create(projetoAtual));
        }

        ordenarProjetosJavaDeterministicamente(projetosJava);

        for (int i = 0; i < projetosJava.size(); i++) {
            IType tipoPorNomeCanonico = projetosJava.get(i).findType(nomeClasse);
            if (tipoPorNomeCanonico != null && tipoPorNomeCanonico.exists()) {
                return tipoPorNomeCanonico;
            }
        }

        List<IType> candidatos = new ArrayList<IType>();
        for (int i = 0; i < projetosJava.size(); i++) {
            IType tipoPorNomeSimples = localizarTipoPorNomeSimples(projetosJava.get(i), nomeClasse);
            if (tipoPorNomeSimples != null && tipoPorNomeSimples.exists()) {
                candidatos.add(tipoPorNomeSimples);
            }
        }

        if (candidatos.isEmpty()) {
            return null;
        }

        ordenarTiposDeterministicamente(candidatos);
        return candidatos.get(0);
    }

    /** * Faz fallback por nome simples percorrendo pacotes fonte do projeto. * * @param javaProject projeto Java de busca * @param nomeClasse nome simples da classe * @return tipo encontrado ou null * * @throws Exception quando houver falha grave de leitura JDT * * @author Renato Tomaz Nati * @since 2026-05-20 */
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

    /** * Localiza o metodo pelo nome dentro do tipo encontrado. * * @param tipo tipo alvo * @param nomeMetodo nome do metodo * @return metodo encontrado ou null * * @throws Exception quando houver falha grave de leitura JDT * * @author Renato Tomaz Nati * @since 2026-05-20 */
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

    /** * Ordena projetos Java de forma deterministica para reduzir resultados * aleatorios em cenarios ambíguos. * * @param projetos lista de projetos Java * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private void ordenarProjetosJavaDeterministicamente(List<IJavaProject> projetos) {
        Collections.sort(projetos, new Comparator<IJavaProject>() {
            @Override
            public int compare(IJavaProject a, IJavaProject b) {
                String na = "";
                String nb = "";

                try {
                    na = a != null && a.getProject() != null ? a.getProject().getName() : "";
                    nb = b != null && b.getProject() != null ? b.getProject().getName() : "";
                } catch (Exception e) {
                }

                return na.compareToIgnoreCase(nb);
            }
        });
    }

    /** * Ordena tipos de forma deterministica para reduzir não determinismo em * fallback por nome simples. * * @param tipos lista de tipos candidatos * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private void ordenarTiposDeterministicamente(List<IType> tipos) {
        Collections.sort(tipos, new Comparator<IType>() {
            @Override
            public int compare(IType a, IType b) {
                String qa = "";
                String qb = "";

                try {
                    qa = a != null ? a.getFullyQualifiedName() : "";
                    qb = b != null ? b.getFullyQualifiedName() : "";
                } catch (Exception e) {
                }

                return qa.compareToIgnoreCase(qb);
            }
        });
    }
}
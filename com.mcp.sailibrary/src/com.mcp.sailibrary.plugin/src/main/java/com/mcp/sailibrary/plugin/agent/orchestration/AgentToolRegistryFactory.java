package com.mcp.sailibrary.plugin.agent.orchestration;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.ICompilationUnit;

import com.mcp.sailibrary.plugin.agent.AgentTool;
import com.mcp.sailibrary.plugin.agent.tools.architecture.ChangeImpactSummaryTool;
import com.mcp.sailibrary.plugin.agent.tools.architecture.InheritanceDiscoveryTool;
import com.mcp.sailibrary.plugin.agent.tools.architecture.ProjectDependencyInspectionTool;
import com.mcp.sailibrary.plugin.agent.tools.architecture.QueryExtractionTool;
import com.mcp.sailibrary.plugin.agent.tools.architecture.TypeImplementationDiscoveryTool;
import com.mcp.sailibrary.plugin.agent.tools.bytecode.CorporateClassDecompilerTool;
import com.mcp.sailibrary.plugin.agent.tools.exploration.DirectoryExplorerTool;
import com.mcp.sailibrary.plugin.agent.tools.exploration.FileContentReadTool;
import com.mcp.sailibrary.plugin.agent.tools.exploration.ProjectRootDetectionTool;
import com.mcp.sailibrary.plugin.agent.tools.exploration.ProjectTextSearchTool;
import com.mcp.sailibrary.plugin.agent.tools.jdt.JdtContextSearchTool;
import com.mcp.sailibrary.plugin.agent.tools.jdt.JdtSourceSliceTool;
import com.mcp.sailibrary.plugin.agent.tools.jdt.MethodCalleesDiscoveryTool;
import com.mcp.sailibrary.plugin.agent.tools.jdt.MethodCallersSearchTool;
import com.mcp.sailibrary.plugin.agent.tools.jdt.MethodOverrideInspectionTool;
import com.mcp.sailibrary.plugin.agent.tools.jdt.RootedContextTool;
import com.mcp.sailibrary.plugin.agent.tools.jdt.SideEffectInspectionTool;
import com.mcp.sailibrary.plugin.agent.tools.memory.ProjectMemoryQueryTool;
import com.mcp.sailibrary.plugin.agent.tools.memory.ProjectMemoryWriteTool;
import com.mcp.sailibrary.plugin.agent.tools.mutation.CreateProjectFileTool;
import com.mcp.sailibrary.plugin.agent.tools.mutation.CreateProjectPackageTool;
import com.mcp.sailibrary.plugin.agent.tools.mutation.DeleteCreatedProjectFileTool;
import com.mcp.sailibrary.plugin.agent.tools.mutation.QueryWorkspaceMutationPolicyTool;
import com.mcp.sailibrary.plugin.agent.tools.mutation.UndoWorkspaceMutationTool;
import com.mcp.sailibrary.plugin.agent.tools.mutation.UpdateProjectFileWithBackupTool;
import com.mcp.sailibrary.plugin.agent.tools.mutation.history.InspectWorkspaceMutationDiffTool;
import com.mcp.sailibrary.plugin.agent.tools.mutation.history.InspectWorkspaceMutationStateTool;
import com.mcp.sailibrary.plugin.agent.tools.mutation.history.ListWorkspaceMutationHistoryTool;
import com.mcp.sailibrary.plugin.agent.tools.mutation.history.RedoWorkspaceMutationTool;
import com.mcp.sailibrary.plugin.agent.tools.mutation.history.RestoreWorkspaceFileTool;

/** * Monta o registro de ferramentas homologadas disponiveis para a IA no * contexto atual do projeto e do editor. * * <p>Esta factory concentra a composicao do arsenal tatico, reduzindo o * acoplamento do orquestrador principal e tornando mais simples a manutencao, * evolucao e testabilidade do conjunto de tools.</p> * * @author Renato Tomaz Nati * @since 2026-05-20 */
public class AgentToolRegistryFactory {

    /** * Construi a lista de ferramentas homologadas disponiveis para o contexto * atual. * * @param raizProjetoWorkspace raiz segura do projeto atual * @param compilationUnitAtual compilation unit atual do editor, quando houver * @param offsetAtual offset atual do editor, quando houver * @return lista de ferramentas registradas * * @author Renato Tomaz Nati * @since 2026-05-20 */
    public List<AgentTool> build(File raizProjetoWorkspace, ICompilationUnit compilationUnitAtual, int offsetAtual) {
        List<AgentTool> registry = new ArrayList<AgentTool>();

        registry.add(new DirectoryExplorerTool(raizProjetoWorkspace));
        registry.add(new FileContentReadTool(raizProjetoWorkspace));
        registry.add(new ProjectTextSearchTool(raizProjetoWorkspace));
        registry.add(new ProjectRootDetectionTool(raizProjetoWorkspace));

        registry.add(new JdtContextSearchTool());
        registry.add(new JdtSourceSliceTool(raizProjetoWorkspace));

        registry.add(new MethodCallersSearchTool(compilationUnitAtual, offsetAtual));
        registry.add(new MethodCalleesDiscoveryTool(compilationUnitAtual, offsetAtual));
        registry.add(new MethodOverrideInspectionTool(compilationUnitAtual, offsetAtual));
        registry.add(new SideEffectInspectionTool(compilationUnitAtual, offsetAtual));

        registry.add(new TypeImplementationDiscoveryTool(raizProjetoWorkspace));
        registry.add(new InheritanceDiscoveryTool(raizProjetoWorkspace));
        registry.add(new QueryExtractionTool(raizProjetoWorkspace, compilationUnitAtual, offsetAtual));
        registry.add(new ChangeImpactSummaryTool(raizProjetoWorkspace, compilationUnitAtual, offsetAtual));
        registry.add(new ProjectDependencyInspectionTool(raizProjetoWorkspace));

        registry.add(new ProjectMemoryQueryTool(raizProjetoWorkspace));
        registry.add(new ProjectMemoryWriteTool(raizProjetoWorkspace));

        registry.add(new CorporateClassDecompilerTool(raizProjetoWorkspace));

        registry.add(new QueryWorkspaceMutationPolicyTool(raizProjetoWorkspace));
        registry.add(new CreateProjectFileTool(raizProjetoWorkspace));
        registry.add(new UpdateProjectFileWithBackupTool(raizProjetoWorkspace));
        registry.add(new DeleteCreatedProjectFileTool(raizProjetoWorkspace));
        registry.add(new CreateProjectPackageTool(raizProjetoWorkspace));

        registry.add(new ListWorkspaceMutationHistoryTool(raizProjetoWorkspace));
        registry.add(new InspectWorkspaceMutationStateTool(raizProjetoWorkspace));
        registry.add(new InspectWorkspaceMutationDiffTool(raizProjetoWorkspace));
        registry.add(new UndoWorkspaceMutationTool(raizProjetoWorkspace));
        registry.add(new RedoWorkspaceMutationTool(raizProjetoWorkspace));
        registry.add(new RestoreWorkspaceFileTool(raizProjetoWorkspace));

        if (compilationUnitAtual != null) {
            registry.add(new RootedContextTool(compilationUnitAtual, offsetAtual));
        }

        return registry;
    }
}
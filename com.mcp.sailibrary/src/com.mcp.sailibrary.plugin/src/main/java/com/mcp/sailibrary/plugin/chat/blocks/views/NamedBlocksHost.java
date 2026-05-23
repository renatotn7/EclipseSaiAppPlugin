package com.mcp.sailibrary.plugin.chat.blocks.views;

import java.util.List;

import com.mcp.sailibrary.plugin.chat.blocks.model.NamedCodeBlock;
import com.mcp.sailibrary.plugin.chat.context.model.NamedStructuralContext;

public interface NamedBlocksHost {

	void atualizarBlocos(List<NamedCodeBlock> primaryBlocks, List<NamedCodeBlock> editables, List<NamedCodeBlock> references);

    void atualizarContextosEstruturais(List<NamedStructuralContext> primaryContexts, List<NamedStructuralContext> editableContexts, List<NamedStructuralContext> referenceContexts);

    void adicionarMensagemStatus(String message);
    
    /** * Solicita a insercao de um alias de contexto na entrada da conversa. * * <p>O alias deve ser inserido no formato textual usado pela sessao, como * por exemplo @nome.</p> * * @param alias alias a ser inserido na area de comando da conversa * * @author Renato Tomaz Nati * @since 2026-05-20 */
    void inserirAliasNaConversa(String alias);
}
package com.mcp.sailibrary.plugin.chat.blocks.views;

import java.util.List;

import com.mcp.sailibrary.plugin.chat.blocks.model.NamedCodeBlock;
import com.mcp.sailibrary.plugin.chat.context.model.NamedStructuralContext;

public interface NamedBlocksHost {

	void atualizarBlocos(List<NamedCodeBlock> primaryBlocks, List<NamedCodeBlock> editables, List<NamedCodeBlock> references);

    void atualizarContextosEstruturais(List<NamedStructuralContext> primaryContexts, List<NamedStructuralContext> editableContexts, List<NamedStructuralContext> referenceContexts);

    void adicionarMensagemStatus(String message);
}
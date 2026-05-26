package com.mcp.sailibrary.plugin.chat.views.tabs;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import com.mcp.sailibrary.plugin.chat.controllers.ChatViewActivityController;

/* --- version: "1.1" libraries: - SWT - StyledText - GridData - GridLayout - Composite - Label - ChatViewActivityController objetivo: "Representar a aba de atividade do agente, deixando a trilha de eventos e o estado sob responsabilidade do controller de atividade." --- */

/** * Painel da aba de atividade do agente. * * <p>Esta classe cuida apenas da interface. A trilha de atividade e montada no * controller dedicado.</p> * * @author Renato Tomaz Nati * @since 2026-05-24 */
public class ChatActivityPanel extends Composite {

    private ChatViewActivityController activityController;
    private Label atividadeStatusLabel;
    private StyledText atividadeAgenteText;

    /** * Caller: ChatView * Callee: criarConteudo, atualizarTela * Objetivo: Inicializar a aba de atividade com o controller dedicado. * Data modificacao: 2026-05-24 00:00 * * @param parent componente pai * @param style estilo SWT * @param activityController controller da atividade * * @author Renato Tomaz Nati * @since 2026-05-24 */
    public ChatActivityPanel(Composite parent, int style, ChatViewActivityController activityController) {
        super(parent, style);
        this.activityController = activityController;

        GridLayout layout = new GridLayout(1, false);
        layout.marginWidth = 8;
        layout.marginHeight = 8;
        layout.verticalSpacing = 8;
        setLayout(layout);

        criarConteudo();
        atualizarTela();
    }

    /** * Caller: construtor * Callee: N/A * Objetivo: Construir os controles visuais da aba de atividade. * Data modificacao: 2026-05-24 00:00 * * @author Renato Tomaz Nati * @since 2026-05-24 */
    private void criarConteudo() {
        atividadeStatusLabel = new Label(this, SWT.WRAP);
        atividadeStatusLabel.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
        atividadeStatusLabel.setText("Sem atividade em andamento.");

        atividadeAgenteText = new StyledText(this, SWT.BORDER | SWT.MULTI | SWT.WRAP | SWT.V_SCROLL);
        GridData gdAtividade = new GridData(SWT.FILL, SWT.FILL, true, true);
        gdAtividade.heightHint = 320;
        atividadeAgenteText.setLayoutData(gdAtividade);
        atividadeAgenteText.setEditable(false);
        atividadeAgenteText.setCaret(null);
    }

    /** * Caller: ChatView * Callee: ChatViewActivityController.registrarAtividade, atualizarTela * Objetivo: Registrar uma nova atividade e atualizar a interface * imediatamente. * Data modificacao: 2026-05-24 00:00 * * @param fase fase logica da atividade * @param detalhe detalhe textual * * @author Renato Tomaz Nati * @since 2026-05-24 */
    public void registrarAtividade(String fase, String detalhe) {
        if (activityController == null) {
            return;
        }

        activityController.registrarAtividade(fase, detalhe);
        atualizarTela();
    }

    /** * Caller: ChatView * Callee: ChatViewActivityController.limparAtividade, atualizarTela * Objetivo: Limpar a trilha visual de atividade e atualizar a interface. * Data modificacao: 2026-05-24 00:00 * * @author Renato Tomaz Nati * @since 2026-05-24 */
    public void limparAtividade() {
        if (activityController == null) {
            return;
        }

        activityController.limparAtividade();
        atualizarTela();
    }

    /** * Caller: ChatView, ChatAiController * Callee: ChatViewActivityController.renderizarTextoAtividade, ChatViewActivityController.getMensagemStatus * Objetivo: Atualizar a tela da aba de atividade com o estado atual do * controller. * Data modificacao: 2026-05-24 00:00 * * @author Renato Tomaz Nati * @since 2026-05-24 */
    public void atualizarTela() {
        if (atividadeAgenteText == null || atividadeAgenteText.isDisposed()) {
            return;
        }

        atividadeAgenteText.setText(activityController != null ? activityController.renderizarTextoAtividade() : "");
        atividadeAgenteText.setTopIndex(atividadeAgenteText.getLineCount() - 1);

        if (atividadeStatusLabel != null && !atividadeStatusLabel.isDisposed()) {
            atividadeStatusLabel.setText(activityController != null
                    ? activityController.getMensagemStatus()
                    : "Sem atividade em andamento.");
        }
    }
}
package com.mcp.sailibrary.plugin.mcp;

/** * Monta o prompt especifico para sugestao de nome curto de bloco ou contexto. * * <p>Esta classe e intencionalmente pura: nao conhece transporte, modelo, * endpoint, cookies, api key nem formato de resposta.</p> * * @author Renato Tomaz Nati * @since 2026-05-26 */
public class BlockNamePromptBuilder {

    /** * Construi o prompt textual de sugestao de nome curto. * * @param selectedCode trecho selecionado * @param kind tipo logico do bloco * @param existingNames nomes ja existentes na sessao * @return prompt final */
    public String build(String selectedCode, String kind, String existingNames) {
        String safeText = safe(selectedCode);
        String safeKind = safe(kind);
        String safeExistingNames = safe(existingNames);

        return "Voce deve gerar um nome curto para um alvo de contexto selecionado.\n"
                + "REGRAS OBRIGATORIAS:\n"
                + "1. Responda com UMA unica palavra.\n"
                + "2. Use apenas letras minusculas e numeros.\n"
                + "3. Nao use acentos.\n"
                + "4. Nao use espacos.\n"
                + "5. Nao use underline, hifen, pontuacao ou simbolos.\n"
                + "6. O nome deve ter no maximo 12 caracteres.\n"
                + "7. O nome deve representar a funcao principal do alvo selecionado.\n"
                + "8. Prefira nomes intuitivos e concretos como validacao, retorno, query, dao, criteria, montagem, atributo, pedido, municipio, usuario, service, repository, config, sql, xml.\n"
                + "9. Se o trecho parecer consulta SQL/HQL/JPQL/Hibernate/JPA, prefira query, sql, hql, criteria ou jdbc conforme o caso.\n"
                + "10. Se o trecho parecer validacao, prefira validacao.\n"
                + "11. Se o trecho parecer retorno, prefira retorno.\n"
                + "12. Se o nome colidir com nomes ja existentes, escolha outro nome curto e diferente.\n"
                + "13. Responda APENAS com o nome final, sem JSON, sem explicacao, sem aspas e sem texto adicional.\n"
                + "\nTIPO DO BLOCO: " + safeKind + "\n"
                + "NOMES JA EXISTENTES: " + safeExistingNames + "\n"
                + "\nTRECHO SELECIONADO:\n"
                + safeText + "\n";
    }

    private String safe(String value) {
        return value != null ? value : "";
    }
}
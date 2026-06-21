package com.mcp.sailibrary.plugin.mcp.core;

/** * Listener simples para receber deltas de resposta streaming. * * <p> * A UI deve implementar este contrato para mostrar parciais no chat * enquanto o modelo ainda esta respondendo. * </p> * * @author Renato Tomaz Nati * @since 2026-05-27 */
public interface ModelStreamingListener {

    void onStreamingStarted(String executionId);

    void onStreamingDelta(String executionId, String delta, String accumulatedText);

    void onStreamingCompleted(String executionId, String finalText);

    void onStreamingFailure(String executionId, Exception exception);
}
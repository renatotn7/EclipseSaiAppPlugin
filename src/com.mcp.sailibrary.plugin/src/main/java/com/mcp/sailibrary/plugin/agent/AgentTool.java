package com.mcp.sailibrary.plugin.agent;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/*
yaml_header:
version: "1.0"
dependencies:
java.io.File
java.util.List
purpose: "Definir o contrato estrito para ferramentas que a IA pode invocar de forma autonoma."
security_level: "High - Sandbox Enforced"

*/
public interface AgentTool {

String getName();

String execute(String jsonParameters);
}
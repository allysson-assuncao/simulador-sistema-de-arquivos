package org.example.comandos;

import org.example.sistema_de_arquivos.SistemaArquivos;

import java.util.List;

@FunctionalInterface
public interface CommandExecutor {
    // Todo comando recebe:
    // 1. O sistema de arquivos (para ter acesso à árvore/memória do caminho atual)
    // 2. A lista de argumentos (strings após o comando)
    void executar(SistemaArquivos fs, List<String> args);
}

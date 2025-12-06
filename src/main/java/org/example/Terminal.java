package org.example;

import org.example.comandos.Comandos;
import org.example.comandos.CommandExecutor;
import org.example.sistema_de_arquivos.SistemaArquivos;

import java.util.*;

public class Terminal {

    private SistemaArquivos fs;
    private Map<String, CommandExecutor> mapaComandos;
    private Comandos implementacao;
    private boolean executando;

    public Terminal() {
        this.fs = new SistemaArquivos();
        this.implementacao = new Comandos();
        this.mapaComandos = new HashMap<>();
        this.executando = true;
        inicializarComandos();
    }

    private void inicializarComandos() {
        mapaComandos.put("mkdir", implementacao::mkdir);
        // mapaComandos.put("ls", implementacao::ls);
        // ...
        mapaComandos.put("cd", implementacao::cd);
        mapaComandos.put("pwd", implementacao::pwd);

        // Comando especial para sair do loop
        mapaComandos.put("exit", (fs, args) -> {
            this.executando = false;
            System.out.println("Encerrando simulação...");
        });
    }

    public void iniciar() {
        Scanner scanner = new Scanner(System.in);
        System.out.println("--- Terminal Simulado Iniciado ---");

        while (executando) {
            // Prompt estilo Linux: /home/user$
            System.out.print(fs.getCaminhoAtual() + "$ ");

            String linha = scanner.nextLine();
            if (linha.trim().isEmpty()) continue;

            processarEntrada(linha);
        }
        scanner.close();
    }

    private void processarEntrada(String entrada) {
        String[] tokens = entrada.trim().split("\\s+");
        String comando = tokens[0];

        List<String> args = new ArrayList<>();
        if (tokens.length > 1) {
            args.addAll(Arrays.asList(tokens).subList(1, tokens.length));
        }

        CommandExecutor executor = mapaComandos.get(comando);
        if (executor != null) {
            try {
                executor.executar(fs, args);
            } catch (Exception e) {
                System.out.println("Erro interno: " + e.getMessage());
            }
        } else {
            System.out.println("Comando não encontrado: " + comando);
        }
    }

    public static void main(String[] args) {
        new Terminal().iniciar();
    }
}
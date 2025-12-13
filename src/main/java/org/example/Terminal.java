package org.example;

import org.example.comandos.Comandos;
import org.example.comandos.CommandExecutor;
import org.example.sistema_de_arquivos.SistemaArquivos;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Terminal {

    private SistemaArquivos sistemaArquivos;
    private Map<String, CommandExecutor> mapaComandos;
    private Comandos implementacao;
    private boolean executando;

    public Terminal() {
        this.sistemaArquivos = new SistemaArquivos();
        this.implementacao = new Comandos();
        this.mapaComandos = new HashMap<>();
        this.executando = true;
        inicializarComandos();
    }

    private void inicializarComandos() {
        // Comando especial para sair do loop
        mapaComandos.put("exit", (fs, args) -> {
            this.executando = false;
            System.out.println("Encerrando simulação...");
        });

        mapaComandos.put("cd", implementacao::cd);
        mapaComandos.put("pwd", implementacao::pwd);
        mapaComandos.put("ls", implementacao::ls);
        mapaComandos.put("mkdir", implementacao::mkdir);
        mapaComandos.put("rm", implementacao::rm);
        mapaComandos.put("rmdir", implementacao::rm); // alias
        mapaComandos.put("touch", implementacao::touch);
        mapaComandos.put("tree", implementacao::tree);
        mapaComandos.put("clear", implementacao::clear);

        // Comando de Ajuda
        mapaComandos.put("help", (fs, args) -> {
            System.out.println("Comandos disponíveis: " + mapaComandos.keySet());
        });

    }

    public void iniciar() {
        Scanner scanner = new Scanner(System.in);
        System.out.println("=== Simulador de Terminal Linux ===");
        System.out.println("Digite 'help' para ver comandos ou 'exit' para sair.");

        while (executando) {
            // Prompt: /caminho/atual$
            System.out.print(sistemaArquivos.getCaminhoCompleto() + "$ ");

            String entrada = scanner.nextLine();
            if (entrada.trim().isEmpty()) continue;

            processarEntrada(entrada);
        }
        scanner.close();
    }

    private void processarEntrada(String entrada) {
        // 1. Captura: Conteúdo entre aspas ou sequências sem espaço
        List<String> tokens = new ArrayList<>();
        Matcher m = Pattern.compile("([^\"]\\S*|\".+?\")\\s*").matcher(entrada);

        while (m.find()) {
            String token = m.group(1);
            // Remove aspas circundantes se houver (ex: "texto" -> texto)
            if (token.startsWith("\"") && token.endsWith("\"")) {
                token = token.substring(1, token.length() - 1);
            }
            tokens.add(token);
        }

        if (tokens.isEmpty()) return;

        // 2. Separação
        String comando = tokens.getFirst();
        List<String> args = new ArrayList<>(tokens.subList(1, tokens.size()));

        // 3. Execução
        CommandExecutor executor = mapaComandos.get(comando);
        if (executor != null) {
            try {
                executor.executar(sistemaArquivos, args);
            } catch (Exception e) {
                System.out.println("Erro inesperado: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            System.out.println(comando + ": comando não encontrado");
        }
    }

    public static void main(String[] args) {
        new Terminal().iniciar();
    }
}
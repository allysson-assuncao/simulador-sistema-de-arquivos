package org.example.comandos;

import org.example.sistema_de_arquivos.SistemaArquivos;

import java.util.List;

// Classe que contém os comandos e seus comportamentos específicos.
public class Comandos {

    /*
    Todo comando deve ser do tipo object, receber parâmetros genéricos e ter um retorno, mesmo que seja nulo
     */

    public Object kill(String... strings) {
        System.out.println("Matando terminal. ");
        System.exit(0);
        return null;
    }

    // [cd <nome> | .. | /]
    public void cd(SistemaArquivos fs, List<String> args) {
        if (args.isEmpty()) return; // Talvez ir pra home
        String msg = fs.cd(args.getFirst());
        if (!msg.isEmpty()) System.out.println(msg);
    }

    // [pwd] - Extra, só para testar navegação
    public void pwd(SistemaArquivos fs, List<String> args) {
        System.out.println(fs.getCaminhoCompleto());
    }

    // [ls <caminho> -modificadores]
    public void ls(SistemaArquivos fs, List<String> args) {
        String alvo = args.isEmpty() ? null : args.get(0);
        System.out.println(fs.ls(alvo));
    }

    // [mkdir <nome>]
    public void mkdir(SistemaArquivos fs, List<String> args) {
        if (args.isEmpty()) {
            System.out.println("Uso: mkdir <nome_diretorio>");
            return;
        }
        // Executa e imprime o resultado
        System.out.println(fs.mkdir(args.getFirst()));
    }

    // [rm <nome/caminho> -modificadores]
    public void rm(SistemaArquivos fs, List<String> args) {
        if (args.isEmpty()) {
            System.out.println("Uso: rm <caminho>");
            return;
        }
        System.out.println(fs.rm(args.getFirst()));
    }

    // clear (simulação, só adiciona uns espaços)
    public void clear(SistemaArquivos fs, List<String> args) {
        for(int i = 0; i < 50; i++) System.out.println();
    }

}

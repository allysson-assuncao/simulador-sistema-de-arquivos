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
        if (args.isEmpty()) {
            // Comportamento padrão Linux sem args: vai para home
            System.out.println(fs.cd("/home/user"));
            return;
        }
        String msg = fs.cd(args.getFirst());
        if (!msg.isEmpty()) System.out.println(msg);
    }

    // [pwd] - Extra, só para testar navegação
    public void pwd(SistemaArquivos fs, List<String> args) {
        System.out.println(fs.getCaminhoCompleto());
    }

    // [ls <caminho> -modificadores] (flags tratadas: -a, -l, -la)
    public void ls(SistemaArquivos fs, List<String> args) {
        boolean mostrarOcultos = false;
        boolean formatoLongo = false;
        String caminho = null;

        for (String arg : args) {
            if (arg.startsWith("-")) {
                if (arg.contains("a")) mostrarOcultos = true;
                if (arg.contains("l")) formatoLongo = true;
            } else {
                caminho = arg; // Assume que o que não é flag, é caminho
            }
        }

        System.out.print(fs.ls(caminho, mostrarOcultos, formatoLongo));
    }

    // [touch <caminho>]
    public void tree(SistemaArquivos fs, List<String> args) {
        System.out.println(fs.tree(args.isEmpty() || args.getFirst().isEmpty() ?  "" : args.getFirst()));
    }

    // [cat <caminho>]
    public void cat(SistemaArquivos fs, List<String> args) {
        if (args.isEmpty()) {
            System.out.println("Uso: cat <arquivo>");
            return;
        }
        // Suporta múltiplos arquivos: cat a.txt b.txt
        for (String arg : args) {
            System.out.println(fs.cat(arg));
        }
    }

    // [history] Novo
    public void history(SistemaArquivos fs, List<String> args) {
        System.out.print(fs.getHistorico());
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

    // [touch <nome>]
    public void touch(SistemaArquivos fs, List<String> args) {
        if (args.isEmpty()) {
            System.out.println("Uso: touch <arquivo>");
            return;
        }
        System.out.println(fs.touch(args.getFirst()));
    }

    // clear (simulação, só adiciona uns espaços)
    public void clear(SistemaArquivos fs, List<String> args) {
        for(int i = 0; i < 50; i++) System.out.println();
    }

    //    Permissões e Propriedades:



    public void chmod(SistemaArquivos fs, List<String> args) {
        if (args.size() < 2) {
            System.out.println("Uso incorreto. Tente: chmod <codigo_octal> <caminho>");
            // chmod 777 arquivo.txt
            return;
        }
        // Pega os argumentos e chama o sistema
        String codigo = args.get(0);
        String caminho = args.get(1);

        System.out.println(fs.chmod(codigo, caminho));
    }

    // [chown <novo_dono> <arquivo>]
    public void chown(SistemaArquivos fs, List<String> args) {
        if (args.size() < 2) {
            System.out.println("Uso incorreto. Tente: chown <novo_usuario> <caminho>");
            // chown admin arquivo.txt
            return;
        }
        // Pega os argumentos e chama o sistema
        String usuario = args.get(0);
        String caminho = args.get(1);

        System.out.println(fs.chown(usuario, caminho));
    }

}

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
        if (args.isEmpty()) return; // talvez direcionar para home se o parametro nao for informado

        String resultado = fs.cd(args.get(0));
        if (!resultado.isEmpty()) {
            System.out.println(resultado);
        }
    }

    // [pwd] - Extra, só para testar navegação
    public void pwd(SistemaArquivos fs, List<String> args) {
        System.out.println(fs.getCaminhoAtual());
    }

    // [mkdir <nome>]
    public void mkdir(SistemaArquivos fs, List<String> args) {
        if (args.isEmpty()) {
            System.out.println("Uso: mkdir <nome_diretorio>");
            return;
        }
        // Executa e imprime o resultado
        System.out.println(fs.mkdir(args.get(0)));
    }

}

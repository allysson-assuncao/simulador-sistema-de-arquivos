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

    // [history]
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

    // [echo <texto >|>> arq>]
    public void echo(SistemaArquivos fs, List<String> args) {
        if (args.isEmpty()) {
            System.out.println(); // echo vazio imprime linha em branco
            return;
        }
        int indexRedirecionar = -1;
        boolean append = false;

        // Procura pelos operadores > ou >>
        for (int i = 0; i < args.size(); i++) {
            if (args.get(i).equals(">")) {
                indexRedirecionar = i;
                break;
            } else if (args.get(i).equals(">>")) {
                indexRedirecionar = i;
                append = true;
                break;
            }
        }

        if (indexRedirecionar != -1) {
            // Validação: Tem que ter arquivo depois do >
            if (indexRedirecionar + 1 >= args.size()) {
                System.out.println("Erro: Sintaxe inválida. Esperado arquivo após redirecionador.");
                return;
            }

            String arquivoDestino = args.get(indexRedirecionar + 1);

            // Junta tudo que vem antes do > como texto
            String texto = String.join(" ", args.subList(0, indexRedirecionar));

            // Chama o sistema para efetuar a escrita
            String erro = fs.escreverNoArquivo(arquivoDestino, texto, append);
            if (!erro.isEmpty()) System.out.println(erro);

        } else {
            // Apenas imprime o texto caso os operadores não tenham sido encontrados
            System.out.println(String.join(" ", args));
        }
    }

    // [rename <antigo> <novo>]
    public void rename(SistemaArquivos fs, List<String> args) {
        if (args.size() < 2) {
            System.out.println("Uso: rename <nome_antigo> <novo_nome>");
            return;
        }
        System.out.println(fs.rename(args.get(0), args.get(1)));
    }

    // [head <caminho> ou head -n 5 <arquivo>]
    public void head(SistemaArquivos fs, List<String> args) {
        tratarLeituraParcial(fs, args, true);
    }

    // [tail] tail <arquivo> ou tail -n 5 <arquivo>
    public void tail(SistemaArquivos fs, List<String> args) {
        tratarLeituraParcial(fs, args, false);
    }

    // Método auxiliar compartilhado para head e tail
    private void tratarLeituraParcial(SistemaArquivos fs, List<String> args, boolean isHead) {
        if (args.isEmpty()) return;

        int linhas = 10; // Padrão Linux caso o paraâmetro não tenha sido informado
        String arquivo;

        // Parsing simples de flag -n
        if (args.getFirst().equals("-n") && args.size() >= 3) {
            try {
                linhas = Integer.parseInt(args.get(1));
                arquivo = args.get(2);
            } catch (NumberFormatException e) {
                System.out.println("Erro: Número de linhas inválido.");
                return;
            }
        } else {
            arquivo = args.getFirst();
        }

        if (isHead) System.out.print(fs.head(arquivo, linhas));
        else System.out.print(fs.tail(arquivo, linhas));
    }

    // [wc] wc <arquivo>
    public void wc(SistemaArquivos fs, List<String> args) {
        if (args.isEmpty()) {
            System.out.println("Uso: wc <arquivo>");
            return;
        }
        System.out.println(fs.wc(args.getFirst()));
    }

    // [stat <caminho>]
    public void stat(SistemaArquivos fs, List<String> args) {
        if (args.isEmpty()) {
            System.out.println("Uso: stat <caminho>");
            return;
        }

        System.out.print(fs.stat(args.getFirst()));
    }

    // clear (simulação, só adiciona uns espaços)
    public void clear(SistemaArquivos fs, List<String> args) {
        for(int i = 0; i < 50; i++) System.out.println();
    }

    // Permissões e Propriedades:
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

    // [find <caminho> -name <nome>]
    public void find(SistemaArquivos fs, List<String> args) {
        if (args.size() < 3 || !args.get(1).equals("-name")) {
            System.out.println("Uso disponível: find <caminho> -name <nome>");
            return;
        }

        String caminho = args.get(0);
        String nome = args.get(2);

        System.out.print(fs.find(caminho, nome));
    }

    // [grep <termo> <arquivo>]
    public void grep(SistemaArquivos fs, List<String> args) {
        if (args.size() < 2) {
            System.out.println("Uso: grep <termo> <arquivo>");
            return;
        }

        String termo = args.get(0);
        String arquivo = args.get(1);

        System.out.print(fs.grep(termo, arquivo));
    }

    // [du <caminho>]
    public void du(SistemaArquivos fs, List<String> args) {
        if (args.isEmpty()) {
            System.out.println("Uso: du <caminho>");
            return;
        }

        System.out.print(fs.du(args.get(0)));
    }
}

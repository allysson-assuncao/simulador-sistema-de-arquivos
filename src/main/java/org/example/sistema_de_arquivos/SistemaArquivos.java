package org.example.sistema_de_arquivos;

//Classe responsável por gerenciar o sistema de arquivos.
public class SistemaArquivos {

    private Diretorio raiz;
    private Diretorio diretorioAtual;

    public SistemaArquivos() {
        // A raiz não tem pai (null)
        this.raiz = new Diretorio("/", null);
        this.diretorioAtual = this.raiz;
    }

    public String getCaminhoAtual() {
        if (this.diretorioAtual == this.raiz) return "/";

        // Reconstrói o caminho subindo a árvore
        StringBuilder path = new StringBuilder();
        Diretorio temp = this.diretorioAtual;
        while (temp != null && temp != this.raiz) {
            path.insert(0, "/" + temp.getNome());
            temp = temp.getPai();
        }
        return path.toString();
    }

    public Diretorio getDiretorioAtual() {
        return this.diretorioAtual;
    }

    // Lógica de mudar diretório (cd)
    public String cd(String caminho) {
        if (caminho.equals("..")) {
            if (diretorioAtual.getPai() != null) {
                diretorioAtual = diretorioAtual.getPai();
                return "";
            }
            return ""; // Já está na raiz, não faz nada
        }

        if (caminho.equals("/")) {
            diretorioAtual = raiz;
            return "";
        }

        NoSistema no = diretorioAtual.getFilho(caminho);
        if (no instanceof Diretorio) {
            diretorioAtual = (Diretorio) no;
            return "";
        } else if (no instanceof Arquivo) {
            return "Erro: '" + caminho + "' não é um diretório.";
        } else {
            return "Erro: Diretório não encontrado.";
        }
    }

    // Lógica de criar diretório (mkdir)
    public String mkdir(String nome) {
        if (diretorioAtual.getFilho(nome) != null) {
            return "Erro: Já existe um arquivo ou diretório com este nome.";
        }
        Diretorio novoDir = new Diretorio(nome, diretorioAtual);
        diretorioAtual.adicionarFilho(novoDir);
        return "Diretório '" + nome + "' criado.";
    }

}

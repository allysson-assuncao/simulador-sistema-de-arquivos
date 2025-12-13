package org.example.sistema_de_arquivos;

public abstract class NoSistema {
    protected String nome;
    protected Diretorio pai; // Referência direta ao objeto pai (para subir na árvore)

    public NoSistema(String nome, Diretorio pai) {
        // Validação de Integridade: Nome não pode conter '/'
        if (nome.contains("/")) {
            throw new IllegalArgumentException("O nome do arquivo/diretório não pode conter '/'.");
        }
        this.nome = nome;
        this.pai = pai;
    }

    public String getNome() { return nome; }
    public Diretorio getPai() { return pai; }

    public abstract String getTipo(); // Diretorio ou arquivo
}

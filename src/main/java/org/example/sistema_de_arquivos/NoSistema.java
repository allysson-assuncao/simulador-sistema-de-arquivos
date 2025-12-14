package org.example.sistema_de_arquivos;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public abstract class NoSistema {
    protected String nome;
    protected Diretorio pai; // Referência direta ao objeto pai (para subir na árvore)

    // Metadados para a exibição detalhada (ls -l)
    protected String permissoes;
    protected String dono;
    protected LocalDateTime dataModificacao;

    public NoSistema(String nome, Diretorio pai) {
        // Validação de Integridade: Nome não pode conter '/'
        if (nome.contains("/")) {
            throw new IllegalArgumentException("O nome do arquivo/diretório não pode conter '/'.");
        }
        this.nome = nome;
        this.pai = pai;
        this.dono = "user"; // Padrão
        this.dataModificacao = LocalDateTime.now();
    }

    public String getNome() { return nome; }
    public Diretorio getPai() { return pai; }
    public String getDataFormatada() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd HH:mm");
        return dataModificacao.format(formatter);
    }
    public String getPermissoes() { return permissoes; }
    public String getDono() { return dono; }

    public abstract String getTipo(); // Diretorio ou arquivo
    public abstract int getTamanho(); // Em bytes (varia entre arquivos e diretórios)
}

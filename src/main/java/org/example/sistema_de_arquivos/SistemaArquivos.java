package org.example.sistema_de_arquivos;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;
import java.util.Base64;
import java.util.zip.Deflater;
import java.util.zip.Inflater;
import java.io.ByteArrayOutputStream;

//Classe responsável por gerenciar o sistema de arquivos.
public class SistemaArquivos {

    private Diretorio raiz;
    private Diretorio diretorioAtual;
    private List<String> historicoComandos;
    private String usuarioLogado = "user";

    // Padrão Linux seguro: Letras, números, ponto, traço e underscore.
    private static final Pattern PADRAO_NOME = Pattern.compile("^[a-zA-Z0-9._-]+$");

    public SistemaArquivos() {
        // Raiz é um caso especial: nome "/" e pai null
        // Instancia direta para burlar a validação do construtor na raiz
        this.raiz = new Diretorio("root", null) {
            @Override
            public String getNome() {
                return "/";
            }
        };
        this.diretorioAtual = raiz;
        this.historicoComandos = new ArrayList<>();

        inicializarEstruturaPadrao();

        try {
            cd("/home/user");
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }

    public void registrarComando(String comando) {
        historicoComandos.add(comando);
    }

    private void inicializarEstruturaPadrao() {
        try {
            // 1. Diretórios da Raiz (Base Linux)
            mkdir("/home");
            mkdir("/lib");
            mkdir("/run");
            mkdir("/tmp");
            mkdir("/usr");
            mkdir("/var");
            mkdir("/etc");
            mkdir("/bin");

            // 2. Estrutura do Usuário
            mkdir("/home/user");
            mkdir("/home/user/Documents");
            mkdir("/home/user/Downloads");
            mkdir("/home/user/Desktop");
            mkdir("/home/user/Pictures");

            // 3. Subdiretórios de Sistema
            mkdir("/var/log");
            mkdir("/usr/bin");

            // 4. Arquivos Iniciais (usando touch)

            // Em Documents
            touch("/home/user/Documents/trabalho_so.txt");
            escreverNoArquivo("/home/user/Documents/trabalho_so.txt", "Rascunho do trabalho de Sistemas Operacionais\nData: 20/01/25");

            touch("/home/user/Documents/receita.txt");
            escreverNoArquivo("/home/user/Documents/receita.txt", "Ingredientes:\n- Leite\n- Ovos\n- Farinha");

            // Em Desktop
            touch("/home/user/Desktop/todo.list");
            escreverNoArquivo("/home/user/Desktop/todo.list", "1. Implementar comandos\n2. Testar cd\n3. Repetir");

            // Em Logs
            touch("/var/log/syslog");
            escreverNoArquivo("/var/log/syslog", "[INFO] Sistema de Arquivos Inicializado com Sucesso.");

        } catch (Exception e) {
            System.err.println("Erro ao inicializar estrutura padrão: " + e.getMessage());
        }
    }

    private void escreverNoArquivo(String caminho, String texto) {
        try {
            NoSistema no = resolverCaminho(caminho);
            if (no.isArquivo()) {
                ((Arquivo) no).setConteudo(texto);
            }
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }

    private void validarNome(String nome) throws IllegalArgumentException {
        if (nome == null || nome.isEmpty()) throw new IllegalArgumentException("Nome não pode ser vazio.");
        if (!PADRAO_NOME.matcher(nome).matches()) {
            throw new IllegalArgumentException("Nome inválido: '" + nome + "'. Use apenas letras, números, ponto (.), traço (-) ou underscore (_).");
        }
    }

    public Diretorio getDiretorioAtual() {
        return diretorioAtual;
    }

    // Método essencial do sistema: Retorna o Nó apontado pelo caminho usando recursividade e tratando casos específicos
    private NoSistema resolverCaminho(String caminho) throws Exception {
        if (caminho == null || caminho.isEmpty()) return diretorioAtual;

        // Atalho simples
        if (caminho.equals("/")) return raiz;
        if (caminho.equals(".")) return diretorioAtual;

        // 1. Decide por onde começar a busca a partir do primeiro caractere
        Diretorio atualNavegacao;
        if (caminho.startsWith("/")) {
            atualNavegacao = raiz; // Caminho Absoluto
            // Remove a primeira barra para evitar a criação de uma string vazia no split
            caminho = caminho.substring(1);
        } else {
            atualNavegacao = diretorioAtual; // Caminho Relativo
        }

        // 2. Pega cada parte do caminho entre as barras
        String[] partes = caminho.split("/");

        // 3. Caminha pela árvore
        for (String parte : partes) {
            // Ignora partes vazias (caso de barras duplas //) ou ponto (.)
            if (parte.isEmpty() || parte.equals(".")) {
                continue;
            } else if (parte.equals("..")) {
                if (atualNavegacao.getPai() != null) {
                    atualNavegacao = atualNavegacao.getPai();
                }
                // Se pai for null (raiz), continua na raiz
                continue;
            }
            // Tenta descer um nível
            NoSistema proximo = atualNavegacao.getFilho(parte);
            if (proximo == null) {
                throw new Exception("Caminho não encontrado: " + parte);
            }

            // Se tem um caminho no meio do caminho: erro
            if (proximo.isArquivo()) {
                if (parte.equals(partes[partes.length - 1])) {
                    return proximo;
                }
                throw new Exception("Erro: '" + parte + "' não é um diretório.");
            }

            // É um diretório, prossegue até o fim do caminho
            atualNavegacao = (Diretorio) proximo;

        }
        return atualNavegacao;
    }

    // Verifica se o usuário logado é dono do nó
    private boolean souDono(NoSistema no) {
        return no.getDono().equals(this.usuarioLogado);
    }

    // Verifica se o usuário tem a permissão no nó
    // modo: 'r', 'w' ou 'x'
    // souDono: true se o usuário é dono, false se for "outros"
    private boolean temPermissao(NoSistema no, char modo, boolean souDono) {
        // Se for root, tudo é permitido
        if (usuarioLogado.equals("root")) return true;

        // Exemplo: -rwxr--r--
        // Índices: 0123456789
        // Dono: 1,2,3 | Grupo: 4,5,6 | Outros: 7,8,9

        String perms = no.getPermissoes();
        int indiceBase = souDono ? 1 : 7;

        int indiceVerificacao = indiceBase;
        if (modo == 'w') indiceVerificacao += 1;
        else if (modo == 'x') indiceVerificacao += 2;

        return perms.charAt(indiceVerificacao) == modo;
    }

    // Metodo principal que une os dois
    private boolean verificarPermissao(NoSistema no, char modo) {
        boolean dono = souDono(no);
        return temPermissao(no, modo, dono);
    }

    // PWD (Exibe o caminho atual completo)
    public String getCaminhoCompleto() {
        if (diretorioAtual == raiz) return "/";

        StringBuilder sb = new StringBuilder();
        Diretorio temp = diretorioAtual;
        while (temp != raiz) {
            sb.insert(0, "/" + temp.getNome());
            temp = temp.getPai();
        }
        return sb.toString();
    }

    // CD (Navega pelo sistema de arquivos)
    public String cd(String caminho) {
        try {
            NoSistema alvo = resolverCaminho(caminho);
            if (!(alvo.isDiretorio())) {
                return "Erro: '" + alvo.getNome() + "' não é um diretório.";
            }
            if (!verificarPermissao(alvo, 'x')) {
                return "Permissão negada: Não é possível acessar '" + alvo.getNome() + "'";
            }
            this.diretorioAtual = (Diretorio) alvo;
            return "";
        } catch (Exception e) {
            return e.getMessage();
        }
    }

    // LS (Lista os diretórios e arquivos dentro de um diretório)
    public String ls(String caminhoOpcional, boolean mostrarOcultos, boolean formatoLongo) {
        try {
            Diretorio alvo = diretorioAtual;
            // Se passou caminho, resolve primeiro
            if (caminhoOpcional != null && !caminhoOpcional.isEmpty()) {
                NoSistema no = resolverCaminho(caminhoOpcional);
                // Se for diretório, precisa de Leitura para listar conteúdo
                if (no.isDiretorio()) {
                    if (!verificarPermissao(no, 'r')) {
                        return "Permissão negada: Não é possível ler o diretório '" + no.getNome() + "'";
                    }
                    alvo = (Diretorio) no;
                } else {
                    // Se for arquivo, não precisa de 'r' no arquivo para ver que ele existe,  apenas 'x' no diretório pai (que o resolverCaminho já atravessou)
                    return formatarSaidaLs(Collections.singletonList(no), formatoLongo);
                }
            }
            if (!verificarPermissao(alvo, 'r')) {
                return "Permissão negada: Não é possível listar o diretório atual.";
            }

            // Pega os filhos
            List<NoSistema> conteudo = new ArrayList<>(alvo.getFilhos().values());

            // Filtra ocultos (começam com .) se a flag -a não estiver ativa
            if (!mostrarOcultos) {
                conteudo.removeIf(no -> no.getNome().startsWith("."));
            }
            // Ordena alfabeticamente
            conteudo.sort(Comparator.comparing(NoSistema::getNome));

            return formatarSaidaLs(conteudo, formatoLongo);

        } catch (Exception e) {
            return "Erro: " + e.getMessage();
        }
    }

    // Auxiliar de formatação do LS
    private String formatarSaidaLs(List<NoSistema> nos, boolean longo) {
        if (nos.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();

        for (NoSistema no : nos) {
            if (longo) {
                // Formato: -rw-r--r-- 1 user 1024 Jan 01 12:00 arquivo.txt
                sb.append(String.format("%s 1 %s %5d %s %s\n",
                        no.getPermissoes(),
                        no.getDono(),
                        no.getTamanho(),
                        no.getDataFormatada(),
                        no.getNome()
                ));
            } else {
                // Formato simples
                sb.append(no.getNome()).append("  ");
            }
        }
        if (!longo) sb.append("\n");
        return sb.toString();
    }

    // Tree (Exibe a arvore de diretórios a partir de um caminho de forma recursivo)
    public String tree(String caminho, boolean mostrarOcultos) {
        StringBuilder sb = new StringBuilder();
        sb.append(".\n");
        if (caminho.isEmpty()) {
            listarRecursivo(diretorioAtual, "", sb, mostrarOcultos);
        } else {
            try {
                NoSistema no = resolverCaminho(caminho);
                if (no.isDiretorio()) {
                    listarRecursivo((Diretorio) no, "", sb, mostrarOcultos);
                }
            } catch (Exception e) {
                listarRecursivo(diretorioAtual, "", sb, mostrarOcultos);
            }
        }
        return sb.toString();
    }

    private void listarRecursivo(Diretorio dir, String prefixo, StringBuilder sb, boolean mostrarOcultos) {
        if (!verificarPermissao(dir, 'r')) {
            return;
        }
        List<NoSistema> filhos = new ArrayList<>(dir.getFilhos().values());
        // Ordenar para facilitar a leitura e identificação
        filhos.sort(Comparator.comparing(NoSistema::getNome));

        // [1] Filtra ocultos
        if (!mostrarOcultos) {
            filhos.removeIf(no -> no.getNome().startsWith("."));
        }

        filhos.sort(Comparator.comparing(NoSistema::getNome));

        for (int i = 0; i < filhos.size(); i++) {
            NoSistema no = filhos.get(i);
            boolean isLast = (i == filhos.size() - 1);

            sb.append(prefixo);
            sb.append(isLast ? "└── " : "├── ");
            sb.append(no.getNome());

            // Verifica se é diretório para continuar descendo
            if (no.isDiretorio()) {
                Diretorio subDir = (Diretorio) no;

                // [2] Check antecipado: Se não tem permissão, avisa na frente do nome
                if (!verificarPermissao(subDir, 'r')) {
                    sb.append(" [Sem Permissão]");
                    sb.append("\n"); // Fecha a linha
                    // Não chama recursivo
                } else {
                    sb.append("\n"); // Fecha a linha normal
                    // Chama recursivo
                    listarRecursivo(subDir, prefixo + (isLast ? "    " : "│   "), sb, mostrarOcultos);
                }
            } else {
                sb.append("\n");
            }
        }
    }

    // CAT (Exibe o conteúdo de arquivos)
    public String cat(String caminho) {
        try {
            NoSistema no = resolverCaminho(caminho);
            if (no.isDiretorio()) return "cat: " + no.getNome() + ": É um diretório";
            if (!verificarPermissao(no, 'r')) {
                return "Permissão negada: Ler '" + no.getNome() + "'";
            }
            return ((Arquivo) no).getConteudo();
        } catch (Exception e) {
            return "cat: " + e.getMessage();
        }
    }

    // HISTORY (Exibe o histórico dos comandos executados)
    public String getHistorico() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < historicoComandos.size(); i++) {
            sb.append(i + 1).append("  ").append(historicoComandos.get(i)).append("\n");
        }
        return sb.toString();
    }

    // MKDIR (Suporta "mkdir pasta" ou "mkdir /a/b/pasta")
    public String mkdir(String caminho) {
        try {
            // Tratamento para evitar a criação de pastas vazias
            if (caminho.endsWith("/")) {
                caminho = caminho.substring(0, caminho.length() - 1);
            }

            // Separa o caminho do pai e o nome do novo diretório
            String nomeNovoDir;
            Diretorio paiAlvo;

            int indiceUltimaBarra = caminho.lastIndexOf('/');

            if (indiceUltimaBarra == -1) {
                // Caso 1: Caminho relativo simples -> "pasta"
                nomeNovoDir = caminho;
                paiAlvo = diretorioAtual;
            } else {
                // Caso 2: Caminho composto -> "/pai/filho"
                String caminhoPai = caminho.substring(0, indiceUltimaBarra);
                nomeNovoDir = caminho.substring(indiceUltimaBarra + 1);

                // Se o pai for vazio, significa que é filho da raiz (ex: "/home")
                if (caminhoPai.isEmpty()) {
                    caminhoPai = "/";
                }

                // Busca o objeto do pai
                NoSistema noPai = resolverCaminho(caminhoPai);

                if (noPai == null) return "Erro: Caminho base não encontrado.";
                if (!(noPai.isDiretorio())) return "Erro: '" + caminhoPai + "' não é um diretório.";

                paiAlvo = (Diretorio) noPai;
            }

            validarNome(nomeNovoDir);

            if (!verificarPermissao(paiAlvo, 'w')) {
                return "Permissão negada: Não é possível criar '" + nomeNovoDir + "' em '" + paiAlvo.getNome() + "'.";
            }

            // Validação: Já existe?
            if (paiAlvo.getFilho(nomeNovoDir) != null) {
                return "Erro: Já existe algo com o nome '" + nomeNovoDir + "'.";
            }

            // Criação e ligação
            Diretorio novo = new Diretorio(nomeNovoDir, paiAlvo);
            novo.setDono(this.usuarioLogado);
            paiAlvo.adicionarFilho(novo);
            return "Diretório '" + nomeNovoDir + "' criado com sucesso.";

        } catch (IllegalArgumentException e) {
            return "Erro: " + e.getMessage();
        } catch (Exception e) {
            return "Erro ao criar diretório: " + e.getMessage();
        }
    }

    // RM (Remove arquivos ou diretórios vazios)
    public String rm(String caminho, boolean recursivo) {
        try {
            NoSistema alvo = resolverCaminho(caminho);

            // 1. Proteções Básicas
            if (alvo == raiz) return "Erro: Não é possível remover a raiz.";

            // Proteção Extra: Não deixar remover o diretório onde o usuário está (ou um ancestral dele)
            if (isAncestral(alvo, diretorioAtual)) {
                return "Erro: Não é possível remover o diretório atual ou um de seus pais enquanto você está dentro dele.";
            }

            Diretorio pai = alvo.getPai();

            // 2. Verificação de Permissão (Precisa de escrita no PAI)
            if (!verificarPermissao(pai, 'w')) {
                return "Permissão negada: Não é possível remover '" + alvo.getNome() + "' (sem permissão de escrita no diretório pai).";
            }

            // 3. Lógica de Diretório e Recursividade
            if (alvo.isDiretorio()) {
                Diretorio dirAlvo = (Diretorio) alvo;

                // Se tem filhos e NÃO foi passado -r/-rf
                if (dirAlvo.temFilhos() && !recursivo) {
                    return "Erro: O diretório '" + alvo.getNome() + "' não está vazio. (Use -rf para forçar recursividade)";
                }
                // Caso contrário segue e remove os filhos internos
            }

            // 4. Remoção
            pai.removerFilho(alvo.getNome());
            return "Removido: " + alvo.getNome();

        } catch (Exception e) {
            return "Erro ao remover: " + e.getMessage();
        }
    }

    // Método auxiliar simples para verificar se 'supostoPai' faz parte do caminho do 'filho'
    private boolean isAncestral(NoSistema supostoAncestral, NoSistema filho) {
        NoSistema temp = filho;
        while (temp != null) {
            if (temp == supostoAncestral) return true;
            temp = temp.getPai();
        }
        return false;
    }

    // TOUCH (Cria um arquivo vazio)
    public String touch(String caminho) {
        try {
            // Resolve o pai (diretório onde o arquivo ficará)
            String nomeArquivo;
            Diretorio paiAlvo;

            int ultimaBarra = caminho.lastIndexOf('/');
            if (ultimaBarra == -1) {
                nomeArquivo = caminho;
                paiAlvo = diretorioAtual;
            } else {
                String caminhoPai = caminho.substring(0, ultimaBarra);
                nomeArquivo = caminho.substring(ultimaBarra + 1);
                if (caminhoPai.isEmpty()) caminhoPai = "/";

                NoSistema noPai = resolverCaminho(caminhoPai);
                if (!(noPai.isDiretorio())) return "Erro: Caminho inválido.";
                paiAlvo = (Diretorio) noPai;
            }

            validarNome(nomeArquivo);

            if (!verificarPermissao(paiAlvo, 'w')) {
                return "Permissão negada: Criar arquivo em '" + paiAlvo.getNome() + "'.";
            }

            // Verifica se já existe
            NoSistema existente = paiAlvo.getFilho(nomeArquivo);
            if (existente != null) {
                if (existente.isDiretorio()) return "Erro: Já existe um diretório com esse nome.";
                // Simulando a atualização do timestamp
                existente.setDono(this.usuarioLogado);
                return "Arquivo '" + nomeArquivo + "' atualizado.";
            }

            // Criação do arquivo
            Arquivo novoArq = new Arquivo(nomeArquivo, paiAlvo);
            novoArq.setDono(this.usuarioLogado);
            paiAlvo.adicionarFilho(novoArq);
            return "Arquivo '" + nomeArquivo + "' criado.";

        } catch (IllegalArgumentException e) {
            return "Nome inválido: " + e.getMessage();
        } catch (Exception e) {
            return "Erro: " + e.getMessage();
        }
    }

    // ECHO (Sobrescreve > ou adiciona >> conteúdo em arquivos)
    public String escreverNoArquivo(String caminho, String texto, boolean append) {
        try {
            // 1. Resolver o arquivo ou o diretório pai (caso o arquivo não exista)
            NoSistema noAlvo = null;
            try {
                noAlvo = resolverCaminho(caminho);
            } catch (Exception e) {
                // Caso o arquivo não existe, tentamos cria-lo no diretório pai encontrado
            }

            Arquivo arquivo = null;
            if (noAlvo != null) {
                // Arquivo existente: Precisa de permissão de escrita no arquivo
                if (!verificarPermissao(noAlvo, 'w')) {
                    return "Permissão negada: Escrever em '" + noAlvo.getNome() + "'";
                }
                if (noAlvo.isDiretorio()) return "Erro: É um diretório.";
                arquivo = (Arquivo) noAlvo;

                Arquivo arq = (Arquivo) noAlvo;
                if (append) arq.appendConteudo(texto);
                else arq.setConteudo(texto);

            } else {
                // Arquivo novo: Precisa de permissão de Escrita no diretório PAI
                String caminhoPai = "/";
                int indiceUltimaBarra = caminho.lastIndexOf('/');
                if (indiceUltimaBarra != -1) caminhoPai = caminho.substring(0, indiceUltimaBarra);
                if (caminhoPai.isEmpty()) caminhoPai = "/";

                NoSistema pai = resolverCaminho(caminhoPai);
                if (!verificarPermissao(pai, 'w')) {
                    return "Permissão negada: Criar arquivo em '" + pai.getNome() + "'";
                }
                // Não existe, encontra o pai e cria
                String res = touch(caminho);
                if (res.startsWith("Erro")) return res; // Falha ao criar com touch
                arquivo = (Arquivo) resolverCaminho(caminho);
            }
            return ""; // Sucesso (sem mensagem de erro)

        } catch (Exception e) {
            return "Erro ao escrever: " + e.getMessage();
        }
    }

    // RENAME (Renomeia arquivos ou diretórios)
    public String rename(String nomeAntigo, String novoNome) {
        try {
            // Validação simples do novo nome
            if (novoNome.contains("/")) return "Erro: O novo nome não pode conter barras (use mv para mover).";

            NoSistema alvo = resolverCaminho(nomeAntigo);
            if (alvo == raiz) return "Erro: Não é possível renomear a raiz.";

            Diretorio pai = alvo.getPai();

            validarNome(novoNome);

            // Verifica se o nome já existe no diretório pai
            if (pai.getFilho(novoNome) != null) {
                return "Erro: Já existe um arquivo/diretório com o nome '" + novoNome + "'.";
            }

            // Remove a referência do arquivo no map a partir da chave antiga
            pai.removerFilho(alvo.getNome());

            // Atualiza o nome interno do objeto
            alvo.setNome(novoNome);
            pai.adicionarFilho(alvo);

            return "Renomeado de '" + nomeAntigo + "' para '" + novoNome + "'.";

        } catch (IllegalArgumentException e) {
            return "Erro: " + e.getMessage();
        } catch (Exception e) {
            return "Erro ao renomear: " + e.getMessage();
        }
    }

    // HEAD (Exibe as primeiras N linhas de um arquivo)
    public String head(String caminho, int linhas) {
        try {
            Arquivo arq = obterArquivoTexto(caminho);
            String[] todasLinhas = arq.getConteudo().split("\n");

            StringBuilder sb = new StringBuilder();
            int limite = Math.min(linhas, todasLinhas.length);

            for (int i = 0; i < limite; i++) {
                sb.append(todasLinhas[i]).append("\n");
            }
            return sb.toString();

        } catch (Exception e) {
            return e.getMessage();
        }
    }

    // TAIL (Exibe as últimas N linhas de um arquivo)
    public String tail(String caminho, int linhas) {
        try {
            Arquivo arq = obterArquivoTexto(caminho);
            String[] todasLinhas = arq.getConteudo().split("\n");

            StringBuilder sb = new StringBuilder();
            int inicio = Math.max(0, todasLinhas.length - linhas);

            for (int i = inicio; i < todasLinhas.length; i++) {
                sb.append(todasLinhas[i]).append("\n");
            }
            return sb.toString();

        } catch (Exception e) {
            return e.getMessage();
        }
    }

    // WC (Conta as palavras de um arquivo)
    public String wc(String caminho) {
        try {
            Arquivo arq = obterArquivoTexto(caminho);
            String conteudo = arq.getConteudo();

            int numLinhas = conteudo.isEmpty() ? 0 : conteudo.split("\n").length;
            int numBytes = conteudo.length();

            // Contar palavras (split por whitespace)
            int numPalavras = 0;
            if (!conteudo.trim().isEmpty()) {
                numPalavras = conteudo.trim().split("\\s+").length;
            }

            return String.format("%d %d %d %s", numLinhas, numPalavras, numBytes, arq.getNome());

        } catch (Exception e) {
            return e.getMessage();
        }
    }

    // Método auxiliar para validar se é arquivo
    private Arquivo obterArquivoTexto(String caminho) throws Exception {
        NoSistema no = resolverCaminho(caminho);
        if (no.isDiretorio()) throw new Exception("Erro: '" + caminho + "' é um diretório.");
        return (Arquivo) no;
    }

    // PERMISSÕES E PROPRIEDADES
    public String chmod(String codigoOctal, String caminho) {
        try {
            // Validação do formato (tem que ser 3 números de 0 a 7)
            if (codigoOctal.length() != 3 || !codigoOctal.matches("[0-7]{3}")) {
                return "Erro: Formato inválido. Use 3 dígitos octais (ex: 755, 777).";
            }

            // Busca o alvo
            NoSistema alvo = resolverCaminho(caminho);

            //  Monta a nova string de permissão
            StringBuilder sb = new StringBuilder();

            // O primeiro caractere indica se é diretório (d) ou arquivo (-)
            if (alvo.isDiretorio()) {
                sb.append("d");
            } else {
                sb.append("-");
            }

            // Converte os 3 números para letras
            sb.append(converterPermissaoOctal(codigoOctal.charAt(0))); // Dono
            sb.append(converterPermissaoOctal(codigoOctal.charAt(1))); // Grupo
            sb.append(converterPermissaoOctal(codigoOctal.charAt(2))); // Outros

            // Aplica a mudança
            alvo.setPermissoes(sb.toString());
            return "Permissões de '" + alvo.getNome() + "' alteradas para " + sb.toString();

        } catch (Exception e) {
            return "Erro ao executar chmod: " + e.getMessage();
        }
    }

    public String chown(String novoDono, String caminho) {
        try {
            // Busca o alvo
            NoSistema alvo = resolverCaminho(caminho);

            if (novoDono == null || novoDono.trim().isEmpty()) {
                return "Erro: Nome de usuário inválido.";
            }

            // Aplica a mudança
            String antigo = alvo.getDono();
            alvo.setDono(novoDono);

            return "Dono de '" + alvo.getNome() + "' alterado: " + antigo + " -> " + novoDono;

        } catch (Exception e) {
            return "Erro ao executar chown: " + e.getMessage();
        }
    }

    // Método Auxiliar: Converte número ('7') para string ("rwx")
    private String converterPermissaoOctal(char numero) {
        switch (numero) {
            case '0':
                return "---";
            case '1':
                return "--x";
            case '2':
                return "-w-";
            case '3':
                return "-wx";
            case '4':
                return "r--";
            case '5':
                return "r-x";
            case '6':
                return "rw-";
            case '7':
                return "rwx";
            default:
                return "---";
        }
    }

    // FIND
    public String find(String caminho, String nome) {
        try {
            NoSistema inicio = resolverCaminho(caminho);

            if (!inicio.isDiretorio()) {
                return "find: '" + caminho + "': Não é um diretório\n";
            }

            StringBuilder resultado = new StringBuilder();
            buscarRecursivo(inicio, nome, resultado);
            return resultado.toString();

        } catch (Exception e) {
            return "find: caminho inválido\n";
        }
    }

    // FIND - Metodo auxiliar
    private void buscarRecursivo(NoSistema no, String nomeBuscado, StringBuilder resultado) {
        boolean dono = souDono(no);
        boolean podeLer = temPermissao(no, 'r', dono);

        if (no.getNome().equals(nomeBuscado)) {
            resultado.append(montarCaminho(no)).append("\n");
        }

        if (no.isDiretorio()) {
            if (podeLer) {
                Diretorio dir = (Diretorio) no;
                for (NoSistema filho : dir.getFilhos().values()) {
                    buscarRecursivo(filho, nomeBuscado, resultado);
                }
            } else {
                resultado.append("find: '")
                        .append(montarCaminho(no))
                        .append("': Permissão negada\n");
            }
        }
    }

    // Metodo auxiliar: monta caminho do nó até o topo
    private String montarCaminho(NoSistema no) {
        if (no == null || no.getPai() == null) return "/";

        String pai = montarCaminho(no.getPai());
        return (pai.equals("/") ? "" : pai) + "/" + no.getNome();
    }

    // GREP
    public String grep(String termo, String caminho) {
        try {
            Arquivo arq = obterArquivoTexto(caminho);
            String conteudo = arq.getConteudo();

            StringBuilder sb = new StringBuilder();
            String[] linhas = conteudo.split("\n");

            for (String linha : linhas) {
                if (linha.contains(termo)) {
                    sb.append(linha).append("\n");
                }
            }

            return sb.toString();

        } catch (Exception e) {
            return "grep: " + e.getMessage() + "\n";
        }
    }

    // STAT
    public String stat(String caminho) {
        try {
            NoSistema no = resolverCaminho(caminho);

            StringBuilder sb = new StringBuilder();

            sb.append("  File: ").append(no.getNome()).append("\n");
            sb.append("  Path: ").append(montarCaminho(no)).append("\n");

            if (no.isDiretorio()) {
                sb.append("  Type: Directory\n");
                sb.append("  Size: 4096 bytes\n"); // tamanho fixo do diretório para fins didáticos
            } else {
                sb.append("  Type: Regular File\n");
                sb.append("  Size: ").append(no.getTamanho()).append(" bytes\n");
            }

            sb.append("  Owner: ").append(no.getDono()).append("\n");
            sb.append("  Access: ").append(no.getPermissoes()).append("\n");
            sb.append("  Modify: ").append(no.getDataFormatada()).append("\n");

            return sb.toString();

        } catch (Exception e) {
            return "stat: " + e.getMessage() + "\n";
        }
    }

    // DU
    public String du(String caminho) {
        try {
            String alvo = (caminho == null || caminho.isEmpty()) ? "." : caminho;
            NoSistema no = resolverCaminho(alvo);

            if (!verificarPermissao(no, 'r')) {
                return "du: permissão negada para '" + alvo + "'\n";
            }

            StringBuilder sb = new StringBuilder();
            gerarSaidaDuRecursivo(no, sb, alvo);
            return sb.toString();
        } catch (Exception e) {
            return "du: " + e.getMessage() + "\n";
        }
    }

    // DU: Metodo auxiliar para imprimir diretorios recursivamente
    private void gerarSaidaDuRecursivo(NoSistema no, StringBuilder sb, String caminhoExibicao) {
        if (no instanceof Diretorio) {
            if (verificarPermissao(no, 'r') && verificarPermissao(no, 'x')) {
                Diretorio dir = (Diretorio) no;

                for (NoSistema filho : dir.getFilhos().values()) {
                    if (filho.isDiretorio()) {
                        String separador = caminhoExibicao.endsWith("/") ? "" : "/";
                        String caminhoFilho = caminhoExibicao + separador + filho.getNome();
                        gerarSaidaDuRecursivo(filho, sb, caminhoFilho);
                    }
                }
            } else {
                sb.append("du: não foi possível ler diretório '").append(caminhoExibicao).append("': Permissão negada\n");
            }

            sb.append(no.getTamanho()).append("\t").append(caminhoExibicao).append("\n");
        } else if (no.isArquivo()) {
            sb.append(no.getTamanho()).append("\t").append(caminhoExibicao).append("\n");
        }
    }

    // cp Copia arquivos ou diretórios de um lugar para outro.
    public String cp(String origem, String destino) {
        try {
            // Busca a origem
            NoSistema noOrigem = resolverCaminho(origem);

            // Busca o destino
            NoSistema noDestino = resolverCaminho(destino);

            if (!noDestino.isDiretorio()) {
                //Se o destino existe mas é um arquivo não podemos copiar para dentro dele
                return "Erro: O destino '" + destino + "' não é um diretório.";
            }

            Diretorio dirDestino = (Diretorio) noDestino;

            // Verifica permissões
            if (!verificarPermissao(noOrigem, 'r')) return "Permissão negada: Ler origem.";
            if (!verificarPermissao(dirDestino, 'w')) return "Permissão negada: Escrever no destino.";

            // Copia
            copiarRecursivo(noOrigem, dirDestino, noOrigem.getNome());

            return "Sucesso: Copiado para '" + dirDestino.getNome() + "/" + noOrigem.getNome() + "'";

        } catch (Exception e) {
            return "Erro ao copiar: " + e.getMessage();
        }
    }


    // Realiza a Cópia Profunda
    private void copiarRecursivo(NoSistema original, Diretorio paiDestino, String novoNome) {
        if (original.isArquivo()) {
            Arquivo originalArq = (Arquivo) original; // Cast para acessar métodos de Arquivo

            // Cria um novo objeto na memória
            Arquivo copia = new Arquivo(novoNome, paiDestino);

            // Copia os dados manuamente
            copia.setConteudo(originalArq.getConteudo()); // Clona o texto
            copia.setPermissoes(originalArq.getPermissoes()); // Clona permissões
            copia.setDono(this.usuarioLogado); // O dono da cópia é quem está copiando (eu), não o dono original

            // Adiciona a cópia na lista de filhos do diretório de destino
            paiDestino.adicionarFilho(copia);

        } else {
            // Diretório
            Diretorio originalDir = (Diretorio) original;

            // Cria a nova pasta no destino
            Diretorio copiaDir = new Diretorio(novoNome, paiDestino);

            // Copia metadados
            copiaDir.setPermissoes(originalDir.getPermissoes());
            copiaDir.setDono(this.usuarioLogado);

            // Adiciona a nova pasta no destino
            paiDestino.adicionarFilho(copiaDir);

            // Percorre todos os filhos da pasta original
            for (NoSistema filho : originalDir.getFilhos().values()) {
                // Chama para cada filho.
                copiarRecursivo(filho, copiaDir, filho.getNome());
            }
        }
    }

    // MV
    public String mv(String origem, String destino) {
        try {
            // Busca quem vamos mover
            NoSistema noOrigem = resolverCaminho(origem);

            // Não podemos mover a raiz do sistema
            if (noOrigem == raiz) return "Erro: Não é possível mover o diretório raiz.";

            // Busca para onde vamos levar
            NoSistema noDestino = resolverCaminho(destino);

            if (!noDestino.isDiretorio()) {
                return "Erro: O destino '" + destino + "' não é um diretório válido.";
            }

            Diretorio dirDestino = (Diretorio) noDestino;
            Diretorio paiAntigo = noOrigem.getPai();

            // Verificações de Permissão
            if (!verificarPermissao(paiAntigo, 'w')) {
                return "Permissão negada: Não pode remover de '" + paiAntigo.getNome() + "'";
            }
            if (!verificarPermissao(dirDestino, 'w')) {
                return "Permissão negada: Não pode mover para '" + dirDestino.getNome() + "'";
            }

            // Verificação de Colisão
            if (dirDestino.getFilho(noOrigem.getNome()) != null) {
                return "Erro: Já existe um arquivo/diretório chamado '" + noOrigem.getNome() + "' no destino.";
            }

            // Remove da lista do pai antigo
            paiAntigo.removerFilho(noOrigem.getNome());

            // Atualiza a referência de pai dentro do objeto
            noOrigem.pai = dirDestino;

            // Adiciona na lista do novo pai
            dirDestino.adicionarFilho(noOrigem);

            return "Sucesso: '" + noOrigem.getNome() + "' movido para '" + dirDestino.getNome() + "'";

        } catch (Exception e) {
            return "Erro ao mover: " + e.getMessage();
        }
    }

    // zip
    public String zip(String nomeZip, String caminhoAlvo) {
        try {

            // Se o usuário não digitar .zip
            if (!nomeZip.endsWith(".zip")) {
                nomeZip += ".zip";
            }


            // O que compactar?
            NoSistema alvo = resolverCaminho(caminhoAlvo);

            // Preparação do Conteúdo
            StringBuilder dadosZip = new StringBuilder();

            // Cabeçalho do arquivo
            dadosZip.append("ARQUIVO ZIPADO\n");

            /*
             "compactarRecursivo" vai encher o StringBuilder com os dados da árvore
             O segundo parâmetro "" é o prefixo do caminho (começa vazio)
             */

            compactarRecursivo(alvo, "", dadosZip);

            // Criação do Arquivo ZIP no sistema
            String resultadoTouch = touch(nomeZip);

            // Se o touch der erro
            if (resultadoTouch.startsWith("Erro") || resultadoTouch.startsWith("Permissão")) {
                return resultadoTouch;
            }

            // Gravação dos dados
            escreverNoArquivo(nomeZip, dadosZip.toString());

            return "Sucesso: Arquivo '" + nomeZip + "' criado com o conteúdo de '" + alvo.getNome() + "'";

        } catch (Exception e) {
            return "Erro ao zipar: " + e.getMessage();
        }
    }


    //Compactar recursivo
    private void compactarRecursivo(NoSistema no, String caminhoRelativo, StringBuilder sb) {
        String pathAtual = caminhoRelativo.isEmpty() ? no.getNome() : caminhoRelativo + "/" + no.getNome();

        // Recupera metadados
        String perms = no.getPermissoes();
        String dono = no.getDono();

        if (no.isArquivo()) {
            Arquivo arq = (Arquivo) no;

            // Usa a compressão REAL (Deflate+Base64) que fizemos antes
            String conteudoComprimido = comprimir(arq.getConteudo());

            // NOVO FORMATO: FILE | CAMINHO | PERMS | DONO | DADOS
            sb.append("FILE|")
                    .append(pathAtual).append("|")
                    .append(perms).append("|")
                    .append(dono).append("|")
                    .append(conteudoComprimido).append("\n");

        } else {
            Diretorio dir = (Diretorio) no;

            // NOVO FORMATO: DIR | CAMINHO | PERMS | DONO
            sb.append("DIR|")
                    .append(pathAtual).append("|")
                    .append(perms).append("|")
                    .append(dono).append("\n");

            for(NoSistema filho : dir.getFilhos().values()) {
                compactarRecursivo(filho, pathAtual, sb);
            }
        }
    }

    //Unzip
    public String unzip(String caminhoZip) {
        try {
            Arquivo zipFile = obterArquivoTexto(caminhoZip);
            String conteudoTotal = zipFile.getConteudo();
            String[] linhas = conteudoTotal.split("\n");

            if (linhas.length == 0 || !linhas[0].equals("ARQUIVO ZIPADO")) {
                return "Erro: Arquivo corrompido ou formato inválido.";
            }

            int itensProcessados = 0;

            for (int i = 1; i < linhas.length; i++) {
                String linha = linhas[i];
                if (linha.trim().isEmpty()) continue;

                String[] partes = linha.split("\\|");

                // Validação, FILE precisa de 5 partes, DIR precisa de 4
                if (partes.length < 4) continue;

                String tipo = partes[0];
                String caminhoRelativo = partes[1];
                String perms = partes[2];
                String dono = partes[3];

                if (tipo.equals("DIR")) {
                    // Cria a pasta
                    this.mkdir(caminhoRelativo);
                    // Restaura permissões
                    aplicarMetadados(caminhoRelativo, perms, dono);

                } else if (tipo.equals("FILE")) {
                    // Cria o arquivo
                    String resultado = this.touch(caminhoRelativo);

                    if (!resultado.startsWith("Erro")) {
                        // Restaura permissões
                        aplicarMetadados(caminhoRelativo, perms, dono);

                        if (partes.length > 4) {
                            String conteudoComprimido = partes[4];
                            // Descomprime
                            String conteudoOriginal = descomprimir(conteudoComprimido);
                            // Grava conteúdo
                            this.escreverNoArquivo(caminhoRelativo, conteudoOriginal, false);
                        }
                    }
                }
                itensProcessados++;
            }

            return "Sucesso: " + itensProcessados + " itens restaurados com metadados.";

        } catch (Exception e) {
            return "Erro no unzip: " + e.getMessage();
        }
    }

    // Métod Auxiliar
    private void aplicarMetadados(String caminho, String perms, String dono) {
        try {
            NoSistema no = resolverCaminho(caminho);
            if (no != null) {
                no.setPermissoes(perms);
                no.setDono(dono);
            }
        } catch (Exception e) {
            // Ignora silenciosamente se falhar ao aplicar metadados, pois o arquivo já foi criado
        }
    }

    /**
     * Deflate + Base64
     * Usa o algoritmo padrão do ZIP (zlib) e codifica em texto legível.
     */

    private String comprimir(String texto) {
        if (texto == null || texto.isEmpty()) return "";

        try {
            // Converte o texto para bytes
            byte[] entrada = texto.getBytes("UTF-8");

            // Configura o compressor
            Deflater deflater = new Deflater();
            deflater.setLevel(Deflater.BEST_COMPRESSION);
            deflater.setInput(entrada);
            deflater.finish();

            // Buffer para receber os dados comprimidos
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream(entrada.length);
            byte[] buffer = new byte[1024];

            while (!deflater.finished()) {
                int count = deflater.deflate(buffer);
                outputStream.write(buffer, 0, count);
            }
            outputStream.close();

            byte[] dadosComprimidos = outputStream.toByteArray();

            // Converte binário feio para Texto Base64 (para salvar no seu sistema)
            return Base64.getEncoder().encodeToString(dadosComprimidos);

        } catch (Exception e) {
            return "Erro na compressão: " + e.getMessage();
        }
    }

    /**
     * Descompactação
     */
    private String descomprimir(String textoComprimidoBase64) {
        if (textoComprimidoBase64 == null || textoComprimidoBase64.isEmpty()) return "";

        try {
            // Decodifica o texto Base64 de volta para binário
            byte[] entrada = Base64.getDecoder().decode(textoComprimidoBase64);

            // Configura o descompressor
            Inflater inflater = new Inflater();
            inflater.setInput(entrada);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream(entrada.length);
            byte[] buffer = new byte[1024];

            while (!inflater.finished()) {
                int count = inflater.inflate(buffer);
                outputStream.write(buffer, 0, count);
            }
            outputStream.close();

            // Converte os bytes de volta para String
            return new String(outputStream.toByteArray(), "UTF-8");

        } catch (Exception e) {
            // Se der erro (ex: tentar unzipar algo que não é zip), retorna o original ou erro
            return "Erro na descompressão: Arquivo corrompido ou formato inválido.";
        }
    }
}

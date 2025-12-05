package org.example;

import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

//Classe responsável por gerenciar o sistema de arquivos.
public class SistemaArquivo {

    //Apenas para teste de exibição.
    private String diretorioAtual = "home/";


    /*
    O construtor será responsável por carregar os comandos(por enquanto.)
     */
    public SistemaArquivo() {
       
        /*
       Crio uma variável Map, chamada comandos e chamo uma função que carrega os comandos. 
        */
        Map<String, Runnable> comandos = carregarComandos(); 
       
        //Chamo a lógica do terminal
        terminal(comandos);
    }

    /*
    Aqui criamos os comandos. função separada a fim de manter ordem no código.
     */
    private Map<String, Runnable> carregarComandos() {
        // Instancia a classe 'Comandos', que contém a lógica real (o que o 'cd' ou 'kill' fazem)
        Comandos comando = new Comandos();

        // Cria o HashMap para armazenar as associações
        Map<String, Runnable> comandos = new HashMap<>();

        // Se a chave for 'cd', aponte para o métdo 'cd' dentro do objeto 'comando'.
        comandos.put("cd", comando::cd);
        comandos.put("kill", comando::kill);

     return comandos;

    }


    private void terminal(Map<String, Runnable> comandos) {
        Scanner t = new Scanner(System.in);
        Comandos comando = new Comandos(); //Cria uma instancia dessa classe



       while (true){

           System.out.println(this.diretorioAtual + " ");
           String entradaUsuario = t.nextLine();
           Runnable terminal = comandos.get(entradaUsuario);

           if(terminal != null){
               terminal.run();
           }



       }


    }


}

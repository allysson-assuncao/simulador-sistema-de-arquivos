package org.example;

/*
Classe que contém os comandos e seu comportamentos. Sem atributos, apenas comportamentos.
 */
public class Comandos {

    /*
    Todo comando deve ser do tipo object, receber parâmetros genéricos e ter um retorno, mesmo que seja nulo
     */
    public Object cd(String... strings) {
        System.out.println("Entrando no diretório X");
        return null;
    }

    public Object kill(String... strings) {
        System.out.println("Matando terminal. ");
        System.exit(0);
        return null;
    }

}

package org.example;
/*
Como temos uma limitação na hora de criar a Map, uma vez que para passar parâmetro ou retornar algo
usamos tipos diferentes (Runnable, Consumer) Esse metdo é responsável por generalizar tudo isso.
 */
@FunctionalInterface
public interface Geral {
    Object execute(String... args);
}

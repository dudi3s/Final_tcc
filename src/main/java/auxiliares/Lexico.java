/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package auxiliares;

/**
 *
 * @author Eduardo
 */
public class Lexico {

    private String palavra;
    private String postag;
    private char tipo_anotacao;
    private int polaridade;

    public Lexico(String palavra, String postag, char tipo_anotacao, int polaridade) {
        this.palavra = palavra;
        this.postag = postag;
        this.tipo_anotacao = tipo_anotacao;
        this.polaridade = polaridade;
    }

    public int getPolaridade() {
        return polaridade;
    }

    public void setPolaridade(int polaridade) {
        this.polaridade = polaridade;
    }

    public String getPalavra() {
        return palavra;
    }

    public void setPalavra(String palavra) {
        this.palavra = palavra;
    }

    public String getPostag() {
        return postag;
    }

    public void setPostag(String postag) {
        this.postag = postag;
    }

    public char getTipo_anotacao() {
        return tipo_anotacao;
    }

    public void setTipo_anotacao(char tipo_anotacao) {
        this.tipo_anotacao = tipo_anotacao;
    }

    @Override
    public String toString() {
        return "Lexico{" + "palavra=" + palavra + ", postag=" + postag + ", tipo_anotacao=" + tipo_anotacao + ", polaridade=" + polaridade + '}';
    }
}

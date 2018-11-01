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
public class Frequencia {

    private int tf;
    private double tf_normalizad;

    public Frequencia(int tf, double tf_normalizad) {
        this.tf = tf;
        this.tf_normalizad = tf_normalizad;
    }

    public int getTf() {
        return tf;
    }

    public void setTf(int tf) {
        this.tf = tf;
    }

    public double getTf_normalizad() {
        return tf_normalizad;
    }

    public void setTf_normalizad(double tf_normalizad) {
        this.tf_normalizad = tf_normalizad;
    }

    @Override
    public String toString() {
        return "Frequencia{" + "tf=" + tf + ", tf_normalizad=" + tf_normalizad + '}';
    }
}

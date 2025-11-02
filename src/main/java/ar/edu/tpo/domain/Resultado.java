package ar.edu.tpo.domain;
public class Resultado {
    private String ganadorEmail; // opcional
    public Resultado(){}
    public Resultado(String ganadorEmail){ this.ganadorEmail = ganadorEmail; }
    public String getGanadorEmail() { return ganadorEmail; }
    public void setGanadorEmail(String ganadorEmail) { this.ganadorEmail = ganadorEmail; }
}

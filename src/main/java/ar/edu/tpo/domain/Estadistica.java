package ar.edu.tpo.domain;

import java.time.LocalDateTime;

public class Estadistica {
    private final String emailJugador;
    private final KDA kda;
    private final double rating; // 0-10
    private final LocalDateTime fechaCarga;

    public Estadistica(String emailJugador, KDA kda, double rating, LocalDateTime fechaCarga) {
        this.emailJugador = emailJugador;
        this.kda = kda;
        this.rating = rating;
        this.fechaCarga = fechaCarga;
    }

    public String getEmailJugador() { return emailJugador; }
    public KDA getKda() { return kda; }
    public double getRating() { return rating; }
    public LocalDateTime getFechaCarga() { return fechaCarga; }
}

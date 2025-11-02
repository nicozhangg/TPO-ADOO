package ar.edu.tpo.domain;

public class Usuario {
    private final String email;
    private final String nickname;
    private final int mmr;
    private final int latenciaMs;
    private final Rol rol;
    private Double kdaHistorico; // opcional

    public Usuario(String email, String nickname, int mmr, int latenciaMs, Rol rol) {
        this.email = email;
        this.nickname = nickname;
        this.mmr = mmr;
        this.latenciaMs = latenciaMs;
        this.rol = rol;
    }

    public String getEmail() { return email; }
    public String getNickname() { return nickname; }
    public int getMmr() { return mmr; }
    public int getLatenciaMs() { return latenciaMs; }
    public Rol getRol() { return rol; }
    public Double getKdaHistorico() { return kdaHistorico; }
    public void setKdaHistorico(Double kdaHistorico) { this.kdaHistorico = kdaHistorico; }

    @Override
    public String toString() {
        return "Usuario{" +
                "email='" + email + '\'' +
                ", nick='" + nickname + '\'' +
                ", mmr=" + mmr +
                ", latMs=" + latenciaMs +
                ", rol=" + rol +
                (kdaHistorico!=null? ", kdaHist="+kdaHistorico:"") +
                '}';
    }
}

package ar.edu.tpo.domain;

// KDA = (kills + assists) / deaths  (double)
public class KDA {
    private final int kills;
    private final int assists;
    private final int deaths;

    public KDA(int kills, int assists, int deaths) {
        if (kills < 0 || assists < 0 || deaths < 0) throw new IllegalArgumentException("Valores negativos no permitidos");
        this.kills = kills;
        this.assists = assists;
        this.deaths = deaths;
    }

    public double valor(){
        if (deaths == 0) return kills + assists; // evitar división por cero
        return (kills + assists) / (double) deaths;
    }

    public int getKills() { return kills; }
    public int getAssists() { return assists; }
    public int getDeaths() { return deaths; }

    @Override
    public String toString() {
        return String.format("KDA{k=%d,a=%d,d=%d, val=%.2f}", kills, assists, deaths, valor());
    }
}

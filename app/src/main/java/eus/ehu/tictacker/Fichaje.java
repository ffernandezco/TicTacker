package eus.ehu.tictacker;

public class Fichaje {
    public int id;
    public String fecha;
    public String horaEntrada;
    public String horaSalida;
    public double latitud;
    public double longitud;
    public String username;

    public Fichaje(int id, String fecha, String horaEntrada, String horaSalida, double latitud, double longitud, String username) {
        this.id = id;
        this.fecha = fecha;
        this.horaEntrada = horaEntrada;
        this.horaSalida = horaSalida;
        this.latitud = latitud;
        this.longitud = longitud;
        this.username = username;
    }

    public Fichaje(String fecha, String horaEntrada, String horaSalida, double latitud, double longitud, String username) {
        this(-1, fecha, horaEntrada, horaSalida, latitud, longitud, username);
    }
}

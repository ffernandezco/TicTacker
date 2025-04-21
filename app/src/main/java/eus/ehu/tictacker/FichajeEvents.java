package eus.ehu.tictacker;

public class FichajeEvents {

    private static FichajeChangeListener listener;

    public interface FichajeChangeListener {
        void onFichajeChanged();
    }

    public static void setListener(FichajeChangeListener l) {
        listener = l;
    }

    // Notificar cambios en el fichaje
    public static void notifyFichajeChanged() {
        if (listener != null) {
            listener.onFichajeChanged();
        }
    }
}
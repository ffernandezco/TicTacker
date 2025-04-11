package eus.ehu.tictacker;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.*;

public class FichajeAdapter extends RecyclerView.Adapter<FichajeAdapter.FichajeViewHolder> {
    private List<Fichaje> fichajes;
    private OnFichajeClickListener listener;

    public interface OnFichajeClickListener {
        void onFichajeClick(Fichaje fichaje);
    }

    public FichajeAdapter(OnFichajeClickListener listener) {
        this.listener = listener;
        this.fichajes = new ArrayList<>();
    }

    public void setFichajes(List<Fichaje> fichajes) {
        this.fichajes = fichajes;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public FichajeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_fichaje, parent, false);
        return new FichajeViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FichajeViewHolder holder, int position) {
        Fichaje fichaje = fichajes.get(position);
        Context context = holder.itemView.getContext();

        holder.tvFecha.setText(context.getString(R.string.fecha, fichaje.fecha));
        holder.tvHoraEntrada.setText(context.getString(R.string.entrada, fichaje.horaEntrada));

        String horaSalida = fichaje.horaSalida != null ? fichaje.horaSalida : context.getString(R.string.pendiente);
        holder.tvHoraSalida.setText(context.getString(R.string.salida, horaSalida));

        // Listener
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onFichajeClick(fichaje);
            }
        });
    }

    @Override
    public int getItemCount() {
        return (fichajes == null) ? 0 : fichajes.size();
    }

    static class FichajeViewHolder extends RecyclerView.ViewHolder {
        TextView tvFecha, tvHoraEntrada, tvHoraSalida;

        public FichajeViewHolder(@NonNull View itemView) {
            super(itemView);
            tvFecha = itemView.findViewById(R.id.tvFecha);
            tvHoraEntrada = itemView.findViewById(R.id.tvHoraEntrada);
            tvHoraSalida = itemView.findViewById(R.id.tvHoraSalida);
        }
    }
}
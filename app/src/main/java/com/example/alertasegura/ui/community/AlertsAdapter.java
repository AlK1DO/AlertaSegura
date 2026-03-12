package com.example.alertasegura.ui.community;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.alertasegura.R;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AlertsAdapter extends RecyclerView.Adapter<AlertsAdapter.VH> {

    private List<Map<String, Object>> list;

    public AlertsAdapter(List<Map<String, Object>> list) {
        this.list = list;
    }

    public void updateList(List<Map<String, Object>> newList) {
        this.list = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_community_alert, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        Map<String, Object> alert = list.get(position);

        String name  = (String) alert.get("senderName");
        String dni   = (String) alert.get("senderDni");
        Object tsObj = alert.get("timestamp");

        // Mostrar nombre truncado + DNI parcial por privacidad
        String displayName = (name != null && !name.isEmpty()) ? name : "Anónimo";
        String displayDni  = (dni  != null && dni.length() >= 4)
                ? "DNI: ****" + dni.substring(dni.length() - 4)
                : "DNI: ****";

        holder.tvName.setText("⚠️  " + displayName);
        holder.tvDni.setText(displayDni);

        // Tiempo relativo
        if (tsObj instanceof Long) {
            long ts       = (Long) tsObj;
            long diffMin  = (System.currentTimeMillis() - ts) / 60000;
            String timeStr = diffMin < 1 ? "ahora mismo"
                    : diffMin < 60 ? "hace " + diffMin + " min"
                    : new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date(ts));
            holder.tvTime.setText(timeStr);
        }

        // Ubicación aproximada
        Object lat = alert.get("approxLat");
        Object lng = alert.get("approxLng");
        if (lat instanceof Double && lng instanceof Double) {
            holder.tvLocation.setText(String.format(Locale.getDefault(),
                    "📍 Aprox. %.4f, %.4f", (Double) lat, (Double) lng));
        } else {
            holder.tvLocation.setText("📍 Ubicación no disponible");
        }
    }

    @Override
    public int getItemCount() { return list != null ? list.size() : 0; }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvName, tvDni, tvTime, tvLocation;
        VH(@NonNull View v) {
            super(v);
            tvName     = v.findViewById(R.id.tvAlertName);
            tvDni      = v.findViewById(R.id.tvAlertDni);
            tvTime     = v.findViewById(R.id.tvAlertTime);
            tvLocation = v.findViewById(R.id.tvAlertLocation);
        }
    }
}
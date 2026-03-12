// ============================================================
// ui/contacts/PhoneMatchesAdapter.java
// Muestra los contactos del teléfono para ser agregados como confianza.
// ============================================================
package com.example.alertasegura.ui.contacts;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.alertasegura.R;
import com.example.alertasegura.util.PhoneContactsHelper;

import java.util.List;

public class PhoneMatchesAdapter extends RecyclerView.Adapter<PhoneMatchesAdapter.VH> {

    public interface OnAddListener {
        void onAdd(PhoneContactsHelper.DeviceContact contact);
    }

    private List<PhoneContactsHelper.DeviceContact> list;
    private final OnAddListener addListener;

    public PhoneMatchesAdapter(List<PhoneContactsHelper.DeviceContact> list, OnAddListener addListener) {
        this.list        = list;
        this.addListener = addListener;
    }

    public void updateList(List<PhoneContactsHelper.DeviceContact> newList) {
        this.list = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_phone_match, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        PhoneContactsHelper.DeviceContact contact = list.get(position);
        holder.tvName.setText(contact.displayName);
        holder.tvPhone.setText("📱 +" + contact.normalizedPhone);
        holder.btnAdd.setOnClickListener(v -> addListener.onAdd(contact));
    }

    @Override
    public int getItemCount() { return list != null ? list.size() : 0; }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvName, tvPhone;
        Button   btnAdd;
        VH(@NonNull View v) {
            super(v);
            tvName  = v.findViewById(R.id.tvMatchName);
            tvPhone = v.findViewById(R.id.tvMatchPhone);
            btnAdd  = v.findViewById(R.id.btnAddMatch);
        }
    }
}

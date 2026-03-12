// ============================================================
// ui/contacts/ContactsAdapter.java
// ============================================================
package com.example.alertasegura.ui.contacts;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.alertasegura.R;
import com.example.alertasegura.data.model.Contact;

import java.util.List;

public class ContactsAdapter extends RecyclerView.Adapter<ContactsAdapter.VH> {

    public interface OnDeleteListener {
        void onDelete(String contactUid);
    }

    private List<Contact> list;
    private final OnDeleteListener deleteListener;

    public ContactsAdapter(List<Contact> list, OnDeleteListener deleteListener) {
        this.list           = list;
        this.deleteListener = deleteListener;
    }

    public void updateList(List<Contact> newList) {
        this.list = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_contact, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        Contact contact = list.get(position);
        holder.tvName.setText(contact.getDisplayName());
        holder.tvPhone.setText(contact.getPhone());

        String statusEmoji = "accepted".equals(contact.getStatus()) ? "✅" : "⏳";
        holder.tvStatus.setText(statusEmoji);

        holder.btnDelete.setOnClickListener(v -> deleteListener.onDelete(contact.getContactUid()));
    }

    @Override
    public int getItemCount() { return list.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvName, tvPhone, tvStatus;
        ImageButton btnDelete;
        VH(@NonNull View v) {
            super(v);
            tvName    = v.findViewById(R.id.tvContactName);
            tvPhone   = v.findViewById(R.id.tvContactPhone);
            tvStatus  = v.findViewById(R.id.tvContactStatus);
            btnDelete = v.findViewById(R.id.btnDeleteContact);
        }
    }
}
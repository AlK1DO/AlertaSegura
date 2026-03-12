// ============================================================
// ui/contacts/ContactsFragment.java
// ============================================================
package com.example.alertasegura.ui.contacts;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.alertasegura.data.model.Contact;
import com.example.alertasegura.databinding.FragmentContactsBinding;
import com.example.alertasegura.viewmodel.ContactViewModel;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;

public class ContactsFragment extends Fragment {

    private FragmentContactsBinding binding;
    private ContactViewModel viewModel;
    private ContactsAdapter  adapter;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentContactsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(requireActivity()).get(ContactViewModel.class);

        // RecyclerView
        adapter = new ContactsAdapter(new ArrayList<>(), contactUid -> {
            new AlertDialog.Builder(requireContext())
                    .setTitle("Eliminar contacto")
                    .setMessage("¿Seguro que deseas eliminar este contacto de confianza?")
                    .setPositiveButton("Eliminar", (d, w) -> viewModel.removeContact(contactUid))
                    .setNegativeButton("Cancelar", null)
                    .show();
        });
        binding.rvContacts.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvContacts.setAdapter(adapter);

        // Observadores
        viewModel.contactsLiveData.observe(getViewLifecycleOwner(), contacts -> {
            if (contacts != null) {
                adapter.updateList(contacts);
                binding.tvEmptyState.setVisibility(contacts.isEmpty() ? View.VISIBLE : View.GONE);
            }
        });

        viewModel.foundUserLiveData.observe(getViewLifecycleOwner(), user -> {
            if (user != null) {
                new AlertDialog.Builder(requireContext())
                        .setTitle("¿Agregar contacto?")
                        .setMessage("¿Agregar a " + user.getFullName() + " como contacto de confianza?")
                        .setPositiveButton("Agregar", (d, w) -> viewModel.addContact(user))
                        .setNegativeButton("Cancelar", null)
                        .show();
                viewModel.foundUserLiveData.setValue(null);
            }
        });

        viewModel.successLiveData.observe(getViewLifecycleOwner(), ok -> {
            if (Boolean.TRUE.equals(ok)) {
                Snackbar.make(binding.getRoot(), "Contacto actualizado", Snackbar.LENGTH_SHORT).show();
                viewModel.loadContacts();
                viewModel.successLiveData.setValue(null);
            }
        });

        viewModel.errorLiveData.observe(getViewLifecycleOwner(), error -> {
            if (error != null && !error.isEmpty()) {
                Snackbar.make(binding.getRoot(), error, Snackbar.LENGTH_LONG).show();
                viewModel.errorLiveData.setValue(null);
            }
        });

        // FAB — buscar por DNI
        binding.fabAddContact.setOnClickListener(v -> showSearchDniDialog());

        viewModel.loadContacts();
    }

    private void showSearchDniDialog() {
        View dialogView = LayoutInflater.from(requireContext())
                .inflate(com.example.alertasegura.R.layout.dialog_search_dni, null);
        TextInputEditText etDni = dialogView.findViewById(com.example.alertasegura.R.id.etDniSearch);

        new AlertDialog.Builder(requireContext())
                .setTitle("Buscar contacto por DNI")
                .setView(dialogView)
                .setPositiveButton("Buscar", (d, w) -> {
                    String dni = etDni.getText() != null ? etDni.getText().toString().trim() : "";
                    if (dni.length() == 8) {
                        viewModel.searchByDni(dni);
                    } else {
                        Snackbar.make(binding.getRoot(), "El DNI debe tener 8 dígitos", Snackbar.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
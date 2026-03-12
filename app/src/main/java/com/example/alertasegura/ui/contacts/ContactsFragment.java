// ============================================================
// ui/contacts/ContactsFragment.java  — Fase 3 rev.
// Sincroniza contactos del teléfono para agregarlos como confianza.
// ============================================================
package com.example.alertasegura.ui.contacts;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.alertasegura.data.model.Contact;
import com.example.alertasegura.databinding.FragmentContactsBinding;
import com.example.alertasegura.viewmodel.ContactViewModel;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;

public class ContactsFragment extends Fragment {

    private FragmentContactsBinding binding;
    private ContactViewModel viewModel;

    private ContactsAdapter savedAdapter;
    private PhoneMatchesAdapter matchesAdapter;

    private final ActivityResultLauncher<String> contactsPermLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) viewModel.syncPhoneContacts();
                else Snackbar.make(binding.getRoot(),
                        "Necesitamos acceso a tus contactos para agregarlos como confianza",
                        Snackbar.LENGTH_LONG).show();
            });

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentContactsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(requireActivity()).get(ContactViewModel.class);

        setupSavedContactsList();
        setupPhoneMatchesList();
        setupObservers();

        viewModel.loadContacts();

        if (hasContactsPermission()) {
            viewModel.syncPhoneContacts();
        } else {
            binding.layoutSuggestions.setVisibility(View.VISIBLE);
            binding.rvPhoneMatches.setVisibility(View.GONE);
            binding.btnGrantContacts.setVisibility(View.VISIBLE);
        }

        binding.btnGrantContacts.setOnClickListener(v ->
                contactsPermLauncher.launch(Manifest.permission.READ_CONTACTS));
    }

    private void setupSavedContactsList() {
        savedAdapter = new ContactsAdapter(new ArrayList<>(), contactUid ->
                new AlertDialog.Builder(requireContext())
                        .setTitle("Eliminar contacto")
                        .setMessage("¿Eliminar este contacto de tu lista de emergencia?")
                        .setPositiveButton("Eliminar", (d, w) -> viewModel.removeContact(contactUid))
                        .setNegativeButton("Cancelar", null)
                        .show()
        );
        binding.rvContacts.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvContacts.setAdapter(savedAdapter);
    }

    private void setupPhoneMatchesList() {
        matchesAdapter = new PhoneMatchesAdapter(new ArrayList<>(), deviceContact -> {
            new AlertDialog.Builder(requireContext())
                    .setTitle("Agregar a " + deviceContact.displayName)
                    .setMessage("¿Agregar a " + deviceContact.displayName + " como contacto de confianza?\nRecibirá un mensaje de WhatsApp cuando actives SOS.")
                    .setPositiveButton("Agregar", (d, w) -> viewModel.addContact(deviceContact))
                    .setNegativeButton("No por ahora", null)
                    .show();
        });
        binding.rvPhoneMatches.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvPhoneMatches.setAdapter(matchesAdapter);
    }

    private void setupObservers() {
        viewModel.contactsLiveData.observe(getViewLifecycleOwner(), contacts -> {
            List<Contact> list = contacts != null ? contacts : new ArrayList<>();
            savedAdapter.updateList(list);
            binding.tvEmptyState.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);
        });

        viewModel.deviceContactsLiveData.observe(getViewLifecycleOwner(), deviceContacts -> {
            if (deviceContacts == null) return;
            binding.layoutSuggestions.setVisibility(View.VISIBLE);
            binding.btnGrantContacts.setVisibility(View.GONE);
            binding.rvPhoneMatches.setVisibility(View.VISIBLE);

            // Filtrar los que ya están agregados por teléfono
            List<Contact> saved = viewModel.contactsLiveData.getValue();
            List<String> savedPhones = new ArrayList<>();
            if (saved != null) for (Contact c : saved) savedPhones.add(c.getPhone());

            List<com.example.alertasegura.util.PhoneContactsHelper.DeviceContact> filtered = new ArrayList<>();
            for (com.example.alertasegura.util.PhoneContactsHelper.DeviceContact dc : deviceContacts) {
                if (!savedPhones.contains(dc.normalizedPhone)) {
                    filtered.add(dc);
                }
            }

            matchesAdapter.updateList(filtered);
            binding.tvNoMatches.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
        });

        viewModel.loadingPhones.observe(getViewLifecycleOwner(), loading -> {
            binding.progressSync.setVisibility(Boolean.TRUE.equals(loading) ? View.VISIBLE : View.GONE);
        });

        viewModel.successLiveData.observe(getViewLifecycleOwner(), ok -> {
            if (Boolean.TRUE.equals(ok)) {
                Snackbar.make(binding.getRoot(), "Contacto actualizado", Snackbar.LENGTH_SHORT).show();
                viewModel.loadContacts();
                if (hasContactsPermission()) viewModel.syncPhoneContacts();
                viewModel.successLiveData.setValue(null);
            }
        });

        viewModel.errorLiveData.observe(getViewLifecycleOwner(), error -> {
            if (error != null && !error.isEmpty()) {
                Snackbar.make(binding.getRoot(), error, Snackbar.LENGTH_LONG).show();
                viewModel.errorLiveData.setValue(null);
            }
        });
    }

    private boolean hasContactsPermission() {
        return ContextCompat.checkSelfPermission(requireContext(),
                Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}

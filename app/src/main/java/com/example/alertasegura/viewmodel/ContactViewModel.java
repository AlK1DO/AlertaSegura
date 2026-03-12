// ============================================================
// viewmodel/ContactViewModel.java  — Fase 3 rev.
// Usa PhoneContactsHelper para leer contactos del teléfono.
// ============================================================
package com.example.alertasegura.viewmodel;

import android.app.Application;
import android.os.AsyncTask;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

import com.example.alertasegura.data.model.Contact;
import com.example.alertasegura.data.repository.ContactRepository;
import com.example.alertasegura.util.PhoneContactsHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ContactViewModel extends AndroidViewModel {

    private final ContactRepository contactRepository;

    public final MutableLiveData<List<Contact>> contactsLiveData       = new MutableLiveData<>();
    public final MutableLiveData<List<PhoneContactsHelper.DeviceContact>> deviceContactsLiveData = new MutableLiveData<>();
    public final MutableLiveData<Boolean>       loadingPhones          = new MutableLiveData<>(false);
    public final MutableLiveData<Boolean>       successLiveData        = new MutableLiveData<>();
    public final MutableLiveData<String>        errorLiveData          = new MutableLiveData<>();

    public ContactViewModel(@NonNull Application application) {
        super(application);
        contactRepository = new ContactRepository();
    }

    /**
     * Lee los contactos del dispositivo en un hilo de fondo.
     * Carga todos los contactos para que el usuario pueda elegir cualquiera como confianza.
     */
    public void syncPhoneContacts() {
        loadingPhones.setValue(true);

        AsyncTask.execute(() -> {
            Map<String, PhoneContactsHelper.DeviceContact> deviceContacts =
                    PhoneContactsHelper.readDeviceContacts(getApplication());

            List<PhoneContactsHelper.DeviceContact> list = new ArrayList<>(deviceContacts.values());
            deviceContactsLiveData.postValue(list);
            loadingPhones.postValue(false);
        });
    }

    /** Agrega un contacto del teléfono como contacto de confianza. */
    public void addContact(PhoneContactsHelper.DeviceContact deviceContact) {
        // Usamos el teléfono como UID si no sabemos si tiene la app
        Contact contact = new Contact(
                deviceContact.normalizedPhone,
                deviceContact.displayName,
                deviceContact.normalizedPhone
        );
        contact.setStatus("accepted");
        contactRepository.addContact(contact, successLiveData, errorLiveData);
    }

    /** Carga la lista de contactos guardados. */
    public void loadContacts() {
        contactRepository.loadContacts(contactsLiveData, errorLiveData);
    }

    /** Elimina un contacto. */
    public void removeContact(String contactUid) {
        contactRepository.removeContact(contactUid, successLiveData);
    }
}

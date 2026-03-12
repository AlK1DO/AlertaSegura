package com.example.alertasegura.viewmodel;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.alertasegura.data.model.Contact;
import com.example.alertasegura.data.model.User;
import com.example.alertasegura.data.repository.ContactRepository;

import java.util.List;

public class ContactViewModel extends ViewModel {

    private final ContactRepository contactRepository;

    public final MutableLiveData<List<Contact>> contactsLiveData  = new MutableLiveData<>();
    public final MutableLiveData<User>          foundUserLiveData = new MutableLiveData<>();
    public final MutableLiveData<Boolean>       successLiveData   = new MutableLiveData<>();
    public final MutableLiveData<String>        errorLiveData     = new MutableLiveData<>();

    public ContactViewModel() {
        contactRepository = new ContactRepository();
    }

    /** Busca un usuario por DNI para invitarlo. */
    public void searchByDni(String dni) {
        foundUserLiveData.setValue(null);
        errorLiveData.setValue(null);
        contactRepository.findUserByDni(dni, foundUserLiveData, errorLiveData);
    }

    /** Confirma el agregado de un contacto encontrado. */
    public void addContact(User user) {
        Contact contact = new Contact(
                user.getUid(),
                user.getFullName(),
                user.getPhone()
        );
        contact.setStatus("accepted"); // bilateral simplificado por ahora
        contactRepository.addContact(contact, successLiveData, errorLiveData);
    }

    /** Carga la lista de contactos del usuario actual. */
    public void loadContacts() {
        contactRepository.loadContacts(contactsLiveData, errorLiveData);
    }

    /** Elimina un contacto. */
    public void removeContact(String contactUid) {
        contactRepository.removeContact(contactUid, successLiveData);
    }
}
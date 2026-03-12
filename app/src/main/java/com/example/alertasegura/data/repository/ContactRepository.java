package com.example.alertasegura.data.repository;

import androidx.lifecycle.MutableLiveData;

import com.example.alertasegura.data.model.Contact;
import com.example.alertasegura.data.model.User;
import com.example.alertasegura.util.Constants;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class ContactRepository {

    private final FirebaseFirestore db;
    private final FirebaseAuth auth;

    public ContactRepository() {
        this.db   = FirebaseFirestore.getInstance();
        this.auth = FirebaseAuth.getInstance();
    }

    private String currentUid() {
        return auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : "";
    }

    /**
     * Busca un usuario por DNI para agregar como contacto.
     */
    public void findUserByDni(String dni,
                              MutableLiveData<User> userLiveData,
                              MutableLiveData<String> errorLiveData) {
        db.collection(Constants.COLLECTION_USERS)
                .whereEqualTo("dni", dni)
                .limit(1)
                .get()
                .addOnSuccessListener(query -> {
                    if (query.isEmpty()) {
                        errorLiveData.setValue("No se encontró ningún usuario con ese DNI");
                    } else {
                        User found = query.getDocuments().get(0).toObject(User.class);
                        if (found != null && found.getUid().equals(currentUid())) {
                            errorLiveData.setValue("No puedes agregarte a ti mismo");
                        } else {
                            userLiveData.setValue(found);
                        }
                    }
                })
                .addOnFailureListener(e -> errorLiveData.setValue(e.getMessage()));
    }

    /**
     * Agrega un contacto de confianza (estado "pending").
     */
    public void addContact(Contact contact,
                           MutableLiveData<Boolean> successLiveData,
                           MutableLiveData<String> errorLiveData) {
        db.collection(Constants.COLLECTION_USERS)
                .document(currentUid())
                .collection(Constants.COLLECTION_CONTACTS)
                .document(contact.getContactUid())
                .set(contact)
                .addOnSuccessListener(unused -> successLiveData.setValue(true))
                .addOnFailureListener(e -> errorLiveData.setValue(e.getMessage()));
    }

    /**
     * Carga la lista de contactos del usuario actual.
     */
    public void loadContacts(MutableLiveData<List<Contact>> contactsLiveData,
                             MutableLiveData<String> errorLiveData) {
        db.collection(Constants.COLLECTION_USERS)
                .document(currentUid())
                .collection(Constants.COLLECTION_CONTACTS)
                .get()
                .addOnSuccessListener(query -> {
                    List<Contact> list = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : query) {
                        Contact c = doc.toObject(Contact.class);
                        list.add(c);
                    }
                    contactsLiveData.setValue(list);
                })
                .addOnFailureListener(e -> errorLiveData.setValue(e.getMessage()));
    }

    /**
     * Elimina un contacto.
     */
    public void removeContact(String contactUid,
                              MutableLiveData<Boolean> successLiveData) {
        db.collection(Constants.COLLECTION_USERS)
                .document(currentUid())
                .collection(Constants.COLLECTION_CONTACTS)
                .document(contactUid)
                .delete()
                .addOnSuccessListener(unused -> successLiveData.setValue(true));
    }
}
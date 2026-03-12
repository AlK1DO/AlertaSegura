
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
    private final FirebaseAuth      auth;

    public ContactRepository() {
        this.db   = FirebaseFirestore.getInstance();
        this.auth = FirebaseAuth.getInstance();
    }

    private String currentUid() {
        return auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : "";
    }

    /**
     * Recibe una lista de números normalizados del teléfono del usuario
     * y devuelve los que tienen cuenta en Alerta Segura.
     *
     * Firestore "in" soporta hasta 30 elementos por query → chunking automático.
     */
    public void findUsersByPhones(List<String> normalizedPhones,
                                  MutableLiveData<List<User>> usersLiveData,
                                  MutableLiveData<String> errorLiveData) {

        if (normalizedPhones.isEmpty()) {
            usersLiveData.setValue(new ArrayList<>());
            return;
        }

        String myUid = currentUid();
        List<User> results = new ArrayList<>();
        int chunkSize = 30;
        int[] pending = { (int) Math.ceil(normalizedPhones.size() / (double) chunkSize) };

        for (int i = 0; i < normalizedPhones.size(); i += chunkSize) {
            List<String> chunk = normalizedPhones.subList(
                    i, Math.min(i + chunkSize, normalizedPhones.size()));

            db.collection(Constants.COLLECTION_USERS)
                    .whereIn("phone", chunk)
                    .get()
                    .addOnSuccessListener(query -> {
                        for (QueryDocumentSnapshot doc : query) {
                            User u = doc.toObject(User.class);
                            // Excluir al propio usuario
                            if (u != null && !u.getUid().equals(myUid)) {
                                results.add(u);
                            }
                        }
                        pending[0]--;
                        if (pending[0] == 0) {
                            usersLiveData.setValue(results);
                        }
                    })
                    .addOnFailureListener(e -> {
                        pending[0]--;
                        if (pending[0] == 0) {
                            if (results.isEmpty()) errorLiveData.setValue(e.getMessage());
                            else usersLiveData.setValue(results);
                        }
                    });
        }
    }

    /** Agrega un contacto de confianza. */
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

    /** Carga la lista de contactos del usuario actual. */
    public void loadContacts(MutableLiveData<List<Contact>> contactsLiveData,
                             MutableLiveData<String> errorLiveData) {
        db.collection(Constants.COLLECTION_USERS)
                .document(currentUid())
                .collection(Constants.COLLECTION_CONTACTS)
                .get()
                .addOnSuccessListener(query -> {
                    List<Contact> list = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : query) {
                        list.add(doc.toObject(Contact.class));
                    }
                    contactsLiveData.setValue(list);
                })
                .addOnFailureListener(e -> errorLiveData.setValue(e.getMessage()));
    }

    /** Elimina un contacto. */
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
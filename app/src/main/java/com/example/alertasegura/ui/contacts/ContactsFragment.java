// ============================================================
// ui/contacts/ContactsFragment.java  (placeholder — Fase 2)
// ============================================================
package com.example.alertasegura.ui.contacts;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

public class ContactsFragment extends Fragment {
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        TextView tv = new TextView(requireContext());
        tv.setText("👥  Contactos — Fase 2");
        tv.setTextSize(22f);
        tv.setPadding(48, 48, 48, 48);
        return tv;
    }
}


// ============================================================
// ui/community/CommunityFragment.java  (placeholder — Fase 3)
// ============================================================
// package com.alertasegura.ui.community;
// ... (misma estructura, texto "Comunidad — Fase 3")


// ============================================================
// ui/map/MapFragment.java  (placeholder — Fase 4)
// ============================================================
// package com.alertasegura.ui.map;
// ... (misma estructura, texto "Mapa de calor — Fase 4")
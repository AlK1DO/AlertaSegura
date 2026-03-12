package com.example.alertasegura.ui.community;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.alertasegura.databinding.FragmentCommunityBinding;
import com.example.alertasegura.viewmodel.AlertViewModel;

import java.util.ArrayList;

public class CommunityFragment extends Fragment {

    private FragmentCommunityBinding binding;
    private AlertViewModel alertViewModel;
    private AlertsAdapter  adapter;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentCommunityBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        alertViewModel = new ViewModelProvider(requireActivity()).get(AlertViewModel.class);

        adapter = new AlertsAdapter(new ArrayList<>());
        binding.rvAlerts.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvAlerts.setAdapter(adapter);

        alertViewModel.communityAlerts.observe(getViewLifecycleOwner(), alerts -> {
            if (alerts != null) {
                adapter.updateList(alerts);
                binding.tvEmptyFeed.setVisibility(alerts.isEmpty() ? View.VISIBLE : View.GONE);
            }
        });

        // Iniciar escucha en tiempo real
        alertViewModel.startListeningCommunityAlerts();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
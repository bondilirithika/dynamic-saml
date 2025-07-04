package com.example.flutto.model;

import java.util.ArrayList;
import java.util.List;

public class SamlProvidersConfig {
    private List<SamlProviderConfig> providers = new ArrayList<>();

    public List<SamlProviderConfig> getProviders() {
        return providers;
    }

    public void setProviders(List<SamlProviderConfig> providers) {
        this.providers = providers;
    }
}
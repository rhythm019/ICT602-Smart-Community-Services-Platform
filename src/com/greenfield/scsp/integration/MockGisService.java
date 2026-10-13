package com.greenfield.scsp.integration;

import com.greenfield.scsp.domain.Location;

import java.util.Optional;

/**
 * Stand-in for the GIS provider. Accepts any address that contains
 * "Greenfield" (the council area) and returns deterministic coordinates,
 * so behaviour is repeatable in tests and demonstrations.
 */
public class MockGisService implements GisService {

    private static final String COUNCIL_AREA = "greenfield";
    private static final double BASE_LAT = -37.8136;
    private static final double BASE_LON = 144.9631;

    @Override
    public Optional<Location> geocode(String address) {
        if (address == null || address.isBlank()
                || !address.toLowerCase().contains(COUNCIL_AREA)) {
            return Optional.empty();
        }
        // Derive a small, stable offset from the text so each address differs.
        int h = Math.abs(address.trim().toLowerCase().hashCode());
        double lat = BASE_LAT + (h % 1000) / 100_000.0;
        double lon = BASE_LON + ((h / 1000) % 1000) / 100_000.0;
        return Optional.of(new Location(address.trim(), lat, lon));
    }
}

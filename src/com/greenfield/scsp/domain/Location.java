package com.greenfield.scsp.domain;

/**
 * Immutable value object for where an issue was reported (FR-05).
 * Coordinates are resolved by the GIS integration.
 *
 * @param address   street address entered by the resident
 * @param latitude  decimal latitude returned by GIS
 * @param longitude decimal longitude returned by GIS
 */
public record Location(String address, double latitude, double longitude) {

    @Override
    public String toString() {
        return String.format("%s (%.4f, %.4f)", address, latitude, longitude);
    }
}

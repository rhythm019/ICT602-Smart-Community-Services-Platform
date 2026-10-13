package com.greenfield.scsp.integration;

import com.greenfield.scsp.domain.Location;

import java.util.Optional;

/**
 * Port to the external GIS provider (SI-01). The real system would call a
 * REST API; the prototype uses {@link MockGisService}.
 */
public interface GisService {

    /**
     * Converts an address into coordinates inside the council boundary.
     *
     * @param address street address typed by the resident
     * @return the resolved location, or empty when the address is outside the
     *         council area or cannot be found
     */
    Optional<Location> geocode(String address);
}

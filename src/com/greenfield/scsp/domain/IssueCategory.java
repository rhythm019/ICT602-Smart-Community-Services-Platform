package com.greenfield.scsp.domain;

/**
 * Categories a resident can choose when reporting an issue (UI wireframe W-03).
 */
public enum IssueCategory {
    ROAD_DAMAGE("Road damage / pothole"),
    WASTE("Missed bin / illegal dumping"),
    GRAFFITI("Graffiti"),
    STREET_LIGHTING("Street lighting"),
    PARKS("Parks and trees");

    private final String label;

    IssueCategory(String label) {
        this.label = label;
    }

    public String getLabel() { return label; }
}

package com.astroscout.backend.sky;

public record Sky101Object(
        String id,
        String name,
        String category,
        String visualType,
        String shortDescription,
        String details,
        Sky101FactSheet facts,
        double x,
        double y,
        double z,
        double radius,
        String colorHex,
        String accentColorHex,
        Double orbitRadius,
        Double orbitSpeed,
        Double orbitPhase,
        java.util.List<Sky101ShapePoint> points,
        java.util.List<Sky101ShapeLink> links
) {}

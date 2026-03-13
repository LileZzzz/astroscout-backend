package com.astroscout.backend.sky;

public record Sky101FactSheet(
        String distanceFromEarth,
        String diameter,
        String mass,
        Integer moonCount,
        String funFact
) {}
package com.astroscout.backend.sky;

import java.util.List;

public record Sky101SceneResponse(
        List<Sky101Object> objects
) {}

package com.kubelize.securewarps.db;

import java.util.UUID;

public record WarpRecord(
    UUID id,
    String name,
    UUID ownerUuid,
    String worldId,
    double x,
    double y,
    double z,
    float rotX,
    float rotY,
    float rotZ
) {}

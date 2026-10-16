package dev.transparentinventory.client.screen;

import dev.transparentinventory.client.anchor.AnchorType;

/** A profile changes framing, never vanilla widgets or menu logic. */
public record SpatialScreenProfile(String id, AnchorType anchor, double scale, double offsetX, double offsetY) { }

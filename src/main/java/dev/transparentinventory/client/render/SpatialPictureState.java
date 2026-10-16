package dev.transparentinventory.client.render;

import org.joml.Matrix3x2fc;

/** Attaches the extraction pose to vanilla picture-in-picture states, whose
 * default pose is identity. Their existing renderers/textures remain unchanged. */
public interface SpatialPictureState {
    void spatial$pose(Matrix3x2fc pose);
}

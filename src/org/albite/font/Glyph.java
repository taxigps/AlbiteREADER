/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.albite.font;

/**
 *
 * @author Albus Dumbledore
 */
public class Glyph {
    protected final int width;
    protected final int[] bitmap;

    public Glyph(
            final int width,
            final int[] bitmap) {

        this.width = width;
        this.bitmap = bitmap;
    }
}
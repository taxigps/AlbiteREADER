/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.albite.io.decoders;

import java.io.IOException;
import java.io.InputStream;

/**
 *
 * @author albus
 */
class DecoderUNICODE extends AlbiteCharacterDecoder {

    private static DecoderUNICODE instance;

    private DecoderUNICODE() {}

    public static AlbiteCharacterDecoder getInstance() {
        if (instance == null) {
            instance = new DecoderUNICODE();
        }
        return instance;
    }

    public final int decode(final InputStream in) throws IOException {

        int char1, char2;

        char1 = in.read();
        char2 = in.read();

        return (char2 << 8) | char1;
    }

    public final String getEncoding() {
        return Encodings.UNICODE;
    }
}
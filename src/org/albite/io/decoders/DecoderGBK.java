/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.albite.io.decoders;

import java.io.IOException;
import java.io.InputStream;
import java.io.DataInputStream;


/**
 *
 * @author taxigps
 */
class DecoderGBK extends AlbiteCharacterDecoder {

    private static DecoderGBK instance;

    private static final short[] MAP = new short[24080];

    static {
        /*
         * Load data from external file
         */
        InputStream is = (new Object()).getClass()
                .getResourceAsStream("/res/gbk2uni.mb");

        if (is != null) {
            DataInputStream in = new DataInputStream(is);
            try {
                try {
                    for (int i = 0; i < 24080; i++) {
                        MAP[i] = in.readShort();
                    }
                } finally {
                    in.close();
                }
            } catch (IOException e) {}
        }
    }

    private DecoderGBK() {}

    public static AlbiteCharacterDecoder getInstance() {
        if (instance == null) {
            instance = new DecoderGBK();
        }
        return instance;
    }

    public final int decode(final InputStream in) throws IOException {

        int char1, char2;

        char1 = in.read();

        if (char1 == -1) {
            return DECODING_DONE;
        }
        
        if ((char1 & 0x80) != 0x80) {
            /*  0xxx xxxx*/
            return char1;
        }
        
        /* 1xxx xxxx   xxxx xxxx*/
        char2 = in.read();
        if (char2 == -1) {
            return SUBSTITUTE_CHAR;
        }

        if ((char2 < 64) || (char2 > 254)) {
            return SUBSTITUTE_CHAR;
        }

        char1 -= 129;
        char2 -= 64;
        return (((char1 <= 125) && (char2 <= 190)) ? 
                MAP[char1 * 191 + char2] : SUBSTITUTE_CHAR);
    }

    public final String getEncoding() {
        return Encodings.GBK;
    }
}
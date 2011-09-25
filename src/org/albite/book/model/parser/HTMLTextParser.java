/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.albite.book.model.parser;

import java.util.Vector;
import org.albite.albite.AlbiteMIDlet;
import org.albite.book.view.StylingConstants;
import org.albite.io.html.HTMLSubstitues;
import org.albite.io.html.XhtmlStreamReader;
import org.albite.lang.AlbiteCharacter;

////#define DEBUG_PARSER

/**
 *
 * This is a <i>very</i> simple HTML parser made for the specific purpose of
 * parsing HTMLs on the fly, i.e. without conversion. It preserves some of the
 * formatting: the one specified by the following tags:
 * - [meta encoding]
 * - [i] or [em]
 * - [b] or [strong]
 * - [center], [left], [right]
 *
 * Because of memory considerations, and File linking issues,
 * images will not be preserved.
 *
 * @author albus
 */
public class HTMLTextParser extends TextParser
        implements HTMLSubstitues, StylingConstants {

    private static final String TAG_P       = "p";
    private static final String TAG_BR      = "br";
    private static final String TAG_DIV     = "div";
    private static final String TAG_TR      = "tr";
    private static final String TAG_LI      = "li";

    private static final String TAG_IMG     = "img";
    private static final String TAG_SVG_IMAGE = "image";

    private static final String TAG_B       = "b";
    private static final String TAG_STRONG  = "strong";
    private static final String TAG_I       = "i";
    private static final String TAG_EM      = "em";

    private static final String TAG_H1      = "h1";
    private static final String TAG_H2      = "h2";
    private static final String TAG_H3      = "h3";
    private static final String TAG_H4      = "h4";
    private static final String TAG_H5      = "h5";
    private static final String TAG_H6      = "h6";

    private static final String TAG_CENTER  = "center";

    private static final String TAG_HR      = "hr";

    private static final String TAG_PRE     = "pre";

    private int ignoreTag = 0;

    private int pre = 0;

    private int bold = 0;
    private int italic = 0;
    private int heading = 0;

    private int center = 0;

    private boolean hr = false;

    private Vector instructions = new Vector(20);

    public HTMLTextParser() {
        processBreaks = false;
    }

    public final void reset() {
        ignoreTag = 0;
        pre = 0;
        bold = 0;
        italic = 0;
        heading = 0;
        center = 0;
        hr = false;
        
        if (instructions != null) {
            instructions.removeAllElements();
        }

        super.reset();
    }

    public final boolean parseNext(
            final char[] text,
            final int textSize) {

        //#ifdef DEBUG_PARSER
//#         AlbiteMIDlet.LOGGER.log("---------------\nParsing: " + text.length + " / " + textSize);
        //#endif

        if (!instructions.isEmpty()) {
            /*
             * Execute instructions before continuing;
             */
            //#ifdef DEBUG_PARSER
//#             AlbiteMIDlet.LOGGER.log("Executing pareser instructions");
            //#endif

            state = ((Integer) instructions.lastElement()).byteValue();
            instructions.removeElementAt(instructions.size() - 1);
            return true;
        }

        if (!proceed(textSize)) {
            return false;
        }

        if (processWhiteSpace(position, text, textSize)) {
            return true;
        }

        //Parse markup instructions
        if (parseMarkup(text, textSize)) {
            return true;
        }

        //#ifdef DEBUG_PARSER
//#         AlbiteMIDlet.LOGGER.log("No markup");
        //#endif

        /*
         * parsing normal text; stopping at stop-chars or end of textbuffer
         */
        state = (ignoreTag > 0 ? STATE_PASS : STATE_TEXT);
        for (int i = position; i < textSize; i++) {
            ch = text[i];
            if (isWhiteSpace(ch) || isNewLine(ch) || ch == START_TAG_CHAR || (i > position && AlbiteCharacter.isChinese(ch))) {
                length = i - position;
                
                //#ifdef DEBUG_PARSER
//#                 AlbiteMIDlet.LOGGER.log("Stop character.");
                //#endif

                return true;
            }
        }

        //#ifdef DEBUG_PARSER
//#         AlbiteMIDlet.LOGGER.log("end of 'parseNext'");
        //#endif

        length = textSize - position;
        state = STATE_TEXT;
        return true;
    }

    private boolean parseMarkup(final char[] text, final int textSize) {

        int pos = position;
        boolean terminatingTag = false;

        /*
         * At least one char for tags
         */
        //#ifdef DEBUG_PARSER
//#         AlbiteMIDlet.LOGGER.log("Trying markup: " + textSize + ", " + pos);
        //#endif

        if (textSize > pos && text[pos] == START_TAG_CHAR) {

            //#ifdef DEBUG_PARSER
//#             AlbiteMIDlet.LOGGER.log("parsing markup...");
            //#endif

            state = STATE_PASS;

            /*
             * check if it's a comment tag
             */
            if (pos + 3 < textSize) {
                if (
                           text[pos + 1] == '!'
                        && text[pos + 2] == '-'
                        && text[pos + 3] == '-') {
                    /*
                     * It's indeed a comment tag
                     */
                    position = pos + 4;
                    length = 0;
                    while (position < textSize) {
                        if (text[position] == END_TAG_CHAR
                                && text[position - 1] == '-'
                                && text[position - 2] == '-') {
                            /*
                             * End of comment
                             */
                            position++;
                            break;
                        }
                        position++;
                    }
                    /*
                     * end of comment (no matter closed or not)
                     */
                    return true;
                }
            }

            /*
             * Let's start parsing tags content (if such exists)
             */
            pos++;

            if (pos >= textSize) {
                /*
                 * It was a single '<' character dangling at the end of the file
                 */
                position = pos;
                return true;
            }

            if (text[pos] == '/') {
                terminatingTag = true;
                pos++;
            }

            /*
             * back to position
             */
            position = pos;
            
            if (text.length <= pos) {
                return false;
            }

            for (int i = pos; i < textSize; i++) {

                ch = text[i];

                if (ch == END_TAG_CHAR) {
                    length = i - position + 1;

                    /*
                     * Parse the name
                     */
                    int len = length - 1;
                    int max = position + length - 1;
                    for (int k = position; k < max; k++) {
                        ch = text[k];

                        if (isWhiteSpace(ch) || isNewLine(ch) || ch == '/') {
                            len = k - position;
                            break;
                        }
                    }

                    final String name = new String(text, position, len);

                    //#ifdef DEBUG_PARSER
//#                     AlbiteMIDlet.LOGGER.log("tag: _" + new String(text, position, length) + "_");
//#                     AlbiteMIDlet.LOGGER.log("tag name: _" + name + "_");
//# 
//#                     if (length + position < textSize) {
//#                         AlbiteMIDlet.LOGGER.log("next char to read after this: _" + text[length + position] + "_, " + ((int) text[length + position]));
//#                     }
                    //#endif

                    if (TAG_IMG.equalsIgnoreCase(name)) {
                        /*
                         * Image
                         */
                        final String attributes = new String(
                                text, position + len, length - 1 - len);

                        final int[] srcPositions =
                                XhtmlStreamReader.readAttribute(
                                attributes, "src");

                        if (srcPositions == null) {
                            imageURLPosition = 0;
                            imageURLLength = 0;
                        } else {
                            imageURLPosition = position + len + srcPositions[0];
                            imageURLLength = srcPositions[1];
                        }

                        final int[] altPositions =
                                XhtmlStreamReader.readAttribute(
                                attributes, "alt");

                        if (altPositions == null) {
                            imageTextPosition = 0;
                            imageTextLength = 0;
                        } else {
                            imageTextPosition =
                                    position + len + altPositions[0];
                            imageTextLength = altPositions[1];
                        }

                        state = STATE_IMAGE;
                        return true;
                    }

                    if (TAG_SVG_IMAGE.equalsIgnoreCase(name)) {
                        /*
                         * SVG Image
                         */
                        final String attributes = new String(
                                text, position + len, length - 1 - len);

                        final int[] srcPositions =
                                XhtmlStreamReader.readAttribute(
                                attributes, "xlink:href");

                        if (srcPositions == null) {
                            imageURLPosition = 0;
                            imageURLLength = 0;
                        } else {
                            imageURLPosition = position + len + srcPositions[0];
                            imageURLLength = srcPositions[1];
                        }

                        imageTextPosition = 0;
                        imageTextLength = 0;

                        state = STATE_IMAGE;
                        return true;
                    }

                    /*
                     * Obviously, the image tag won't affect the `hr` variable
                     */
                    final boolean hrOld = hr;
                    hr = false;

                    if (TAG_BR.equalsIgnoreCase(name)) {
                        /*
                         * New line
                         */
                        state = STATE_NEW_LINE;
                        return true;
                    }

                    if (TAG_P.equalsIgnoreCase(name)
                            || TAG_DIV.equalsIgnoreCase(name)
                            || TAG_TR.equalsIgnoreCase(name)
                            || TAG_LI.equalsIgnoreCase(name)) {
                        /*
                         * New line
                         */
                        //#ifdef DEBUG_PARSER
//#                         AlbiteMIDlet.LOGGER.log("executed: <P>");
                        //#endif
                        state = STATE_NEW_SOFT_LINE;
                        return true;
                    }

                    if (TAG_HR.equalsIgnoreCase(name)) {
                        /*
                         * Horizontal ruler
                         */
                        hr = true;

                        if (!hrOld) {
                            instructions.addElement(new Integer(STATE_NEW_SOFT_LINE));
                            instructions.addElement(new Integer(STATE_RULER));
                            instructions.addElement(new Integer(STATE_NEW_SOFT_LINE));
                        }
                        state = STATE_PASS;
                        return true;
                    }

                    if (terminatingTag) {
                        if (TAG_B.equalsIgnoreCase(name)
                                || TAG_STRONG.equalsIgnoreCase(name)) {
                            bold--;

                            if (bold <= 0) {
                                bold = 0;
                                disableBold = true;
                                state = STATE_STYLING;
                            } else {
                                state = STATE_PASS;
                            }
                            return true;
                        }

                        if (TAG_I.equalsIgnoreCase(name)
                                || TAG_EM.equalsIgnoreCase(name)) {
                            italic--;

                            if (italic <= 0) {
                                italic = 0;
                                disableItalic = true;
                                state = STATE_STYLING;
                            } else {
                                state = STATE_PASS;
                            }
                            return true;
                        }

                        if (TAG_H1.equalsIgnoreCase(name)
                                || TAG_H2.equalsIgnoreCase(name)
                                || TAG_H3.equalsIgnoreCase(name)
                                || TAG_H4.equalsIgnoreCase(name)
                                || TAG_H5.equalsIgnoreCase(name)
                                || TAG_H6.equalsIgnoreCase(name)) {
                            heading--;

                            if (heading <= 0) {
                                heading = 0;
                                disableHeading = true;
                                instructions.addElement(new Integer(STATE_STYLING));
                            }
                            
                            state = STATE_NEW_SOFT_LINE;
                            return true;
                        }

                        if (TAG_CENTER.equalsIgnoreCase(name)) {
                            center--;

                            if (center <= 0) {
                                center = 0;
                                disableCenterAlign = true;
                                instructions.addElement(new Integer(STATE_STYLING));
                            }
                            
                            state = STATE_NEW_SOFT_LINE;
                            return true;
                        }

                        if (TAG_PRE.equalsIgnoreCase(name)) {
                            pre--;

                            if (pre <= 0) {
                                pre = 0;
                                processBreaks = false;
                            }

                            state = STATE_PASS;
                            return true;
                        }

                        if (isIgnoreTag(name)) {
                            ignoreTag--;

                            if (ignoreTag < 0) {
                                ignoreTag = 0;
                            }
                            return true;
                        }
                    } else {
                        if (TAG_B.equalsIgnoreCase(name)
                                || TAG_STRONG.equalsIgnoreCase(name)) {
                            bold++;

                            enableBold = true;
                            state = STATE_STYLING;
                            return true;
                        }

                        if (TAG_I.equalsIgnoreCase(name)
                                || TAG_EM.equalsIgnoreCase(name)) {
                            italic++;

                            enableItalic = true;
                            state = STATE_STYLING;
                            return true;
                        }

                        if (TAG_H1.equalsIgnoreCase(name)
                                || TAG_H2.equalsIgnoreCase(name)
                                || TAG_H3.equalsIgnoreCase(name)
                                || TAG_H4.equalsIgnoreCase(name)
                                || TAG_H5.equalsIgnoreCase(name)
                                || TAG_H6.equalsIgnoreCase(name)) {
                            heading++;

                            enableHeading = true;
                            instructions.addElement(new Integer(STATE_NEW_SOFT_LINE));
                            state = STATE_STYLING;
                            return true;
                        }

                        if (TAG_CENTER.equalsIgnoreCase(name)) {
                            center++;

                            enableCenterAlign = true;
                            instructions.addElement(new Integer(STATE_NEW_SOFT_LINE));
                            state = STATE_STYLING;
                            return true;
                        }

                        if (TAG_PRE.equalsIgnoreCase(name)) {
                            int k = position + length + 1;

                            if (k < textSize) {
                                if (text[k] == '\n') {
                                    length += 2;
                                } else if (text[k] == '\r') {
                                    length += 2;
                                    k++;
                                    if (k < textSize && text[k] == '\n') {
                                        length++;
                                    }
                                }
                            }
                            pre++;
                            processBreaks = true;
                            state = STATE_PASS;
                            return true;
                        }

                        if (isIgnoreTag(name)) {
                            ignoreTag++;
                            return true;
                        }
                    }

                    return true;
                }
            }

            /*
             * TODO: Do not know if next line is OK.
             */
            position = textSize;
            length = 1;
            return true;
        }

        return false;
    }

    private static boolean isIgnoreTag(final String s) {
        return
                   "head".equalsIgnoreCase(s)
                || "style".equalsIgnoreCase(s)
                || "form".equalsIgnoreCase(s)
                || "frameset".equalsIgnoreCase(s)
                || "map".equalsIgnoreCase(s)
                || "script".equalsIgnoreCase(s)
                || "object".equalsIgnoreCase(s)
                || "applet".equalsIgnoreCase(s)
                || "noscript".equalsIgnoreCase(s)
                ;
    }
}
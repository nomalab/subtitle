/*
 *  This file is part of the noOp organization .
 *
 *  (c) Cyrille Lebeaupin <clebeaupin@noop.fr>
 *
 *  For the full copyright and license information, please view the LICENSE
 *  file that was distributed with this source code.
 *
 */

package fr.noop.subtitle.ttml;

import java.io.IOException;
import java.io.InputStream;

import fr.noop.subtitle.model.SubtitleParser;
import fr.noop.subtitle.model.SubtitleParsingException;

/**
 * Created by clebeaupin on 11/10/15.
 */
public class TtmlParser implements SubtitleParser {

    public TtmlParser(String charset) {
    }

    @Override
    public TtmlObject parse(InputStream is) throws IOException, SubtitleParsingException {
    	return new TtmlObject();
    }

    @Override
    public TtmlObject parse(InputStream is, boolean strict) throws IOException, SubtitleParsingException {
    	return new TtmlObject();
    }

}

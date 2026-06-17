/*
 *  This file is part of the noOp organization .
 *
 *  (c) Cyrille Lebeaupin <clebeaupin@noop.fr>
 *
 *  For the full copyright and license information, please view the LICENSE
 *  file that was distributed with this source code.
 *
 */

package fr.noop.subtitle.vtt;

import fr.noop.subtitle.util.SubtitleStyle;
import fr.noop.subtitle.util.SubtitleStyledText;

/**
 * Created by clebeaupin on 14/10/15.
 */
public class VttStyledText extends SubtitleStyledText {
    private String voice;

    public VttStyledText(String text, SubtitleStyle style, String voice) {
        super(text, style);
        this.voice = voice;
    }

    public String getVoice() {
        return this.voice;
    }
}

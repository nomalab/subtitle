/*
 *  This file is part of the noOp organization .
 *
 *  (c) Cyrille Lebeaupin <clebeaupin@noop.fr>
 *
 *  For the full copyright and license information, please view the LICENSE
 *  file that was distributed with this source code.
 *
 */

package fr.noop.subtitle.base;

/**
 * Created by clebeaupin on 09/10/15.
 */

import fr.noop.subtitle.model.SubtitleLine;
import fr.noop.subtitle.model.SubtitleText;

import java.util.ArrayList;
import java.util.List;

public abstract class BaseSubtitleLine implements SubtitleLine {
    List<SubtitleText> texts;
    String voice;

    public BaseSubtitleLine() {
        this.texts = new ArrayList<>();
        this.voice = new String();
    }

    public BaseSubtitleLine(List<SubtitleText> texts, String voice) {
        this.texts = texts;
        this.voice = voice;
    }

    public List<SubtitleText> getTexts() {
        return this.texts;
    }

    public void addText(SubtitleText text) {
        this.texts.add(text);
    }

    public String getVoice() {
        return this.voice;
    }

    public void setVoice(String voice) {
        this.voice = voice;
    }

    public boolean isEmpty() {
        return this.toString().isEmpty();
    }

    @Override
    public String toString() {
        String[] texts = new String[this.texts.size()];

        for (int i=0; i<texts.length; i++) {
            texts[i] = this.texts.get(i).toString();
        }

        return String.join("\n", texts);
    }
}

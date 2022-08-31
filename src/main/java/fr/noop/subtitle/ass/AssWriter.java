package fr.noop.subtitle.ass;

import fr.noop.subtitle.ass.HexBGR.Color;
import fr.noop.subtitle.model.SubtitleCue;
import fr.noop.subtitle.model.SubtitleLine;
import fr.noop.subtitle.model.SubtitleObject;
import fr.noop.subtitle.model.SubtitleRegionCue;
import fr.noop.subtitle.model.SubtitleStyled;
import fr.noop.subtitle.model.SubtitleText;
import fr.noop.subtitle.model.SubtitleWriterWithFrameRate;
import fr.noop.subtitle.model.SubtitleWriterWithHeader;
import fr.noop.subtitle.model.SubtitleWriterWithTimecode;
import fr.noop.subtitle.util.SubtitleRegion;
import fr.noop.subtitle.util.SubtitleStyle;
import fr.noop.subtitle.util.SubtitleStyle.FontStyle;
import fr.noop.subtitle.util.SubtitleStyle.FontWeight;
import fr.noop.subtitle.util.SubtitleStyle.TextAlign;
import fr.noop.subtitle.util.SubtitleStyle.TextDecoration;
import fr.noop.subtitle.util.SubtitleTimeCode;
import fr.noop.subtitle.util.SubtitleRegion.VerticalAlign;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;

public class AssWriter implements SubtitleWriterWithHeader, SubtitleWriterWithFrameRate, SubtitleWriterWithTimecode {
    private String charset; // Charset used to encode file
    private String headerText;
    private String newFrameRate;
    private String outputTimecode;

    public AssWriter(String charset) {
        this.charset = charset;
    }

    @Override
    public void write(SubtitleObject subtitleObject, OutputStream os) throws IOException {
        try {
            if (this.headerText != null) {
                // Write Header from file ([Script Info] & [V4+ Styles])
                os.write(headerText.getBytes(this.charset));
                os.write(new String("\n").getBytes(this.charset));
            } else {
                this.writeDefaultHeader(subtitleObject, os);
            }

            // Write cues
            this.writeEvents(subtitleObject, os, outputTimecode, headerText, newFrameRate);
        } catch (UnsupportedEncodingException e) {
            throw new IOException("Encoding error in input subtitle");
        }
    }

    private void writeScriptInfo(SubtitleObject subtitleObject, OutputStream os) throws IOException {
        os.write(new String("[Script Info]\n").getBytes(this.charset));
        os.write(new String("; Script generated by Nomalab Subtitle library\n").getBytes(this.charset));
        if (subtitleObject.hasProperty(SubtitleObject.Property.TITLE)) {
            // Write title
            os.write(String.format("Title: %s\n",
                    subtitleObject.getProperty(SubtitleObject.Property.TITLE)
            ).getBytes(this.charset));
        }
        os.write(new String("ScriptType: v4.00+\n").getBytes(this.charset));
        os.write(new String("PlayDepth: 0\n").getBytes(this.charset));
        os.write(new String("PlayResX: 1920\n").getBytes(this.charset));
        os.write(new String("PlayResY: 1080\n").getBytes(this.charset));
        os.write(new String("\n").getBytes(this.charset));
    }

    private void writeV4Styles(OutputStream os) throws IOException {
        os.write(new String("[V4+ Styles]\n").getBytes(this.charset));
        os.write(new String(
                "Format: Name, Fontname, Fontsize, PrimaryColour, SecondaryColour, OutlineColour, BackColour, Bold, Italic, Underline, StrikeOut, ScaleX, ScaleY, Spacing, Angle, BorderStyle, Outline, Shadow, Alignment, MarginL, MarginR, MarginV, Encoding\n"
        ).getBytes(this.charset));
        os.write(new String(
                "Style: Nomalab_Default,Arial,52,&H00FFFFFF,&H0300FFFF,&H00000000,&H02000000,0,0,0,0,100,100,0,0,1,2,0,2,10,10,10,1\n"
        ).getBytes(this.charset));
        os.write(new String("\n").getBytes(this.charset));
    }

    private void writeDefaultHeader(SubtitleObject subtitleObject, OutputStream os) throws IOException {
        // Write Script Info
        this.writeScriptInfo(subtitleObject, os);

        // Write Style
        this.writeV4Styles(os);
    }

    private void writeEvents(SubtitleObject subtitleObject, OutputStream os, String outputTimecode, String headerText, String newFrameRate) throws IOException {
        SubtitleTimeCode startTimecode = new SubtitleTimeCode(0);
        if (subtitleObject.hasProperty(SubtitleObject.Property.START_TIMECODE_PRE_ROLL)) {
            startTimecode = (SubtitleTimeCode) subtitleObject.getProperty(SubtitleObject.Property.START_TIMECODE_PRE_ROLL);
        }
        float frameRate = 25;
        if (subtitleObject.hasProperty(SubtitleObject.Property.FRAME_RATE)) {
            frameRate = (float) subtitleObject.getProperty(SubtitleObject.Property.FRAME_RATE);
        }
        os.write(new String("[Events]\n").getBytes(this.charset));
        os.write(new String(
                "Format: Layer, Start, End, Style, Name, MarginL, MarginR, MarginV, Effect, Text\n"
        ).getBytes(this.charset));
        for (SubtitleCue cue : subtitleObject.getCues()) {
            String cueText = "";

            SubtitleTimeCode startTC = cue.getStartTime().convertWithOptions(startTimecode, outputTimecode, frameRate, newFrameRate, null);
            SubtitleTimeCode endTC = cue.getEndTime().convertWithOptions(startTimecode, outputTimecode, frameRate, newFrameRate, null);

            String styleName = "Nomalab_Default";
            int vp = 0;

            if (headerText != null) {
                // styles defined in input header file
                styleName = "Default";
                if (cue instanceof SubtitleRegionCue) {
                    SubtitleRegion region = ((SubtitleRegionCue) cue).getRegion();
                    SubtitleText firstLineText = cue.getLines().get(0).getTexts().get(0);
                    if (firstLineText instanceof SubtitleStyled) {
                        SubtitleStyle style = ((SubtitleStyled) firstLineText).getStyle();
                        if (region.getVerticalAlign() == VerticalAlign.TOP) {
                            if (style.getTextAlign() == TextAlign.CENTER) {
                                styleName = "Top";
                            }
                            if (style.getTextAlign() == TextAlign.LEFT) {
                                styleName = "Top_Left";
                            }
                            if (style.getTextAlign() == TextAlign.RIGHT) {
                                styleName = "Top_Right";
                            }
                        } else {
                            if (style.getTextAlign() == TextAlign.LEFT) {
                                styleName = "Bottom_Left";
                            }
                            if (style.getTextAlign() == TextAlign.RIGHT) {
                                styleName = "Bottom_Right";
                            }
                        }
                    }
                }
            } else {
                if (cue instanceof SubtitleRegionCue) {
                    vp = ((SubtitleRegionCue) cue).getRegion().getVerticalPosition();
                }
            }

            cueText += addStyle(cue, headerText);

            os.write(String.format("Dialogue: 0,%s,%s,%s,,0,0,%d,,%s\n",
                    startTC.singleHourTimeToString(), endTC.singleHourTimeToString(), styleName, vp, cueText
            ).getBytes(this.charset));
        }
        os.write(new String("\n").getBytes(this.charset));
    }

    private String addStyle(SubtitleCue cue, String headerText) {
        String styled = "";
        if (headerText == null) {
            int posX = 1920 / 2;
            int posY = 1020; //default vertical position
            if (cue instanceof SubtitleRegionCue) {
                SubtitleRegion region = ((SubtitleRegionCue) cue).getRegion();
                if (region.getVerticalAlign() == VerticalAlign.TOP) {
                    posY = 160;
                }
            }
            String position = String.format("{\\pos(%d,%d)}", posX, posY);
            styled += position;
        }
        int lineIndex = 0;
            for (SubtitleLine line : cue.getLines()) {
                lineIndex++;
                for (SubtitleText text : line.getTexts()) {
                    String endStyle = "";
                    if (text instanceof SubtitleStyled) {
                        SubtitleStyle style = ((SubtitleStyled) text).getStyle();
                        if (style.getFontStyle() == FontStyle.ITALIC || style.getFontStyle() == FontStyle.OBLIQUE) {
                            styled += "{\\i1}";
                            endStyle += "{\\i0}";
                        }
                        if (style.getFontWeight() == FontWeight.BOLD) {
                            styled += "{\\b1}";
                            endStyle += "{\\b0}";
                        }
                        if (style.getTextDecoration() == TextDecoration.UNDERLINE) {
                            styled += "{\\u1}";
                            endStyle += "{\\u0}";
                        }
                        if (style.getColor() != null){
                            Color color = HexBGR.Color.getEnumFromName(style.getColor());
                            styled += String.format("{\\c%s}", color.getHexValue());
                        }
                    }
                    styled += text.toString();
                    styled += endStyle;
                    // Add line break between rows
                    if (cue.getLines().size() > lineIndex) {
                        styled += "\\N";
                    }
                }
            }
        return styled;
    }

    @Override
    public void setHeaderText(String headerText) {
        this.headerText = headerText;
    }

    @Override
    public void setFrameRate(String frameRate) {
        this.newFrameRate = frameRate;
    }

    @Override
    public void setTimecode(String timecode) {
        this.outputTimecode= timecode;
    }
}

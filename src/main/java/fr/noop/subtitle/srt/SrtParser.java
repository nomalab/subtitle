/*
 *  This file is part of the noOp organization .
 *
 *  (c) Cyrille Lebeaupin <clebeaupin@noop.fr>
 *
 *  For the full copyright and license information, please view the LICENSE
 *  file that was distributed with this source code.
 *
 */

package fr.noop.subtitle.srt;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import fr.noop.subtitle.model.SubtitleParser;
import fr.noop.subtitle.model.SubtitleParsingException;
import fr.noop.subtitle.srt.HexRGB.Color;
import fr.noop.subtitle.util.SubtitlePlainText;
import fr.noop.subtitle.util.SubtitleRegion;
import fr.noop.subtitle.util.SubtitleStyle;
import fr.noop.subtitle.util.SubtitleStyledText;
import fr.noop.subtitle.util.SubtitleTextLine;
import fr.noop.subtitle.util.SubtitleTimeCode;
import fr.noop.subtitle.util.SubtitleStyle.TextAlign;
import fr.noop.subtitle.util.SubtitleStyle.FontStyle;
import fr.noop.subtitle.util.SubtitleStyle.FontWeight;
import fr.noop.subtitle.util.SubtitleStyle.TextDecoration;

/**
 * Created by clebeaupin on 21/09/15.
 */
public class SrtParser implements SubtitleParser {
    private enum CursorStatus {
        NONE,
        CUE_ID,
        CUE_TIMECODE,
        CUE_TEXT;
    }

    private String charset; // Charset of the input files

    public SrtParser(String charset) {
        this.charset = charset;
    }

    @Override
    public SrtObject parse(InputStream is) throws IOException, SubtitleParsingException {
        return parse(is, true);
    }

    @Override
    public SrtObject parse(InputStream is, boolean strict) throws IOException, SubtitleParsingException {
        // Create srt object
        SrtObject srtObject = new SrtObject();

        // Read each lines
        BufferedReader br = new BufferedReader(new InputStreamReader(is, this.charset));
        String textLine = "";
        CursorStatus cursorStatus = CursorStatus.NONE;
        SrtCue cue = null;
        String hexCode = null;
        boolean color = false;
        SubtitleRegion region = null;

        while ((textLine = br.readLine()) != null) {
            textLine = textLine.trim();

            if (cursorStatus == CursorStatus.NONE) {
                if (textLine.isEmpty()) {
                    continue;
                }

                // New cue
                cue = new SrtCue();
                region = new SubtitleRegion(0, 0);

                // First textLine is the cue number
                try {
                    Integer.parseInt(textLine);
                } catch (NumberFormatException e) {
                    throw new SubtitleParsingException(String.format(
                            "Unable to parse cue number: %s",
                            textLine));
                }

                cue.setId(textLine);
                cursorStatus = CursorStatus.CUE_ID;
                continue;
            }

            // Second textLine defines the start and end time codes
            // 00:01:21,456 --> 00:01:23,417
            if (cursorStatus == CursorStatus.CUE_ID) {
                if (!textLine.substring(13, 16).equals("-->")) {
                    throw new SubtitleParsingException(String.format(
                            "Timecode textLine is badly formated: %s", textLine));
                }

                cue.setStartTime(SubtitleTimeCode.parseTimeCode(textLine.substring(0, 12)));
                cue.setEndTime(SubtitleTimeCode.parseTimeCode(textLine.substring(17)));
                cursorStatus = CursorStatus.CUE_TIMECODE;
                continue;
            }

            // Following lines are the cue lines
            if (!textLine.isEmpty() && (cursorStatus == CursorStatus.CUE_TIMECODE ||
                    cursorStatus == CursorStatus.CUE_TEXT)) {
                SubtitleTextLine line = new SubtitleTextLine();
                SubtitleStyle textStyle = new SubtitleStyle();

                if (textLine.contains("{\\an1}")) {
                    region.setVerticalAlign(SubtitleRegion.VerticalAlign.BOTTOM);
                    textStyle.setTextAlign(TextAlign.LEFT);
                    textLine = textLine.replaceAll("\\{\\\\an1\\}", "");
                }
                if (textLine.contains("{\\an2}")) {
                    region.setVerticalAlign(SubtitleRegion.VerticalAlign.BOTTOM);
                    textStyle.setTextAlign(TextAlign.CENTER);
                    textLine = textLine.replaceAll("\\{\\\\an2\\}", "");
                }
                if (textLine.contains("{\\an3}")) {
                    region.setVerticalAlign(SubtitleRegion.VerticalAlign.BOTTOM);
                    textStyle.setTextAlign(TextAlign.RIGHT);
                    textLine = textLine.replaceAll("\\{\\\\an3\\}", "");
                }
                if (textLine.contains("{\\an4}")) {
                    region.setVerticalAlign(SubtitleRegion.VerticalAlign.MIDDLE);
                    textStyle.setTextAlign(TextAlign.LEFT);
                    textLine = textLine.replaceAll("\\{\\\\an4\\}", "");
                }
                if (textLine.contains("{\\an5}")) {
                    region.setVerticalAlign(SubtitleRegion.VerticalAlign.MIDDLE);
                    textStyle.setTextAlign(TextAlign.CENTER);
                    textLine = textLine.replaceAll("\\{\\\\an5\\}", "");
                }
                if (textLine.contains("{\\an6}")) {
                    region.setVerticalAlign(SubtitleRegion.VerticalAlign.MIDDLE);
                    textStyle.setTextAlign(TextAlign.RIGHT);
                    textLine = textLine.replaceAll("\\{\\\\an6\\}", "");
                }
                if (textLine.contains("{\\an7}")) {
                    region.setVerticalAlign(SubtitleRegion.VerticalAlign.TOP);
                    textStyle.setTextAlign(TextAlign.LEFT);
                    textLine = textLine.replaceAll("\\{\\\\an7\\}", "");
                }
                if (textLine.contains("{\\an8}")) {
                    region.setVerticalAlign(SubtitleRegion.VerticalAlign.TOP);
                    textStyle.setTextAlign(TextAlign.CENTER);
                    textLine = textLine.replaceAll("\\{\\\\an8\\}", "");
                }
                if (textLine.contains("{\\an9}")) {
                    textStyle.setTextAlign(TextAlign.RIGHT);
                    region.setVerticalAlign(SubtitleRegion.VerticalAlign.TOP);
                    textLine = textLine.replaceAll("\\{\\\\an9\\}", "");
                }

                if (textLine.contains("<font color=")) {
                    color = true;
                    Pattern pattern = Pattern.compile("#(?:[a-fA-F\\d]{3}){1,2}\\b");
                    Matcher matcher = pattern.matcher(textLine);
                    if (matcher.find()) {
                        hexCode = matcher.group();
                    }
                    textLine = textLine.replaceAll("<font color=\"#(?:[a-fA-F\\d]{3}){1,2}\\b\">", "");
                }
                if (color && hexCode != null) {
                    Color c = Color.getEnumFromHexCode(hexCode);
                    if (c != Color.WHITE) {
                        textStyle.setColor(c.getColorName());
                    }
                }
                if (textLine.contains("</font>")) {
                    color = false;
                    textLine = textLine.replaceAll("</font>", "");
                }

                if (textLine.contains("<i>") || textLine.contains("<b>") || textLine.contains("<u>")) {
                    int cIndex = 0;
                    String newText = new String();
                    SubtitleStyle newStyle = new SubtitleStyle(textStyle);
                    while (cIndex < textLine.length()) {
                        char cc = textLine.charAt(cIndex);
                        if (cc == '<') {
                            int tagLength = 3;
                            if (textLine.charAt(cIndex+1) == '/') {
                                tagLength = 4;
                            }
                            String tag = textLine.substring(cIndex, cIndex+tagLength);
                            if (!newText.isEmpty()) {
                                if (newStyle.hasProperties()) {
                                    line.addText(new SubtitleStyledText(newText, new SubtitleStyle(newStyle)));
                                } else {
                                    line.addText(new SubtitlePlainText(newText));
                                }
                            }
                            if (tag.equals("<i>")) {
                                newStyle.setFontStyle(FontStyle.ITALIC);
                            }
                            if (tag.equals("<b>")) {
                                newStyle.setFontWeight(FontWeight.BOLD);
                            }
                            if (tag.equals("<u>")) {
                                newStyle.setTextDecoration(TextDecoration.UNDERLINE);
                            }
                            if (tag.equals("</i>")) {
                                newStyle.setFontStyle(FontStyle.NORMAL);
                            }
                            if (tag.equals("</b>")) {
                                newStyle.setFontWeight(FontWeight.NORMAL);
                            }
                            if (tag.equals("</u>")) {
                                newStyle.setTextDecoration(TextDecoration.NONE);
                            }

                            newText = new String();
                            cIndex += tagLength;
                        } else {
                            newText += cc;
                            cIndex++;
                        }
                    }
                    if (!newText.isEmpty()) {
                        if (newStyle.hasProperties()) {
                            line.addText(new SubtitleStyledText(newText, newStyle));
                        } else {
                            line.addText(new SubtitlePlainText(newText));
                        }
                    }
                }

                if (textLine.contains("\\N")) {
                    // Ignore \N
                    textLine = textLine.replaceAll("\\\\N", "");
                }

                if (line.isEmpty() && !textLine.isEmpty()) {
                    if (textStyle.hasProperties()) {
                        line.addText(new SubtitleStyledText(textLine, textStyle));
                    } else {
                        line.addText(new SubtitlePlainText(textLine));
                    }
                }
                if (!line.isEmpty()) {
                    cue.addLine(line);
                }
                cursorStatus = CursorStatus.CUE_TEXT;
                continue;
            }

            if (cursorStatus == CursorStatus.CUE_TEXT && textLine.isEmpty()) {
                // End of cue
                cue.setRegion(region);
                srtObject.addCue(cue);
                cue = null;
                region = null;
                cursorStatus = CursorStatus.NONE;
                continue;
            }

            throw new SubtitleParsingException(String.format(
                    "Unexpected line: %s", textLine));
        }

        if (cue != null) {
            cue.setRegion(region);
            srtObject.addCue(cue);
        }

        return srtObject;
    }
}

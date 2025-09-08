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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import fr.noop.subtitle.model.SubtitleLine;
import fr.noop.subtitle.model.SubtitleParser;
import fr.noop.subtitle.model.SubtitleParsingException;
import fr.noop.subtitle.util.SubtitleRegion;
import fr.noop.subtitle.util.SubtitlePlainText;
import fr.noop.subtitle.util.SubtitleStyle;
import fr.noop.subtitle.util.SubtitleStyledText;
import fr.noop.subtitle.util.SubtitleTimeCode;

import org.apache.commons.lang3.StringUtils;

/**
 * Created by clebeaupin on 11/10/15.
 */
public class VttParser implements SubtitleParser {

    private enum CursorStatus {
        NONE,
        SIGNATURE,
        HEADER,
        STYLE,
        EMPTY_LINE,
        CUE_ID,
        CUE_TIMECODE,
        CUE_TEXT,
        CUE_POSITION;
    }

    private enum TagStatus {
        NONE,
        OPEN,
        CLOSE
    }

    private String charset; // Charset of the input files

    public VttParser(String charset) {
        this.charset = charset;
    }

    @Override
    public VttObject parse(InputStream is) throws IOException, SubtitleParsingException {
    	return parse(is, true);
    }

    @Override
    public VttObject parse(InputStream is, boolean strict) throws IOException, SubtitleParsingException {
        // Create srt object
        VttObject vttObject = new VttObject();

        // Read each lines
        BufferedReader br = new BufferedReader(new InputStreamReader(is, this.charset));
        String textLine = "";
        CursorStatus cursorStatus = CursorStatus.NONE;
        VttCue cue = null;
        VttStyle style = new VttStyle();
        String cueText = ""; // Text of the cue
        SubtitleTimeCode previousIn = new SubtitleTimeCode(0);
        SubtitleTimeCode previousOut = new SubtitleTimeCode(0);

        while ((textLine = br.readLine()) != null) {
            textLine = textLine.trim();

            // Remove BOM
            if (cursorStatus == CursorStatus.NONE) {
                textLine = fr.noop.subtitle.util.StringUtils.removeBOM(textLine);
            }

            // All Vtt files start with WEBVTT
            if (cursorStatus == CursorStatus.NONE && textLine.equals("WEBVTT")) {
                cursorStatus = CursorStatus.SIGNATURE;
                continue;
            }

            // Optional X-TIMESTAMP-MAP header (HLS)
            if (cursorStatus == CursorStatus.SIGNATURE && textLine.startsWith("X-TIMESTAMP-MAP")) {
                cursorStatus = CursorStatus.HEADER;
                continue;
            }

            // Optional STYLE blocks
            if ((cursorStatus == CursorStatus.HEADER ||
                    cursorStatus == CursorStatus.SIGNATURE ||
                    cursorStatus == CursorStatus.EMPTY_LINE) &&
                    vttObject.getCues().isEmpty() &&
                    textLine.startsWith("STYLE")) {
                cursorStatus = CursorStatus.STYLE;
                continue;
            }

            // Parse STYLE block
            if (cursorStatus == CursorStatus.STYLE) {
                List<String> styleBLock = new ArrayList<>();
                String nextLine = "";
                styleBLock.add(textLine);

                // read until the end of the style block
                while ((nextLine = br.readLine()) != null && !nextLine.isBlank()) {
                    nextLine = nextLine.trim();
                    styleBLock.add(nextLine);
                }

                style.addStyleBlock(String.join("", styleBLock));
                cursorStatus = CursorStatus.EMPTY_LINE;
                continue;
            }

            if (cursorStatus == CursorStatus.SIGNATURE ||
                    cursorStatus == CursorStatus.HEADER ||
                    cursorStatus == CursorStatus.EMPTY_LINE) {
                if (textLine.isEmpty()) {
                    continue;
                }

                // New cue
                cue = new VttCue();
                cursorStatus = CursorStatus.CUE_ID;

                if (
                    textLine.length() < 16 ||
                    !textLine.substring(13, 16).equals("-->")
                ) {
                    // First textLine is the cue number
                    cue.setId(textLine);
                    continue;
                }

                // There is no cue number
            }


            // Second textLine defines the start and end time codes
            // 00:01:21.456 --> 00:01:23.417
            if (cursorStatus == CursorStatus.CUE_ID) {
                if (textLine.length() < 29 ||
                    !textLine.substring(13, 16).equals("-->")
                ) {
                    throw new SubtitleParsingException(String.format(
                            "Timecode textLine is badly formated: %s", textLine));
                }

                SubtitleTimeCode startTime = SubtitleTimeCode.parseTimeCode(textLine.substring(0, 12));
                SubtitleTimeCode endTime = SubtitleTimeCode.parseTimeCode(textLine.substring(17));
                if (previousOut.compareTo(startTime) == 1) {
                    System.out.printf("Subtitle from %s to %s overlaps previous subtitle (%s - %s)\n",
                    startTime, endTime, previousIn, previousOut);
                }
                previousIn = startTime;
                previousOut = endTime;

                cue.setStartTime(startTime);
                cue.setEndTime(endTime);
                cursorStatus = CursorStatus.CUE_TIMECODE;

                SubtitleRegion region = new SubtitleRegion(0, 0);
                if (textLine.contains("line:")) {
                    String line = StringUtils.substringAfter(textLine, "line:").split(" ")[0];
                    float positionPercent = 0;
                    if (line.contains("%")) {
                        line = line.replaceAll("%", "");
                        positionPercent = Float.parseFloat(line);
                    } else {
                        if (Float.parseFloat(line) < 0) {
                            positionPercent = (1080 + Float.parseFloat(line)) / 1080 * 100;
                        } else {
                            positionPercent = Float.parseFloat(line) / 1080 * 100;
                        }
                    }
                    if (positionPercent <= 50) {
                        region.setVerticalAlign(SubtitleRegion.VerticalAlign.TOP);
                    }
                    cursorStatus = CursorStatus.CUE_POSITION;
                }
                cue.setRegion(region);
                continue;
            }

            if (
                (
                    cursorStatus == CursorStatus.CUE_TIMECODE ||
                    cursorStatus == CursorStatus.CUE_POSITION
                ) &&
                textLine.isEmpty() &&
                strict
            ) {
                // Do not accept empty subtitle if strict
                throw new SubtitleParsingException(String.format(
                        "Empty subtitle is not allowed in WebVTT for cue at timecode: %s", cue.getStartTime()));
            }

            // Enf of cue
            if (
                (
                    cursorStatus == CursorStatus.CUE_TIMECODE ||
                    cursorStatus == CursorStatus.CUE_POSITION ||
                    cursorStatus == CursorStatus.CUE_TEXT
                ) &&
                textLine.isEmpty()
            ) {
                // End of cue
                // Process multilines text in one time
                // A class or a style can be applied for more than one line
                cue.setLines(parseCueText(cueText, style));
                vttObject.addCue(cue);
                cue = null;
                cueText = "";
                cursorStatus = CursorStatus.EMPTY_LINE;
                continue;
            }

            // Add new text to cue
            if (cursorStatus == CursorStatus.CUE_TIMECODE ||
                cursorStatus == CursorStatus.CUE_POSITION ||
                cursorStatus ==  CursorStatus.CUE_TEXT
            ) {
                // New line
                if (!cueText.isEmpty()) {
                    cueText += "\n";
                }

                cueText += textLine;
                cursorStatus = CursorStatus.CUE_TEXT;
                continue;
            }



        	throw new SubtitleParsingException(String.format(
        			"Unexpected line: %s", textLine));
        }

        // Add last line
        if (cursorStatus == CursorStatus.CUE_TEXT && !cueText.isEmpty()) {
            cue.setLines(parseCueText(cueText, style));
            vttObject.addCue(cue);
        }

        return vttObject;
    }

    private List<SubtitleLine> parseCueText(String cueText, VttStyle styleBlocks) {
        String text = "";
        String color = null;
        List<String> tags = new ArrayList<>();
        List<SubtitleLine> cueLines = new ArrayList<>();
        VttLine cueLine = null; // Current cue line

        // Process:
        // - voice
        // - class
        // - styles
        for (int i=0; i<cueText.length(); i++) {
            String tag = null;
            TagStatus tagStatus = TagStatus.NONE;
            char c = cueText.charAt(i);

            if (c != '\n') {
                // Remove this newline from text
                text += c;
            }

            // Last characters (3 characters max)
            String textEnd = text.substring(Math.max(0, text.length()-3), text.length());

            // Get style blocks
            Map<VttStyle.Property, Object> allCuesStyle = styleBlocks.getStyleForTag(VttStyle.VttTextTag.ALL);
            Map<VttStyle.Property, Object> boldCuesStyle = styleBlocks.getStyleForTag(VttStyle.VttTextTag.BOLD);
            Map<VttStyle.Property, Object> classCuesStyle = styleBlocks.getStyleForTag(VttStyle.VttTextTag.CLASS);
            Map<VttStyle.Property, Object> italicCuesStyle = styleBlocks.getStyleForTag(VttStyle.VttTextTag.ITALIC);
            Map<VttStyle.Property, Object> langCuesStyle = styleBlocks.getStyleForTag(VttStyle.VttTextTag.LANG);
            Map<VttStyle.Property, Object> rubyCuesStyle = styleBlocks.getStyleForTag(VttStyle.VttTextTag.RUBY);
            Map<VttStyle.Property, Object> rubyTextCuesStyle = styleBlocks.getStyleForTag(VttStyle.VttTextTag.RUBY_TEXT);
            Map<VttStyle.Property, Object> underlineCuesStyle = styleBlocks.getStyleForTag(VttStyle.VttTextTag.UNDERLINE);
            Map<VttStyle.Property, Object> voiceCuesStyle = styleBlocks.getStyleForTag(VttStyle.VttTextTag.VOICE);

            if (textEnd.equals("<b>") || textEnd.equals("<u>") || textEnd.equals("<i>") ||
                    textEnd.equals("<v ") || textEnd.equals("<c.") || textEnd.equals("<c ")) {
                // Open tag
                tag = String.valueOf(textEnd.charAt(1));
                tagStatus = TagStatus.OPEN;

                // Add tag
                tags.add(tag);

                // Remove open tag from text
                text = text.substring(0, text.length()-3);
            } else if (textEnd.equals("<br")) {
                tag = "br";
                tagStatus = TagStatus.OPEN;
                tags.add(tag);
                text = text.substring(0, text.length()-3);
                continue;
            } else if (c == '>') {
                // Close tag
                tagStatus = TagStatus.CLOSE;

                // Pop tag from tags
                tag = tags.remove(tags.size()-1);

                int closeTagLength = 1; // Size in chars of the close tag

                if (textEnd.charAt(0) == '/') {
                    if (tag == "br") {
                        closeTagLength = 2;
                    } else {
                        // Real close tag: </u>, </c>, </b>, </i>
                        closeTagLength = 4;
                    }
                }

                // Remove close tag from text
                text = text.substring(0, text.length()-closeTagLength);
            } else if (c != '\n' && i < cueText.length()-1){
                continue;
            }

            if (c != '\n' && text.isEmpty()) {
                if (cueLine != null && !cueLine.isEmpty()) {
                    // Line is finished
                    cueLines.add(cueLine);
                    cueLine = null;
                }
                continue;
            }

            if (cueLine == null) {
                cueLine = new VttLine();
            }

            // Create text, apply styles and append to the cue line
            SubtitleStyle style = new SubtitleStyle();
            List<String> analyzedTags = new ArrayList<>();
            analyzedTags.addAll(tags);

            if (tagStatus == TagStatus.CLOSE) {
                // Apply style from last close tag
                analyzedTags.add(tag);
            } else if (tagStatus == TagStatus.OPEN) {
                analyzedTags.remove(tags.size() - 1);
            }

            // Apply styles from all cues block
            allCuesStyle.forEach((k, v) -> style.setProperty(k, v));

            for (String analyzedTag: analyzedTags) {
                if (analyzedTag.equals("v")) {
                    voiceCuesStyle.forEach((k, v) -> style.setProperty(k, v));
                    cueLine.setVoice(text);
                    text = "";
                    break;
                }

                // Bold characters
                if (analyzedTag.equals("b")) {
                    boldCuesStyle.forEach((k, v) -> style.setProperty(k, v));
                    style.setProperty(VttStyle.Property.FONT_WEIGHT, VttStyle.FontWeight.BOLD);
                    continue;
                }

                // Italic characters
                if (analyzedTag.equals("i")) {
                    italicCuesStyle.forEach((k, v) -> style.setProperty(k, v));
                    style.setProperty(VttStyle.Property.FONT_STYLE, VttStyle.FontStyle.ITALIC);
                    continue;
                }

                // Underline characters
                if (analyzedTag.equals("u")) {
                    underlineCuesStyle.forEach((k, v) -> style.setProperty(k, v));
                    style.setProperty(VttStyle.Property.TEXT_DECORATION, VttStyle.TextDecoration.UNDERLINE);
                    continue;
                }

                // Class apply to characters
                if (analyzedTag.equals("c")) {
                    // Cannot convert class
                    if (tagStatus == TagStatus.CLOSE && tag.equals("c") && !textEnd.equals("/c>")) {
                        // This is not a real close tag
                        // so push it again
                        color = text;
                        text = "";
                        tags.add(tag);
                    }
                    if (color != null) {
                        classCuesStyle.forEach((k, v) -> style.setProperty(k, v));
                        style.setColor(color);
                    }

                    continue;
                }
            }

            if (!text.isEmpty()) {
                if (style.hasProperties()) {
                    cueLine.addText(new SubtitleStyledText(text, style));
                } else {
                    cueLine.addText(new SubtitlePlainText(text));
                }
            }

            if ((c == '\n' || i == (cueText.length()-1)) && !cueLine.isEmpty()) {
                // Line is finished
                cueLines.add(cueLine);
                cueLine = null;
            }

            text = "";
        }

        return cueLines;
    }
}
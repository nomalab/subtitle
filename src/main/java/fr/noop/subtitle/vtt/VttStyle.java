package fr.noop.subtitle.vtt;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import fr.noop.subtitle.util.SubtitleStyle;

public class VttStyle extends SubtitleStyle {

    // https://developer.mozilla.org/en-US/docs/Web/API/WebVTT_API/Web_Video_Text_Tracks_Format#cue_payload_text_tags
    private enum VttTextTag {
        ALL,
        ITALIC,
        BOLD,
        UNDERLINE,
        CLASS,
        RUBY,
        RUBY_TEXT,
        VOICE,
        LANG;
    }

    private Map<VttTextTag, List<Map<Property, String>>> styleBlocks = new HashMap<>();

    public Map<VttTextTag, List<Map<Property, String>>> getstyleBlocks() {
        return styleBlocks;
    }

    public void setStyleBlocks(String styleBlock) {
        String[] parts = styleBlock.split("\\{");

        // clean tag list
        String[] tagList = parts[0].split(",");
        // clean css list
        String[] cssList = parts[1].trim().substring(0, parts[1].length() - 1).split(";");

        for (String tag : tagList) {
            // get each text tag
            tag = tag.trim();
            VttTextTag textTag = getVttTextTag(tag);

            for (String css : cssList) {
                // get each css property and value
                String[] cssParts = css.split(":");
                String property = cssParts[0].trim();
                String value = cssParts[1].trim();

                // save new styles for the text tag
                Map<Property, String> prop = new HashMap<>();
                prop.put(getProperty(property), value);
                styleBlocks
                        .computeIfAbsent(textTag, k -> new ArrayList<>())
                        .add(prop);
            }
        }
    }

    private VttTextTag getVttTextTag(String tag) {
        switch (tag) {
            case "::cue(i)":
                return VttTextTag.ITALIC;
            case "::cue(b)":
                return VttTextTag.BOLD;
            case "::cue(u)":
                return VttTextTag.UNDERLINE;
            case "::cue(c)":
                return VttTextTag.CLASS;
            case "::cue(ruby)":
                return VttTextTag.RUBY;
            case "::cue(rt)":
                return VttTextTag.RUBY_TEXT;
            case "::cue(v)":
                return VttTextTag.VOICE;
            case "::cue(lang)":
                return VttTextTag.LANG;
            case "::cue":
                return VttTextTag.ALL;
            default:
                throw new IllegalArgumentException(
                        "Unknown VTT text tag or not supported yet (feel free to contribute !) : " + tag);
        }
    }

    private SubtitleStyle.Property getProperty(String property) {
        switch (property) {
            case "direction":
                return Property.DIRECTION;
            case "text-align":
                return Property.TEXT_ALIGN;
            case "color":
                return Property.COLOR;
            case "font-style":
                return Property.FONT_STYLE;
            case "font-weight":
                return Property.FONT_WEIGHT;
            case "text-decoration":
                return Property.TEXT_DECORATION;
            case "effect":
                return Property.EFFECT;
            default:
                throw new IllegalArgumentException(
                        "Unknown CSS property or not supported yet (feel free to contribute !) : " + property);
        }
    }
}

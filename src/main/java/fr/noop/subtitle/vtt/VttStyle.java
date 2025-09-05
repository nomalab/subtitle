package fr.noop.subtitle.vtt;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import fr.noop.subtitle.util.SubtitleStyle;

public class VttStyle extends SubtitleStyle {

    // https://developer.mozilla.org/en-US/docs/Web/API/WebVTT_API/Web_Video_Text_Tracks_Format#cue_payload_text_tags
    public enum VttTextTag {
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

    private Map<VttTextTag, List<Map<Property, Object>>> styleBlocks = new HashMap<>();

    public Map<VttTextTag, List<Map<Property, Object>>> getstyleBlocks() {
        return styleBlocks;
    }

    public void setStyleBlocks(String styleBlock) {
        // remove last '}' and split block into tag and css lists
        String[] parts = styleBlock.substring(0, styleBlock.length() - 1).split("\\{");

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

                // check property and value then save
                Map<Property, Object> style = new HashMap<>();
                SubtitleStyle.Property prop = getProperty(property);
                style.put(prop, checkProperty(prop, value));
                styleBlocks
                        .computeIfAbsent(textTag, k -> new ArrayList<>())
                        .add(style);
            }
        }
    }

    public Map<Property, Object> getStyleForTag(VttTextTag tag) {
        Map<Property, Object> props = new HashMap<>();
        if (styleBlocks.containsKey(tag)) {
            for (Map<Property, Object> prop : styleBlocks.get(tag)) {
                for (Map.Entry<Property, Object> entry : prop.entrySet()) {
                    props.put(entry.getKey(), entry.getValue());
                }
            }
        }
        return props;
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

    private Object checkProperty(SubtitleStyle.Property property, String value) {
        switch (property) {
            case DIRECTION:
                return mapDirection(value);
            case TEXT_ALIGN:
                return mapTextAlign(value);
            case COLOR:
                return value;
            case FONT_STYLE:
                return mapFontStyle(value);
            case FONT_WEIGHT:
                return mapFontWeight(value);
            case TEXT_DECORATION:
                return mapTextDecoration(value);
            case EFFECT:
                return mapEffect(value);
            default:
                throw new IllegalArgumentException(
                        "Unknown CSS property or not supported yet : " + property);
        }
    }

    private TextDecoration mapTextDecoration(String css) {
        css = css.trim().toLowerCase();
        switch (css) {
            case "underline":
                return TextDecoration.UNDERLINE;
            case "overline":
                return TextDecoration.OVERLINE;
            case "line-through":
                return TextDecoration.LINE_THROUGH;
            default:
                throw new IllegalArgumentException(
                        "Unknown CSS property or not supported yet : " + css);
        }
    }

    private Direction mapDirection(String css) {
        css = css.trim().toLowerCase();
        switch (css) {
            case "rtl":
                return Direction.RTL;
            case "ltr":
                return Direction.LTR;
            default:
                throw new IllegalArgumentException(
                        "Unknown CSS property or not supported yet : " + css);
        }
    }

    private TextAlign mapTextAlign(String css) {
        css = css.trim().toLowerCase();
        switch (css) {
            case "center":
                return TextAlign.CENTER;
            case "right":
                return TextAlign.RIGHT;
            case "left":
                return TextAlign.LEFT;
            default:
                throw new IllegalArgumentException(
                        "Unknown CSS property or not supported yet : " + css);
        }
    }

    private FontStyle mapFontStyle(String css) {
        css = css.trim().toLowerCase();
        switch (css) {
            case "italic":
                return FontStyle.ITALIC;
            case "oblique":
                return FontStyle.OBLIQUE;
            case "normal":
                return FontStyle.NORMAL;
            default:
                throw new IllegalArgumentException(
                        "Unknown CSS property or not supported yet : " + css);
        }
    }

    private FontWeight mapFontWeight(String css) {
        css = css.trim().toLowerCase();
        switch (css) {
            case "bold":
                return FontWeight.BOLD;
            case "normal":
                return FontWeight.NORMAL;
            default:
                throw new IllegalArgumentException(
                        "Unknown CSS property or not supported yet : " + css);
        }
    }

    private Effect mapEffect(String css) {
        css = css.trim().toLowerCase();
        switch (css) {
            case "box":
                return Effect.BOX;
            default:
                throw new IllegalArgumentException(
                        "Unknown CSS property or not supported yet : " + css);
        }
    }
}

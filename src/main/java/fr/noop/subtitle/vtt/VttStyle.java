package fr.noop.subtitle.vtt;

import java.util.ArrayList;
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
        VOICE,
    }

    private Map<VttTextTag, List<Map<Property, Object>>> styleBlocks = new HashMap<>();

    public void addStyleBlock(String styleBlock) {
        styleBlock = removeCssComments(styleBlock);
        // remove last '}' and split block into tag and css lists
        String[] parts = styleBlock.substring(0, styleBlock.length() - 1).split("\\{");

        // clean lists
        String[] tagList = parts[0].split(",");
        String[] cssList = parts[1].trim().substring(0, parts[1].length() - 1).split(";");

        for (String tag : tagList) {
            // get each vtt text tag
            tag = tag.trim();
            VttTextTag textTag = fromStrToVttTextTag(tag);

            for (String css : cssList) {
                // get each css property and value
                String[] cssParts = css.split(":");
                if (cssParts.length < 2) {
                    throw new IllegalArgumentException("Invalid CSS declaration : " + css);
                }
                String cssProperty = cssParts[0].trim();
                String cssValue = cssParts[1].trim();

                // check property and value then save
                Map<Property, Object> style = new HashMap<>();
                try {
                    SubtitleStyle.Property subtitleStylePropery = fromStrToProperty(cssProperty);
                    style.put(subtitleStylePropery, checkAndGetCSSValue(subtitleStylePropery, cssValue));
                } catch (Exception e) {
                    System.out.println(e.getMessage());
                }
                styleBlocks
                        .computeIfAbsent(textTag, k -> new ArrayList<>())
                        .add(style);
            }
        }
    }

    private String removeCssComments(String str) {
        while (str.contains("/*") && str.contains("*/") && str.indexOf("/*") < str.indexOf("*/")) {
            int startIndex = 0, endIndex = 0, i = 0;
            char lastC = 'a';
            for (char c : str.toCharArray()) {
                // if there is '/*' and start is not set
                if (c == '*' && lastC == '/' && startIndex == 0) {
                    startIndex = i - 1;
                }
                // if there is '*/' and start is set
                if (c == '/' && lastC == '*' && startIndex != 0) {
                    endIndex = i + 1;
                    str = str.substring(0, startIndex) + str.substring(endIndex);
                    // as css block is less longer than before, we must decresease index by the comment size
                    i = i - (endIndex - startIndex);
                    startIndex = 0;
                }
                i = i + 1;
                lastC = c;
            }
        }
        return str;
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

    private VttTextTag fromStrToVttTextTag(String tag) {
        return switch (tag) {
            case "::cue(i)" -> VttTextTag.ITALIC;
            case "::cue(b)" -> VttTextTag.BOLD;
            case "::cue(u)" -> VttTextTag.UNDERLINE;
            case "::cue(c)" -> VttTextTag.CLASS;
            case "::cue(v)" -> VttTextTag.VOICE;
            case "::cue" -> VttTextTag.ALL;
            default -> throw new IllegalArgumentException("Unknown VTT text tag or not supported yet : " + tag);
        };
    }

    private SubtitleStyle.Property fromStrToProperty(String property) {
        return switch (property) {
            case "direction" -> Property.DIRECTION;
            case "text-align" -> Property.TEXT_ALIGN;
            case "color" -> Property.COLOR;
            case "font-style" -> Property.FONT_STYLE;
            case "font-weight" -> Property.FONT_WEIGHT;
            case "text-decoration-line" -> Property.TEXT_DECORATION;
            default -> throw new IllegalArgumentException("Unknown CSS property or not supported yet : " + property);
        };
    }

    private Object checkAndGetCSSValue(SubtitleStyle.Property property, String value) {
        return switch (property) {
            case DIRECTION -> checkAndGetDirection(value);
            case TEXT_ALIGN -> checkAndGetTextAlign(value);
            case COLOR -> value;
            case FONT_STYLE -> checkAndGetFontStyle(value);
            case FONT_WEIGHT -> checkAndGetFontWeight(value);
            case TEXT_DECORATION -> checkAndGetTextDecoration(value);
            default -> throw new IllegalArgumentException("Unknown CSS property or not supported yet : " + property);
        };
    }

    private Direction checkAndGetDirection(String css) {
        return switch (css.trim().toLowerCase()) {
            case "rtl" -> Direction.RTL;
            case "ltr" -> Direction.LTR;
            default -> throw new IllegalArgumentException("Unknown CSS direction value or not supported yet : " + css);
        };
    }

    private TextAlign checkAndGetTextAlign(String css) {
        return switch (css.trim().toLowerCase()) {
            case "center" -> TextAlign.CENTER;
            case "right" -> TextAlign.RIGHT;
            case "left" -> TextAlign.LEFT;
            default -> throw new IllegalArgumentException("Unknown CSS text-align value or not supported yet : " + css);
        };
    }

    private FontStyle checkAndGetFontStyle(String css) {
        return switch (css.trim().toLowerCase()) {
            case "italic" -> FontStyle.ITALIC;
            case "oblique" -> FontStyle.OBLIQUE;
            case "normal" -> FontStyle.NORMAL;
            default -> throw new IllegalArgumentException("Unknown CSS font-style value or not supported yet : " + css);
        };
    }

    private FontWeight checkAndGetFontWeight(String css) {
        return switch (css.trim().toLowerCase()) {
            case "bold" -> FontWeight.BOLD;
            case "normal" -> FontWeight.NORMAL;
            default ->
                throw new IllegalArgumentException("Unknown CSS font-weight value or not supported yet : " + css);
        };
    }

    private TextDecoration checkAndGetTextDecoration(String css) {
        return switch (css.trim().toLowerCase()) {
            case "underline" -> TextDecoration.UNDERLINE;
            case "overline" -> TextDecoration.OVERLINE;
            case "line-through" -> TextDecoration.LINE_THROUGH;
            default ->
                throw new IllegalArgumentException("Unknown CSS text decoration value or not supported yet : " + css);
        };
    }
}

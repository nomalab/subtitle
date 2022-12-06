package fr.noop.subtitle.ass;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.HashMap;

import fr.noop.subtitle.model.SubtitleObject;
import fr.noop.subtitle.model.SubtitleLine;
import fr.noop.subtitle.model.SubtitleParser;
import fr.noop.subtitle.model.SubtitleParsingException;
import fr.noop.subtitle.util.*;
import fr.noop.subtitle.util.SubtitleStyle.TextAlign;
import fr.noop.subtitle.util.SubtitleTextLine;

public class AssParser implements SubtitleParser {
	private String charset; // Charset of the input files

	private enum CursorStatus {
		NONE,
		SCRIPT_INFO,
		STYLES,
		EVENTS;
	}

	public AssParser(String charset) {
		this.charset = charset;
	}

	@Override
	public AssObject parse(InputStream is) throws IOException, SubtitleParsingException {
		return parse(is, true);
	}

	@Override
	public AssObject parse(InputStream is, boolean strict) throws IOException, SubtitleParsingException {
		AssObject assObject = new AssObject();

		BufferedReader br = new BufferedReader(new InputStreamReader(is, this.charset));
		String line = "";
		CursorStatus cursorStatus = CursorStatus.NONE;

		AssCue cue = null;

		boolean isASS = false;
		List<String> styleFormat = new ArrayList<>();
		List<String> dialogueFormat = new ArrayList<>();
		float timer = 100;

		while ((line = br.readLine()) != null) {
			line = line.trim();
			if (cursorStatus == CursorStatus.NONE && line.equalsIgnoreCase("[Script info]")) {
				cursorStatus = CursorStatus.SCRIPT_INFO;
				continue;
			}
			if (cursorStatus == CursorStatus.SCRIPT_INFO) {
				if (line.startsWith("Title:")) {
					assObject.setProperty(SubtitleObject.Property.TITLE, line.split(":")[1].trim());
				}
				if (line.startsWith("OriginalScript:")) {
					assObject.setProperty(SubtitleObject.Property.COPYRIGHT, line.split(":")[1].trim());
				}
				if (line.startsWith("ScriptType:")) {
					if (line.split(":")[1].trim().equalsIgnoreCase("v4.00+"))
						isASS = true;
					else if (!line.split(":")[1].trim().equalsIgnoreCase("v4.00"))
						System.out
								.print("Script version is older than 4.00, it may produce parsing errors.");
				}
				if (line.startsWith("Timer:")) {
					timer = Float.parseFloat(line.split(":")[1].trim().replace(',', '.'));
				}

				if (line.equalsIgnoreCase("[v4 Styles]") ||
						line.equalsIgnoreCase("[v4 Styles+]") ||
						line.equalsIgnoreCase("[v4+ Styles]")) {
					if (line.contains("+") && isASS == false) {
						isASS = true;
						System.out.print("ScriptType should be set to v4:00+ in the [Script Info] section.\n\n");
					}
					cursorStatus = CursorStatus.STYLES;
				}
				continue;
			}
			if (cursorStatus == CursorStatus.STYLES) {
				if (line.startsWith("Format:")) {
					styleFormat = Arrays.asList(line.split(":")[1].trim().split(","));
				}
				if (line.startsWith("Style:")) {
					if (styleFormat.size() == 000) {
						// mettre une erreur
						throw new SubtitleParsingException(String.format(
								"Format Don't exist !"));
					}
					SubtitleStyle style = new SubtitleStyle();
					Map<String, String> stylesAss = new HashMap<String, String>();

					style = parseStyleForASS(line.split(":")[1].trim().split(","), styleFormat, line, isASS);

					/*************************
					 * Ici il faudrait utiliser plutôt le SubtitleStyle et ses fonctions
					 * setFontStyle, setColor, etc
					 * SubtitleStyle style = new SubtitleStyle(); // surement plutôt une liste ou un
					 * hashmap pour pouvoir stocker la liste de styles => Map<String, SubtitleStyle>
					 * styles = new HashMap<>()
					 * a voir pour mettre ça dans ta fonction parseStyleForASS
					 * + stocker le nouveau style dans le hashmap avec son nom comme key (j'ai vu
					 * que tu avais fais un Hashtable, ça peut rester un Hashtable si tu veux, je
					 * connais pas assez java pour savoir ce qui est mieux)
					 ******************/

				}

				if (line.equalsIgnoreCase("[Events]")) {
					cursorStatus = CursorStatus.EVENTS;
				}
				continue;
			}
			if (cursorStatus == CursorStatus.EVENTS) {
				if (line.startsWith("Format:")) {
					dialogueFormat = Arrays.asList(line.split(":")[1].trim().split(","));
				}
				if (line.startsWith("Dialogue:")) {
					if (dialogueFormat.size() == 0) {
						// mettre une erreur
					}

					/**********************
					 * Utiliser la variable cue et les fonction addLine ou setLines et
					 * assObject.addCue(cue) pour remplacer ça :
					 * caption = parseDialogueForASS(
					 * line.split(":", 2)[1].trim().split(",", dialogueFormat.length),
					 * dialogueFormat,
					 * timer, assObject);
					 * // and save the caption
					 * int key = caption.start.mseconds;
					 * // in case the key is already there, we increase it by a millisecond, since
					 * no
					 * // duplicates are allowed
					 * while (assObject.captions.containsKey(key))
					 * key++;
					 * assObject.captions.put(key, caption);
					 * assObject.captions.put(key, caption);
					 * assObject.captions.put(key, caption);
					 ************************/
					SubtitleTextLine line.split(":",2)[1].trim().split(",",dialogueFormat.length) = new SubtitleTextLine();
					line.addText(new SubtitlePlainText(text));
					cue.addLine(line);

				}
				continue;
			}
		}

		return assObject;
	}

	private Style parseStyleForASS(String[] line, String[] styleFormat, int index, boolean isASS) {
		String name = "";
		SubtitleStyle style = new SubtitleStyle();
		if (line.length != styleFormat.length) {
			// both should have the same size
		} else {
			for (int i = 0; i < styleFormat.length; i++) {
				String trimmedStyleFormat = styleFormat[i].trim();
				if (trimmedStyleFormat.equalsIgnoreCase("Name")) {
					name = line[i].trim();
				}
				// we go through every format parameter and save the interesting values
				else if (trimmedStyleFormat.equalsIgnoreCase("PrimaryColour")) {
					style.setProperty(SubtitleStyle.Property.COLOR, );

				} else if (trimmedStyleFormat.equalsIgnoreCase("Bold")) {
					// we save if bold
					style.setProperty(SubtitleStyle.Property.FONT_WEIGHT, SubtitleStyle.FontWeight.BOLD);

				} else if (trimmedStyleFormat.equalsIgnoreCase("Italic")) {
					// we save if italic
					style.setProperty(SubtitleStyle.Property.FONT_STYLE, SubtitleStyle.FontStyle.ITALIC);

				} else if (trimmedStyleFormat.equalsIgnoreCase("Underline")) {
					// we save if underlined
					style.setProperty(SubtitleStyle.Property.TEXT_DECORATION, SubtitleStyle.TextDecoration.UNDERLINE);

				} else if (trimmedStyleFormat.equalsIgnoreCase("Alignment")) {
					// we save the alignment
					int placement = Integer.parseInt(line[i].trim());
					if (isASS) {
						switch (placement) {
							case 1:
								style.setTextAlign(TextAlign.LEFT);
								break;
							case 2:
								style.setTextAlign(TextAlign.CENTER);
								break;
							case 3:
								style.setTextAlign(TextAlign.RIGHT);
								break;
						}
					} else {
						switch (placement) {
							case 9:
								style.setTextAlign(TextAlign.LEFT);
								break;
							case 10:
								style.setTextAlign(TextAlign.CENTER);
								break;
							case 11:
								style.setTextAlign(TextAlign.RIGHT);
								break;
						}
					}
				}

			}
		}
		stylesAss.put(name, style);
		return style;
	}
}

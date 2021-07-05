package fr.noop.subtitle.ass;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import fr.noop.subtitle.model.SubtitleParser;
import fr.noop.subtitle.model.SubtitleParsingException;

public class AssParser implements SubtitleParser {

	Style style;
	Caption caption = new Caption();

	// for the clock timer
	float timer = 100;

	// if the file is .SSA or .ASS
	boolean isASS = false;

	// variables to store the formats
	String[] styleFormat;
	String[] dialogueFormat;
	private String charset; // Charset of the input files

	public AssParser(String charset) {
		this.charset = charset;
	}

	@Override
	public AssObject parse(InputStream is) throws IOException, SubtitleParsingException {
		return parse(is, true);
	}

	@Override
	public AssObject parse(InputStream is, boolean strict) throws IOException, SubtitleParsingException {
		// Create srt object
		AssObject assObject = new AssObject();

		// Read each lines
		BufferedReader br = new BufferedReader(new InputStreamReader(is, this.charset));
		String line;
		int lineCounter = 0;
		try {
			// we scour the file
			line = br.readLine();
			lineCounter++;
			while (line != null) {
				line = line.trim();
				// we skip any line until we find a section [section name]
				if (line.startsWith("[")) {
					// now we must identify the section
					if (line.equalsIgnoreCase("[Script info]")) {
						// its the script info section section
						lineCounter++;
						line = br.readLine().trim();
						// Each line is scanned for useful info until a new section is detected
						while (!line.startsWith("[")) {
							if (line.startsWith("Title:"))
								// We have found the title
								assObject.title = line.split(":")[1].trim();
							else if (line.startsWith("Original Script:"))
								// We have found the author
								assObject.author = line.split(":")[1].trim();
							else if (line.startsWith("Script Type:")) {
								// we have found the version
								if (line.split(":")[1].trim().equalsIgnoreCase("v4.00+"))
									isASS = true;
								// we check the type to set isASS or to warn if it comes from an older version
								else if (!line.split(":")[1].trim().equalsIgnoreCase("v4.00"))
									System.out
											.print("Script version is older than 4.00, it may produce parsing errors.");
								// than the studied specs
							} else if (line.startsWith("Timer:"))
								// We have found the timer
								timer = Float.parseFloat(line.split(":")[1].trim().replace(',', '.'));
							// we go to the next line
							lineCounter++;
							line = br.readLine().trim();
						}

					} else if (line.equalsIgnoreCase("[v4 Styles]")
							|| line.equalsIgnoreCase("[v4 Styles+]")
							|| line.equalsIgnoreCase("[v4+ Styles]")) {
						// its the Styles description section
						if (line.contains("+") && isASS == false) {
							// its ASS and it had not been noted
							isASS = true;
							System.out.print("ScriptType should be set to v4:00+ in the [Script Info] section.\n\n");
						}
						lineCounter++;
						line = br.readLine().trim();
						// the first line should define the format
						if (!line.startsWith("Format:")) {
							// if not, we scan for the format.
							while (!line.startsWith("Format:")) {
								lineCounter++;
								line = br.readLine().trim();
							}
						}
						// we recover the format's fields
						styleFormat = line.split(":")[1].trim().split(",");
						lineCounter++;
						line = br.readLine().trim();
						// we parse each style until we reach a new section
						while (!line.startsWith("[")) {
							// we check it is a style
							if (line.startsWith("Style:")) {
								// we parse the style
								style = parseStyleForASS(line.split(":")[1].trim().split(","), styleFormat, lineCounter,
										isASS);
								// and save the style
								assObject.styling.put(style.iD, style);
							}
							// next line
							lineCounter++;
							line = br.readLine().trim();
						}

					} else if (line.trim().equalsIgnoreCase("[Events]")) {
						// its the events specification section
						lineCounter++;
						line = br.readLine().trim();
						// the first line should define the format of the dialogues
						if (!line.startsWith("Format:")) {
							// if not, we scan for the format.
							while (!line.startsWith("Format:")) {
								lineCounter++;
								line = br.readLine().trim();
							}
							System.out.print("Format: (format definition) expected at line " + line
									+ " for the styles section\n\n");
						}
						// we recover the format's fields
						dialogueFormat = line.split(":")[1].trim().split(",");
						// next line
						lineCounter++;
						line = br.readLine().trim();
						// we parse each style until we reach a new section
						while (!line.startsWith("[")) {
							// we check it is a dialogue
							// WARNING: all other events are ignored.
							if (line.startsWith("Dialogue:")) {
								// we parse the dialogue
								caption = parseDialogueForASS(
										line.split(":", 2)[1].trim().split(",", dialogueFormat.length), dialogueFormat,
										timer, assObject);
								// and save the caption
								int key = caption.start.mseconds;
								// in case the key is already there, we increase it by a millisecond, since no
								// duplicates are allowed
								while (assObject.captions.containsKey(key))
									key++;
								assObject.captions.put(key, caption);
							}
							// next line
							lineCounter++;
							line = br.readLine().trim();
						}

					} else if (line.trim().equalsIgnoreCase("[Fonts]") || line.trim().equalsIgnoreCase("[Graphics]")) {
						// its the custom fonts or embedded graphics section
						// these are not supported
						System.out
								.print("The section " + line.trim()
										+ " is not supported for conversion, all information there will be lost.\n\n");
						line = br.readLine().trim();
					} else {
						System.out
								.print("Unrecognized section: " + line.trim() + " all information there is ignored.");
						line = br.readLine().trim();
					}
				} else {
					line = br.readLine();
					lineCounter++;
				}
			}
			assObject.cleanUnusedStyles();
		} catch (NullPointerException e) {
			throw new SubtitleParsingException(String.format(
					"unexpected end of file, maybe last caption is not complete"));
		} finally {
			// we close the reader
			is.close();
		}
		assObject.built = true;

		return assObject;
	}

	private Style parseStyleForASS(String[] line, String[] styleFormat, int index, boolean isASS) {

		Style newStyle = new Style(Style.defaultID());
		if (line.length != styleFormat.length) {
		} else {
			for (int i = 0; i < styleFormat.length; i++) {
				String trimmedStyleFormat = styleFormat[i].trim();
				// we go through every format parameter and save the interesting values
				if (trimmedStyleFormat.equalsIgnoreCase("Name")) {
					// we save the name
					newStyle.iD = line[i].trim();
				} else if (trimmedStyleFormat.equalsIgnoreCase("Fontname")) {
					// we save the font
					newStyle.font = line[i].trim();
				} else if (trimmedStyleFormat.equalsIgnoreCase("Fontsize")) {
					// we save the size
					newStyle.fontSize = line[i].trim();
				} else if (trimmedStyleFormat.equalsIgnoreCase("PrimaryColour")) {
					// we save the color
					String color = line[i].trim();
					if (isASS) {
						if (color.startsWith("&H"))
							newStyle.color = Style.getRGBValue("&HAABBGGRR", color);
						else
							newStyle.color = Style.getRGBValue("decimalCodedAABBGGRR", color);
					} else {
						if (color.startsWith("&H"))
							newStyle.color = Style.getRGBValue("&HBBGGRR", color);
						else
							newStyle.color = Style.getRGBValue("decimalCodedBBGGRR", color);
					}
				} else if (trimmedStyleFormat.equalsIgnoreCase("BackColour")) {
					// we save the background color
					String color = line[i].trim();
					if (isASS) {
						if (color.startsWith("&H"))
							newStyle.backgroundColor = Style.getRGBValue("&HAABBGGRR", color);
						else
							newStyle.backgroundColor = Style.getRGBValue("decimalCodedAABBGGRR", color);
					} else {
						if (color.startsWith("&H"))
							newStyle.backgroundColor = Style.getRGBValue("&HBBGGRR", color);
						else
							newStyle.backgroundColor = Style.getRGBValue("decimalCodedBBGGRR", color);
					}
				} else if (trimmedStyleFormat.equalsIgnoreCase("Bold")) {
					// we save if bold
					newStyle.bold = Boolean.parseBoolean(line[i].trim());
				} else if (trimmedStyleFormat.equalsIgnoreCase("Italic")) {
					// we save if italic
					newStyle.italic = Boolean.parseBoolean(line[i].trim());
				} else if (trimmedStyleFormat.equalsIgnoreCase("Underline")) {
					// we save if underlined
					newStyle.underline = Boolean.parseBoolean(line[i].trim());
				} else if (trimmedStyleFormat.equalsIgnoreCase("Alignment")) {
					// we save the alignment
					int placement = Integer.parseInt(line[i].trim());
					if (isASS) {
						switch (placement) {
							case 1:
								newStyle.textAlign = "bottom-left";
								break;
							case 2:
								newStyle.textAlign = "bottom-center";
								break;
							case 3:
								newStyle.textAlign = "bottom-right";
								break;
							case 4:
								newStyle.textAlign = "mid-left";
								break;
							case 5:
								newStyle.textAlign = "mid-center";
								break;
							case 6:
								newStyle.textAlign = "mid-right";
								break;
							case 7:
								newStyle.textAlign = "top-left";
								break;
							case 8:
								newStyle.textAlign = "top-center";
								break;
							case 9:
								newStyle.textAlign = "top-right";
								break;
						}
					} else {
						switch (placement) {
							case 9:
								newStyle.textAlign = "bottom-left";
								break;
							case 10:
								newStyle.textAlign = "bottom-center";
								break;
							case 11:
								newStyle.textAlign = "bottom-right";
								break;
							case 1:
								newStyle.textAlign = "mid-left";
								break;
							case 2:
								newStyle.textAlign = "mid-center";
								break;
							case 3:
								newStyle.textAlign = "mid-right";
								break;
							case 5:
								newStyle.textAlign = "top-left";
								break;
							case 6:
								newStyle.textAlign = "top-center";
								break;
							case 7:
								newStyle.textAlign = "top-right";
								break;
						}
					}
				}

			}
		}

		return newStyle;
	}

	/**
	 * This methods transforms a dialogue line from ASS according to a format
	 * definition into an Caption object.
	 * 
	 * @param line           the dialogue line without its declaration
	 * @param dialogueFormat the list of attributes in this dialogue line
	 * @param timer          % to speed or slow the clock, above 100% span of the
	 *                       subtitles is reduced.
	 * @return a new Caption object
	 */
	private Caption parseDialogueForASS(String[] line, String[] dialogueFormat, float timer, AssObject assObject) {

		Caption newCaption = new Caption();

		for (int i = 0; i < dialogueFormat.length; i++) {
			String trimmedDialogueFormat = dialogueFormat[i].trim();
			// we go through every format parameter and save the interesting values
			if (trimmedDialogueFormat.equalsIgnoreCase("Style")) {
				// we save the style
				Style s = assObject.styling.get(line[i].trim());
				if (s != null)
					newCaption.style = s;
			} else if (trimmedDialogueFormat.equalsIgnoreCase("Start")) {
				// we save the starting time
				newCaption.start = new Time("h:mm:ss.cs", line[i].trim());
			} else if (trimmedDialogueFormat.equalsIgnoreCase("End")) {
				// we save the starting time
				newCaption.end = new Time("h:mm:ss.cs", line[i].trim());
			} else if (trimmedDialogueFormat.equalsIgnoreCase("Text")) {
				// we save the text
				String captionText = line[i];
				newCaption.rawContent = captionText;
				// text is cleaned before being inserted into the caption
				newCaption.content = captionText.replaceAll("\\{.*?\\}", "").replace("\n", "<br />").replace("\\N",
						"<br />");
			}
		}

		// timer is applied
		if (timer != 100) {
			newCaption.start.mseconds /= (timer / 100);
			newCaption.end.mseconds /= (timer / 100);
		}
		return newCaption;
	}
}

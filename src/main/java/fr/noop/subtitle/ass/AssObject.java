
package fr.noop.subtitle.ass;

import fr.noop.subtitle.base.BaseSubtitleObject;

import java.util.Hashtable;
import java.util.Iterator;
import java.util.TreeMap;

public class AssObject extends BaseSubtitleObject {
    public String title = "";
    public String description = "";
    public String copyrigth = "";
    public String author = "";
    public String fileName = "";
    public String language = "";

    // list of styles (id, reference)
    public Hashtable<String, Style> styling;

    // list of layouts (id, reference)
    public Hashtable<String, Region> layout;

    // list of captions (begin time, reference)
    // represented by a tree map to maintain order
    public TreeMap<Integer, Caption> captions;

    // **** OPTIONS *****
    // to know whether file should be saved as .ASS or .SSA
    public boolean useASSInsteadOfSSA = true;
    // to delay or advance the subtitles, parsed into +/- milliseconds
    public int offset = 0;

    // to know if a parsing method has been applied
    public boolean built = false;

    /**
     * Protected constructor so it can't be created from outside
     */
    protected AssObject() {

        styling = new Hashtable<>();
        layout = new Hashtable<>();
        captions = new TreeMap<>();
    }

    /*
     * PROTECTED METHODS
     * 
     */

    /**
     * This method simply checks the style list and eliminate any style not
     * referenced by any caption
     * This might come useful when default styles get created and cover too much.
     * It require a unique iteration through all captions.
     * 
     */
    protected void cleanUnusedStyles() {
        // here all used styles will be stored
        Hashtable<String, Style> usedStyles = new Hashtable<>();
        // we iterate over the captions
        Iterator<Caption> itrC = captions.values().iterator();
        while (itrC.hasNext()) {
            // new caption
            Caption current = itrC.next();
            // if it has a style
            if (current.style != null) {
                String iD = current.style.iD;
                // if we haven't saved it yet
                if (!usedStyles.containsKey(iD))
                    usedStyles.put(iD, current.style);
            }
        }
        // we saved the used styles
        this.styling = usedStyles;
    }

}

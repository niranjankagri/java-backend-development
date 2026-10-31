package jbdl.session26.slug;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Turns a listing title into the slug used in the listing's URL,
 * for example "Sunny Room & Garden" becomes "sunny-room-and-garden"
 * and the URL is /listings/42/sunny-room-and-garden.
 *
 * The slug only contains a-z, 0-9 and single dashes. It never starts
 * or ends with a dash. Accents are removed ("Hôtel" becomes "hotel"),
 * "&" becomes "and", and any other symbol or space becomes one dash.
 * If nothing usable is left (for example "@@@"), the slug is "listing".
 * Long slugs are cut at a whole word, at most 40 characters.
 */
public class ListingSlug {

    private static final int MAX_LENGTH = 40;
    private static final String FALLBACK = "listing";
    private static final Pattern ACCENT_MARKS = Pattern.compile("\\p{M}+");
    private static final Pattern NOT_ALLOWED = Pattern.compile("[^a-z0-9]+");
    private static final Pattern EDGE_DASHES = Pattern.compile("^-+|-+$");

    /**
     * Builds the URL slug for a listing title.
     *
     * @param title the title typed by the host, in any language
     * @return a lowercase slug with a-z, 0-9 and single dashes
     */
    public static String of(String title) {
        if (title == null) {
            return FALLBACK;
        }

        // Step 1: split accented letters ("ô" becomes "o" + accent mark), then remove the marks.
        String text = Normalizer.normalize(title, Normalizer.Form.NFD);
        text = ACCENT_MARKS.matcher(text).replaceAll("");

        // Step 2: use lowercase letters only.
        text = text.toLowerCase(Locale.ROOT);

        // Step 3: write "&" as the word "and".
        text = text.replace("&", " and ");

        // Step 4: every run of anything that is not a-z or 0-9 becomes one dash.
        String slug = NOT_ALLOWED.matcher(text).replaceAll("-");

        // Step 5: remove dashes at the start and end.
        slug = EDGE_DASHES.matcher(slug).replaceAll("");

        if (slug.isEmpty()) {
            return FALLBACK;
        }

        // Step 6: if the slug is too long, cut it without splitting a word.
        if (slug.length() > MAX_LENGTH) {
            String cut = slug.substring(0, MAX_LENGTH);
            if (slug.charAt(MAX_LENGTH) != '-') {
                // We cut in the middle of a word, so go back to the last dash.
                int lastDash = cut.lastIndexOf('-');
                if (lastDash > 0) {
                    cut = cut.substring(0, lastDash);
                }
            }
            slug = EDGE_DASHES.matcher(cut).replaceAll("");
        }

        return slug;
    }
}

package jbdl.session26.slug;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Topic 3: the 12 hidden cases every generated slug class was scored with, run against the class
 * the model wrote for prompt C in run 2 (copied unchanged). The model never saw these cases.
 */
class ListingSlugTest {

    // '...' quotes a value in @CsvSource, so the spaces at the start and end are kept
    @ParameterizedTest(name = "{0} -> {1}")
    @CsvSource(delimiter = '|', textBlock = """
            'Cozy Flat in Goa'                                               | cozy-flat-in-goa
            '  Sea View!!  Villa  '                                          | sea-view-villa
            'Café Résidence'                                                 | cafe-residence
            'Bed & Breakfast'                                                | bed-and-breakfast
            'Rock&Roll Loft'                                                 | rock-and-roll-loft
            '3BHK Apartment, Floor 12'                                       | 3bhk-apartment-floor-12
            'Beautiful sunny apartment close to the beach and the old town'  | beautiful-sunny-apartment-close-to-the
            'Supercalifragilisticexpialidociousnessness flat'                | supercalifragilisticexpialidociousnessne
            '!!!'                                                            | listing
            'Ünïcödé Ñame'                                                   | unicode-name
            'Hello -- World'                                                 | hello-world
            'Quiet studio with a garden view and fast wifi'                  | quiet-studio-with-a-garden-view-and-fast
            """)
    void hiddenCases(String title, String slug) {
        assertThat(ListingSlug.of(title)).isEqualTo(slug);
    }

    // the five examples of prompt C must of course work too
    @ParameterizedTest(name = "{0} -> {1}")
    @CsvSource(delimiter = '|', textBlock = """
            'Sunny Room & Garden'                                            | sunny-room-and-garden
            'Hôtel Été'                                                      | hotel-ete
            '  Loft -- 2 Beds '                                              | loft-2-beds
            '@@@'                                                            | listing
            'Bright two bedroom apartment near the central railway station'  | bright-two-bedroom-apartment-near-the
            """)
    void promptExamples(String title, String slug) {
        assertThat(ListingSlug.of(title)).isEqualTo(slug);
    }
}

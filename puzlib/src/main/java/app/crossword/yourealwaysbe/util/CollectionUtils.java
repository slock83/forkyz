
package app.crossword.yourealwaysbe.util;

import java.util.AbstractCollection;
import java.util.Collection;
import java.util.Iterator;
import java.util.stream.Stream;

public class CollectionUtils {

    /**
     * Simple join of two collections, does not remove duplicates
     */
    public static <T> Collection<T> join(
        Collection<T> collection1, Collection<T> collection2
    ) {
        return new AbstractCollection<T>() {
            public Iterator<T> iterator() {
                return Stream.concat(
                    collection1.stream(), collection2.stream()
                ).iterator();
            }

            public int size() {
                return collection1.size() + collection2.size();
            }
        };
    }
}

package com.gudosnest.backend.service;

import com.gudosnest.backend.model.Property;
import com.gudosnest.backend.model.Unit;
import com.gudosnest.backend.repository.PropertyRepository;
import com.gudosnest.backend.structures.Trie;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * What the renter's Browse page needs: every bookable listing, location suggestions from a Trie,
 * and the search (text, category, dates, guests).
 */
public class BrowseService {

    /** The category pills on Browse. A property matches a pill when its name, location or a unit's text has one of the words. */
    public static final String ALL_HOMES = "All homes";

    private static final Map<String, List<String>> CATEGORIES = new LinkedHashMap<>();

    static {
        CATEGORIES.put(ALL_HOMES, List.of());
        CATEGORIES.put("Villas", List.of("villa"));
        CATEGORIES.put("Lofts", List.of("loft"));
        CATEGORIES.put("Cabins", List.of("cabin", "cottage"));
        CATEGORIES.put("Work-friendly", List.of("work", "desk", "fiber", "studio"));
        CATEGORIES.put("Oceanfront", List.of("ocean", "sea", "cove", "coast", "beach", "water"));
        CATEGORIES.put("Mountain view", List.of("mountain", "forest", "pine", "ridge", "highland", "hill"));
    }

    private final PropertyRepository properties = new PropertyRepository();
    private final BookingService bookings;

    public BrowseService() {
        this(new BookingService());
    }

    public BrowseService(BookingService bookings) {
        this.bookings = bookings;
    }

    public List<String> categories() {
        return List.copyOf(CATEGORIES.keySet());
    }

    /** Every listing with at least one unit. */
    public List<Property> listings() {
        return properties.bookable();
    }

    /** A Trie of every location and property name, for the suggestions under the Location box. */
    public Trie suggestions() {
        Trie trie = new Trie();
        for (String word : properties.searchWords()) {
            if (word != null && !word.isBlank()) {
                trie.insert(word);
            }
        }
        return trie;
    }

    /**
     * Listings that match the search. Blank text, no category and no dates match everything that
     * fits the guests. With both dates, a listing must have a unit that is free on those dates.
     */
    public List<Property> search(List<Property> all, String text, String category,
                                 LocalDate checkIn, LocalDate checkOut, int guests) {
        checkDates(checkIn, checkOut);
        String query = text == null ? "" : text.strip().toLowerCase(Locale.ROOT);
        return all.stream()
                .filter(p -> query.isEmpty() || p.getLocation().toLowerCase(Locale.ROOT).contains(query)
                        || p.getName().toLowerCase(Locale.ROOT).contains(query))
                .filter(p -> inCategory(p, category))
                .filter(p -> !fittingUnits(p, checkIn, checkOut, guests).isEmpty())
                .toList();
    }

    /** The units of a listing that sleep this many guests and (when dates are given) are free then. */
    public List<Unit> fittingUnits(Property property, LocalDate checkIn, LocalDate checkOut, int guests) {
        boolean dated = checkIn != null && checkOut != null;
        return property.getUnits().stream()
                .filter(u -> u.getCapacity() >= guests)
                .filter(u -> !dated || bookings.isAvailable(u.getId(), checkIn, checkOut))
                .toList();
    }

    private static void checkDates(LocalDate checkIn, LocalDate checkOut) {
        if ((checkIn == null) != (checkOut == null)) {
            throw new RuleException("Choose both check-in and check-out, or neither.");
        }
        if (checkIn != null && !checkOut.isAfter(checkIn)) {
            throw new RuleException("Check-out must be at least one night after check-in.");
        }
    }

    private static boolean inCategory(Property p, String category) {
        List<String> words = CATEGORIES.getOrDefault(category == null ? ALL_HOMES : category, List.of());
        if (words.isEmpty()) {
            return true;
        }
        StringBuilder text = new StringBuilder(p.getName()).append(' ').append(p.getLocation());
        for (Unit u : p.getUnits()) {
            text.append(' ').append(u.getName()).append(' ').append(u.getDescription());
        }
        String haystack = text.toString().toLowerCase(Locale.ROOT);
        return words.stream().anyMatch(haystack::contains);
    }
}

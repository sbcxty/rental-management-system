package com.gudosnest.backend.service;

import com.gudosnest.backend.model.Booking;
import com.gudosnest.backend.model.Property;
import com.gudosnest.backend.model.Unit;
import com.gudosnest.backend.repository.BookingRepository;
import com.gudosnest.backend.repository.PropertyRepository;
import com.gudosnest.backend.structures.AvailabilityCalendar;
import com.gudosnest.backend.structures.Trie;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * What renters see on Browse: the bookable listings, location suggestions, and the search
 * (location text, category, dates, guests). Booking itself goes through {@link BookingService}.
 */
public class BrowseService {

    // PATTERN: Strategy
    // Each category pill is one filter strategy with the same method, matches(property).
    // The search calls category.matches(p) without knowing which pill was picked, so adding a
    // category is one new constant here; the search code doesn't change.
    public enum Category {
        ALL("All homes") {
            @Override
            public boolean matches(Property p) {
                return true;
            }
        },
        WHOLE_HOUSES("Whole houses") {
            @Override
            public boolean matches(Property p) {
                return p.isHouse();
            }
        },
        MULTI_UNIT("Multi-unit") {
            @Override
            public boolean matches(Property p) {
                return !p.isHouse();
            }
        },
        AVAILABLE_NOW("Available now") {
            @Override
            public boolean matches(Property p) {
                return p.getOccupiedCount() < p.getUnits().size();
            }
        };

        private final String label;

        Category(String label) {
            this.label = label;
        }

        public String label() {
            return label;
        }

        public abstract boolean matches(Property p);
    }

    /**
     * Everything Browse needs, loaded once per page load (4 queries in all):
     *  - listings: bookable properties, without the renter's own (a two-role user can't book themselves);
     *  - suggestions: a Trie of property names, locations and each part of a location ("Cavite");
     *  - calendars: each unit's approved stays that haven't ended, unit id -> TreeMap calendar.
     */
    public record Catalog(List<Property> listings, Trie suggestions,
                          Map<Integer, AvailabilityCalendar<Integer>> calendars) {
    }

    private final PropertyRepository properties = new PropertyRepository();
    private final BookingRepository bookings = new BookingRepository();
    private final BookingService bookingRules = new BookingService();

    public Catalog catalog(int renterId) {
        List<Property> listings = properties.bookable().stream()
                .filter(p -> p.getOwnerId() != renterId)
                .toList();

        Trie suggestions = new Trie();
        for (Property p : listings) {
            suggestions.insert(p.getName());
            suggestions.insert(p.getLocation());
            for (String part : p.getLocation().split(",")) {
                if (!part.isBlank()) {
                    suggestions.insert(part.strip());
                }
            }
        }

        // DSA: HashMap of TreeMaps. One query loads every approved stay still ahead; each goes into
        // its unit's AvailabilityCalendar. Checking a unit is then an O(1) map lookup plus an
        // O(log n) calendar lookup, instead of one database query per unit per search.
        Map<Integer, AvailabilityCalendar<Integer>> calendars = new HashMap<>();
        for (Booking b : bookings.approvedEndingAfter(LocalDateTime.now())) {
            calendars.computeIfAbsent(b.getUnitId(), id -> new AvailabilityCalendar<>())
                    .add(b.getCheckIn(), b.getCheckOut(), b.getId());
        }
        return new Catalog(listings, suggestions, calendars);
    }

    /**
     * The listings that match the search. Dates are optional, but both or neither: with dates,
     * a property shows only if one of its units fits the guests and is free on those dates.
     */
    public List<Property> search(Catalog catalog, String text, Category category,
                                 LocalDate checkIn, LocalDate checkOut, int guests) {
        if ((checkIn == null) != (checkOut == null)) {
            throw new RuleException("Choose both check-in and check-out dates, or clear them to see every stay.");
        }
        if (checkIn != null) {
            bookingRules.checkDates(checkIn, checkOut);
        }
        String query = text == null ? "" : text.strip().toLowerCase(Locale.ROOT);
        return catalog.listings().stream()
                .filter(category::matches)
                .filter(p -> query.isEmpty()
                        || p.getLocation().toLowerCase(Locale.ROOT).contains(query)
                        || p.getName().toLowerCase(Locale.ROOT).contains(query))
                .filter(p -> !fittingUnits(catalog, p, checkIn, checkOut, guests).isEmpty())
                .toList();
    }

    /** The property's units that hold this many guests and (when dates are given) are free then. */
    public List<Unit> fittingUnits(Catalog catalog, Property p, LocalDate checkIn, LocalDate checkOut, int guests) {
        return p.getUnits().stream()
                .filter(u -> u.getCapacity() >= guests)
                .filter(u -> checkIn == null || checkOut == null || isFree(catalog, u, checkIn, checkOut))
                .toList();
    }

    private static boolean isFree(Catalog catalog, Unit unit, LocalDate checkIn, LocalDate checkOut) {
        AvailabilityCalendar<Integer> calendar = catalog.calendars().get(unit.getId());
        return calendar == null || calendar.isFree(checkIn.atTime(BookingService.CHECK_IN_TIME),
                checkOut.atTime(BookingService.CHECK_OUT_TIME));
    }
}

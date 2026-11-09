package com.gudosnest.frontend.owner;

import com.gudosnest.backend.model.Property;

import javax.swing.JComboBox;
import java.util.List;

/** "All properties" dropdown used on owner pages. Remembers nothing; each page owns its choice. */
final class PropertyFilter extends JComboBox<PropertyFilter.Choice> {

    /** One entry; property null means "All properties". */
    record Choice(Property property) {
        @Override
        public String toString() {
            return property == null ? "All properties" : property.getName();
        }
    }

    PropertyFilter(List<Property> properties, Integer selectedId) {
        addItem(new Choice(null));
        for (Property p : properties) {
            Choice c = new Choice(p);
            addItem(c);
            if (selectedId != null && p.getId() == selectedId) {
                setSelectedItem(c);
            }
        }
    }

    /** The chosen property id, or null for all. */
    Integer selectedId() {
        Choice c = (Choice) getSelectedItem();
        return c == null || c.property() == null ? null : c.property().getId();
    }

    Property selectedProperty() {
        Choice c = (Choice) getSelectedItem();
        return c == null ? null : c.property();
    }

    void onChange(Runnable action) {
        addActionListener(e -> action.run());
    }
}

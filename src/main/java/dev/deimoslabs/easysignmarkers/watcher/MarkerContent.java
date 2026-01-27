package dev.deimoslabs.easysignmarkers.watcher;

import com.flowpowered.math.vector.Vector3d;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Simple data holder describing marker content (three label lines, position and metadata).
 * Includes a nested {@code Builder} to construct instances with a fluent API.
 */
public class MarkerContent {

    private String label1;
    private String label2;
    private String label3;
    private long timestampMillis;
    private String author;
    private Vector3d position;

    private static final String DATE_FORMAT = "dd MMM yyyy HH:mm";

    /**
     * Private constructor used by the Builder.
     */
    private MarkerContent() {
    }

    /**
     * Creates a new {@link Builder} for {@link MarkerContent}.
     *
     * @return a fresh Builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    // Getters
    public String getLabel1() {
        return label1;
    }

    public String getLabel2() {
        return label2;
    }

    public String getLabel3() {
        return label3;
    }

    public int getX() {
        return position.getFloorX();
    }

    public int getY() {
        return position.getFloorY();
    }

    public int getZ() {
        return position.getFloorZ();
    }

    public String getAuthor() {
        return author;
    }

    public String getTimestamp() {
        if (timestampMillis <= 0) return "";
        Instant instant = Instant.ofEpochMilli(timestampMillis);
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern(DATE_FORMAT, Locale.ENGLISH).withZone(ZoneId.systemDefault());
        return fmt.format(instant);
    }

    /**
     * Builder for {@link MarkerContent} providing a fluent API.
     */
    public static class Builder {
        private final MarkerContent instance;

        private Builder() {
            instance = new MarkerContent();
        }

        /**
         * Sets the first label line.
         *
         * @param label1 first label
         * @return this builder
         */
        public Builder label1(String label1) {
            instance.label1 = label1;
            return this;
        }

        /**
         * Sets the second label line.
         *
         * @param label2 second label
         * @return this builder
         */
        public Builder label2(String label2) {
            instance.label2 = label2;
            return this;
        }

        /**
         * Sets the third label line.
         *
         * @param label3 third label
         * @return this builder
         */
        public Builder label3(String label3) {
            instance.label3 = label3;
            return this;
        }

        /**
         * Sets the World position of the marker.
         *
         * @param position {@link Vector3d}
         * @return this builder
         */
        public Builder position(Vector3d position) {
            instance.position = position;
            return this;
        }

        /**
         * Sets the author of the marker.
         *
         * @param author author name
         * @return this builder
         */
        public Builder author(String author) {
            instance.author = author;
            return this;
        }


        /**
         * Builds the {@link MarkerContent} instance.
         *
         * @return a fully constructed MarkerContent
         */
        public MarkerContent build() {
            instance.timestampMillis = System.currentTimeMillis();
            return instance;
        }
    }
}

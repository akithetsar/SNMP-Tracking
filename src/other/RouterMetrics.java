package other;

import java.util.Objects;

/**
 * A data class to hold the aggregated metrics for a single router at a specific point in time.
 * Using 'long' to prevent counter overflow issues with standard integer types.
 */
public class RouterMetrics {
    final long timestamp; // Timestamp in milliseconds when the poll occurred.

    // Using long for all counters to match SNMP standards (Counter32/Counter64)
    long totalInOctets = 0;
    long totalOutOctets = 0;
    long totalInUcastPkts = 0;
    long totalOutUcastPkts = 0;
    long totalInNUcastPkts = 0;
    long totalOutNUcastPkts = 0;

    public RouterMetrics() {
        this.timestamp = System.currentTimeMillis();
    }

    // This is essential for the data history comparison logic.
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RouterMetrics that = (RouterMetrics) o;
        return timestamp == that.timestamp && totalInOctets == that.totalInOctets && totalOutOctets == that.totalOutOctets && totalInUcastPkts == that.totalInUcastPkts && totalOutUcastPkts == that.totalOutUcastPkts && totalInNUcastPkts == that.totalInNUcastPkts && totalOutNUcastPkts == that.totalOutNUcastPkts;
    }

    @Override
    public int hashCode() {
        return Objects.hash(timestamp, totalInOctets, totalOutOctets, totalInUcastPkts, totalOutUcastPkts, totalInNUcastPkts, totalOutNUcastPkts);
    }
}
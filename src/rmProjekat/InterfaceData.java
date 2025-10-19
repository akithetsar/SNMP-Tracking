package rmProjekat;

import java.util.Objects;

/**
 * A data class to hold all monitored information for a single network interface.
 * This ensures data is handled in a structured way, not dependent on list indices.
 */
public class InterfaceData {
    final int index;
    String description = "N/A";
    String type = "N/A";
    String mtu = "N/A";
    String speed = "N/A";
    String physAddress = "N/A";
    String adminStatus = "N/A";
    String operStatus = "N/A";

    public InterfaceData(int index) {
        this.index = index;
    }

    // Overriding equals and hashCode is crucial for the change-detection logic.
    // The UI will only update if the data has actually changed.
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        InterfaceData that = (InterfaceData) o;
        return index == that.index &&
               Objects.equals(description, that.description) &&
               Objects.equals(type, that.type) &&
               Objects.equals(mtu, that.mtu) &&
               Objects.equals(speed, that.speed) &&
               Objects.equals(physAddress, that.physAddress) &&
               Objects.equals(adminStatus, that.adminStatus) &&
               Objects.equals(operStatus, that.operStatus);
    }

    @Override
    public int hashCode() {
        return Objects.hash(index, description, type, mtu, speed, physAddress, adminStatus, operStatus);
    }
}
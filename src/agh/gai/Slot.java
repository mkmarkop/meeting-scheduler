package agh.gai;

public class Slot {
    private double preference;
    private boolean isAvailable;

    public Slot() {
        this(0);
    }

    public Slot(double preference) {
        this.preference = preference;
        this.isAvailable = true;
    }

    public double getPreference() {
        return preference;
    }

    public void setPreference(double preference) {
        this.preference = preference;
    }

    public boolean getIsAvailable() {
        return isAvailable;
    }

    public void setIsAvailable(boolean isAvailable) {
        this.isAvailable = isAvailable;
    }
}

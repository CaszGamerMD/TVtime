package com.caszgamermd.tvtime.item;

public enum PipCorner {
    TOP_LEFT("Top left"),
    TOP_RIGHT("Top right"),
    BOTTOM_LEFT("Bottom left"),
    BOTTOM_RIGHT("Bottom right");

    private final String label;

    PipCorner(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}

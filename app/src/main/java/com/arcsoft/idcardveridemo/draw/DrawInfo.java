package com.arcsoft.idcardveridemo.draw;

import android.graphics.Rect;

public class DrawInfo {
    private Rect rect;
    private String text;
    private int color;

    public DrawInfo() {
    }

    public DrawInfo(Rect rect, String text, int color) {
        this.rect = rect;
        this.text = text;
        this.color = color;
    }

    public Rect getRect() {
        return rect;
    }

    public void setRect(Rect rect) {
        this.rect = rect;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public int getColor() {
        return color;
    }

    public void setColor(int color) {
        this.color = color;
    }
}

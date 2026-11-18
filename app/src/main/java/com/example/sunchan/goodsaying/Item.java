package com.example.sunchan.goodsaying;

public class Item {
    private int id;
    private String text;
    private boolean notificationEnabled;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
    public boolean isNotificationEnabled() { return notificationEnabled; }
    public void setNotificationEnabled(boolean enabled) { notificationEnabled = enabled; }
}

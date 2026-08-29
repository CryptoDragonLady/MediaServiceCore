package com.liskovsoft.youtubeapi.app.playerdata;

import java.util.LinkedHashMap;
import java.util.Map;

public final class PlayerUrlCache<T> {
    private final Map<String, T> mEntries;

    public PlayerUrlCache(int maxSize) {
        if (maxSize < 1) {
            throw new IllegalArgumentException("maxSize must be positive");
        }

        mEntries = new LinkedHashMap<String, T>(maxSize, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, T> eldest) {
                return size() > maxSize;
            }
        };
    }

    public T get(String playerUrl) {
        return mEntries.get(playerUrl);
    }

    public void put(String playerUrl, T value) {
        mEntries.put(playerUrl, value);
    }

    public boolean isEmpty() {
        return mEntries.isEmpty();
    }
}

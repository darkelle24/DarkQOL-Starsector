package darkqol.utils;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;

public class SaveOneData<T> {
    private final String key;
    private final T defaultValue;

    public SaveOneData(String key, T defaultValue) {
        this.key = key;
        this.defaultValue = defaultValue;
    }

    public T getData() {
        MemoryAPI memory = Global.getSector().getMemoryWithoutUpdate();
        if (!memory.contains(key)) {
            memory.set(key, defaultValue);
        }
        return (T) memory.get(key); // Cast forcé mais sécurisé
    }

    public void setData(T value) {
        MemoryAPI memory = Global.getSector().getMemoryWithoutUpdate();
        memory.set(key, value);
    }
}

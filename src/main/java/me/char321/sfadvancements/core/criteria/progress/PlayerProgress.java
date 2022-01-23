package me.char321.sfadvancements.core.criteria.progress;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonWriter;
import me.char321.sfadvancements.SFAdvancements;
import me.char321.sfadvancements.api.Advancement;
import me.char321.sfadvancements.api.criteria.Criterion;
import me.char321.sfadvancements.util.Utils;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * a per-player object that stores their advancement progress <br>
 *
 * json <br>
 *
 * {@code
 * {
 *      "sfadvancements:hi" : {
 *          "done": false
 *          "criteria": {
 *              "eaj" : 1
 *              "ejfiow" : false
 *              "fioewjo" :
 *          }
 *      }
 * }
 * }
 *
 */
public class PlayerProgress {
    private final UUID player;
    private final Map<NamespacedKey, AdvancementProgress> progressMap = new HashMap<>();

    private PlayerProgress(UUID player) {
        this.player = player;
    }

    public static PlayerProgress get(Player player) {
        return get(player.getUniqueId());
    }

    public static PlayerProgress get(UUID player) {
        PlayerProgress res = new PlayerProgress(player);

        File f = new File("plugins/" + SFAdvancements.instance().getName() + "/advancements", player.toString()+".json");
        if(f.exists()) {
            try {
                JsonObject object = JsonParser.parseReader(new BufferedReader(new FileReader(f, StandardCharsets.UTF_8))).getAsJsonObject();
                res.loadFromObject(object);
            } catch (IOException e) {
                SFAdvancements.info("error reading file: " + e);
            }
        }
        return res;
    }

    public void doCriterion(Criterion cri) {
        NamespacedKey adv = cri.getAdvancement();
        if(!progressMap.containsKey(adv)) {
            progressMap.put(adv, new AdvancementProgress(adv));
        }

        AdvancementProgress advProgress = progressMap.get(adv);
        if(advProgress.done) {
            return;
        }

        for (CriteriaProgress progress : advProgress.criteria) {
            if(!progress.id.equals(cri.getId())) {
                continue;
            }

            if(progress.progress < cri.getCount()) {
                progress.progress++;
                if(progress.progress >= cri.getCount()) {
                    progress.done = true;
                    advProgress.updateDone();
                }
            }
        }
    }

    public boolean revokeAdvancement(NamespacedKey adv) {
        if(!progressMap.containsKey(adv)) {
            return false;
        }
        progressMap.get(adv).done = false;
        for (CriteriaProgress progress : progressMap.get(adv).criteria) {
            progress.progress = 0;
        }
        return true;
    }

    public List<NamespacedKey> getCompletedAdvancements() {
        List<NamespacedKey> res = new ArrayList<>();
        for (Map.Entry<NamespacedKey, AdvancementProgress> entry : progressMap.entrySet()) {
            if(entry.getValue().done) {
                res.add(entry.getKey());
            }
        }
        return res;
    }

    private void loadFromObject(JsonObject object) {
        for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
            NamespacedKey advkey = NamespacedKey.fromString(entry.getKey());
            AdvancementProgress newprogress = new AdvancementProgress(advkey);
            progressMap.put(advkey, newprogress);
            newprogress.loadFromObject(entry.getValue().getAsJsonObject());
        }
    }

    public void save() throws IOException {
        File f = new File("plugins/" + SFAdvancements.instance().getName() + "/advancements", player +".json");
        f.mkdirs();
        //this is probably bad
        f.delete();
        f.createNewFile();
        JsonWriter writer = new JsonWriter(new BufferedWriter(new FileWriter(f)));
        writer.beginObject();
        for (Map.Entry<NamespacedKey, AdvancementProgress> entry : progressMap.entrySet()) {
            writer.name(entry.getKey().toString());
            writer.beginObject();
            writer.name("done");
            writer.value(entry.getValue().done);
            writer.name("criteria");
            writer.beginObject();
            for (CriteriaProgress criterion : entry.getValue().criteria) {
                writer.name(criterion.id);
                writer.value(criterion.progress);
            }
            writer.endObject();
            writer.endObject();
        }
        writer.endObject();
        writer.close();
    }

    public boolean isCompleted(NamespacedKey key) {
        if(!progressMap.containsKey(key)) {
            return false;
        }
        AdvancementProgress prog = progressMap.get(key);
        return prog.done;
    }

    class AdvancementProgress {
        Advancement adv;
        boolean done = false;
        CriteriaProgress[] criteria;

        AdvancementProgress(NamespacedKey adv) {
            this(Utils.fromKey(adv));
        }

        AdvancementProgress(Advancement adv) {
            this.adv = adv;
            this.criteria = new CriteriaProgress[adv.getCriteria().length];
            for (int i = 0; i < adv.getCriteria().length; i++) {
                criteria[i] = new CriteriaProgress(adv.getCriteria()[i].getId());
            }
        }

        void updateDone() {
            for (CriteriaProgress criterion : criteria) {
                if(!criterion.done) {
                    return;
                }
            }
            this.done = true;
            adv.complete(Bukkit.getPlayer(player));
        }

        void loadFromObject(JsonObject object) {
            done = object.get("done").getAsBoolean();
            JsonObject jsonCriteria = object.get("criteria").getAsJsonObject();
            criteria = new CriteriaProgress[jsonCriteria.size()];
            int i = 0;
            for (Map.Entry<String, JsonElement> entry : jsonCriteria.entrySet()) {
                CriteriaProgress newProgress = new CriteriaProgress(entry.getKey(), entry.getValue().getAsInt());
                newProgress.done = entry.getValue().getAsInt() >= adv.getCriterion(entry.getKey()).getCount(); //no
                criteria[i] = newProgress;
                i++;
            }
        }
    }

    static class CriteriaProgress {
        String id;
        boolean done = false;
        //TODO make this easier to use so people can add their own criteria progress types like string
        int progress;

        CriteriaProgress(String id) {
            this(id, 0);
        }

        CriteriaProgress(String id, int progress) {
            this.id = id;
            this.progress = progress;
        }
    }
}
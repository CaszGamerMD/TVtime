package com.caszgamermd.caszualtvtime.camera;

import com.caszgamermd.caszualtvtime.CaszualTvTime;
import com.caszgamermd.caszualtvtime.block.CameraBlockEntity;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Persisted directory: offline cameras remain remotely selectable. Camera
 * blocks still store authoritative settings in their own block entities.
 */
public final class CameraSavedData extends SavedData {
    private static final Codec<CameraSavedData> CODEC = Codec.STRING.listOf()
        .xmap(CameraSavedData::new, CameraSavedData::serialize);
    private static final SavedDataType<CameraSavedData> TYPE = new SavedDataType<>(
        CaszualTvTime.id("camera_registry"), CameraSavedData::new, CODEC, null);

    private final Map<BlockPos, Entry> entries = new LinkedHashMap<>();

    public CameraSavedData() {}

    private CameraSavedData(List<String> raw) {
        for (String line : raw) {
            try {
                String[] v = line.split(";", -1);
                if (v.length != 7) continue;
                BlockPos pos = BlockPos.of(Long.parseLong(v[0]));
                Entry e = new Entry(pos, decode(v[1]), decode(v[2]),
                    Boolean.parseBoolean(v[3]), Float.parseFloat(v[4]),
                    Float.parseFloat(v[5]), Float.parseFloat(v[6]));
                entries.put(pos, e);
            } catch (RuntimeException ignored) {
                // Discard damaged directory entries, not the world.
            }
        }
    }

    private static String encode(String string) {
        return Base64.getUrlEncoder().withoutPadding()
            .encodeToString(string.getBytes(StandardCharsets.UTF_8));
    }

    private static String decode(String string) {
        return new String(Base64.getUrlDecoder().decode(string), StandardCharsets.UTF_8);
    }

    private List<String> serialize() {
        List<String> output = new ArrayList<>();
        for (Entry e : entries.values()) {
            output.add(e.pos().asLong() + ";" + encode(e.name()) + ";" + encode(e.channel())
                + ";" + e.active() + ";" + e.pan() + ";" + e.tilt() + ";" + e.zoom());
        }
        return output;
    }

    public static CameraSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(TYPE);
    }

    public List<Entry> entries() { return List.copyOf(entries.values()); }
    public boolean contains(BlockPos pos) { return entries.containsKey(pos); }
    public Entry entry(BlockPos pos) { return entries.get(pos); }

    public void put(CameraBlockEntity cam) {
        Entry e = new Entry(cam.getBlockPos().immutable(), cam.cameraName(),
            cam.channel(), cam.active(), cam.pan(), cam.tilt(), cam.zoom());
        if (!e.equals(entries.put(e.pos(), e))) setDirty();
    }

    public void remove(BlockPos pos) {
        if (entries.remove(pos) != null) setDirty();
    }

    public record Entry(BlockPos pos, String name, String channel,
                        boolean active, float pan, float tilt, float zoom) {}
}

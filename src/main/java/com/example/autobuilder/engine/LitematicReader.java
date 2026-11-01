package com.example.autobuilder.engine;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;

public final class LitematicReader {

    private LitematicReader() {}

    public static SchematicData read(Path file) throws IOException {
        try (DataInputStream in = new DataInputStream(
                new BufferedInputStream(
                        new GZIPInputStream(Files.newInputStream(file))))) {

            if (in.readUnsignedByte() != 10) {
                throw new IOException("Invalid Litematic NBT root");
            }

            in.readUTF();
            Object rootObject = readPayload(in, 10);

            if (!(rootObject instanceof Map<?, ?> root)) {
                throw new IOException("Invalid Litematic root compound");
            }

            Object regionsObject = root.get("Regions");
            if (!(regionsObject instanceof Map<?, ?> regions)
                    || regions.isEmpty()) {
                throw new IOException("No regions found in Litematic");
            }

            List<SchematicBlock> allBlocks = new ArrayList<>();

            int minX = Integer.MAX_VALUE;
            int minY = Integer.MAX_VALUE;
            int minZ = Integer.MAX_VALUE;
            int maxX = Integer.MIN_VALUE;
            int maxY = Integer.MIN_VALUE;
            int maxZ = Integer.MIN_VALUE;

            for (Object regionObject : regions.values()) {
                if (!(regionObject instanceof Map<?, ?> region)) {
                    continue;
                }

                Map<?, ?> size = requireCompound(region.get("Size"), "Size");
                Map<?, ?> position = requireCompound(region.get("Position"), "Position");

                int sx = Math.abs(number(size.get("x"), "Size.x"));
                int sy = Math.abs(number(size.get("y"), "Size.y"));
                int sz = Math.abs(number(size.get("z"), "Size.z"));

                int px = number(position.get("x"), "Position.x");
                int py = number(position.get("y"), "Position.y");
                int pz = number(position.get("z"), "Position.z");

                if (sx == 0 || sy == 0 || sz == 0) {
                    continue;
                }

                Object paletteObject = region.get("BlockStatePalette");
                if (!(paletteObject instanceof List<?> paletteEntries)) {
                    throw new IOException("Invalid BlockStatePalette list");
                }

                Object statesObject = region.get("BlockStates");
                if (!(statesObject instanceof long[] states)) {
                    throw new IOException("Missing BlockStates");
                }

                List<String> paletteIds = new ArrayList<>();
                for (Object entryObject : paletteEntries) {
                    if (!(entryObject instanceof Map<?, ?> entry)
                            || !(entry.get("Name") instanceof String name)) {
                        throw new IOException("Invalid block palette entry");
                    }
                    paletteIds.add(name);
                }

                if (paletteIds.isEmpty()) {
                    continue;
                }

                int bits = Math.max(2,
                        32 - Integer.numberOfLeadingZeros(paletteIds.size() - 1));

                long mask = (1L << bits) - 1L;
                long total = (long) sx * sy * sz;

                if (total > 20_000_000L) {
                    throw new IOException("Region is too large to load safely");
                }

                long expectedWords = (total * bits + 63) / 64;
                if (states.length < expectedWords) {
                    throw new IOException("BlockStates data is incomplete");
                }

                for (long index = 0; index < total; index++) {
                    long bitIndex = index * bits;
                    int wordIndex = (int) (bitIndex >>> 6);
                    int bitOffset = (int) (bitIndex & 63);

                    long value = states[wordIndex] >>> bitOffset;

                    if (bitOffset + bits > 64) {
                        value |= states[wordIndex + 1] << (64 - bitOffset);
                    }

                    int paletteIndex = (int) (value & mask);

                    if (paletteIndex >= paletteIds.size()) {
                        throw new IOException("Invalid palette index");
                    }

                    String blockId = paletteIds.get(paletteIndex);

                    if ("minecraft:air".equals(blockId)
                            || "minecraft:cave_air".equals(blockId)
                            || "minecraft:void_air".equals(blockId)) {
                        continue;
                    }

                    int localX = (int) (index % sx);
                    int localZ = (int) ((index / sx) % sz);
                    int localY = (int) (index / ((long) sx * sz));

                    int worldX = px + (size.get("x") instanceof Number n
                            && n.intValue() < 0 ? -localX : localX);
                    int worldY = py + (size.get("y") instanceof Number n
                            && n.intValue() < 0 ? -localY : localY);
                    int worldZ = pz + (size.get("z") instanceof Number n
                            && n.intValue() < 0 ? -localZ : localZ);

                    allBlocks.add(new SchematicBlock(
                            blockId, worldX, worldY, worldZ));

                    minX = Math.min(minX, worldX);
                    minY = Math.min(minY, worldY);
                    minZ = Math.min(minZ, worldZ);
                    maxX = Math.max(maxX, worldX);
                    maxY = Math.max(maxY, worldY);
                    maxZ = Math.max(maxZ, worldZ);
                }
            }

            if (allBlocks.isEmpty()) {
                throw new IOException("No non-air blocks found");
            }

            List<SchematicBlock> normalized = new ArrayList<>(allBlocks.size());

            for (SchematicBlock block : allBlocks) {
                normalized.add(new SchematicBlock(
                        block.blockId(),
                        block.x() - minX,
                        block.y() - minY,
                        block.z() - minZ
                ));
            }

            return new SchematicData(
                    file.getFileName().toString(),
                    maxX - minX + 1,
                    maxY - minY + 1,
                    maxZ - minZ + 1,
                    normalized
            );
        }
    }

    private static Map<?, ?> requireCompound(Object value, String name)
            throws IOException {
        if (value instanceof Map<?, ?> map) {
            return map;
        }
        throw new IOException("Missing or invalid " + name);
    }

    private static int number(Object value, String name) throws IOException {
        if (value instanceof Number number) {
            return number.intValue();
        }
        throw new IOException("Missing or invalid " + name);
    }

    private static Object readPayload(DataInputStream in, int type)
            throws IOException {
        return switch (type) {
            case 1 -> in.readByte();
            case 2 -> in.readShort();
            case 3 -> in.readInt();
            case 4 -> in.readLong();
            case 5 -> in.readFloat();
            case 6 -> in.readDouble();
            case 7 -> {
                int length = checkedLength(in.readInt());
                byte[] data = new byte[length];
                in.readFully(data);
                yield data;
            }
            case 8 -> in.readUTF();
            case 9 -> {
                int childType = in.readUnsignedByte();
                int length = checkedLength(in.readInt());
                List<Object> list = new ArrayList<>(length);
                for (int i = 0; i < length; i++) {
                    list.add(readPayload(in, childType));
                }
                yield list;
            }
            case 10 -> {
                Map<String, Object> map = new java.util.LinkedHashMap<>();
                while (true) {
                    int childType = in.readUnsignedByte();
                    if (childType == 0) {
                        break;
                    }
                    String name = in.readUTF();
                    map.put(name, readPayload(in, childType));
                }
                yield map;
            }
            case 11 -> {
                int length = checkedLength(in.readInt());
                int[] data = new int[length];
                for (int i = 0; i < length; i++) {
                    data[i] = in.readInt();
                }
                yield data;
            }
            case 12 -> {
                int length = checkedLength(in.readInt());
                long[] data = new long[length];
                for (int i = 0; i < length; i++) {
                    data[i] = in.readLong();
                }
                yield data;
            }
            default -> throw new IOException("Unsupported NBT tag: " + type);
        };
    }

    private static int checkedLength(int length) throws IOException {
        if (length < 0 || length > 20_000_000) {
            throw new IOException("Invalid NBT collection length: " + length);
        }
        return length;
    }
}

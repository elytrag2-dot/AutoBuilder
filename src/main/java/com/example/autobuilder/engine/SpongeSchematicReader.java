package com.example.autobuilder.engine;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.zip.GZIPInputStream;

public class SpongeSchematicReader {

    private DataInputStream input;

    public SchematicData read(Path path) throws IOException {
        try (DataInputStream stream = new DataInputStream(
                new BufferedInputStream(
                        new GZIPInputStream(Files.newInputStream(path))))) {

            input = stream;

            int rootType = input.readUnsignedByte();
            if (rootType != 10) {
                throw new IOException("Invalid NBT root compound");
            }

            input.readUTF();

            Map<String, Object> root = readCompound();

            int width = number(root, "Width");
            int height = number(root, "Height");
            int length = number(root, "Length");

            if (width <= 0 || height <= 0 || length <= 0) {
                throw new IOException("Invalid schematic dimensions");
            }

            Object blocksObject = root.get("Blocks");
            Map<?, ?> blocksTag = blocksObject instanceof Map<?, ?> map
                    ? map : root;

            Object paletteObject = blocksTag.get("Palette");
            Object dataObject = blocksTag.containsKey("Data")
                    ? blocksTag.get("Data") : root.get("BlockData");

            if (!(paletteObject instanceof Map<?, ?> palette)
                    || !(dataObject instanceof byte[] blockData)) {
                throw new IOException("Missing Blocks.Palette or Blocks.Data");
            }

            Map<Integer, String> reversePalette = new HashMap<>();

            for (Map.Entry<?, ?> entry : palette.entrySet()) {
                if (entry.getKey() instanceof String name
                        && entry.getValue() instanceof Number id) {
                    reversePalette.put(id.intValue(), name);
                }
            }

            List<SchematicBlock> blocks = new ArrayList<>();

            int total = Math.multiplyExact(
                    Math.multiplyExact(width, height), length);

            int index = 0;
            int dataIndex = 0;

            while (index < total) {
                int paletteId = 0;
                int shift = 0;

                while (true) {
                    if (dataIndex >= blockData.length || shift >= 35) {
                        throw new IOException("Invalid BlockData varint");
                    }

                    int value = blockData[dataIndex++] & 0xFF;
                    paletteId |= (value & 0x7F) << shift;

                    if ((value & 0x80) == 0) {
                        break;
                    }

                    shift += 7;
                }

                String blockId = reversePalette.get(paletteId);

                if (blockId == null) {
                    throw new IOException("Unknown palette ID: " + paletteId);
                }

                if (!blockId.equals("minecraft:air")
                        && !blockId.equals("minecraft:cave_air")
                        && !blockId.equals("minecraft:void_air")) {

                    int x = index % width;
                    int z = (index / width) % length;
                    int y = index / (width * length);

                    blocks.add(new SchematicBlock(blockId, x, y, z));
                }

                index++;
            }

            return new SchematicData(
                    path.getFileName().toString(),
                    width,
                    height,
                    length,
                    blocks
            );
        }
    }

    private int number(Map<String, Object> map, String key)
            throws IOException {
        Object value = map.get(key);

        if (value instanceof Number number) {
            return number.intValue();
        }

        throw new IOException("Missing numeric NBT field: " + key);
    }

    private Map<String, Object> readCompound() throws IOException {
        Map<String, Object> result = new LinkedHashMap<>();

        while (true) {
            int type = input.readUnsignedByte();

            if (type == 0) {
                break;
            }

            String name = input.readUTF();
            result.put(name, readPayload(type));
        }

        return result;
    }

    private Object readPayload(int type) throws IOException {
        return switch (type) {
            case 1 -> input.readByte();
            case 2 -> input.readShort();
            case 3 -> input.readInt();
            case 4 -> input.readLong();
            case 5 -> input.readFloat();
            case 6 -> input.readDouble();

            case 7 -> {
                int size = input.readInt();
                checkSize(size);
                yield input.readNBytes(size);
            }

            case 8 -> input.readUTF();

            case 9 -> {
                int childType = input.readUnsignedByte();
                int size = input.readInt();
                checkSize(size);

                List<Object> list = new ArrayList<>(size);

                for (int i = 0; i < size; i++) {
                    list.add(readPayload(childType));
                }

                yield list;
            }

            case 10 -> readCompound();

            case 11 -> {
                int size = input.readInt();
                checkSize(size);

                int[] array = new int[size];

                for (int i = 0; i < size; i++) {
                    array[i] = input.readInt();
                }

                yield array;
            }

            case 12 -> {
                int size = input.readInt();
                checkSize(size);

                long[] array = new long[size];

                for (int i = 0; i < size; i++) {
                    array[i] = input.readLong();
                }

                yield array;
            }

            default -> throw new IOException("Unknown NBT tag: " + type);
        };
    }

    private void checkSize(int size) throws IOException {
        if (size < 0 || size > 16_000_000) {
            throw new IOException("Invalid NBT collection size: " + size);
        }
    }
}

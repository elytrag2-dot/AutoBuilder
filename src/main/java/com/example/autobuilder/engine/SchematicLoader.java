package com.example.autobuilder.engine;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Locale;

public final class SchematicLoader {

    private SchematicLoader() {
    }

    public static SchematicData load(Path path) throws IOException {
        if (path == null || !java.nio.file.Files.isRegularFile(path)) {
            throw new IOException("Schematic file not found");
        }

        String fileName = path.getFileName()
                .toString()
                .toLowerCase(Locale.ROOT);

        if (fileName.endsWith(".litematic")) {
            return LitematicReader.read(path);
        }

        if (fileName.endsWith(".schem")) {
            return new SpongeSchematicReader().read(path);
        }

        throw new IOException(
                "Unsupported format. Use .litematic or .schem"
        );
    }
}
